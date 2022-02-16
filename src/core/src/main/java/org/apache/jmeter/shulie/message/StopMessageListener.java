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
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.util.ShutdownClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
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
            int port = JMeterUtils.getPropDefault("jmeterengine.nongui.port", ShutdownClient.UDP_PORT_DEFAULT); // $NON-NLS-1$
            int maxPort = JMeterUtils.getPropDefault("jmeterengine.nongui.maxport", 4455); // $NON-NLS-1$
            if (port > 1000) {
                final DatagramSocket socket = getSocket(port, maxPort);
                //命令支持参考Jmeter#waitForSignals, StopTestNow立即停止，Shutdown关闭，HeapDump，ThreadDump
                String command = "Shutdown";
                byte[] buf = command.getBytes(StandardCharsets.US_ASCII);
                InetAddress address = InetAddress.getByName("localhost");
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
            }
        } catch (Throwable t) {
            log.error("engine stop failed!taskId="+taskId, t);
        }
        return true;
    }

    private static DatagramSocket getSocket(int udpPort, int udpPortMax) {
        DatagramSocket socket = null;
        int i = udpPort;
        while (i <= udpPortMax) {
            try {
                socket = new DatagramSocket(i);
                break;
            } catch (SocketException e) { // NOSONAR
                i++;
            }
        }

        return socket;
    }
}
