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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.shulie.data.DynamicContext;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testbeans.gui.GenericTestBeanCustomizer;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.DoubleProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.timers.ConstantThroughputTimer;
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
    private Double dynamicThroughput;
    private Object lock = new Object();

    @Override
    public Object clone() {
        final PreciseThroughputTimer newTimer = (PreciseThroughputTimer) super.clone();
        newTimer.testStarted = testStarted; // JMeter cloning does not clone fields
        newTimer.dynamicThroughput = dynamicThroughput;
        log.info("clone!!dynamicThroughput="+dynamicThroughput+", testStarted="+testStarted);
        return newTimer;
    }

    @Override
    public void testStarted() {
        testStarted(null);
    }

    @Override
    public void testStarted(String host) {
        groupEvents.clear();
        testStarted = System.currentTimeMillis();
    }

    @Override
    public void testEnded() {
        // NOOP
    }

    @Override
    public void testEnded(String s) {
        // NOOP
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
        String threadGroupTestName = JMeterContextService.getContext().getThreadGroup().getName();
        Double dynamicTps = DynamicContext.getTpsTargetLevel(threadGroupTestName);
        if (null != dynamicTps && dynamicTps > 0 && !valuesAreEqualWithAb(dynamicTps, dynamicThroughput)) {
            log.info("1 --> dynamicThroughput=" + dynamicThroughput+", dynamicTps="+dynamicTps+", valuesAreEqualWithAb="+valuesAreEqualWithAb(dynamicTps, dynamicThroughput)+", this="+this);
            synchronized (lock) {
                if (!valuesAreEqualWithAb(dynamicTps, dynamicThroughput)) {
                    log.info("2 --> dynamicThroughput=" + dynamicThroughput+", dynamicTps="+dynamicTps+", valuesAreEqualWithAb="+valuesAreEqualWithAb(dynamicTps, dynamicThroughput));
                    testStarted = System.currentTimeMillis();
                    throughput = dynamicTps;//修改无效
                    dynamicThroughput = dynamicTps;
                    groupEvents.clear();
                    log.info("3 --> dynamicThroughput=" + dynamicThroughput+", dynamicTps="+dynamicTps+", valuesAreEqualWithAb="+valuesAreEqualWithAb(dynamicTps, dynamicThroughput));
                    JMeterProperty property = this.getProperty("throughput");
                    property.setObjectValue(throughput);
                    property.setRunningVersion(false);
                    this.setProperty(property);
                }
            }
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
        long delay = (long) (nextEvent * TimeUnit.SECONDS.toMillis(1) + testStarted - now);
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
        log.info("delay="+delay+", nextEvent="+nextEvent+", time="+(now - testStarted));
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

    private AtomicInteger INITIALIZED = new AtomicInteger(0);

    /**
     * Returns number of generated samples per {@link #getThroughputPeriod}
     * @return number of samples per {@link #getThroughputPeriod}
     */
    public double getThroughput() {
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
