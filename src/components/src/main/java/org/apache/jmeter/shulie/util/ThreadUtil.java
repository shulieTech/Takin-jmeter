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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtil {
    private final static Logger logger = LoggerFactory.getLogger(ThreadUtil.class);

    /**
     * 获取当前线程组正在运行的线程数
     */
    public static int getCurrentGroupActiveThreadNum() {
        int activityThreadNum = 0;
        try {
            ThreadGroup group = Thread.currentThread().getThreadGroup();
            ThreadGroup topGroup = group;
            // 遍历线程组树，获取根线程组
            while (null != group) {
                topGroup = group;
                group = group.getParent();
            }
            int slackSize = topGroup.activeCount() * 2;
            Thread[] slackThreads = new Thread[slackSize];
            int actualSize = topGroup.enumerate(slackThreads);
            Thread[] actualThreads = new Thread[actualSize];
            System.arraycopy(slackThreads, 0, actualThreads, 0, actualSize);
            for (Thread thread : actualThreads) {
                //tps并发线程组名称：shulie - ConcurrencyThreadGroup-ThreadStarter 1-333
//                if (!thread.getName().contains("Thread Group-ThreadStarter")) {
//                    continue;
//                }
                if (thread.getState() == Thread.State.RUNNABLE || thread.getState() == Thread.State.BLOCKED) {
                    activityThreadNum++;
                }
            }
        } catch (Exception e) {
            logger.error("getCurrentGroupActiveThreadNum error!", e);
        }
        return activityThreadNum;
    }

}
