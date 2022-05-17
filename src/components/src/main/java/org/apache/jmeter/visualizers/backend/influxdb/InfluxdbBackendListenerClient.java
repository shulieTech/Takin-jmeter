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

import com.alibaba.fastjson.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.util.DataUtil;
import org.apache.jmeter.shulie.util.JsonUtil;
import org.apache.jmeter.shulie.util.NumberUtil;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jmeter.visualizers.backend.SamplerMetric;
import org.apache.jmeter.visualizers.backend.UserMetric;
import org.apache.jmeter.visualizers.backend.influxdb.entity.BusinessActivityConfig;
import org.apache.jmeter.visualizers.backend.influxdb.entity.EventMetrics;
import org.apache.jmeter.visualizers.backend.influxdb.entity.ResponseMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.jmeter.visualizers.backend.influxdb.entity.Constants.METRICS_EVENTS_ENDED;
import static org.apache.jmeter.visualizers.backend.influxdb.entity.Constants.METRICS_EVENTS_STARTED;

/**
 * Implementation of {@link AbstractBackendListenerClient} to write in an InfluxDB using
 * custom schema
 *
 * @since 3.2
 */
public class InfluxdbBackendListenerClient extends AbstractBackendListenerClient implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(InfluxdbBackendListenerClient.class);
    private static final String TAG_OK = "ok";
    private static final String TAG_KO = "ko";
    private static final String TAG_ALL = "all";
    private static final String CUMULATED_METRICS = "all";
    private static final long SEND_INTERVAL = JMeterUtils.getPropDefault("backend_influxdb.send_interval", 5);
    private static final int MAX_POOL_SIZE = 1;
    private static final String SEPARATOR = ";";
    private static final Object LOCK = new Object();
    private static final Map<String, String> DEFAULT_ARGS = new LinkedHashMap<>();

    static {
        DEFAULT_ARGS.put("influxdbMetricsSender", HttpJsonMetricsSender.class.getName());
        DEFAULT_ARGS.put("influxdbUrl", "http://host_to_change:8086/write?db=jmeter");
        DEFAULT_ARGS.put("application", "application name");
        DEFAULT_ARGS.put("measurement", "jmeter");
        DEFAULT_ARGS.put("summaryOnly", "false");
        DEFAULT_ARGS.put("samplersRegex", ".*");
        DEFAULT_ARGS.put("percentiles", "99;95;90");
        DEFAULT_ARGS.put("testTitle", "Test name");
        DEFAULT_ARGS.put("eventTags", "");
    }

    private final ConcurrentHashMap<String, SamplerMetric> metricsPerSampler = new ConcurrentHashMap<>();
    private boolean summaryOnly;
    private String samplersRegex = "";
    private Pattern samplersToFilter;
    private Map<String, Float> okPercentiles;
    private Map<String, Float> koPercentiles;
    private Map<String, Float> allPercentiles;

    private InfluxdbMetricsSender influxdbMetricsManager;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> timerHandle;

    private Map<String, BusinessActivityConfig> bizMap = new HashMap<>();

    private PrintWriter pw;

    public InfluxdbBackendListenerClient() {
        super();
    }

    /**
     * 使用newScheduledThreadPool线程池 每5S执行一次
     */
    @Override
    public void run() {
        sendMetrics();
    }

    private void sendMetrics() {
        synchronized (LOCK) {
            //算线程数单独拎出来，不要每个活动都去算，提升效率
            //            int activeThreadNum = getActiveThreadNum();
            UserMetric userMetrics = getUserMetrics();
            for (Map.Entry<String, SamplerMetric> entry : metricsPerSampler.entrySet()) {
                String transaction = CUMULATED_METRICS.equals(entry.getKey()) ? CUMULATED_METRICS : AbstractInfluxdbMetricsSender.tagToStringValue(entry.getKey());
                SamplerMetric metric = entry.getValue();
                ResponseMetrics responseMetrics = buildResponseMetricsAndClean(entry.getKey(), metric);
                if (CUMULATED_METRICS.equals(transaction)) {
                    //当transcation为all时返回的saCount均设置为0，因为all的sa count为空，让cloud去聚合all的sacount数据
                    responseMetrics.setSaCount(0);
                    responseMetrics.setActiveThreads(userMetrics.getAllActiveThreadNum());
                }
                influxdbMetricsManager.addMetric(responseMetrics);
                metric.resetForTimeInterval();
            }
            userMetrics.resetForTimeInterval();
        }
        influxdbMetricsManager.writeAndSendMetrics();
    }

    public ResponseMetrics buildResponseMetricsAndClean(String transaction, SamplerMetric metric) {
        ResponseMetrics responseMetrics = new ResponseMetrics();
        responseMetrics.setTransaction(transaction);
        responseMetrics.setCount(metric.getTotal());
        responseMetrics.setFailCount(metric.getFailures());
        responseMetrics.setMaxRt(NumberUtil.maybeNaN(metric.getAllMaxTime()));
        responseMetrics.setMinRt(NumberUtil.maybeNaN(metric.getAllMinTime()));
        responseMetrics.setTimestamp(System.currentTimeMillis());
        responseMetrics.setRt(NumberUtil.maybeNaN(metric.getAllMean()));
        //modify by 李鹏 当transcation为all时返回的saCount均设置为0，因为all的sa count为空，让cloud去聚合all的sacount数据
        // 平台会设置每个业务活动的目标rt，而不会给all设置目标rt，设置目标rt根据脚本后端监听器中的businessMap参数传递过来
        responseMetrics.setSaCount(metric.getSaSuccess());
        //modify end
        String podNumber = System.getProperty("pod.number");
        Map<String, String> tags = new HashMap<>(1);
        tags.put("podNum", podNumber == null ? "" : podNumber);
        responseMetrics.setTags(tags);
        responseMetrics.setSentBytes(metric.getSentBytes());
        responseMetrics.setReceivedBytes(metric.getReceivedBytes());
        responseMetrics.setActiveThreads(metric.getActiveThreads());
        responseMetrics.setErrorInfos(new HashSet<>());
        //add end
        //add by 李鹏 添加sumRt
        responseMetrics.setSumRt(metric.getSumRt());
        //把他放在最后，getPercentMap中有清数据
        responseMetrics.setPercentData(DataUtil.percentMapToString(metric.getPercentMap()));
        return responseMetrics;
    }

    public String getSamplersRegex() {
        return samplersRegex;
    }

    /**
     * @param samplersList the samplersList to set
     */
    public void setSamplersList(String samplersList) {
        this.samplersRegex = samplersList;
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
        synchronized (LOCK) {
            UserMetric userMetrics = getUserMetrics();
            for (SampleResult sampleResult : sampleResults) {
                userMetrics.add(sampleResult);
                addMetric(sampleResult);
                //                Matcher matcher = samplersToFilter.matcher(sampleResult.getSampleLabel());
                //                if (!summaryOnly && (matcher.find())) {
                //                    addMetric(sampleResult);
                //                }
                //                //TODO optimize sf add switch
                //                SamplerMetric cumulatedMetrics = getSamplerMetricInfluxdb(CUMULATED_METRICS, sampleResult.getTransactionUrl());
                //                cumulatedMetrics.addCumulated(sampleResult);
            }
        }
    }

    private void addMetric(SampleResult sampleResult) {
        Matcher matcher = samplersToFilter.matcher(sampleResult.getSampleLabel());
        if (!summaryOnly && (matcher.find())) {
            addMetricSelf(sampleResult);
        }
        if (null != sampleResult.getSubResults() && sampleResult.getSubResults().length > 0) {
            for (SampleResult r : sampleResult.getSubResults()) {
                addMetric(r);
            }
        } else {
            SamplerMetric cumulatedMetrics = getSamplerMetricInfluxdb(CUMULATED_METRICS, sampleResult.getTransactionUrl());
            cumulatedMetrics.addCumulated(sampleResult);
        }
    }

    private void addMetricSelf(SampleResult sampleResult) {
        SamplerMetric samplerMetric = getSamplerMetricInfluxdb(sampleResult.getSampleLabel(), sampleResult.getTransactionUrl());
        samplerMetric.add(sampleResult);
    }

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        summaryOnly = context.getBooleanParameter("summaryOnly", false);
        samplersRegex = context.getParameter("samplersRegex", "");
        String bizArgs = context.getParameter("businessMap", "");
        if (StringUtils.isNotBlank(bizArgs)) {
            bizMap = JsonUtil.parseObject(bizArgs, new TypeReference<Map<String, BusinessActivityConfig>>() {});
        }

        initPercentiles(context);
        initInfluxdbMetricsManager(context);

        samplersToFilter = Pattern.compile(samplersRegex);
        addAnnotation(true);

        scheduler = Executors.newScheduledThreadPool(MAX_POOL_SIZE);
        // Start immediately the scheduler and put the pooling ( 5 seconds by default )
        this.timerHandle = scheduler.scheduleAtFixedRate(this, 0, SEND_INTERVAL, TimeUnit.SECONDS);
        //测试 每500ms获取一次数据
        //        this.timerHandle = scheduler.scheduleAtFixedRate(this, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void initInfluxdbMetricsManager(BackendListenerContext context) throws Exception {
        Class<?> troCloudClazz = Class.forName(context.getParameter("influxdbMetricsSender").trim());
        // 获得实例
        influxdbMetricsManager = (HttpJsonMetricsSender)troCloudClazz.getDeclaredConstructor().newInstance();
        String influxdbUrl = context.getParameter("influxdbUrl").trim();
        String influxdbToken = context.getParameter("influxdbToken");
        String metricsFile = context.getParameter("metricsFile").trim();
        pw = getFileWriter(metricsFile);
        influxdbMetricsManager.setup(influxdbUrl, influxdbToken, pw);
    }

    private void initPercentiles(BackendListenerContext context) {
        String percentilesAsString = context.getParameter("percentiles", "");
        String[] percentilesStringArray = percentilesAsString.split(SEPARATOR);
        okPercentiles = new HashMap<>(percentilesStringArray.length);
        koPercentiles = new HashMap<>(percentilesStringArray.length);
        allPercentiles = new HashMap<>(percentilesStringArray.length);
        DecimalFormat format = new DecimalFormat("0.##");
        for (String percentile : percentilesStringArray) {
            String trimmedPercentile = percentile.trim();
            if (StringUtils.isEmpty(trimmedPercentile)) {
                continue;
            }
            try {
                Float percentileValue = Float.valueOf(trimmedPercentile);
                String key = AbstractInfluxdbMetricsSender.tagToStringValue(format.format(percentileValue));
                okPercentiles.put(key, percentileValue);
                koPercentiles.put(key, percentileValue);
                allPercentiles.put(key, percentileValue);
            } catch (Exception e) {
                log.error("Error parsing percentile: '{}'", percentile, e);
            }
        }
    }

    private SamplerMetric getSamplerMetricInfluxdb(String sampleLabel, String transactionUrl) {
        SamplerMetric samplerMetric = metricsPerSampler.get(sampleLabel);
        if (samplerMetric != null) {
            return samplerMetric;
        }
        SamplerMetric newSamplerMetric = new SamplerMetric();
        //add by 李鹏  添加业务活动url
        newSamplerMetric.setTransactionUrl(transactionUrl);
        String transaction = DataUtil.getTransaction(sampleLabel);
        BusinessActivityConfig config = bizMap.get(transaction);
        newSamplerMetric.setStandRt(DataUtil.getValue(0, config, BusinessActivityConfig::getRt));
        SamplerMetric oldValue = metricsPerSampler.putIfAbsent(sampleLabel, newSamplerMetric);
        if (oldValue != null) {
            newSamplerMetric = oldValue;
        }
        return newSamplerMetric;
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        boolean cancelState = timerHandle.cancel(false);
        log.debug("Canceled state: {}", cancelState);
        scheduler.shutdown();
        try {
            scheduler.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Error waiting for end of scheduler");
            Thread.currentThread().interrupt();
        }

        addAnnotation(false);

        // Send last set of data before ending
        log.info("Sending last metrics");
        sendMetrics();
        influxdbMetricsManager.destroy();
        //metrics文件
        pw.flush();
        pw.close();
        super.teardownTest(context);
    }

    /**
     *
     */
    private void addAnnotation(boolean isStartOfTest) {
        EventMetrics eventMetrics = new EventMetrics();
        eventMetrics.setEventName(isStartOfTest ? METRICS_EVENTS_STARTED : METRICS_EVENTS_ENDED);
        //add by 李鹏 tags添加当前jtl文件名
        Map<String, String> tags = new HashMap<>(1);
        tags.put(PressureConstants.CURRENT_JTL_FILE_NAME_SYSTEM_PROP_KEY
            , System.getProperty(PressureConstants.CURRENT_JTL_FILE_NAME_SYSTEM_PROP_KEY));
        eventMetrics.setTags(tags);
        eventMetrics.setTimestamp(System.currentTimeMillis());
        influxdbMetricsManager.addEventMetrics(eventMetrics);
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        DEFAULT_ARGS.forEach(arguments::addArgument);
        return arguments;
    }

    //    @FunctionalInterface
    //    private interface PercentileProvider {
    //        double getPercentileValue(double percentile);
    //    }

    private PrintWriter getFileWriter(String filename){
        PrintWriter writer = null;
        try {
            File pdir = new File(filename).getParentFile();
            if (pdir != null) {
                // returns false if directory already exists, so need to check again
                if (pdir.mkdirs()) {
                    if (log.isInfoEnabled()) {
                        log.info("Folder at {} was created", pdir.getAbsolutePath());
                    }
                } // else if you might have been created by another process so not a problem
                if (!pdir.exists()) {
                    log.warn("Error creating directories for {}", pdir);
                }
            }
            writer = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(filename,
                    true)), SaveService.getFileEncoding(StandardCharsets.UTF_8.name())), true);
            if (log.isDebugEnabled()) {
                log.debug("Opened file: {} in thread {}", filename, Thread.currentThread().getName());
            }
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            e.printStackTrace();
        }
        return writer;
    }


}
