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

package org.apache.jmeter.shulie.model;

import org.apache.jmeter.shulie.constants.PressureConstants;

/**
 * @Author: liyuanba
 * @Date: 2022/1/28 10:02 上午
 */
public class EventInfo extends PressureInfo {
    private EventEnum event;
    private String message;
    private Long time;

    public EventEnum getEvent() {
        return event;
    }

    public void setEvent(EventEnum event) {
        this.event = event;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private EventEnum event;
        private String message;

        public EventInfo build() {
            EventInfo info = new EventInfo();
            info.setSceneId(PressureConstants.pressureEngineParamsInstance.getSceneId());
            info.setTaskId(PressureConstants.pressureEngineParamsInstance.getResultId());
            info.setCustomerId(PressureConstants.pressureEngineParamsInstance.getCustomerId());
            info.setPodNo(PressureConstants.pressureEngineParamsInstance.getPodNumber());
            info.setSceneType(PressureConstants.pressureEngineParamsInstance.getSceneType());
            info.setEvent(this.getEvent());
            info.setMessage(this.getMessage());
            info.setTime(System.currentTimeMillis());
            return info;
        }

        public EventEnum getEvent() {
            return event;
        }

        public Builder setEvent(EventEnum event) {
            this.event = event;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }
    }
}
