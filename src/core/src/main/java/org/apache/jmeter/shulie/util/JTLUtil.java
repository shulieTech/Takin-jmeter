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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.PressureJtlFileConfig;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.shulie.util.model.TraceBizData;

import com.alibaba.fastjson.JSON;

/**
 * JTL工具类
 *
 * @author lipeng
 * @date 2021-04-26 2:58 下午
 */
public abstract class JTLUtil {

    //每个字段分隔符
    private static final char QUOTER = "|".charAt(0);

    public static final String EMPTY_TEXT = "";

    //字符串截取长度
    public static final int STRING_TRUNCATE_LENGTH = 100;

    //移除换行符
    private static final Pattern LINE_PATTERN = Pattern.compile("\t|\r|\n|\\|");

    //http协议
    public static List<String> HTTP_AND_HTTPS_PROTOCOL = new ArrayList<String>();

    static {
        HTTP_AND_HTTPS_PROTOCOL.add("http");
        HTTP_AND_HTTPS_PROTOCOL.add("https");
    }

    //traceId请求头前缀
    public static final String TRACE_ID_HEADER_KEY_PREFIX = "p-pradar-traceid: ";

    //reportId请求头前缀
    public static final String REPORT_ID_HEADER_KEY_PREFIX = "p-pradar-userdata: ";

    //压测标
    public static final String[] PERFOMANCE_TEST_HEADERS = {"User-Agent: PerfomanceTest"
            , "src-p-pradar-cluster-test: 1", "src-p-pradar-cluster-test: true"
            , "p-pradar-cluster-test: 1", "p-pradar-cluster-test: true"};

    //压力引擎应用名称
    private static final String PRESSURE_ENGINE_APPLICATION_NAME = "pressure-engine";

    //1/1000 trace will be logged
    public static boolean isForceTraced(String id, int mustSamplingInterval) {
        if (id == null || id.length() == 0) {
            return false;
        }
        if (mustSamplingInterval == 1000 && ('0' == id.charAt(id.length() - 6))
                && ('0' == id.charAt(id.length() - 7)) && ('0' == id.charAt(id.length() - 8))) {
            return true;
        } else if (mustSamplingInterval == 100 && ('0' == id.charAt(id.length() - 6)) && ('0' == id.charAt(id.length() - 7))) {
            return true;
        } else {
            return mustSamplingInterval == 10 && ('0' == id.charAt(id.length() - 6));
        }
    }

    /**
     * 检查当前上下文是否被采样，有效范围在 [1, 9999] 之间，超出范围的数值都作为全采样处理。
     *
     * @return <code>true</code> 则需要输出日志，<code>false</code> 不输出
     */
    public static boolean isTraceSampled(String traceId, final int si) {
        if (traceId == null) {
            return false;
        }

        //一定有一部分的trace id 被全量采集
        if (isForceTraced(traceId, si)) {
            return true;
        }
        if (si <= 1 || si >= 10000) {
            return true;
        }
        if (traceId.length() < 25) {
            return traceId.hashCode() % si == 0;
        }
        int count = traceId.charAt(21) - '0';
        count = count * 10 + traceId.charAt(22) - '0';
        count = count * 10 + traceId.charAt(23) - '0';
        count = count * 10 + traceId.charAt(24) - '0';
        return count % si == 0;
    }


    /**
     * 将sample结果转为存储为jtl的数据格式
     *
     * @param event
     * @param sample
     * @param saveConfig
     * @return
     */
    public static String resultToDelimitedString(SampleEvent event,
                                                 SampleResult sample,
                                                 SampleSaveConfiguration saveConfig,
                                                 TraceBizData traceBizData) {
        CSVSaveService.StringQuoter text = new CSVSaveService.StringQuoter(QUOTER);
        //按大数据格式拼接
        //traceId|startTime|agentId|invokeId|invokeType|appName|cost|middlewareName|serviceName|methodName
        // |resultCode|request|response|flags|callbackMsg|#samplingInterval|@attributes|@localAttributes

        //traceId
        String traceId = traceBizData.getTraceId();
        text.append(traceId);
        //startTime
        if (saveConfig.saveTimestamp()) {
            if (saveConfig.printMilliseconds()) {
                text.append(sample.getTimeStamp());
            } else if (saveConfig.threadSafeLenientFormatter() != null) {
                String stamp = saveConfig.threadSafeLenientFormatter().format(
                        new Date(sample.getTimeStamp()));
                text.append(stamp);
            }
        }
        //agentId
        text.append(PRESSURE_ENGINE_APPLICATION_NAME);
        //invokeId
        text.append("0");
        //invokeType
        text.append("0");
        //appName
        text.append(PRESSURE_ENGINE_APPLICATION_NAME);
        //cost
        text.append(sample.getTime());
        URL url = sample.getURL();
        if (null == url) {
            //mq类型
            text.append(sample.getMqType());
            //serviceName
            text.append(sample.getMqTopic());
            //methodName
            text.append(sample.getMqPartition());
        } else {
            //middlewarename
            text.append(url.getProtocol());
            //serviceName
            text.append(url.getPath());
            //methodName
            text.append(sample.getHTTPMethod());
        }
        //resultCode  00 成功  01 响应失败  05 断言失败
        boolean responseSuccess = "200".equals(sample.getResponseCode());


        //modify by lipeng at 20210426 记录所有断言失败信息 而不是第一个失败信息
        boolean assertFailed = false;
        List<Map<String, String>> assertResultList = new ArrayList<Map<String, String>>();
        AssertionResult[] assertionResults = sample.getAssertionResults();
        int assertionLen = assertionResults.length;
        for (int i = 0; i < assertionLen; i++) {
            AssertionResult item = assertionResults[i];
            if (item.isFailure() || item.isError()) {
                Map<String, String> assertResult = new HashMap<String, String>();
                assertResult.put("assertName", item.getName());
                assertResult.put("assertMessage", item.getFailureMessage());
                assertResultList.add(assertResult);
                assertFailed = true;
            }
        }

        text.append(sample.isSuccessful() ? "00" : responseSuccess && assertFailed ? "05" : "01");
        //request
        //如果报文大于截取长度 需要截取
        String requestString = sample.getQueryString();
        if (PressureJtlFileConfig.defaultConfig.isJtlCutoff() && StringUtils.isNotBlank(requestString) && requestString.length() > STRING_TRUNCATE_LENGTH) {
            requestString = requestString.substring(0, STRING_TRUNCATE_LENGTH) + "..";
        }
        Matcher requestMatcher = LINE_PATTERN.matcher(requestString);
        requestString = requestMatcher.replaceAll(EMPTY_TEXT);
        text.append(requestString);

        //response
        //如果报文大于截取长度 需要截取
        String responseString = EMPTY_TEXT;
        byte[] responseBytes = sample.getResponseData();
        if (PressureJtlFileConfig.defaultConfig.isJtlCutoff() && responseBytes.length > STRING_TRUNCATE_LENGTH) {
            try {
                responseString = new String(responseBytes, 0, STRING_TRUNCATE_LENGTH
                        , sample.getDataEncodingWithDefault()) + "..";
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else {
            responseString = sample.getResponseDataAsString();
        }
        Matcher responseMatcher = LINE_PATTERN.matcher(responseString);
        responseString = responseMatcher.replaceAll(EMPTY_TEXT);
        text.append(responseString);
        //flags
        text.append(traceBizData.isPerfomanceTest() + "~false~false~false~true");
        //callbackMsg
        text.append(assertFailed ? JSON.toJSONString(assertResultList) : EMPTY_TEXT);
        //#samplingInterval
        text.append("#1");
        //@attributes
        text.append("@" + PRESSURE_ENGINE_APPLICATION_NAME + "~~~" + traceBizData.getReportId());
        //@localAttributes
        text.append("@" + PRESSURE_ENGINE_APPLICATION_NAME + "~~~0~0");
        return text.toString();
    }

    public static boolean ifWrite(boolean respResult, long respCost) {
        //不过滤
        if (!PressureJtlFileConfig.defaultConfig.isJtlErrorOnly() && !PressureJtlFileConfig.defaultConfig.isJtlTimeoutOnly()) {
            return true;
        }

        if (PressureJtlFileConfig.defaultConfig.isJtlErrorOnly() && !respResult) {
            return true;
        }
        if (PressureJtlFileConfig.defaultConfig.isJtlTimeoutOnly()
                && PressureJtlFileConfig.defaultConfig.getTimeoutThreshold() > -1
                && respCost >= PressureJtlFileConfig.defaultConfig.getTimeoutThreshold()) {
            return true;
        }
        return false;
    }

}
