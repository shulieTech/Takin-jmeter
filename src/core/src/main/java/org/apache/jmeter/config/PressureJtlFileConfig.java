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
public class PressureJtlFileConfig {

    public static PressureJtlFileConfig defaultConfig;

    private boolean jtlEnable;
    private boolean jtlErrorOnly;
    private boolean jtlTimeoutOnly;
    private Long timeoutThreshold;
    private boolean jtlCutoff;


    public PressureJtlFileConfig() {
    }

    public PressureJtlFileConfig(boolean jtlEnable, boolean jtlErrorOnly, boolean jtlTimeoutOnly, Long timeoutThreshold, boolean jtlCutoff) {
        this.jtlEnable = jtlEnable;
        this.jtlErrorOnly = jtlErrorOnly;
        this.jtlTimeoutOnly = jtlTimeoutOnly;
        this.timeoutThreshold = timeoutThreshold;
        this.jtlCutoff = jtlCutoff;
    }

    public boolean isJtlEnable() {
        return jtlEnable;
    }

    public void setJtlEnable(boolean jtlEnable) {
        this.jtlEnable = jtlEnable;
    }

    public boolean isJtlErrorOnly() {
        return jtlErrorOnly;
    }

    public void setJtlErrorOnly(boolean jtlErrorOnly) {
        this.jtlErrorOnly = jtlErrorOnly;
    }

    public boolean isJtlTimeoutOnly() {
        return jtlTimeoutOnly;
    }

    public void setJtlTimeoutOnly(boolean jtlTimeoutOnly) {
        this.jtlTimeoutOnly = jtlTimeoutOnly;
    }

    public Long getTimeoutThreshold() {
        return timeoutThreshold;
    }

    public void setTimeoutThreshold(Long timeoutThreshold) {
        this.timeoutThreshold = timeoutThreshold;
    }

    public boolean isJtlCutoff() {
        return jtlCutoff;
    }

    public void setJtlCutoff(boolean jtlCutoff) {
        this.jtlCutoff = jtlCutoff;
    }

    public static PressureJtlFileConfig create(boolean jtlEnable, boolean jtlErrorOnly, boolean jtlTimeoutOnly, Long timeoutThreshold, boolean jtlCutoff) {
        defaultConfig = new PressureJtlFileConfig(jtlEnable,jtlErrorOnly,jtlTimeoutOnly,timeoutThreshold,jtlCutoff);
        return defaultConfig;
    }

    @Override
    public String toString() {
        return "PressureJtlConfig{" +
                "jtlEnable=" + jtlEnable +
                ", jtlErrorOnly=" + jtlErrorOnly +
                ", jtlTimeoutOnly=" + jtlTimeoutOnly +
                ", timeoutThreshold=" + timeoutThreshold +
                ", jtlCutoff=" + jtlCutoff +
                '}';
    }
}
