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

package org.apache.jmeter.shulie.data.pusher;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.pamirs.pradar.log.parser.DataType;
import com.pamirs.pradar.remoting.protocol.CommandVersion;
import io.shulie.jmeter.tool.amdb.GlobalVariables;
import io.shulie.jmeter.tool.amdb.log.data.pusher.callback.LogCallback;
import io.shulie.jmeter.tool.amdb.log.data.pusher.push.DataPusher;
import io.shulie.jmeter.tool.amdb.log.data.pusher.push.ServerOptions;
import io.shulie.jmeter.tool.amdb.log.data.pusher.push.tcp.TcpDataPusher;
import io.shulie.jmeter.tool.amdb.log.data.pusher.server.ServerAddrProvider;
import io.shulie.jmeter.tool.amdb.log.data.pusher.server.ServerProviderOptions;
import io.shulie.jmeter.tool.amdb.zookeeper.ZkClientSpec;
import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.kafka.impl.KafkaSendServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.util.HttpNotifyTroCloudUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author moriarty
 */
public class LogPusher implements Runnable {

    public static final Logger logger = LoggerFactory.getLogger(LogPusher.class);

    private String reportId;

    private int threadIndex;

    private String threadName = "uploadThread_";

    private AtomicLong logCount;


    private Queue<String> queue;

    private PrintWriter pw;

    private boolean isEnded;

    private MessageSendService messageSendService;

    public LogPusher(Queue<String> queue, int threadIndex, String reportId) {
        this.queue = queue;
        this.threadIndex = threadIndex;
        this.reportId = reportId;
        logCount = new AtomicLong(0);
        isEnded = false;
    }

    public void start() {
        long threadId = Thread.currentThread().getId();
        this.threadName = this.threadName + this.reportId + "_" + this.threadIndex;
        Thread.currentThread().setName(this.threadName);
        logger.info("启动第{}个日志上传线程,线程ID:{},启动时间:{}", threadIndex, threadId, System.currentTimeMillis());
        messageSendService = new KafkaSendServiceImpl();
        messageSendService.init();
        logger.info("日志上传开始--线程ID:{},线程名称:{},开始时间：{},报告ID:{}", threadId, this.threadName, System.currentTimeMillis(),
                reportId);
        //打开文件
        OutputStreamWriter out = null;
        while (!GlobalVariables.stopFlag.get() || !queue.isEmpty()) {
            String logData = pollLogData();
            if (StringUtils.isNotBlank(logData)) {
                messageSendService.send(DataType.TRACE_LOG, CommandVersion.V1, logData, "127.0.0.1", new MessageSendCallBack() {
                    @Override
                    public void success() {
                    }

                    @Override
                    public void fail(String errorMessage) {
                        //消息发送失败，将数据写入文件
                        logger.info("上报jtl失败,数据写入文件。。");
                        writeUnReportLog(logData);
                    }
                });
            }
        }
        isEnded = true;
        logger.info("日志上传完成--线程ID:{},线程名称:{},结束时间：{},报告ID:{}，上传数量:{}", threadId, this.threadName,
                System.currentTimeMillis(), this.reportId, logCount.get());
        messageSendService.stop();
        //关闭文件
    }

    private String pollLogData() {
        long count = 0;
        StringBuilder stringBuilder = new StringBuilder();
        while (count < GlobalVariables.UPLOAD_SIZE && !this.queue.isEmpty()) {
            Object log = this.queue.poll();
            if (StringUtils.isNotBlank(log.toString())) {
                GlobalVariables.uploadCount.getAndIncrement();
                logCount.getAndIncrement();
                stringBuilder.append(log.toString()).append("\r\n");
                count += log.toString().getBytes().length;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    logger.error("日志上传异常--异常信息：{}", e.toString());
                }
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public void run() {
        start();
    }

    public void setPw(PrintWriter pw) {
        this.pw = pw;
    }

    /**
     * 写入未上报的日志
     *
     * @param dataLog
     */
    public void writeUnReportLog(String dataLog) {
        if (dataLog.charAt(dataLog.length() - 1) == '\n') {
            pw.write(dataLog + "\r");
        } else {
            pw.write(dataLog);
        }
        pw.flush();
    }

    public boolean isEnded() {
        return isEnded;
    }
}
