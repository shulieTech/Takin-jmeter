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

import com.google.common.collect.Lists;
import io.shulie.jmeter.tool.redis.domain.TkMessage;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.util.NumberUtil;
import org.apache.jmeter.threads.JMeterContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @Author: liyuanba
 * @Date: 2022/2/10 5:58 下午
 */
public class StopMessageListener extends AbstractCloudNotifyMessageListener {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Override
    public List<String> getTags() {
        return Lists.newArrayList("stop");
    }

    @Override
    public boolean receive(TkMessage message) {
        long taskId = NumberUtil.parseInt(message.getKey());
        if (taskId != PressureConstants.pressureEngineParamsInstance.getResultId()) {
            return true;
        }
        try {
            JMeterContextService.getContext().getEngine().stopTest();
        } catch (Throwable t) {
            log.error("engine stop failed!taskId="+taskId, t);
        }
        return true;
    }
}
