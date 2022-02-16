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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class NumberUtil {
    private static final Logger logger = LoggerFactory.getLogger(NumberUtil.class);

    public static double divide(long b, long d) {
        if (d == 0) {
            return 0d;
        }
        return ((double) b)/d;
    }

    public static int compareTo(Double d1, Double d2) {
        if (null == d1 && null == d2) {
            return 0;
        }
        if (null == d1) {
            return -1;
        }
        if (null == d2) {
            return 1;
        }
        BigDecimal b1 = new BigDecimal(d1);
        BigDecimal b2 = new BigDecimal(d2);
        return b1.compareTo(b2);
    }

    public static int parseInt(Object value) {
        return parseInt(value, 0);
    }

    public static int parseInt(Object obj, int defValue) {
        String value = StringUtil.valueOf(obj);
        if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
            return defValue;
        }
        if (value.contains(".")) {
            value = StringUtil.removePoint(value);
        }
        int v = defValue;
        try {
            v = Integer.parseInt(value);
        } catch (Exception e) {
            logger.error("parseInt failed!value="+value, e);
        }
        return v;
    }

    public static double parseDouble(Object value) {
        return parseDouble(value, 0d);
    }

    public static double parseDouble(Object obj, double defValue) {
        String value = StringUtil.valueOf(obj);
        if (org.apache.commons.lang3.StringUtils.isBlank(value)) {
            return defValue;
        }
        double v = defValue;
        try {
            v = Double.parseDouble(value);
        } catch (Exception e) {
            logger.error("parseDouble failed!value="+value, e);
        }
        return v;
    }

    /**
     * 可能返回为null
     */
    public static Double valueOf(Object value) {
        if (null == value) {
            return null;
        }
        Double r = null;
        try {
            r = Double.parseDouble(StringUtil.valueOf(value));
        } catch (Exception e) {
            logger.error("parseDouble failed!value="+value, e);
        }
        return r;
    }

    /**
     * 判断值是否是NaN，不是则返回该值，否则返回0
     */
    public static double maybeNaN(double value) {
        return maybeNaN(value, 0d);
    }
    /**
     * 判断值是否是NaN，不是则返回该值，否则返回默认值
     */
    public static double maybeNaN(double value, double defValue) {
        return Double.isNaN(value) ? defValue : value;
    }
}
