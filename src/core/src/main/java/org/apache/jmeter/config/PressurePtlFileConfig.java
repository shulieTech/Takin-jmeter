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

package org.apache.jmeter.config;

/**
 * jtl 日志文件输出配置
 *
 * @author xr.l
 */
public class PressurePtlFileConfig {

    public static PressurePtlFileConfig defaultConfig;

    public static final String PTL_UPLOAD_FROM_ENGINE = "engine";

    public static final String PTL_UPLOAD_FROM_CLOUD = "cloud";

    private String ptlUploadFrom;
    private boolean ptlEnable;
    private boolean ptlErrorOnly;
    private boolean ptlTimeoutOnly;
    private Long timeoutThreshold;
    private boolean ptlCutoff;


    public PressurePtlFileConfig() {
    }

    public PressurePtlFileConfig(String ptlUploadFrom, boolean ptlEnable, boolean ptlErrorOnly, boolean ptlTimeoutOnly, Long timeoutThreshold, boolean ptlCutoff) {
        this.ptlUploadFrom = ptlUploadFrom;
        this.ptlEnable = ptlEnable;
        this.ptlErrorOnly = ptlErrorOnly;
        this.ptlTimeoutOnly = ptlTimeoutOnly;
        this.timeoutThreshold = timeoutThreshold;
        this.ptlCutoff = ptlCutoff;
    }

    public String getPtlUploadFrom() {
        return ptlUploadFrom;
    }

    public boolean isPtlEnable() {
        return ptlEnable;
    }

    public boolean isPtlErrorOnly() {
        return ptlErrorOnly;
    }

    public boolean isPtlTimeoutOnly() {
        return ptlTimeoutOnly;
    }

    public Long getTimeoutThreshold() {
        return timeoutThreshold;
    }

    public boolean isPtlCutoff() {
        return ptlCutoff;
    }

    public static PressurePtlFileConfig create(String ptlUploadFrom, boolean ptlEnable, boolean ptlErrorOnly, boolean ptlTimeoutOnly, Long timeoutThreshold, boolean ptlCutoff) {
        defaultConfig = new PressurePtlFileConfig(ptlUploadFrom,ptlEnable,ptlErrorOnly,ptlTimeoutOnly,timeoutThreshold,ptlCutoff);
        return defaultConfig;
    }

    @Override
    public String toString() {
        return "PressurePtlFileConfig{" +
                "ptlUploadFrom='" + ptlUploadFrom + '\'' +
                ", ptlEnable=" + ptlEnable +
                ", ptlErrorOnly=" + ptlErrorOnly +
                ", ptlTimeoutOnly=" + ptlTimeoutOnly +
                ", timeoutThreshold=" + timeoutThreshold +
                ", ptlCutoff=" + ptlCutoff +
                '}';
    }
}
