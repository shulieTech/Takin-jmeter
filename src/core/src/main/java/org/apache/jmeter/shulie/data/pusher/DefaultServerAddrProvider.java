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

import io.shulie.jmeter.tool.amdb.log.data.pusher.server.ConnectInfo;
import io.shulie.jmeter.tool.amdb.log.data.pusher.server.ServerAddrProvider;
import io.shulie.jmeter.tool.amdb.log.data.pusher.server.ServerProviderOptions;
import io.shulie.jmeter.tool.amdb.log.data.pusher.server.hash.Node;
import io.shulie.jmeter.tool.amdb.zookeeper.NetflixCuratorZkClientFactory;
import io.shulie.jmeter.tool.executors.ExecutorServiceFactory;
import io.shulie.surge.data.common.zk.ZkClient;
import io.shulie.surge.data.common.zk.ZkPathChildrenCache;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ClassName:    DefaultServerAddrProvider
 * Package:    org.apache.jmeter.shulie.data.pusher
 * Description:
 * Datetime:    2022/5/31   09:43
 * Author:   chenhongqiao@shulie.com
 */
public class DefaultServerAddrProvider implements ServerAddrProvider {
    private final static Logger LOGGER = LoggerFactory.getLogger(io.shulie.jmeter.tool.amdb.log.data.pusher.server.impl.DefaultServerAddrProvider.class.getName());
    private ZkClient zkClient;
    private ZkPathChildrenCache zkServerPath;
    private List<Node> availableNodes;

    public DefaultServerAddrProvider(final ServerProviderOptions serverProviderOptions) {
        this.availableNodes = new ArrayList<>();
        try {
            this.zkClient = NetflixCuratorZkClientFactory.getInstance().create(serverProviderOptions.getSpec());
        } catch (Exception e) {
            LOGGER.error("获取zk连接异常{}",e.getMessage());
        }

        try {
            this.zkClient.ensureParentExists(serverProviderOptions.getServerZkPath());
        } catch (Exception e) {
            LOGGER.error("ensureParentExists err:{}!", serverProviderOptions.getServerZkPath(), e);
        }
        this.zkServerPath = this.zkClient.createPathChildrenCache(serverProviderOptions.getServerZkPath());
        this.zkServerPath.setUpdateExecutor(ExecutorServiceFactory.GLOBAL_EXECUTOR_SERVICE);
        zkServerPath.setUpdateListener(() -> {
            try {
                collectLogServer();
            } catch (Throwable e) {
                LOGGER.error("write log server path err!", e);
            }
        });

        ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!zkServerPath.isRunning()) {
                        zkServerPath.startAndRefresh();
                        LOGGER.info("successfully watch log server path status from zookeeper, path={}",
                                serverProviderOptions.getServerZkPath());
                    }
                    collectLogServer();
                } catch (Throwable e) {
                    LOGGER.error("fail to watch log server path status from zookeeper, path={}. retry next times.",
                            serverProviderOptions.getServerZkPath(), e);
                    ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.schedule(this, 3, TimeUnit.SECONDS);
                }
            }
        }, 5,5, TimeUnit.SECONDS);
        collectLogServer();
    }

    /**
     * 收集日志服务端信息
     */
    private void collectLogServer() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("PRESSURE_ENGINE: start collecting log servers. current servers is {}", availableNodes);
        }

        List<String> addChildren = this.zkServerPath.getAddChildren();

        List<String> removeChildren = this.zkServerPath.getDeleteChildren();
        List<Node> addNodes = new ArrayList<>();
        for (String node : addChildren) {
            if (StringUtils.isBlank(node)) {
                continue;
            }
            if (StringUtils.indexOf(node, ':') == -1) {
                LOGGER.warn("listener add a valid log server,name : {}", node);
                continue;
            }
            String[] addrPort = StringUtils.split(node, ':');
            if (addrPort.length != 2) {
                LOGGER.warn("listener add a valid log server,name : {}", node);
                continue;
            }
            String addr = StringUtils.trim(addrPort[0]);
            String portStr = StringUtils.trim(addrPort[1]);
            if (!NumberUtils.isDigits(portStr)) {
                LOGGER.warn("listener add a valid log server,port is invalid,name : {}", node);
                continue;
            }
            Node n = new Node();
            n.setHost(addr);
            n.setPort(Integer.parseInt(portStr));
            addNodes.add(n);
        }

        List<Node> removeNodes = new ArrayList<>();
        for (String node : removeChildren) {
            if (StringUtils.isBlank(node)) {
                continue;
            }
            if (StringUtils.indexOf(node, ':') == -1) {
                LOGGER.warn("listener remove a valid log server,name : {}", node);
                continue;
            }
            String[] addrPort = StringUtils.split(node, ':');
            if (addrPort.length != 2) {
                LOGGER.warn("listener remove a valid log server,name : {}", node);
                continue;
            }
            String addr = StringUtils.trim(addrPort[0]);
            String portStr = StringUtils.trim(addrPort[1]);
            if (!NumberUtils.isDigits(portStr)) {
                LOGGER.warn("listener remove a valid log server,port is invalid,name : {}", node);
                continue;
            }
            Node n = new Node();
            n.setHost(addr);
            n.setPort(Integer.parseInt(portStr));
            removeNodes.add(n);
        }

        this.availableNodes.removeAll(removeNodes);
        this.availableNodes.addAll(addNodes);

        try {
            if (this.availableNodes.isEmpty()){
                LOGGER.warn("未获取到addNodes，使用listChildren获取");
                List<String> paths = this.zkClient.listChildren(zkServerPath.getPath());
                if (!paths.isEmpty()){
                    for (String path : paths){
                        if (StringUtils.indexOf(path, ":") != -1) {
                            String[] split = path.split(":");
                            Node node = new Node(split[0], Integer.parseInt(split[1]));
                            this.availableNodes.add(node);
                        }
                    }
                }
            }
            this.zkServerPath.refresh();
        } catch (Exception e) {
            LOGGER.error("zkServerPath refresh error {}", e);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("PRESSURE_ENGINE: collect log servers finished. current servers is {}", availableNodes);
        }
    }

    @Override
    public ConnectInfo selectConnectInfo() {
        if (CollectionUtils.isEmpty(this.availableNodes)) {
            LOGGER.error("can't found any available log server nodes!");
            return null;
        }

        Node node = null;
        long total = Long.MAX_VALUE;
        /**
         * 取的逻辑是错误越少并且最近出错的时间越远的节点
         * 计算逻辑取出错次数 * 最近出错时间来进行计算
         */
        for (Node n : this.availableNodes) {
            /**
             * 避免值过大，错误时间只取到秒级即可
             */
            long lastErrorTime = n.getLastErrorTimeSec();
            if (n.getErrorCount() * lastErrorTime < total) {
                node = n;
                total = n.getErrorCount() * lastErrorTime;
            }
        }

        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setServerAddr(node.getHost());
        connectInfo.setPort(node.getPort());
        return connectInfo;
    }

    @Override
    public void errorConnectInfo(ConnectInfo connectInfo) {
        if (CollectionUtils.isEmpty(this.availableNodes)) {
            return;
        }
        if (connectInfo == null) {
            return;
        }
        for (Node node : this.availableNodes) {
            if (StringUtils.equals(node.getHost(), connectInfo.getServerAddr())
                    && node.getPort() == connectInfo.getPort()) {
                node.error();
                break;
            }
        }
    }

    static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
