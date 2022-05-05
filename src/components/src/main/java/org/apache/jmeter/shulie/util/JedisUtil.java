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

package org.apache.jmeter.shulie.util;

import io.shulie.jmeter.tool.redis.RedisConfig;
import io.shulie.jmeter.tool.redis.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JedisUtil {
    private final static Logger logger = LoggerFactory.getLogger(JedisUtil.class);
    /** REDIS 压测实例 格式化串 */
    public static final String PRESSURE_ENGINE_INSTANCE_REDIS_KEY_FORMAT = "PRESSURE:ENGINE:INSTANCE:%s:%s:%s";
    /**
     * tps上浮因子
     */
    public static final String REDIS_TPS_FACTOR = "REDIS_TPS_FACTOR";

    private static RedisUtil redisUtil;

    private static String redisMasterKey;

    public static String getRedisMasterKey() {
        if (StringUtils.isBlank(redisMasterKey)) {
            String sid = PressureConstants.pressureEngineParamsInstance.getSceneId();
            String sceneId = null != sid ? String.valueOf(sid) : System.getProperty("SCENE_ID");
            Long resultId = PressureConstants.pressureEngineParamsInstance.getResultId();
            String reportId = null != resultId ? String.valueOf(resultId) : System.getProperty("__ENGINE_REPORT_ID__");
            Long customerId = PressureConstants.pressureEngineParamsInstance.getCustomerId();
            redisMasterKey = String.format(PRESSURE_ENGINE_INSTANCE_REDIS_KEY_FORMAT, sceneId, reportId, String.valueOf(customerId));
        }
        return redisMasterKey;
    }

    public static String hget(String key) {
        RedisUtil redisUtil = getRedisUtil();
        if (null == redisUtil) {
            logger.error("redisUtil is not inited!");
            return null;
        }
        return hget(getRedisMasterKey(), key);
    }

    public static String hget(String key, String hashKey) {
        RedisUtil redisUtil = getRedisUtil();
        if (null == redisUtil) {
            logger.error("redisUtil没有初始化!");
            return null;
        }
        return redisUtil.hget(key, hashKey);
    }

    public static String get(String key) {
        RedisUtil redisUtil = getRedisUtil();
        if (null == redisUtil) {
            logger.error("redisUtil没有初始化!");
            return null;
        }
        return redisUtil.get(key);
    }

    public synchronized static RedisUtil getRedisUtil() {
        if (null != redisUtil){
            return redisUtil;
        }
        String engineRedisAddress = System.getProperty("engineRedisAddress");
        String engineRedisPort = System.getProperty("engineRedisPort");
        String engineRedisSentinelNodes = System.getProperty("engineRedisSentinelNodes");
        String engineRedisSentinelMaster = System.getProperty("engineRedisSentinelMaster");
        String engineRedisPassword = System.getProperty("engineRedisPassword");
        logger.info("redis start..");
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
            redisUtil = RedisUtil.getInstance(redisConfig);
        } catch (Exception e) {
            logger.error("Redis 连接失败，redisAddress is {}， redisPort is {}， encryptRedisPassword is {},engineRedisSentinelNodes is {}," +
                            "engineRedisSentinelMaster is {}"
                    , engineRedisAddress, engineRedisPort, engineRedisPassword,engineRedisSentinelNodes,engineRedisSentinelMaster);
            logger.error("失败详细错误栈：", e);
            System.exit(-1);
        }
        return redisUtil;
    }

//    public static void closeJedis() {
//        try {
//            if (null != jedis) {
//                jedis.close();
//            }
//        } catch (Exception e) {
//            logger.warn("关闭redis失败，已忽略", e);
//        }
//    }
}
