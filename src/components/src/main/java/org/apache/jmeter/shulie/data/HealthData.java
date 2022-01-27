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

package org.apache.jmeter.shulie.data;

import io.shulie.jmeter.tool.redis.domain.AbstractEntry;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.util.DataUtil;

/**
 * @Author: liyuanba
 * @Date: 2022/1/27 4:36 下午
 */
public class HealthData extends AbstractEntry {
    /**
     * pod编号
     */
    private String podNo = DataUtil.getPodNo();
    /**
     * 当前时间
     */
    private long time = System.currentTimeMillis();
    /**
     * 当前场景id
     */
    private Long sceneId = PressureConstants.pressureEngineParamsInstance.getSceneId();
    /**
     * 任务id
     */
    private Long taskId = PressureConstants.pressureEngineParamsInstance.getResultId();

    public String getPodNo() {
        return podNo;
    }

    public void setPodNo(String podNo) {
        this.podNo = podNo;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Long getSceneId() {
        return sceneId;
    }

    public void setSceneId(Long sceneId) {
        this.sceneId = sceneId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}
