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

public class TaskDynamicTps {
    /**
     * 压测任务ID
     */
    private Long taskId;

    /**
     * 线程组xpath值
     */
    private String transaction;

    /**
     * 压力值
     */
    private Double tps;

    /**
     * 时间戳 毫秒
     */
    private Long ms;

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Double getTps() {
        return tps;
    }

    public void setTps(Double tps) {
        this.tps = tps;
    }

    public Long getMs() {
        return ms;
    }

    public void setMs(Long ms) {
        this.ms = ms;
    }
}