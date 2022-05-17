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

package org.apache.jmeter.visualizers.backend.influxdb;

import org.apache.jmeter.visualizers.backend.influxdb.entity.AbstractMetrics;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpJsonMetricsSenderThread {
    private static final Logger log = LoggerFactory.getLogger(HttpJsonMetricsSenderThread.class);
    private LinkedBlockingQueue<List<AbstractMetrics>> queue = new LinkedBlockingQueue<>();
    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);
    private AtomicBoolean started = new AtomicBoolean(false);
    private HttpJsonMetricsSender sender;
    private AtomicInteger queueSize = new AtomicInteger(0);
    //销毁阻塞
    private CyclicBarrier destroyCb;

    public HttpJsonMetricsSenderThread(HttpJsonMetricsSender sender) {
        this.sender = sender;
    }

    public void send(List<AbstractMetrics> metrics) {
        queueSize.incrementAndGet();
        queue.add(metrics);
//        if (started.compareAndSet(false, true)) {
//            start();
//        }
    }

    public void start() {
        PrintWriter pw = null;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                super.run();
                pw.flush();
                pw.close();
            }
        });
        Runnable runnable = () -> {
            for (; ; ) {
                List<AbstractMetrics> metrics = null;
                try {
                    metrics = queue.take();
                } catch (InterruptedException e) {
                    log.error("Error take metrics from queue!queue.size=" + queue.size());
                }
                int times = 1;//重试次数
                if (null != metrics && metrics.size() > 0 && !sender.writeAndSendMetrics(metrics, times)) {
                    long t = System.currentTimeMillis();
                    do {
                        try {
                            log.error("retry send data times:" + (++times) + ",t=" + (System.currentTimeMillis() - t));
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            log.error("Thread sleep error!", e);
                        }
                    } while (!sender.writeAndSendMetrics(metrics, times));
                }
                queueSize.decrementAndGet();
                if (null != destroyCb && queue.size() <= 0) {
                    await();
                }
            }
        };
        fixedThreadPool.execute(runnable);
    }

    public void await() {
        try {
            if (null == destroyCb) {
                destroyCb = new CyclicBarrier(2);
            }
            destroyCb.await();
        } catch (InterruptedException e) {
            log.error("CyclicBarrier await error!", e);
        } catch (BrokenBarrierException e) {
            log.error("CyclicBarrier await error!", e);
        }
    }

    /**
     * 确保数据全部发送,否则阻塞进程
     */
    public void destroy() {
        log.info("start to destroy!");
        if (queueSize.get() > 0) {
            log.info("destroy blocked, queue is not empty!queueSize=" + queueSize.get());
            await();
            log.info("destroy block releaseed!");
        }
        log.info("destroyed!");
    }
}
