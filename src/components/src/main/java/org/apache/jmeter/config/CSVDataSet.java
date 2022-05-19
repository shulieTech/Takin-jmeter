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

package org.apache.jmeter.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.shulie.jmeter.tool.executors.ExecutorServiceFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoConfigMerge;
import org.apache.jmeter.gui.GUIMenuSortOrder;
import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.save.CSVSaveService;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.services.FileService;
import org.apache.jmeter.services.PositionFileInputStream;
import org.apache.jmeter.services.PositionFileServer;
import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.model.CsvFilePosition;
import org.apache.jmeter.shulie.util.HttpUtil;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testbeans.gui.GenericTestBeanCustomizer;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.jorphan.util.JOrphanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Read lines from a file and split int variables.
 * <p>
 * The iterationStart() method is used to set up each set of values.
 * <p>
 * By default, the same file is shared between all threads
 * (and other thread groups, if they use the same file name).
 * <p>
 * The shareMode can be set to:
 * <ul>
 * <li>All threads - default, as described above</li>
 * <li>Current thread group</li>
 * <li>Current thread</li>
 * <li>Identifier - all threads sharing the same identifier</li>
 * </ul>
 * <p>
 * The class uses the FileServer alias mechanism to provide the different share modes.
 * For all threads, the file alias is set to the file name.
 * Otherwise, a suffix is appended to the filename to make it unique within the required context.
 * For current thread group, the thread group identityHashcode is used;
 * for individual threads, the thread hashcode is used as the suffix.
 * Or the user can provide their own suffix, in which case the file is shared between all
 * threads with the same suffix.
 */
@GUIMenuSortOrder(1)
@TestElementMetadata(labelResource = "displayName")
public class CSVDataSet extends ConfigTestElement
    implements TestBean, LoopIterationListener, NoConfigMerge {
    private static final Logger log = LoggerFactory.getLogger(CSVDataSet.class);

    private static final long serialVersionUID = 233L;

    private static final String EOFVALUE = // value to return at EOF
        JMeterUtils.getPropDefault("csvdataset.eofstring", "<EOF>"); //$NON-NLS-1$ //$NON-NLS-2$

    private static final String DEFAULT_VALUE = "DEFAULT";

    private volatile static ConcurrentHashMap<String, String> csvFileLockMap = new ConcurrentHashMap<>();

    private transient String filename;

    private transient String fileEncoding;

    private transient String variableNames;

    private transient String delimiter;

    private transient boolean quoted;

    private transient boolean recycle = true;

    private transient boolean stopThread;

    private transient String[] vars;

    private transient String alias;

    private transient String shareMode;

    private boolean firstLineIsNames = false;

    private boolean ignoreFirstLine = false;

    private Object readResolve() {
        recycle = true;
        return this;
    }

    /**
     * Override the setProperty method in order to convert
     * the original String shareMode property.
     * This used the locale-dependent display value, so caused
     * problems when the language was changed.
     * If the "shareMode" value matches a resource value then it is converted
     * into the resource key.
     * To reduce the need to look up resources, we only attempt to
     * convert values with spaces in them, as these are almost certainly
     * not variables (and they are definitely not resource keys).
     */
    @Override
    public void setProperty(JMeterProperty property) {
        if (!(property instanceof StringProperty)) {
            super.setProperty(property);
            return;
        }

        final String propName = property.getName();
        if (!"shareMode".equals(propName)) {
            super.setProperty(property);
            return;
        }

        final String propValue = property.getStringValue();
        if (propValue.contains(" ")) { // variables are unlikely to contain spaces, so most likely a translation
            try {
                final BeanInfo beanInfo = Introspector.getBeanInfo(this.getClass());
                final ResourceBundle rb = (ResourceBundle)beanInfo.getBeanDescriptor().getValue(GenericTestBeanCustomizer.RESOURCE_BUNDLE);
                for (String resKey : CSVDataSetBeanInfo.getShareTags()) {
                    if (propValue.equals(rb.getString(resKey))) {
                        if (log.isDebugEnabled()) {
                            log.debug("Converted {}={} to {} using Locale: {}", propName, propValue, resKey, rb.getLocale());
                        }
                        ((StringProperty)property).setValue(resKey); // reset the value
                        super.setProperty(property);
                        return;
                    }
                }
                // This could perhaps be a variable name
                log.warn("Could not translate {}={} using Locale: {}", propName, propValue, rb.getLocale());
            } catch (IntrospectionException e) {
                log.error("Could not find BeanInfo; cannot translate shareMode entries", e);
            }
        }
        super.setProperty(property);
    }

    @Override
    public void iterationStart(LoopIterationEvent iterEvent) {
        boolean hasFilePosition = hasFilePosition();
        final JMeterContext context = getThreadContext();
        String delim = getDelimiter();
        if ("\\t".equals(delim)) {
            delim = "\t";// Make it easier to enter a Tab
        } else if (delim.isEmpty()) {
            log.debug("Empty delimiter, will use ','");
            delim = ",";
        }
        FileService server;
        if (hasFilePosition) {
            server = PositionFileServer.getFileServer();
            if (vars == null) {
                initVars(server, context, delim);
            }
        } else {
            server = FileServer.getFileServer();
            if (vars == null) {
                initVarsWithoutPosition(server, context, delim);
            }
        }

        // 读取csv数据  mark by 李鹏
        // TODO: fetch this once as per vars above?
        JMeterVariables threadVars = context.getVariables();
        String[] lineValues = {};
        try {
            if (getQuotedData()) {
                lineValues = server.getParsedLine(alias, recycle,
                    firstLineIsNames || ignoreFirstLine, delim.charAt(0));
            } else {
                String line = server.readLine(alias, recycle,
                    firstLineIsNames || ignoreFirstLine);
                lineValues = JOrphanUtils.split(line, delim, false);
            }
            for (int a = 0; a < vars.length && a < lineValues.length; a++) {
                threadVars.put(vars[a], lineValues[a]);
            }
        } catch (IOException e) { // treat the same as EOF
            log.error(e.toString());
        }
        //文件名
        String fileName = getFilename().trim();
        //每个file初始化一次
        if (hasFilePosition && csvFileLockMap.putIfAbsent(fileName, DEFAULT_VALUE) == null) {
            fileName = filename.substring(fileName.lastIndexOf("/") + 1);
            PositionFileInputStream inputStream = PositionFileServer.positionMap.get(fileName);
            if (Objects.nonNull(inputStream)) {
                cachePosition(inputStream, fileName);
            }
        }
        if (lineValues.length == 0) {// i.e. EOF
            if (getStopThread()) {
                throw new JMeterStopThreadException("End of file:" + getFilename() + " detected for CSV DataSet:"
                    + getName() + " configured with stopThread:" + getStopThread() + ", recycle:" + getRecycle());
            }
            for (String var : vars) {
                threadVars.put(var, EOFVALUE);
            }
        }
    }

    /**
     * 判断变量中是否存在文件的位点信息
     *
     * @return -
     */
    private boolean hasFilePosition() {
        JMeterVariables variables = getThreadContext().getVariables();
        String variableMapStr = variables.get("__ENGINE_GLOBAL_VARIABLES__");
        if (StringUtils.isBlank(variableMapStr)) {
            return false;
        }
        String filename = getFilename().trim();
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        return variableMapStr.contains(filename);
    }

    protected void initVars(FileService server, final JMeterContext context, String delim) {
        String fileName = getFilename().trim();
        setAlias(context, fileName);
        Pair<Long, Long> position = getPosition(context, fileName);
        final String names = getVariableNames();
        if (StringUtils.isEmpty(names)) {
            String header = server.reserveFile(position.getLeft(), position.getRight(), fileName, getFileEncoding(), alias, true);
            try {
                vars = CSVSaveService.csvSplitString(header, delim.charAt(0));
                firstLineIsNames = true;
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not split CSV header line from file:" + fileName, e);
            }
        } else {
            server.reserveFile(position.getLeft(), position.getRight(), fileName, getFileEncoding(), alias, ignoreFirstLine);
            vars = JOrphanUtils.split(names, ",");
        }
        trimVarNames(vars);
    }

    private void initVarsWithoutPosition(FileService server, final JMeterContext context, String delim) {
        String fileName = getFilename().trim();
        setAlias(context, fileName);
        final String names = getVariableNames();
        if (StringUtils.isEmpty(names)) {
            String header = server.reserveFile(fileName, getFileEncoding(), alias, true);
            try {
                vars = CSVSaveService.csvSplitString(header, delim.charAt(0));
                firstLineIsNames = true;
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not split CSV header line from file:" + fileName, e);
            }
        } else {
            server.reserveFile(fileName, getFileEncoding(), alias, ignoreFirstLine);
            vars = JOrphanUtils.split(names, ",");
        }
        trimVarNames(vars);
    }

    private void setAlias(final JMeterContext context, String fileName) {
        String mode = getShareMode();
        int modeInt = CSVDataSetBeanInfo.getShareModeAsInt(mode);
        switch (modeInt) {
            case CSVDataSetBeanInfo.SHARE_ALL:
                alias = fileName;
                break;
            case CSVDataSetBeanInfo.SHARE_GROUP:
                alias = fileName + "@" + System.identityHashCode(context.getThreadGroup());
                break;
            case CSVDataSetBeanInfo.SHARE_THREAD:
                alias = fileName + "@" + System.identityHashCode(context.getThread());
                break;
            default:
                alias = fileName + "@" + mode; // user-specified key
                break;
        }
    }

    /**
     * 获取位置信息
     *
     * @param context JMeter上下文
     */
    private Pair<Long, Long> getPosition(final JMeterContext context, String filename) {
        JMeterVariables variables = getThreadContext().getVariables();
        String variableMapStr = variables.get("__ENGINE_GLOBAL_VARIABLES__");
        if (StringUtils.isBlank(variableMapStr)) {
            return null;
        }
        System.setProperty("positionVariablesStr", variableMapStr);
        log.info("获取到文件信息：{}", variableMapStr);
        JSONObject positionJson = JSONObject.parseObject(variableMapStr);
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        JSONObject object = positionJson.getJSONObject(filename);
        if (null == object) {
            return null;
        }
        if (object.containsKey("start") && object.containsKey("end")) {
            return Pair.of(object.getLong("start"), object.getLong("end"));
        }
        return null;
    }

    public void cachePosition(PositionFileInputStream inputStream, String fileName) {
        String key = String.format("CSV_READ_POSITION_%s", PressureConstants.pressureEngineParamsInstance.getSceneId());
        String url = System.getProperty("__ENGINE_CSV_POSITION_URL__", "");
        //TaskId
        Long tId = PressureConstants.pressureEngineParamsInstance.getResultId();
        String taskId = null != tId ? String.valueOf(tId) : System.getProperty("__ENGINE_REPORT_ID__");
        String podNum = StringUtils.isBlank(System.getProperty("pod.number")) ? "1" : System.getProperty("pod.number");
        String field = String.format("%s_pod_num_%s", fileName, podNum);
        System.setProperty("SCENE_ID", PressureConstants.pressureEngineParamsInstance.getSceneId() + "");
        Pair<Long, Long> pair = getPosition(null, fileName);
        final long startPosition = pair.getLeft();
        final long endPosition = pair.getRight();
//        RedisUtil redisUtil = JedisUtil.getRedisUtil();
        ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.scheduleAtFixedRate(() -> {
            try {
                long position;
                long available = inputStream.longAvailable();
                if (available <= 0) {
                    available = 0;
                }
                position = endPosition - available;
//                Map<String, Long> value = new HashMap<>(3);
//                value.put("startPosition", startPosition);
//                value.put("readPosition", position);
//                value.put("endPosition", endPosition);
//                log.info("缓存文件读取位点信息{}", value);
                CsvFilePosition csvFilePosition = new CsvFilePosition() {{
                    setTaskId(taskId);
                    setFileName(fileName);
                    setPodNum(podNum);
                    setStartPosition(startPosition);
                    setReadPosition(position);
                    setEndPosition(endPosition);
                }};
                log.info("缓存文件读取位点信息{}", csvFilePosition);
                HttpUtil.post(url, JSON.parseObject(JSON.toJSONString(csvFilePosition)));
//                redisUtil.hset(key, field, JSON.toJSONString(value));
            } catch (IOException e) {
                log.error("获取可读文件大小失败{}", e.getMessage());
            }

        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * trim content of array varNames
     *
     * @param varsNames 变量名
     */
    private void trimVarNames(String[] varsNames) {
        for (int i = 0; i < varsNames.length; i++) {
            varsNames[i] = varsNames[i].trim();
        }
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename The filename to set.
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return Returns the file encoding.
     */
    public String getFileEncoding() {
        return fileEncoding;
    }

    /**
     * @param fileEncoding The fileEncoding to set.
     */
    public void setFileEncoding(String fileEncoding) {
        this.fileEncoding = fileEncoding;
    }

    /**
     * @return Returns the variableNames.
     */
    public String getVariableNames() {
        return variableNames;
    }

    /**
     * @param variableNames The variableNames to set.
     */
    public void setVariableNames(String variableNames) {
        this.variableNames = variableNames;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public boolean getQuotedData() {
        return quoted;
    }

    public void setQuotedData(boolean quoted) {
        this.quoted = quoted;
    }

    public boolean getRecycle() {
        return recycle;
    }

    public void setRecycle(boolean recycle) {
        this.recycle = recycle;
    }

    public boolean getStopThread() {
        return stopThread;
    }

    public void setStopThread(boolean value) {
        this.stopThread = value;
    }

    public String getShareMode() {
        return shareMode;
    }

    public void setShareMode(String value) {
        this.shareMode = value;
    }

    /**
     * @return the ignoreFirstLine
     */
    public boolean isIgnoreFirstLine() {
        return ignoreFirstLine;
    }

    /**
     * @param ignoreFirstLine the ignoreFirstLine to set
     */
    public void setIgnoreFirstLine(boolean ignoreFirstLine) {
        this.ignoreFirstLine = ignoreFirstLine;
    }
}
