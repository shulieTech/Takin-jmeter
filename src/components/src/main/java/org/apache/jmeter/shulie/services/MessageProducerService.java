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

package org.apache.jmeter.shulie.services;

import io.shulie.jmeter.tool.redis.RedisConfig;
import io.shulie.jmeter.tool.redis.domain.TkMessage;
import io.shulie.jmeter.tool.redis.message.MessageProducer;
import org.apache.jmeter.shulie.util.JedisUtil;
import org.apache.jmeter.shulie.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: liyuanba
 * @Date: 2022/1/27 2:58 下午
 */
public class MessageProducerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static MessageProducerService messagePorducerService;
    private MessageProducer messageProducer;

    public static MessageProducerService getInstance() {
        if (null == messagePorducerService) {
            messagePorducerService = new MessageProducerService();
            messagePorducerService.init();
        }
        return messagePorducerService;
    }

    public void init() {
        RedisConfig config = JedisUtil.getRedisConfig();
        messageProducer = MessageProducer.getInstance(config);
    }

    public boolean send(TkMessage message) {
        try {
            return messageProducer.send(message);
        } catch (Exception e) {
            logger.error("message send failed!message="+ JsonUtil.toJson(message), e);
        }
        return false;
    }
}
