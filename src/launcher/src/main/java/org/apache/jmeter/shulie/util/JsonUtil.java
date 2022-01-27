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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author: liyuanba
 * @Date: 2021/10/18 4:41 下午
 */
public class JsonUtil {
    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    public static String toJson(Object o) {
        if (null == o) {
            return null;
        }
        try {
            return JSON.toJSONString(o);
        } catch (Exception e) {
            log.error("toJson failed!o="+o);
        }
        return null;
    }

    public static JSONObject parse(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        JSONObject json = null;
        try {
            json = JSON.parseObject(text);
        } catch (Exception e) {
            log.error("parse json failed!text="+text);
        }
        return json;
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        T result = null;
        try {
            result = JSON.parseObject(text, clazz);
        } catch (Exception e) {
            log.error("parse json to object class failed!text="+text);
        }
        return result;
    }

    public static <T> T parseObject(String text, TypeReference<T> type) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        T result = null;
        try {
            result = JSON.parseObject(text, type);
        } catch (Exception e) {
            log.error("parse json to object type failed!text="+text);
        }
        return result;
    }

    public static JSONArray parseArray(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        JSONArray result = null;
        try {
            result = JSON.parseArray(text);
        } catch (Exception e) {
            log.error("json parseArray failed!text="+text);
        }
        return result;
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        List<T> result = null;
        try {
            result = JSON.parseArray(text, clazz);
        } catch (Exception e) {
            log.error("json parseArray failed!text="+text);
        }
        return result;
    }


}
