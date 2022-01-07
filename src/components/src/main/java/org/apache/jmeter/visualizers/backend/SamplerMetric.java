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

package org.apache.jmeter.visualizers.backend;

import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.shulie.util.DataUtil;
import org.apache.jmeter.shulie.util.NumberUtil;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jmeter.visualizers.backend.influxdb.entity.ResponseMetrics;
import org.apache.jorphan.documentation.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sampler metric
 *
 * @since 2.13
 */
public class SamplerMetric {
    private static final Logger logger = LoggerFactory.getLogger(SamplerMetric.class);
    private static final int SLIDING_WINDOW_SIZE = JMeterUtils.getPropDefault("backend_metrics_window", 100);
    private static final int LARGE_SLIDING_WINDOW_SIZE = JMeterUtils.getPropDefault("backend_metrics_large_window",
            5000);

    private static volatile WindowMode globalWindowMode = WindowMode.get();

    /**
     * Response times for OK samples
     */
    private DescriptiveStatistics okResponsesStats = new DescriptiveStatistics(LARGE_SLIDING_WINDOW_SIZE);
    /**
     * Response times for KO samples
     */
    private DescriptiveStatistics koResponsesStats = new DescriptiveStatistics(LARGE_SLIDING_WINDOW_SIZE);
    /**
     * Response times for All samples
     */
    private DescriptiveStatistics allResponsesStats = new DescriptiveStatistics(LARGE_SLIDING_WINDOW_SIZE);
    /**
     * OK, KO, ALL stats
     */
    private List<DescriptiveStatistics> windowedStats = initWindowedStats();
    /**
     * Timeboxed percentiles don't makes sense
     */
    private DescriptiveStatistics pctResponseStats = new DescriptiveStatistics(SLIDING_WINDOW_SIZE);
    private int successes;
    private int failures;
    private int hits;
    private Map<ErrorMetric, Integer> errors = new HashMap<>();
    private long sentBytes;
    private long receivedBytes;
    /**
     * 压测返回的 RT ，是否达到页面设置的 RT 值，如果小于等于，则认为是成功的。
     */
    private int saSuccess;

    /**
     * 页面根据业务活动设置的标准 RT 值
     */
    private int standRt;

    /**
     * 添加sumRt 增加计算rt精准性
     * add by lipeng
     */
    private long sumRt;

    /**
     * 添加业务活动URL
     * add by lipeng
     */
    private String transactionUrl;
    /**
     * 累加的活跃线程数
     */
    private long sumActiveThreads = 0;
    /**
     * add 次数
     */
    private long count = 0;

    /**
     *
     */
    public SamplerMetric(int standRt) {
        logger.info("SamplerMetric globalWindowMode="+globalWindowMode);
        // Limit to sliding window of SLIDING_WINDOW_SIZE values for FIXED mode
        if (globalWindowMode == WindowMode.FIXED) {
            for (DescriptiveStatistics stat : windowedStats) {
                stat.setWindowSize(SLIDING_WINDOW_SIZE);
            }
        }
        this.standRt = standRt;
    }

    public SamplerMetric() {
        logger.info("SamplerMetric globalWindowMode="+globalWindowMode);
        // Limit to sliding window of SLIDING_WINDOW_SIZE values for FIXED mode
        if (globalWindowMode == WindowMode.FIXED) {
            for (DescriptiveStatistics stat : windowedStats) {
                stat.setWindowSize(SLIDING_WINDOW_SIZE);
            }
        }
        this.standRt = 0;
    }

    public long getSumRt() {
        return sumRt;
    }

    /**
     * Set {@link WindowMode} to use for newly created metrics.
     *
     * @param windowMode new visibility mode
     * @deprecated only used for internal testing
     */
    @Deprecated
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static void setDefaultWindowMode(WindowMode windowMode) {
        logger.info("setDefaultWindowMode globalWindowMode="+globalWindowMode);
        globalWindowMode = windowMode;
    }

    /**
     * @return List of {@link DescriptiveStatistics}
     */
    private List<DescriptiveStatistics> initWindowedStats() {
        return Arrays.asList(okResponsesStats, koResponsesStats, allResponsesStats);
    }

    /**
     * Add a {@link SampleResult} to be used in the statistics
     *
     * @param result {@link SampleResult} to be used
     */
    public synchronized void add(SampleResult result) {
        add(result, false);
    }

    /**
     * Add a {@link SampleResult} and its sub-results to be used in the statistics
     *
     * @param result {@link SampleResult} to be used
     */
    public synchronized void addCumulated(SampleResult result) {
        add(result, true);
    }


    /**
     * Add a {@link SampleResult} to be used in the statistics
     *
     * @param result      {@link SampleResult} to be used
     * @param isCumulated is the overall Sampler Metric
     */
    private synchronized void add(SampleResult result, boolean isCumulated) {
        sumActiveThreads += result.getGroupThreads();
        count++;
        if (result.isSuccessful()) {
            successes += result.getSampleCount() - result.getErrorCount();
        } else {
            failures += result.getErrorCount();
            ErrorMetric error = new ErrorMetric(result);
            errors.put(error, errors.getOrDefault(error, 0) + result.getErrorCount());
        }
        long time = result.getTime();

        allResponsesStats.addValue(time);
        pctResponseStats.addValue(time);
        if (result.isSuccessful()) {
            // Should we also compute KO , all response time ?
            // only take successful requests for time computing
            okResponsesStats.addValue(time);
            if (time <= standRt) {
                saSuccess += result.getSampleCount() - result.getErrorCount();
            }
        } else {
            koResponsesStats.addValue(time);
        }
        addHits(result, isCumulated);
        addNetworkData(result, isCumulated);
        //添加sumRt计算 add by lipeng
        this.sumRt += result.getTime();
    }

    /**
     * Increment traffic metrics. A Parent sampler cumulates its children metrics.
     *
     * @param result      SampleResult
     * @param isCumulated related to the overall sampler metric
     */
    private void addNetworkData(SampleResult result, boolean isCumulated) {
        if (isCumulated && TransactionController.isFromTransactionController(result)
                && result.getSubResults().length == 0) { // Transaction controller without generate parent sampler
            return;
        }
        sentBytes += result.getSentBytes();
        receivedBytes += result.getBytesAsLong();
    }

    /**
     * Compute hits from result
     *
     * @param result      {@link SampleResult}
     * @param isCumulated related to the overall sampler metric
     */
    private void addHits(SampleResult result, boolean isCumulated) {
        SampleResult[] subResults = result.getSubResults();
        if (isCumulated && TransactionController.isFromTransactionController(result)
                && subResults.length == 0) { // Transaction controller without generate parent sampler
            return;
        }
        if (!(TransactionController.isFromTransactionController(result) && subResults.length > 0)) {
            hits += result.getSampleCount();
        }
        for (SampleResult subResult : subResults) {
            addHits(subResult, isCumulated);
        }
    }

    /**
     * 计算点击1-100%的点位的耗时Map
     */
    public synchronized Map<Integer, Map<String, Number>> getPercentMap() {
        double[] values = allResponsesStats.getSortedValues();
        if (null == values || values.length <= 0) {
            return null;
        }

        Map<Integer, Map<String, Number>> distributes = new HashMap<>();
        int lastIdx = 0;
        int count = 0;
        for (int i=1; i<=100; i++) {
            int idx = (int)Math.ceil(values.length*i/100d);
            idx = Math.max(idx, 1);
            idx = Math.min(idx, values.length);
            double v = values[idx-1];
            for (int j=lastIdx; j<values.length; j++) {
                lastIdx = j;
                if (values[j] > v) {
                    break;
                }
                count++;
            }
            Map<String, Number> item = new HashMap<>();
            item.put("count", count);
            item.put("rt", v);
            distributes.put(i, item);
        }
        return distributes;
    }

    /**
     * Reset metric except for percentile related data
     */
    public synchronized void resetForTimeInterval() {
        switch (globalWindowMode) {
            case FIXED:
                // We don't clear responsesStats nor usersStats as it will slide as per my understanding of
                // http://commons.apache.org/proper/commons-math/userguide/stat.html
                break;
            case TIMED:
                for (DescriptiveStatistics stat : windowedStats) {
                    stat.clear();
                }
                break;
            default:
                // This cannot happen
        }
        errors.clear();
        successes = 0;
        failures = 0;
        hits = 0;
        sentBytes = 0;
        saSuccess = 0;
        receivedBytes = 0;
        sumRt = 0;
        sumActiveThreads = 0;
        count = 0;
    }

    public int getActiveThreads() {
        return (int) Math.round(NumberUtil.divide(sumActiveThreads, count));
    }

    /**
     * Get the number of total requests for the current time slot
     *
     * @return number of total requests
     */
    public int getTotal() {
        return successes + failures;
    }

    /**
     * Get the number of successful requests for the current time slot
     *
     * @return number of successful requests
     */
    public int getSuccesses() {
        return successes;
    }

    /**
     * Get the number of failed requests for the current time slot
     *
     * @return number of failed requests
     */
    public int getFailures() {
        return failures;
    }

    /**
     * Get the maximal elapsed time for requests within sliding window
     *
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     * been added yet
     */
    public double getOkMaxTime() {
        return okResponsesStats.getMax();
    }

    /**
     * Get the minimal elapsed time for requests within sliding window
     *
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     * added yet
     */
    public double getOkMinTime() {
        return okResponsesStats.getMin();
    }

    /**
     * Get the arithmetic mean of the stored values
     *
     * @return The arithmetic mean of the stored values
     */
    public double getOkMean() {
        return okResponsesStats.getMean();
    }

    /**
     * Returns an estimate for the requested percentile of the stored values.
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     * values.
     */
    public double getOkPercentile(double percentile) {
        return okResponsesStats.getPercentile(percentile);
    }

    /**
     * Get the maximal elapsed time for requests within sliding window
     *
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     * been added yet
     */
    public double getKoMaxTime() {
        return koResponsesStats.getMax();
    }

    /**
     * Get the minimal elapsed time for requests within sliding window
     *
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     * added yet
     */
    public double getKoMinTime() {
        return koResponsesStats.getMin();
    }

    /**
     * Get the arithmetic mean of the stored values
     *
     * @return The arithmetic mean of the stored values
     */
    public double getKoMean() {
        return koResponsesStats.getMean();
    }

    /**
     * Returns an estimate for the requested percentile of the stored values.
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     * values.
     */
    public double getKoPercentile(double percentile) {
        return koResponsesStats.getPercentile(percentile);
    }

    /**
     * Get the maximal elapsed time for requests within sliding window
     *
     * @return the maximal elapsed time, or <code>0</code> if no requests have
     * been added yet
     */
    public double getAllMaxTime() {
        return allResponsesStats.getMax();
    }

    /**
     * Get the minimal elapsed time for requests within sliding window
     *
     * @return the minTime, or {@link Long#MAX_VALUE} if no requests have been
     * added yet
     */

    public double getAllMinTime() {
        return allResponsesStats.getMin();
    }

    /**
     * Get the arithmetic mean of the stored values
     *
     * @return The arithmetic mean of the stored values
     */
    public double getAllMean() {
        return allResponsesStats.getMean();
    }

    /**
     * Returns an estimate for the requested percentile of the stored values.
     *
     * @param percentile the requested percentile (scaled from 0 - 100)
     * @return Returns an estimate for the requested percentile of the stored
     * values.
     */
    public double getAllPercentile(double percentile) {
        return pctResponseStats.getPercentile(percentile);
    }

    /**
     * Returns hits to server
     *
     * @return the hits
     */
    public int getHits() {
        return hits;
    }

    /**
     * Returns by type ( response code and message ) the count of errors occurs
     *
     * @return errors
     */
    public Map<ErrorMetric, Integer> getErrors() {
        return errors;
    }

    /**
     * @return the sentBytes
     */
    public long getSentBytes() {
        return sentBytes;
    }

    /**
     * @return the receivedBytes
     */
    public long getReceivedBytes() {
        return receivedBytes;
    }

    public int getSaSuccess() {
        return saSuccess;
    }

    public void setSaSuccess(int saSuccess) {
        this.saSuccess = saSuccess;
    }

    public int getStandRt() {
        return standRt;
    }

    public void setStandRt(int standRt) {
        this.standRt = standRt;
    }

    public String getTransactionUrl() {
        return transactionUrl;
    }

    public void setTransactionUrl(String transactionUrl) {
        this.transactionUrl = transactionUrl;
    }
}
