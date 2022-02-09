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

import org.apache.jmeter.shulie.constants.PressureConstants;

/**
 * @Author: liyuanba
 * @Date: 2022/1/27 4:36 下午
 */
public class HealthData extends PressureInfo {
    /**
     * 当前时间
     */
    private Long time;

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public static HealthData.Builder create() {
        return new HealthData.Builder();
    }

    public static class Builder {
        public HealthData build() {
            HealthData data = new HealthData();
            data.setSceneId(PressureConstants.pressureEngineParamsInstance.getSceneId());
            data.setTaskId(PressureConstants.pressureEngineParamsInstance.getResultId());
            data.setCustomerId(PressureConstants.pressureEngineParamsInstance.getCustomerId());
            data.setPodNo(PressureConstants.pressureEngineParamsInstance.getPodNumber());
            data.setSceneType(PressureConstants.pressureEngineParamsInstance.getSceneType());
            data.setTime(System.currentTimeMillis());
            return data;
        }
    }
}
