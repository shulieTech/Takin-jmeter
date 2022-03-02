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

import org.apache.jmeter.DynamicClassLoader;

import java.lang.reflect.Method;

/**
 * @Author: liyuanba
 * @Date: 2021/10/18 4:41 下午
 */
public class JsonUtils {
    private static DynamicClassLoader loader;
    private static Class<?> jsonClass;
    private static Method toJsonMethod;

    public static void init(DynamicClassLoader loader) {
        JsonUtils.loader = loader;
    }

    private static Class<?> getJsonClass() {
        if (null == jsonClass) {
            try {
                jsonClass = JsonUtils.loader.loadClass("com.alibaba.fastjson.JSON");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return jsonClass;
    }

    public static Method getToJsonMethod() {
        if (null == toJsonMethod) {
            try {
                Class<?> jsonClass = getJsonClass();
                toJsonMethod = jsonClass.getDeclaredMethod("toJSONString", new Class[]{Object.class});
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return toJsonMethod;
    }

    public static String toJson(Object o) {
        if (null == o) {
            return null;
        }
        try {
            Method toJsonMethod = getToJsonMethod();
            Object res = toJsonMethod.invoke(null, o);
            return String.valueOf(res);
        } catch (Exception e) {
            System.err.println("toJson failed!o="+o);
        }
        return null;
    }
}
