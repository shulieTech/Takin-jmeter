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

package org.apache.jmeter.shulie.util;

import io.shulie.jmeter.tool.redis.domain.GroupTopicEnum;
import io.shulie.jmeter.tool.redis.domain.TkMessage;
import io.shulie.jmeter.tool.redis.message.MessageProducer;
import org.apache.jmeter.shulie.model.EventEnum;
import org.apache.jmeter.shulie.model.EventInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: liyuanba
 * @Date: 2022/1/27 5:13 下午
 */
public class MessageUtil {
    private static final Logger log = LoggerFactory.getLogger(MessageUtil.class);
    /**
     * 消息分组和topic
     */
    public static GroupTopicEnum JMETER_REPORT = new GroupTopicEnum("default", "jmeter_report");
    
    private static MessageProducer messageProducer;

    static {
        messageProducer = MessageProducer.getInstance(JedisUtil.getRedisConfig());
    }

    public static boolean sendEvent(EventEnum event, String message) {
        EventInfo info = EventInfo.create()
                .setEvent(event)
                .setMessage(message)
                .build();
        return send("event", "", info);
    }

    public static boolean send(String tag, String key, Object content) {
        return send(tag, key, JsonUtil.toJson(content));
    }

    public static boolean send(String tag, String key, String content) {
        TkMessage message = TkMessage.create().setGroupTopic(JMETER_REPORT)
                .setTag(tag)
                .setKey(key)
                .setContent(content)
                .build();
        return send(message);
    }

    public static boolean send(TkMessage message) {
        try {
            return messageProducer.send(message);
        } catch (Exception e) {
            log.error("message send failed!message="+ JsonUtil.toJson(message), e);
        }
        return false;
    }
}
