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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.jmeter.shulie.constants.PressureConstants;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataUtil {
    /**
     * 从元素中获取唯一标识，老版本是testname，新版是testname中的MD5
     */
    public static String getTransaction(String sampleLabel) {
        int splitPos = sampleLabel.lastIndexOf(PressureConstants.TEST_NAME_MD5_SPLIT);
        String transaction = sampleLabel;
        if (-1 != splitPos) {
            transaction = sampleLabel.substring(splitPos + PressureConstants.TEST_NAME_MD5_SPLIT.length());
        }
        return transaction;
    }
    /**
     * POD序号
     */
    public static String getPodNo() {
        String podNo = PressureConstants.pressureEngineParamsInstance.getPodNumber();
        if (StringUtils.isBlank(podNo)) {
            podNo = StringUtils.isBlank(System.getProperty("pod.number")) ? "1" : System.getProperty("pod.number");
        }
        return podNo;
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

    /**
     * 取值选择器
     * @param defValue  默认值
     * @param t         取值对象
     * @param func      取值方法，从对象中取值的方法
     * @param <T>       取值对象类型
     * @param <R>       返回值对象类型
     */
    public static <T, R> R getValue(R defValue, T t, Function<T, R> func) {
        R result = defValue;
        if (null != t) {
            R r = func.apply(t);
            if (null != r) {
                if (r instanceof String) {
                    if (StringUtils.isNotBlank((String) r)) {
                        result = r;
                    }
                } else if (r instanceof List) {
                    if (CollectionUtils.isNotEmpty((List<?>) r)) {
                        result = r;
                    }
                } else if (r instanceof Map) {
                    if (MapUtils.isNotEmpty((Map<?, ?>) r)) {
                        result = r;
                    }
                } else {
                    result = r;
                }
            }
        }
        return result;
    }

    /**
     * @param throwable List of {@link Throwable}
     * @return String
     */
    public static String throwableToString(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter); // NOSONAR
        builder.append(stringWriter.toString())
                .append("\r\n");
        return builder.toString();
    }
}
