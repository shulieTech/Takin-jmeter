/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.timers.poissonarrivals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.shulie.DynamicContext;
import org.apache.jmeter.shulie.DynamicContextByLongPolling;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.timers.Timer;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apiguardian.api.API;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This timer generates Poisson arrivals with constant throughput.
 * On top of that, it tries to maintain the exact amount of arrivals for a given timeframe ({@link #throughputPeriod}).
 * @since 4.0
 */
@GUIMenuSortOrder(3)
@TestElementMetadata(labelResource = "displayName")
public class PreciseThroughputTimer extends AbstractTestElement implements Cloneable, Timer, TestStateListener, TestBean, ThroughputProvider, DurationProvider {
    private static final Logger log = LoggerFactory.getLogger(PreciseThroughputTimer.class);

    private static final long serialVersionUID = 4;
    private static final ConcurrentMap<AbstractThreadGroup, EventProducer> groupEvents = new ConcurrentHashMap<>();
    private static final double PRECISION = 0.00001;

    /**
     * Desired throughput configured as {@code throughput/throughputPeriod} per second.
     */
    private double throughput;
    private int throughputPeriod;

    /**
     * This is used to ensure you'll get {@code duration*throughput/throughputPeriod} samples during "test duration" timeframe.
     * Even though arrivals are random, business users want to see round numbers in reports like "100 samples per hour",
     * so the timer picks only those random arrivals that end up with round total numbers.
     */
    private long duration;

    private long testStarted;

    /**
     * When number of required samples exceeds {@code exactLimit}, random generator would resort to approximate match of
     * number of generated samples.
     */
    private int exactLimit;
    private double allowedThroughputSurplus;

    /**
     * This enables to reproduce exactly the same sequence of delays by reusing the same seed.
     */
    private Long randomSeed;

    /**
     * This enables to generate events in batches (e.g. pairs of events with {@link #batchThreadDelay} sec in between)
     * TODO: this should be either rewritten to double / ms, or dropped in favour of other approach
     */
    private int batchSize;
    private int batchThreadDelay;

    /**
     * tps目标乘积因子（percent*(1+tpsTargetLevelFactor)）
     * percent：百分比，总目标的百分比，这里指活动占总目标的百分比
     * tpsTargetLevelFactor：上浮因子，该值在cloud的配置项中配置：jmx.script.tpsTargetLevelFactor
     * 该值在pressure-engine中计算生成
     */
    private double tpsFactor = 0d;
    /**
     * 东条调整tps（这里修改throughput的值无效，在TestBeanHelper96行会重新将旧值赋值进来）
     */
    private static final ConcurrentMap<AbstractThreadGroup, Double> dynamicThroughputMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<AbstractThreadGroup, Long> testStartedMap = new ConcurrentHashMap<>();

    @Override
    public Object clone() {
        final PreciseThroughputTimer newTimer = (PreciseThroughputTimer) super.clone();
        newTimer.testStarted = testStarted; // JMeter cloning does not clone fields
        return newTimer;
    }

    @Override
    public void testStarted() {
        testStarted(null);
    }

    @Override
    public void testStarted(String host) {
        testStartedMap.clear();
        dynamicThroughputMap.clear();
        groupEvents.clear();
        testStarted = System.currentTimeMillis();
    }

    @Override
    public void testEnded() {
        // NOOP
        testStartedMap.clear();
        dynamicThroughputMap.clear();
    }

    @Override
    public void testEnded(String s) {
        // NOOP
        testStartedMap.clear();
        dynamicThroughputMap.clear();
    }

    private boolean valuesAreEqualWithAb(Double a, Double b) {
        if (null == a && null == b) {
            return true;
        } else if (null == a || null == b) {
            return false;
        }
        return Math.abs(a - b) < PRECISION;
    }

    private void resetThroughput() {
        AbstractThreadGroup tg = getThreadContext().getThreadGroup();
        if (null == tg) {
            return;
        }
        String threadGroupTestName = tg.getName();
        Double dynamicTps = DynamicContextByLongPolling.getTpsTargetLevel(threadGroupTestName);
        if (null == dynamicTps || dynamicTps <= 0) {
            return;
        }
        Double dynamicThroughput = dynamicThroughputMap.get(tg);
        if (valuesAreEqualWithAb(dynamicTps, dynamicThroughput)) {
            return;
        }
        synchronized (dynamicThroughputMap) {
            if (valuesAreEqualWithAb(dynamicTps, dynamicThroughput)) {
                return;
            }
            testStarted = System.currentTimeMillis();
            dynamicThroughputMap.put(tg, dynamicTps);
            testStartedMap.put(tg, testStarted);
            groupEvents.clear();
        }
    }

    @Override
    public long delay() {
        resetThroughput();
        double nextEvent;
        EventProducer events = getEventProducer();
        synchronized (events) {
            nextEvent = events.next();
        }
        long now = System.currentTimeMillis();
        long delay = (long) (nextEvent * TimeUnit.SECONDS.toMillis(1) + getTestStarted() - now);
        if (log.isDebugEnabled()) {
            log.debug("Calculated delay is {}", delay);
        }
        delay = Math.max(0, delay);
        long endTime = getThreadContext().getThread().getEndTime();
        if (endTime > 0 && now + delay > endTime) {
            throw new JMeterStopThreadException("The thread is scheduled to stop in " +
                    (endTime - now) + " ms" +
                    " and the throughput timer generates a delay of " + delay + "." +
                    " Terminating the thread manually."
            );
        }
//        log.info("delay="+delay+", nextEvent="+nextEvent+", now="+getTestStarted()+", testStarted="+testStarted+", time="+(now - getTestStarted())+", throughput="+getThroughput());
        return delay;
    }

    private EventProducer getEventProducer() {
        AbstractThreadGroup tg = getThreadContext().getThreadGroup();
        Long seed = randomSeed == null || randomSeed == 0 ? null : randomSeed;
        return
                groupEvents.computeIfAbsent(tg, x -> new ConstantPoissonProcessGenerator(
                        () -> PreciseThroughputTimer.this.getThroughput() / throughputPeriod,
                        batchSize, batchThreadDelay, this, seed, true));
    }

    public long getTestStarted() {
        AbstractThreadGroup tg = getThreadContext().getThreadGroup();
        Long dynamicTime = testStartedMap.get(tg);
        if (null != dynamicTime && dynamicTime > 0) {
            return dynamicTime;
        }
        return testStarted;
    }

    /**
     * Returns number of generated samples per {@link #getThroughputPeriod}
     * @return number of samples per {@link #getThroughputPeriod}
     */
    public double getThroughput() {
        AbstractThreadGroup tg = getThreadContext().getThreadGroup();
        Double dynamicThroughput = dynamicThroughputMap.get(tg);
        if (null != dynamicThroughput && dynamicThroughput > 0) {
            return dynamicThroughput;
        }
        return throughput;
    }


    /**
     * Sets number of generated samples per {@link #getThroughputPeriod}
     * @param throughput number of samples per {@link #getThroughputPeriod}
     */
    public void setThroughput(double throughput) {
        this.throughput = throughput;
    }

    public double getTpsFactor() {
        if (null != DynamicContext.TPS_FACTOR) {
            return DynamicContext.TPS_FACTOR;
        }
        return tpsFactor;
    }

    public void setTpsFactor(double tpsFactor) {
        this.tpsFactor = tpsFactor;
    }

    /**
     * Allows to use business values for throughput configuration.
     * For instance, 100 samples per hour vs 100 samples per minute.
     * @return the length of the throughput period in seconds
     */
    public int getThroughputPeriod() {
        return throughputPeriod;
    }

    public void setThroughputPeriod(int throughputPeriod) {
        this.throughputPeriod = throughputPeriod;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Deprecated
    @API(status = API.Status.DEPRECATED, since = "5.3.0")
    public int getExactLimit() {
        return exactLimit;
    }

    @Deprecated
    @API(status = API.Status.DEPRECATED, since = "5.3.0")
    public void setExactLimit(int exactLimit) {
        this.exactLimit = exactLimit;
    }

    @Deprecated
    @API(status = API.Status.DEPRECATED, since = "5.3.0")
    public double getAllowedThroughputSurplus() {
        return allowedThroughputSurplus;
    }

    @Deprecated
    @API(status = API.Status.DEPRECATED, since = "5.3.0")
    public void setAllowedThroughputSurplus(double allowedThroughputSurplus) {
        this.allowedThroughputSurplus = allowedThroughputSurplus;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getBatchThreadDelay() {
        return batchThreadDelay;
    }

    public void setBatchThreadDelay(int batchThreadDelay) {
        this.batchThreadDelay = batchThreadDelay;
    }

}
