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

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.util.EntityUtils;
import org.apache.jmeter.report.utils.MetricUtils;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.influxdb.entity.AbstractMetrics;
import org.apache.jmeter.visualizers.backend.influxdb.entity.EventMetrics;
import org.apache.jmeter.visualizers.backend.influxdb.entity.ResponseMetrics;
import org.apache.jmeter.visualizers.backend.influxdb.tro.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

class HttpJsonMetricsSender extends AbstractInfluxdbMetricsSender {
    private static final Logger log = LoggerFactory.getLogger(HttpJsonMetricsSender.class);

    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String AUTHORIZATION_HEADER_VALUE = "Token ";

    //后端监听器链接超时时间
    private static final int BACKEND_CONNECTION_TIMEOUT = 1000;

    //后端监听器链接socket时间
    private static final int BACKEND_SOCKET_TIMEOUT = 1000;

    private final Object lock = new Object();

    private List<AbstractMetrics> metrics = new ArrayList<>();

    private HttpPost httpRequest;
    private CloseableHttpAsyncClient httpClient;
    private URL url;
    private String token;
    private Integer SEND_INTERVAL;

    private Future<HttpResponse> lastRequest;
    private HttpJsonMetricsSenderThread thread;

    HttpJsonMetricsSender() {
        super();
    }

    /**
     * The HTTP API is the primary means of writing data into InfluxDB, by
     * sending POST requests to the /write endpoint. Initiate the HttpClient
     * client with a HttpPost request from influxdb url
     *
     * @param influxdbUrl   example : http://localhost:8086/write?db=myd&rp=one_week
     * @param influxDBToken example: my-token
     * @see InfluxdbMetricsSender#setup(String, String)
     */
    @Override
    public void setup(String influxdbUrl, String influxDBToken) throws Exception {
        // Create I/O reactor configuration
        IOReactorConfig ioReactorConfig = IOReactorConfig
                .custom()
                .setIoThreadCount(1)
                .setConnectTimeout(JMeterUtils.getPropDefault("backend_influxdb.connection_timeout", BACKEND_CONNECTION_TIMEOUT))
                .setSoTimeout(JMeterUtils.getPropDefault("backend_influxdb.socket_timeout", BACKEND_SOCKET_TIMEOUT))
                .build();
        // Create a custom I/O reactor
        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);


        // Create a connection manager with custom configuration.
        PoolingNHttpClientConnectionManager connManager =
                new PoolingNHttpClientConnectionManager(ioReactor);

        httpClient = HttpAsyncClientBuilder.create()
                .setConnectionManager(connManager)
                .setMaxConnPerRoute(2)
                .setMaxConnTotal(2)
                .setUserAgent("ApacheJMeter" + JMeterUtils.getJMeterVersion())
                .disableCookieManagement()
                .disableConnectionState()
                .build();
        url = new URL(influxdbUrl);
        token = influxDBToken;
        SEND_INTERVAL = JMeterUtils.getPropDefault("backend_influxdb.send_interval", 5);
        httpRequest = createRequest(url, token);
        httpClient.start();
    }

    /**
     * @param url   {@link URL} Influxdb Url
     * @param token Influxdb 2.0 authorization token
     * @return {@link HttpPost}
     * @throws URISyntaxException
     */
    private HttpPost createRequest(URL url, String token) throws URISyntaxException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setConnectTimeout(JMeterUtils.getPropDefault("backend_influxdb.connection_timeout", BACKEND_CONNECTION_TIMEOUT))
                .setSocketTimeout(JMeterUtils.getPropDefault("backend_influxdb.socket_timeout", BACKEND_SOCKET_TIMEOUT))
                .setConnectionRequestTimeout(JMeterUtils.getPropDefault("backend_influxdb.connection_request_timeout", 100))
                .build();

        HttpPost currentHttpRequest = new HttpPost(url.toURI());
        currentHttpRequest.setConfig(defaultRequestConfig);
        if (StringUtils.isNotBlank(token)) {
            currentHttpRequest.setHeader(AUTHORIZATION_HEADER_NAME, AUTHORIZATION_HEADER_VALUE + token);
        }
        log.debug("Created Collection centre MetricsSender with url: {}", url);
        return currentHttpRequest;
    }

    @Override
    public void addMetric(ResponseMetrics responseMetrics) {
        synchronized (lock) {
            //add by lipeng for test
            if (log.isDebugEnabled()) {
                log.debug("------------ log activeThreads start ------------");
                //获取当前线程数量
                log.debug(JMeterContextService.getThreadCounts().activeThreads + "");
                log.debug("------------ log activeThreads end ------------");
            }
            //add end
            this.metrics.add(responseMetrics);
        }
    }

    @Override
    public void addEventMetrics(EventMetrics eventMetrics) {
        synchronized (lock) {
            this.metrics.add(eventMetrics);
        }
    }


    @Override
    public void writeAndSendMetrics() {
        List<AbstractMetrics> copyMetrics;
        synchronized (lock) {
            copyMetrics = metrics;
            metrics = new ArrayList<>(copyMetrics.size());
        }
        writeAndSendMetrics(copyMetrics);
    }
    private static final int MAX_RETRY_COUNT = 3;

    //TODO mark by lipeng 这里改为直接向influxdb写数据 而不是传到cloud
    private void writeAndSendMetrics(List<AbstractMetrics> copyMetrics) {
        try {
            if (httpRequest == null) {
                httpRequest = createRequest(url, token);
            }
            //请求数据
            httpRequest.setEntity(new StringEntity(JsonUtil.toJson(copyMetrics), ContentType.APPLICATION_JSON));
            lastRequest = httpClient.execute(httpRequest, new MetricsSenderCallback(httpRequest, MAX_RETRY_COUNT
                    , copyMetrics));
        } catch (URISyntaxException | JsonProcessingException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private class MetricsSenderCallback implements FutureCallback<HttpResponse> {
        private int count;
        private HttpPost httpRequest;
        private List<AbstractMetrics> copyMetrics;
        public MetricsSenderCallback(HttpPost httpRequest,int count,List<AbstractMetrics> copyMetrics) {
            this.count = count;
            this.httpRequest = httpRequest;
            this.copyMetrics = copyMetrics;
        }

        @Override
        public void completed(final HttpResponse response) {
            int code = response.getStatusLine().getStatusCode();
            /*
             * If your write request received HTTP
             * 204 No Content: it was a success!
             * 4xx: InfluxDB could not understand the request.
             * 5xx: The system is overloaded or significantly impaired.
             */
            if (MetricUtils.isSuccessCode(code)) {
                //查看日志
                log.info("Success, number of metrics written: {}", copyMetrics.size());
//                        if (log.isDebugEnabled()) {
//                            log.debug("Success, number of metrics written: {}", copyMetrics.size());
//                        }
            } else {
                log.error(
                        "Error writing metrics to Collection centre Url: {}, responseCode: {}, responseBody: {}",
                        url, code, getBody(response));
            }
        }

        @Override
        public void failed(final Exception ex) {
            count--;
            if (count <0)
            {
                return;
            }
            httpClient.execute(httpRequest, this);
            log.error("failed to send data to Collection centre server.", ex);
        }

        @Override
        public void cancelled() {
            log.warn("Request to Collection centre server was cancelled");
        }
    }

    /**
     * @param response HttpResponse
     * @return String entity Body if any
     */
    private static String getBody(final HttpResponse response) {
        String body = "";
        try {
            if (response != null && response.getEntity() != null) {
                body = EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) { // NOSONAR
            // NOOP
        }
        return body;
    }

    @Override
    public void destroy() {
        // Give some time to send last metrics before shutting down
        log.info("Destroying ");
        try {
            lastRequest.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error waiting for last request to be send to InfluxDB", e);
        }
        if (httpRequest != null) {
            httpRequest.abort();
        }
        IOUtils.closeQuietly(httpClient);
    }

}
