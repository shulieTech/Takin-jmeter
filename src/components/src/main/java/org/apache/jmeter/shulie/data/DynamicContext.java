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

package org.apache.jmeter.shulie.data;

import io.shulie.jmeter.tool.executors.ExecutorServiceFactory;
import org.apache.jmeter.shulie.util.JedisUtil;
import org.apache.jmeter.shulie.util.NumberUtil;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 动态数据容器
 */
public class DynamicContext {
    private final static Logger logger = LoggerFactory.getLogger(DynamicContext.class);

    /**
     * 每秒总目标tps，0表示取脚本文件中的值
     */
    public static Double TPS_TARGET_LEVEL;
    /**
     * 目标tps上浮因子
     */
    public static Double TPS_FACTOR;

    private static AtomicBoolean inited = new AtomicBoolean(false);
    static {
        if (inited.compareAndSet(false, true)) {
            int flushTime = JMeterUtils.getPropDefault("tps_target_level_flush_time", 5000);
            ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.scheduleWithFixedDelay(() -> {
                flushTpsTargetLevel();
                flushTpsFactor();
            }, flushTime, flushTime, TimeUnit.MILLISECONDS);
        }
    }

    private static void flushTpsTargetLevel() {
        try {
            Double tpsTargetLevel = NumberUtil.valueOf(JedisUtil.hget(JedisUtil.REDIS_TPS_LIMIT_FIELD));
            if (null == tpsTargetLevel || tpsTargetLevel <= 0) {
                return;
            }
            if (NumberUtil.compareTo(TPS_TARGET_LEVEL, tpsTargetLevel) != 0) {
                logger.info("TPS_TARGET_LEVEL:" + TPS_TARGET_LEVEL + " -> " + tpsTargetLevel);
            }
            TPS_TARGET_LEVEL = tpsTargetLevel;
        } catch (Exception e) {
            logger.error("flush tps target level failed!", e);
        }
    }

    private static void flushTpsFactor() {
        try {
            Double tpsFactor = NumberUtil.valueOf(JedisUtil.hget(JedisUtil.REDIS_TPS_FACTOR));
            if (null == tpsFactor) {
                return;
            }
            if (NumberUtil.compareTo(TPS_FACTOR, tpsFactor) != 0) {
                logger.info("TPS_FACTOR:" + TPS_FACTOR + " -> " + tpsFactor);
            }
            TPS_FACTOR = tpsFactor;
        } catch (Exception e) {
            logger.error("flush tps factor failed!", e);
        }
    }
}
