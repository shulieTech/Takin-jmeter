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

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class DataUtil {
    /**
     * POD序号
     */
    public static String getPodNo() {
        return StringUtils.isBlank(System.getProperty("pod.number")) ? "1" : System.getProperty("pod.number");
    }

    /**
     * 1-100%的sa数据序列化
     */
    public static String percentMapToString(Map<Integer, Map<String, Number>> map) {
        if (null == map || map.size() <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Map<String, Number>> e : map.entrySet()) {
            Map<String, Number> m = e.getValue();
            Number count = m.get("count");
            Number rt = m.get("rt");
            sb.append(e.getKey()).append(",").append(count).append(",").append(rt).append("|");
        }
        return sb.toString();
    }
}
