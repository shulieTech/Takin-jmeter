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

package org.apache.jmeter.shulie.message;

import io.shulie.jmeter.tool.redis.domain.GroupTopicEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.shulie.constants.PressureConstants;

import java.util.UUID;

/**
 * @Author: liyuanba
 * @Date: 2022/2/10 5:36 下午
 */
public abstract class AbstractCloudNotifyMessageListener extends AbstractListener {
    private static final GroupTopicEnum GT_NOTIFY_ENGINE = new GroupTopicEnum("default", "notify_engine");
    private String consumer;

    @Override
    public GroupTopicEnum getGroupTopic() {
        return GT_NOTIFY_ENGINE;
    }

    /**
     * 来自cloud通知每个pod都要处理，所以每个pod的consumer都应该不一样
     */
    @Override
    public String getConsumer() {
        if (StringUtils.isBlank(consumer)) {
            Long taskId = PressureConstants.pressureEngineParamsInstance.getResultId();
            String podNo = PressureConstants.pressureEngineParamsInstance.getPodNumber();
            if (StringUtils.isBlank(podNo)) {
                podNo = UUID.randomUUID().toString();
            }
            consumer = "jmeter-"+taskId+"-"+podNo;
        }
        return consumer;
    }
}
