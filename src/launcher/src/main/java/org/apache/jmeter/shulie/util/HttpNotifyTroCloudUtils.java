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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import io.shulie.jmeter.tool.redis.domain.TkMessage;
import io.shulie.jmeter.tool.redis.util.JsonUtil;
import org.apache.jmeter.DynamicClassLoader;
import org.apache.jmeter.shulie.model.EventEnum;
import org.apache.jmeter.shulie.model.EventInfo;
import org.apache.jmeter.shulie.model.PressureEngineParams;
import org.apache.jmeter.shulie.model.PressureInfo;

import static org.apache.jmeter.shulie.util.MessageUtils.JMETER_REPORT;

/**
 * 通知cloud工具类
 *
 */
public class HttpNotifyTroCloudUtils {

    //压测引擎异常信息前缀
    private static final String PRESSURE_ENGINE_EXCEPTION_PREFIX = "【压测引擎】";

    private static DynamicClassLoader loader;

    private static final String eventUrl;

    public static void init(DynamicClassLoader loader) {
        HttpNotifyTroCloudUtils.loader = loader;
    }

    static {
        eventUrl = System.getProperty("eventUrl");
    }

    public static boolean hasEvevtUrl() {
        return StringUtils.isBlank(eventUrl);
    }

    public static boolean sendEvent(EventEnum event, String message) {
        EventInfo info = EventInfo.create()
                .setEvent(event)
                .setMessage(message)
                .build();
        return send("event", info);
    }

    public static boolean send(String tag, PressureInfo info) {
        return send(tag, info.getTaskId(), info);
    }

    public static boolean send(String tag, Object key, Object content) {
        return send(tag, String.valueOf(key), JsonUtil.toJson(content));
    }

    public static boolean send(String tag, String key, String content) {
        TkMessage message = TkMessage.create().setGroupTopic(JMETER_REPORT)
                .setTag(tag)
                .setKey(key)
                .setContent(content)
                .build();
        return send(message);
    }

    public static boolean send(TkMessage message) {
        return send(JsonUtils.toJson(message));
    }

    public static boolean send(String message) {
        boolean r = false;
        try {
            String response = HttpUtils.doPost(eventUrl, message);
            if (StringUtils.isNotBlank(response)) {
                r = true;
            }
            System.out.println("上报消息. >> message="+message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return r;
    }

    /**
     * 通知cloud信息
     *
     * @param params
     * @param status
     * @param errMsg
     */
    public static void notifyTroCloud(final PressureEngineParams params, final String status, final String errMsg) {
        //launcher包中没有日志组件，使用systemout打印日志
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("sceneId", params.getSceneId());
        requestParams.put("resultId", params.getResultId());
        requestParams.put("customerId", params.getCustomerId());
        //pod 序号
        requestParams.put("podNum", DataUtil.getPodNo());
        requestParams.put("status", status);
        requestParams.put("msg", PRESSURE_ENGINE_EXCEPTION_PREFIX + errMsg);
        try {
            Class<?> jsonClass = HttpNotifyTroCloudUtils.loader.loadClass("com.alibaba.fastjson.JSON");
            Method toJsonStringMethod = jsonClass.getDeclaredMethod("toJSONString", new Class[] { Object.class });
            Object res = toJsonStringMethod.invoke(null, requestParams);
            HttpUtils.doPost(params.getCallbackUrl(), String.valueOf(res));
            System.out.println("org.apache.jmeter.shulie.util.HttpNotifyTroCloudUtils|49|上报cloud启动日志完成.. >> status is ["+status+"]");
        } catch(Throwable e) {
            System.out.println("org.apache.jmeter.shulie.util.HttpNotifyTroCloudUtils|51|"+e.getMessage());
        }
    }
}
