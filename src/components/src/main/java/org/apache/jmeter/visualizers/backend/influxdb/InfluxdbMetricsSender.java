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

package org.apache.jmeter.visualizers.backend.influxdb;

import org.apache.jmeter.visualizers.backend.influxdb.entity.EventMetrics;
import org.apache.jmeter.visualizers.backend.influxdb.entity.ResponseMetrics;

import java.io.PrintWriter;

/**
 * InfluxDB Sender interface
 *
 * @since 3.2
 */
interface InfluxdbMetricsSender {

    /**
     * 发送正常的响应数据
     */
    void addMetric(ResponseMetrics responseMetrics);

    /**
     * 发送事件
     */
    void addEventMetrics(EventMetrics eventMetrics);

    /**
     * Write metrics to Influxdb with HTTP API with InfluxDB's Line Protocol
     */
    void writeAndSendMetrics();

    /**
     * Setup sender using influxDBUrl
     *
     * @param influxDBUrl   url pointing to influxdb
     * @param influxDBToken authorization token to influxdb 2.0
     * @throws Exception when setup fails
     */
    void setup(String influxDBUrl, String influxDBToken, PrintWriter pw) throws Exception; // NOSONAR

    /**
     * Destroy sender
     */
    void destroy();

}
