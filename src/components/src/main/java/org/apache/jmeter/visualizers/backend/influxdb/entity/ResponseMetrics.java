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

package org.apache.jmeter.visualizers.backend.influxdb.entity;

import static org.apache.jmeter.visualizers.backend.influxdb.entity.Constants.METRICS_TYPE_RESPONSE;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 成功的响应
 *
 * @author shiyajian
 * create: 2020-10-10
 */
public class ResponseMetrics extends AbstractMetrics {

    private static final long serialVersionUID = 1L;

    private String transaction;
    private Integer count;
    private Integer failCount;
    private Long sentBytes;
    private Long receivedBytes;
    private Double rt;
    private Long sumRt;
    private Integer saCount;
    private Double maxRt;
    private Double minRt;
    private long timestamp;
    //add by 李鹏 添加当前活跃线程数和transactionUrl
    private Integer activeThreads;
    private String transactionUrl;
    //add end
    private Set<ErrorInfo> errorInfos;
    private Map<String, String> tags = new HashMap<>();
    //1-100%点位耗时和请求量数据
    private String percentData;

    public static class ErrorInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String responseMessage;
        private String responseCode;

        public String getResponseMessage() {
            return responseMessage;
        }

        public void setResponseMessage(String responseMessage) {
            this.responseMessage = responseMessage;
        }

        public String getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(String responseCode) {
            this.responseCode = responseCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            ErrorInfo errorInfo = (ErrorInfo)o;
            return Objects.equals(responseMessage, errorInfo.responseMessage)
                    && Objects.equals(responseCode, errorInfo.responseCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(responseMessage, responseCode);
        }
    }

    public ResponseMetrics() {
        super(METRICS_TYPE_RESPONSE);
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getFailCount() {
        return failCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Double getMaxRt() {
        return maxRt;
    }

    public void setMaxRt(Double maxRt) {
        this.maxRt = maxRt;
    }

    public Double getMinRt() {
        return minRt;
    }

    public void setMinRt(Double minRt) {
        this.minRt = minRt;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getActiveThreads() {
        return activeThreads;
    }

    public void setActiveThreads(Integer activeThreads) {
        this.activeThreads = activeThreads;
    }

    public Double getRt() {
        return rt;
    }

    public void setRt(Double rt) {
        this.rt = rt;
    }

    public Long getSumRt() {
        return sumRt;
    }

    public void setSumRt(Long sumRt) {
        this.sumRt = sumRt;
    }

    public Integer getSaCount() {
        return saCount;
    }

    public void setSaCount(Integer saCount) {
        this.saCount = saCount;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Long getSentBytes() {
        return sentBytes;
    }

    public void setSentBytes(Long sentBytes) {
        this.sentBytes = sentBytes;
    }

    public Long getReceivedBytes() {
        return receivedBytes;
    }

    public void setReceivedBytes(Long receivedBytes) {
        this.receivedBytes = receivedBytes;
    }

    public Set<ErrorInfo> getErrorInfos() {
        return errorInfos;
    }

    public void setErrorInfos(
            Set<ErrorInfo> errorInfos) {
        this.errorInfos = errorInfos;
    }

    public String getPercentData() {
        return percentData;
    }

    public void setPercentData(String percentData) {
        this.percentData = percentData;
    }
}
