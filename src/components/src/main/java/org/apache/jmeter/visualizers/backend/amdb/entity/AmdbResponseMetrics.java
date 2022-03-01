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

package org.apache.jmeter.visualizers.backend.amdb.entity;

import java.util.List;

import static org.apache.jmeter.visualizers.backend.influxdb.entity.Constants.METRICS_TYPE_RESPONSE;

/**
 * ClassName:    AmdbMetricsRequest
 * Package:    org.apache.jmeter.visualizers.backend.amdb.entity
 * Description: amdb数据实体
 * Datetime:    2022/2/24   4:44 下午
 * Author:   chenhongqiao@shulie.com
 */
public class AmdbResponseMetrics extends AbstractMetrics {
    private Long time;
    private Long eventTime;
    private String measurement;
    private List<ResponseMetricsTag> tag;
    private List<ResponseMetricsField> field;

    public AmdbResponseMetrics() {
        super(METRICS_TYPE_RESPONSE);
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
    }

    public List<ResponseMetricsTag> getTag() {
        return tag;
    }

    public void setTag(List<ResponseMetricsTag> tag) {
        this.tag = tag;
    }

    public List<ResponseMetricsField> getField() {
        return field;
    }

    public void setField(List<ResponseMetricsField> field) {
        this.field = field;
    }
}
