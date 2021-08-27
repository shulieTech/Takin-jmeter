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

package org.apache.jmeter.report.core;

import java.util.List;
import java.util.Map;

import net.sf.saxon.ma.json.JsonParser;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * The class JsonUtil provides helper functions to generate Json.
 *
 * @since 3.0
 */
public final class JsonUtil {

    /**
     * Converts the specified array to a json-like array string.
     *
     * @param array
     *            the array
     * @return the json string
     */
    public static String toJsonArray(final String... array) {
        return '[' + StringUtils.join(array, ", ") + ']';
    }

    /**
     * Converts the specified map to a json-like object string.
     *
     * @param map
     *            the map
     * @return the string
     */
    public static String toJsonObject(Map<String, String> map) {
        String result = "{";
        if (map != null) {
            String[] array = new String[map.size()];
            int index = 0;
            for (Map.Entry<String, String> entry : map.entrySet()) {
                array[index] = '"' + entry.getKey() + "\": " + entry.getValue();
                index++;
            }
            result += StringUtils.join(array, ", ");
        }
        return result + "}";
    }

    /**
     * list 转 json
     *
     * @param list
     * @return
     */
    public static String toJsonArrayString(List<Map<String, String>> list) {
        if(list == null || list.size() == 0) {
            return "[]";
        }
        StringBuffer result = new StringBuffer();
        result.append("[");
        int idx = 0;
        for(int i=0; i<list.size(); i++) {
            if(idx > 0) {
                result.append(",");
            }
            result.append(toJsonString(list.get(i)));
            idx++;
        }
        result.append("]");
        return result.toString();
    }

    /**
     * map 转 json
     *
     * @return
     */
    public static String toJsonString(Map<String, String> map) {
        if(map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("{");
        int idx = 0;
        for(Map.Entry<String, String> entry : map.entrySet()) {
            if(idx > 0) {
                buffer.append(",");
            }
            buffer.append("\""+entry.getKey()+"\"");
            buffer.append(":");
            buffer.append("\""+entry.getValue()+"\"");
            idx++;
        }
        buffer.append("}");
        return buffer.toString();
    }
}
