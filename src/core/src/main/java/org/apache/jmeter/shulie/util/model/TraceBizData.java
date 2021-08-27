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

package org.apache.jmeter.shulie.util.model;

/**
 * trace业务数据
 *
 * @author lipeng
 * @date 2021-05-17 2:30 下午
 */
public class TraceBizData {

    private String traceId;

    private String reportId;

    private boolean perfomanceTest;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public boolean isPerfomanceTest() {
        return perfomanceTest;
    }

    public void setPerfomanceTest(boolean perfomanceTest) {
        this.perfomanceTest = perfomanceTest;
    }

    private TraceBizData(String traceId, String reportId, boolean perfomanceTest) {
        this.traceId = traceId;
        this.reportId = reportId;
        this.perfomanceTest = perfomanceTest;
    }

    public static TraceBizData create(String traceId, String reportId, boolean perfomanceTest) {
        return new TraceBizData(traceId, reportId, perfomanceTest);
    }
}
