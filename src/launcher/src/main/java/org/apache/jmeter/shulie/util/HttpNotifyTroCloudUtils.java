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

import org.apache.jmeter.DynamicClassLoader;
import org.apache.jmeter.shulie.model.PressureEngineParams;

/**
 * 通知cloud工具类
 *
 */
public class HttpNotifyTroCloudUtils {

    //压测引擎异常信息前缀
    private static final String PRESSURE_ENGINE_EXCEPTION_PREFIX = "【压测引擎】";

    private static DynamicClassLoader loader;

    public static void init(DynamicClassLoader loader) {
        HttpNotifyTroCloudUtils.loader = loader;
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
        String podNumber = System.getProperty("pod.number");
        requestParams.put("podNum", (podNumber == null || podNumber.length() == 0) ? "1" : podNumber);
        requestParams.put("status", status);
        requestParams.put("msg", PRESSURE_ENGINE_EXCEPTION_PREFIX + errMsg);
        try {
            Class<?> jsonClass = HttpNotifyTroCloudUtils.loader.loadClass("com.alibaba.fastjson.JSON");
            Method toJsonStringMethod = jsonClass.getDeclaredMethod("toJSONString", new Class[] { Object.class });
            Object res = toJsonStringMethod.invoke(null, requestParams);
            HttpUtils.doPost(params.getCallbackUrl(),
                    String.valueOf(res));
            System.out.println("org.apache.jmeter.shulie.util.HttpNotifyTroCloudUtils|49|上报cloud启动日志完成.. >> status is ["+status+"]");
        } catch(Throwable e) {
            System.out.println("org.apache.jmeter.shulie.util.HttpNotifyTroCloudUtils|51|"+e.getMessage());
        }
    }
}
