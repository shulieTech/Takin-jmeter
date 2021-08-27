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

package org.apache.jmeter.control;

import java.io.Serializable;

import io.shulie.jmeter.tool.redis.RedisConfig;

import io.shulie.jmeter.tool.redis.RedisUtil;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.shulie.consts.ThroughputConstants;
import org.apache.jmeter.shulie.util.DesUtil;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.*;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

/**
 * This class represents a controller that can control the number of times that
 * it is executed, either by the total number of times the user wants the
 * controller executed (BYNUMBER) or by the percentage of time it is called
 * (BYPERCENT)
 * <p>
 * The current implementation executes the first N samples (BYNUMBER)
 * or the last N% of samples (BYPERCENT).
 */
public class ThroughputController
        extends GenericController
        implements Serializable, LoopIterationListener, TestStateListener {

    private static final long serialVersionUID = 234L;

    private static final Logger log = LoggerFactory.getLogger(ThroughputController.class);

    public static final int BYNUMBER = 0;
    public static final int BYPERCENT = 1;

    private static final String STYLE = "ThroughputController.style";// $NON-NLS-1$
    private static final String PERTHREAD = "ThroughputController.perThread";// $NON-NLS-1$
    private static final String MAXTHROUGHPUT = "ThroughputController.maxThroughput";// $NON-NLS-1$
    private static final String PERCENTTHROUGHPUT = "ThroughputController.percentThroughput";// $NON-NLS-1$

    private static class MutableInteger {
        private int integer;

        MutableInteger(int value) {
            integer = value;
        }

        int incr() {
            return ++integer;
        }

        int intValue() {
            return integer;
        }
    }

    // These items are shared between threads in a group by the clone() method
    // They are initialised by testStarted() so don't need to be serialised
    private transient MutableInteger globalNumExecutions;
    private transient MutableInteger globalIteration;
    private transient Object counterLock = new Object(); // ensure counts are updated correctly

    /**
     * Number of iterations on which we've chosen to deliver samplers.
     */
    private int numExecutions = 0;

    /**
     * Index of the current iteration. 0-based.
     */
    private int iteration = -1;

    /**
     * Whether to deliver samplers on this iteration.
     */
    private boolean runThisTime;

    //redis 吞吐量百分比的key
    private String redisThroughputPercentageKey;

    public ThroughputController() {
        setStyle(BYNUMBER);
        setPerThread(true);
        setMaxThroughput(1);
        setPercentThroughput(100);
        runThisTime = false;

        //FIXME 之后要改为hget
        //TPS模式需要初始化Redis
//        JMeterVariables variables = JMeterContextService.getContext().getVariables();
//        if (variables != null) {
//            //tps模式需要初始化redis add by lipeng
//            synchronized (this) {
//                //只初始化一次
//                if (ThroughputConstants.jedisClient == null) {
//                    //是否TPS模式
//                    ThroughputConstants.IS_TPS_MODE = ThroughputConstants
//                            .ENGINE_PRESSURE_MODE_TPS_VALUE.equals(variables.get("__ENGINE_PRESSURE_MODE__"));
//                    //tps模式需要初始化redis
//                    if (ThroughputConstants.IS_TPS_MODE) {
//                        String sceneId = variables.get("__ENGINE_SCENE_ID__");
//                        String reportId = variables.get("__ENGINE_REPORT_ID__");
//                        String customerId = variables.get("__ENGINE_CUSTOMER_ID__");
//                        redisThroughputPercentageKey = String.format(ThroughputConstants.REDIS_ACTIVITY_PERCENTAGE_KEY_FORMAT
//                                , sceneId, reportId, customerId, this.getName());
//                        //初始化redis
//                        this.initRedis(variables);
//                    }
//                }
//            }
//        } else {
//            log.warn("全局用户参数为空");
//        }
    }

    public void setStyle(int style) {
        setProperty(new IntegerProperty(STYLE, style));
    }

    public int getStyle() {
        return getPropertyAsInt(STYLE);
    }

    public void setPerThread(boolean perThread) {
        setProperty(new BooleanProperty(PERTHREAD, perThread));
    }

    public boolean isPerThread() {
        return getPropertyAsBoolean(PERTHREAD);
    }

    public void setMaxThroughput(int maxThroughput) {
        setProperty(new IntegerProperty(MAXTHROUGHPUT, maxThroughput));
    }

    public void setMaxThroughput(String maxThroughput) {
        setProperty(new StringProperty(MAXTHROUGHPUT, maxThroughput));
    }

    public String getMaxThroughput() {
        return getPropertyAsString(MAXTHROUGHPUT);
    }

    protected int getMaxThroughputAsInt() {
        JMeterProperty prop = getProperty(MAXTHROUGHPUT);
        int retVal = 1;
        if (prop instanceof IntegerProperty) {
            retVal = prop.getIntValue();
        } else {
            String valueString = prop.getStringValue();
            try {
                retVal = Integer.parseInt(valueString);
            } catch (NumberFormatException e) {
                log.warn("Error parsing '{}'", valueString, e);
            }
        }
        return retVal;
    }

    public void setPercentThroughput(float percentThroughput) {
        setProperty(new FloatProperty(PERCENTTHROUGHPUT, percentThroughput));
    }

    public void setPercentThroughput(String percentThroughput) {
        setProperty(new StringProperty(PERCENTTHROUGHPUT, percentThroughput));
    }

    public String getPercentThroughput() {
        return getPropertyAsString(PERCENTTHROUGHPUT);
    }

    // 这里PercentThroughput是每个吞吐量控制器的百分比
    // 如果需要根据每个业务活动单独调整TPS，需要修改这里
    //  我们可以在redis中将每个吞吐量控制器使用 __REDIS_THROUGHPUT_TPS_LIMIT_KEY_%s_%s_%s_%s_
    //  以上4个参数分别为sceneId, reportId, customerId, testname(这里是吞吐器的，规则是sample的testname+"-tc")
    protected float getPercentThroughputAsFloat() {
        //tps模式需要从redis获取percent
//        if (ThroughputConstants.IS_TPS_MODE) {
//            float result = 0F;
//            try {
//                result = Float.parseFloat(ThroughputConstants.jedisClient.get(redisThroughputPercentageKey));
//            } catch (NumberFormatException e) {
//                log.error("redis key '{}' is not a number", redisThroughputPercentageKey);
//                System.exit(-1);
//            }
//            log.debug("redis change tps percentThroughput to {}", result);
//            return result;
//        }

        JMeterProperty prop = getProperty(PERCENTTHROUGHPUT);
        float retVal = 100;
        if (prop instanceof FloatProperty) {
            retVal = prop.getFloatValue();
        } else {
            String valueString = prop.getStringValue();
            try {
                retVal = Float.parseFloat(valueString);
            } catch (NumberFormatException e) {
                log.warn("Error parsing '{}'", valueString, e);
            }
        }
        return retVal;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    private int getExecutions() {
        if (!isPerThread()) {
            synchronized (counterLock) {
                return globalNumExecutions.intValue();
            }
        }
        return numExecutions;
    }

    @Override
    public Sampler next() {
        if (runThisTime) {
            return super.next();
        }
        return null;
    }

    /**
     * Decide whether to return any samplers on this iteration.
     */
    private boolean decide(int executions, int iterations) {
        if (getStyle() == BYNUMBER) {
            return executions < getMaxThroughputAsInt();
        }
        return (100.0 * executions + 50.0) / (iterations + 1) < getPercentThroughputAsFloat();
    }

    @Override
    public boolean isDone() {
        return subControllersAndSamplers.isEmpty()
                ||
                (
                        (getStyle() == BYNUMBER
                                && (
                                (getExecutions() >= getMaxThroughputAsInt()
                                        && current >= getSubControllers().size())
                                        || (getMaxThroughputAsInt() == 0)))
                                || (getStyle() == BYPERCENT
                                && Float.compare(getPercentThroughputAsFloat(), 0.0f) == 0)
                );
    }

    @Override
    public Object clone() {
        ThroughputController clone = (ThroughputController) super.clone();
        clone.numExecutions = numExecutions;
        clone.iteration = iteration;
        clone.runThisTime = false;
        // Ensure global counters and lock are shared across threads in the group
        clone.globalIteration = globalIteration;
        clone.globalNumExecutions = globalNumExecutions;
        clone.counterLock = counterLock;
        return clone;
    }

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void iterationStart(LoopIterationEvent iterEvent) {
        if (!isPerThread()) {
            synchronized (counterLock) {
                globalIteration.incr();
                runThisTime = decide(globalNumExecutions.intValue(), globalIteration.intValue());
                if (runThisTime) {
                    globalNumExecutions.incr();
                }
            }
        } else {
            iteration++;
            runThisTime = decide(numExecutions, iteration);
            if (runThisTime) {
                numExecutions++;
            }
        }
    }

    @Override
    @SuppressWarnings("SynchronizeOnNonFinalField")
    public void testStarted() {
        synchronized (counterLock) {
            globalNumExecutions = new MutableInteger(0);
            globalIteration = new MutableInteger(-1);
        }
    }

    @Override
    public void testStarted(String host) {
        testStarted();
    }

    @Override
    public void testEnded() {
        // NOOP
    }

    @Override
    public void testEnded(String host) {
        // NOOP
    }

    @Override
    protected Object readResolve() {
        super.readResolve();
        counterLock = new Object();
        return this;
    }

    // add by lipeng 支持 redis
    /**
     * 初始化Redis
     * <p>
     * 是否有必要使用连接池？
     *
     * @author lipeng
     */
    private void initRedis(JMeterVariables variables) {
        if (null != ThroughputConstants.redisUtil){
            return;
        }
        String engineRedisAddress = System.getProperty("engineRedisAddress");
        String engineRedisPort = System.getProperty("engineRedisPort");
        String engineRedisSentinelNodes = System.getProperty("engineRedisSentinelNodes");
        String engineRedisSentinelMaster = System.getProperty("engineRedisSentinelMaster");
        String engineRedisPassword = System.getProperty("engineRedisPassword");
        log.info("redis start..");
        // 解密redis密码
        try {
            RedisConfig redisConfig = new RedisConfig();
            redisConfig.setNodes(engineRedisSentinelNodes);
            redisConfig.setMaster(engineRedisSentinelMaster);
            redisConfig.setHost(engineRedisAddress);
            redisConfig.setPort(Integer.parseInt(engineRedisPort));
            redisConfig.setPassword(engineRedisPassword);
            redisConfig.setMaxIdle(1);
            redisConfig.setMaxTotal(1);
            redisConfig.setTimeout(3000);
            ThroughputConstants.redisUtil = RedisUtil.getInstance(redisConfig);
        } catch (Exception e) {
            log.error("Redis 连接失败，redisAddress is {}， redisPort is {}， encryptRedisPassword is {},engineRedisSentinelNodes is {}," +
                            "engineRedisSentinelMaster is {}"
                    , engineRedisAddress, engineRedisPort, engineRedisPassword,engineRedisSentinelNodes,engineRedisSentinelMaster);
            log.error("失败详细错误栈：", e);
            System.exit(-1);
        }
        log.info("redis inited..");
    }

    /**
     * 关闭redis客户端
     */
//    private void closeRedis() {
//        try {
//            if (ThroughputConstants.redisUtil != null) {
//                ThroughputConstants.redisUtil.close();
//            }
//        } catch (Exception e) {
//            log.warn("关闭redis失败，已忽略", e);
//        }
//    }

    // add end

}
