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

package org.apache.jmeter.shulie.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.services.CsvPositionRecord;
import org.apache.jmeter.services.PositionFileInputStream;
import org.apache.jmeter.services.PositionFileServer;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.consts.ThroughputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import io.shulie.jmeter.tool.redis.RedisConfig;
import io.shulie.jmeter.tool.redis.RedisUtil;

/**
 * @author xr.l
 */
public class CsvPosition implements CsvPositionRecord {
    private static final Logger log = LoggerFactory.getLogger(CsvPosition.class);
    @Override
    public void recordCsvPosition() {
        if (PositionFileServer.positionMap.size() > 0) {
            if (ThroughputConstants.redisUtil == null) {
                initRedis();
            }
            String sceneId;
            Long sId = PressureConstants.pressureEngineParamsInstance.getSceneId();
            sceneId = null != sId ? String.valueOf(sId) : System.getProperty("SCENE_ID");
            String key = String.format("CSV_READ_POSITION_%s", sceneId);
            String podNumber = StringUtils.isBlank(System.getProperty("pod.number")) ? "1" : System.getProperty("pod.number");
            log.info("最后一次缓存文件读取位点信息。SCENE_ID:{},POD_NUM:{}", sceneId, podNumber);
            for (Map.Entry<String, PositionFileInputStream> entry : PositionFileServer.positionMap.entrySet()){
                try {
                    String variableMapStr = System.getProperty("positionVariablesStr");
                    if (StringUtils.isNotBlank(variableMapStr)){
                        log.info("获取到文件信息：{}",variableMapStr);
                        JSONObject positionJson = JSONObject.parseObject(variableMapStr);
                        JSONObject object = positionJson.getJSONObject(entry.getKey());
                        if (Objects.nonNull(object)){
                            if (object.containsKey("start") && object.containsKey("end")){
                                Map<String,Long> resultMap = new HashMap<>(3);
                                resultMap.put("startPosition", object.getLongValue("start"));
                                resultMap.put("readPosition", object.getLongValue("end") - entry.getValue().longAvailable());
                                resultMap.put("endPosition", object.getLongValue("end"));
                                String field = String.format("%s_pod_num_%s", entry.getKey()
                                        , podNumber);
                                ThroughputConstants.redisUtil.hset(key, field, JSON.toJSONString(resultMap));
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error("更新缓存中的CSV文件位点信息失败，异常信息{}",e.getMessage());
                }
            }
        }
    }

    private void initRedis(){
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
            redisConfig.setMaxIdle(4);
            redisConfig.setMaxTotal(10);
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
}
