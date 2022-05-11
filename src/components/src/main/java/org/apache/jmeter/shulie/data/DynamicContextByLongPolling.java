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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.shulie.jmeter.tool.executors.ExecutorServiceFactory;
import org.apache.jmeter.shulie.consts.ThroughputConstants;
import org.apache.jmeter.shulie.model.TaskDynamicTps;
import org.apache.jmeter.shulie.util.HttpUtils;
import org.apache.jmeter.shulie.util.JedisUtil;
import org.apache.jmeter.shulie.util.NumberUtil;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 动态数据容器
 *
 * @author 数列科技
 */
public class DynamicContextByLongPolling {
    private final static Logger logger = LoggerFactory.getLogger(DynamicContextByLongPolling.class);

    /**
     * <ul>
     *     <li>key:线程组MD5</li>
     *     <li>value:每秒总目标tps，0表示取脚本文件中的值</li>
     * </ul>
     */
    private static final ConcurrentHashMap<String, Double> TPS_TARGET_LEVEL = new ConcurrentHashMap<>(16);
    /**
     * 目标tps上浮因子
     */
    public static Double TPS_FACTOR;
    /**
     * TPS目标值的Map
     * <p>Redis中hash存储</p>
     * <ul>
     *     <li>key:线程组MD5</li>
     *     <li>value:目标值</li>
     * </ul>
     */
    //private static final String REDIS_TPS_MAP = JedisUtil.getRedisMasterKey() + ":REDIS_TPS_MAP";
    /**
     * TPS目标值的全部线程组md5
     */
    //private static final String REDIS_TPS_ALL_KEY = JedisUtil.getRedisMasterKey() + ":REDIS_TPS_ALL_KEY";
    /**
     * 初始化标识
     */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private static String dynamicTaskTpsUrl;

    static {
        dynamicTaskTpsUrl = System.getProperty("__ENGINE_DYNAMIC_TASK_TPS_URL__");
        startTest();
    }

    public static void startTest() {
        if (INITIALIZED.compareAndSet(false, true)) {
            int flushTime = JMeterUtils.getPropDefault("tps_target_level_flush_time", 5000);
            ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.scheduleWithFixedDelay(() -> {
                flushTpsTargetLevel();
                //flushTpsFactor();
            }, flushTime, flushTime, TimeUnit.MILLISECONDS);
            //if (null == stopMessageListener) {
            //    stopMessageListener = new StopMessageListener();
            //}
        }
    }

    /**
     * 刷新TPS目标值
     * <p>取REDIS中REDIS_TPS_ALL_KEY的值，解析为JSON数组循环</p>
     */
    private static void flushTpsTargetLevel() {
        try {
            JSONObject json = HttpUtils.get(dynamicTaskTpsUrl);
            if (!json.getBoolean("success") || !json.containsKey("data") || Objects.isNull(json.get("data"))) {
                return;
            }
            List<TaskDynamicTps> taskRes = JSONArray.parseArray(json.getString("data"),
                    TaskDynamicTps.class);

            for (TaskDynamicTps dynamicTps : taskRes) {
                TPS_TARGET_LEVEL.put(dynamicTps.getRef(), dynamicTps.getTps());
            }
        } catch (Exception e) {
            logger.error("刷新TPS目标值异常！", e);
        }
    }

    public static void main(String[] args) {
        dynamicTaskTpsUrl = "http://127.0.0.1:10181/api/config/dynamic/tps/get?taskId=77";
        new Thread(() -> {
            do {
                flushTpsTargetLevel();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } while (true);
        }).start();
        while (true) {
            String md5 = "@MD5:7dae7383a28b5c45069b528a454d1164";
            Double tps = getTpsTargetLevel(md5);
            System.out.println("tps:" + tps);
            if (Objects.nonNull(tps)) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 获取TPS目标值
     *
     * @param threadGroupName 线程组名称
     * @return TPS目标值.可能为空
     */
    public static Double getTpsTargetLevel(String threadGroupName) {
        // 1. 从线程组名称中获取md5值
        int splitPos = threadGroupName.lastIndexOf(ThroughputConstants.TEST_NAME_MD5_SPLIT);
        String transaction;
        if (-1 != splitPos) {
            transaction = threadGroupName.substring(splitPos + ThroughputConstants.TEST_NAME_MD5_SPLIT.length());
        } else {
            transaction = "all";
        }
        // 2. 返回REDIS中的缓存
        return TPS_TARGET_LEVEL.get(transaction);
    }

    public static void destroy() {
        ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.shutdown();
    }
}
