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

package org.apache.jmeter;

// N.B. this must only use standard Java packages

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.jmeter.shulie.constants.PressureConstants;
import org.apache.jmeter.shulie.model.PressureEngineParams;
import org.apache.jmeter.shulie.util.HttpNotifyTroCloudUtils;

/**
 * Main class for JMeter - sets up initial classpath and the loader.
 */
public final class NewDriver {

    private static final String CLASSPATH_SEPARATOR = File.pathSeparator;

    private static final String OS_NAME = System.getProperty("os.name");

    private static final String OS_NAME_LC = OS_NAME.toLowerCase(java.util.Locale.ENGLISH);

    private static final String JAVA_CLASS_PATH = "java.class.path";

    private static final String JMETER_LOGFILE_SYSTEM_PROPERTY = "jmeter.logfile";

    private static final String HEADLESS_MODE_PROPERTY = "java.awt.headless";
    /**
     * The class loader to use for loading JMeter classes.
     */
    private static final DynamicClassLoader loader;

    /**
     * The directory JMeter is installed in.
     */
    private static final String JMETER_INSTALLATION_DIRECTORY;

    private static final List<Exception> EXCEPTIONS_IN_INIT = new ArrayList<>();

    static {
        final List<URL> jars = new LinkedList<>();
        final String initialClasspath = System.getProperty(JAVA_CLASS_PATH);

        // Find JMeter home dir from the initial classpath
        String tmpDir;
        StringTokenizer tok = new StringTokenizer(initialClasspath, File.pathSeparator);
        // Java on Mac OS can add a second entry to the initial classpath
        if (tok.countTokens() == 1
            || (tok.countTokens() == 2 && OS_NAME_LC.startsWith("mac os x"))) {
            File jar = new File(tok.nextToken());
            try {
                tmpDir = jar.getCanonicalFile().getParentFile().getParent();
            } catch (IOException e) {
                tmpDir = null;
            }
        }
        // e.g. started from IDE with full classpath
        else {
            // Allow override $NON-NLS-1$ $NON-NLS-2$
            tmpDir = System.getProperty("jmeter.home", System.getenv("JMETER_HOME"));
            if (tmpDir == null || tmpDir.length() == 0) {
                File userDir = new File(System.getProperty("user.dir"));
                tmpDir = userDir.getAbsoluteFile().getParent();
            }
        }
        if (tmpDir == null) {
            tmpDir = System.getenv("JMETER_HOME");
        }
        JMETER_INSTALLATION_DIRECTORY = tmpDir;

        /*
         * Does the system support UNC paths? If so, may need to fix them up
         * later
         */
        boolean usesUNC = OS_NAME_LC.startsWith("windows");

        // Add standard jar locations to initial classpath
        StringBuilder classpath = new StringBuilder();
        File[] libDirs = new File[] {new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib"),
            new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "ext"),
            new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "junit")};
        for (File libDir : libDirs) {
            File[] libJars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (libJars == null) {
                new Throwable("Could not access " + libDir).printStackTrace();
                continue;
            }
            // Bug 50708 Ensure predictable order of jars
            Arrays.sort(libJars);
            for (File libJar : libJars) {
                try {
                    String s = libJar.getPath();

                    // Fix path to allow the use of UNC URLs
                    if (usesUNC) {
                        if (s.startsWith("\\\\") && !s.startsWith("\\\\\\")) {
                            s = "\\\\" + s;
                        } else if (s.startsWith("//") && !s.startsWith("///")) {
                            s = "//" + s;
                        }
                    }

                    // See Java bug 4496398
                    jars.add(new File(s).toURI().toURL());
                    classpath.append(CLASSPATH_SEPARATOR);
                    classpath.append(s);
                } catch (MalformedURLException e) {
                    EXCEPTIONS_IN_INIT.add(new Exception("Error adding jar:" + libJar.getAbsolutePath(), e));
                }
            }
        }

        // ClassFinder needs the classpath
        System.setProperty(JAVA_CLASS_PATH, initialClasspath + classpath);
        loader = AccessController.doPrivileged(
            (PrivilegedAction<DynamicClassLoader>)() ->
                new DynamicClassLoader(jars.toArray(new URL[jars.size()]))
        );
    }

    /**
     * Prevent instantiation.
     */
    private NewDriver() {
    }

    /**
     * Generate an array of jar files located in a directory.
     * Jar files located in sub directories will not be added.
     *
     * @param dir to search for the jar files.
     */
    private static File[] listJars(File dir) {
        if (dir.isDirectory()) {
            return dir.listFiles((f, name) -> {
                if (name.endsWith(".jar")) {
                    File jar = new File(f, name);
                    return jar.isFile() && jar.canRead();
                }
                return false;
            });
        }
        return new File[0];
    }

    /**
     * Add a URL to the loader classpath only; does not update the system classpath.
     *
     * @param path to be added.
     * @throws MalformedURLException when <code>path</code> points to an invalid url
     */
    public static void addURL(String path) throws MalformedURLException {
        File furl = new File(path);
        // See Java bug 4496398
        loader.addURL(furl.toURI().toURL());
        File[] jars = listJars(furl);
        for (File jar : jars) {
            // See Java bug 4496398
            loader.addURL(jar.toURI().toURL());
        }
    }

    /**
     * Add a URL to the loader classpath only; does not update the system
     * classpath.
     *
     * @param url The {@link URL} to add to the classpath
     */
    public static void addURL(URL url) {
        loader.addURL(url);
    }

    /**
     * Add a directory or jar to the loader and system classpaths.
     *
     * @param path to add to the loader and system classpath
     * @throws MalformedURLException if <code>path</code> can not be transformed to a valid
     * {@link URL}
     */
    public static void addPath(String path) throws MalformedURLException {
        File file = new File(path);
        // Ensure that directory URLs end in "/"
        if (file.isDirectory() && !path.endsWith("/")) {
            file = new File(path + "/");
        }
        // See Java bug 4496398
        loader.addURL(file.toURI().toURL());
        StringBuilder sb = new StringBuilder(System.getProperty(JAVA_CLASS_PATH));
        sb.append(CLASSPATH_SEPARATOR);
        sb.append(path);
        File[] jars = listJars(file);
        for (File jar : jars) {
            // See Java bug 4496398
            loader.addURL(jar.toURI().toURL());
            sb.append(CLASSPATH_SEPARATOR);
            sb.append(jar.getPath());
        }

        // ClassFinder needs this
        System.setProperty(JAVA_CLASS_PATH, sb.toString());
    }

    /**
     * Get the directory where JMeter is installed. This is the absolute path
     * name.
     *
     * @return the directory where JMeter is installed.
     */
    public static String getJMeterDir() {
        return JMETER_INSTALLATION_DIRECTORY;
    }

    /**
     * The main program which actually runs JMeter.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        HttpNotifyTroCloudUtils.init(loader);
        //add by 李鹏 添加callbackurl及参数
        PressureConstants.pressureEngineParamsInstance = getPressureEngineParams(args);
        // add end
        if (!EXCEPTIONS_IN_INIT.isEmpty()) {
            String excetionsMsg = exceptionsToString(EXCEPTIONS_IN_INIT);
            // NOSONAR Intentional System.err use
            System.err.println("Configuration error during init, see exceptions:" + excetionsMsg);
            //add by 李鹏 错误信息上报cloud
            HttpNotifyTroCloudUtils.notifyTroCloud(PressureConstants.pressureEngineParamsInstance, PressureConstants.ENGINE_STATUS_FAILED, excetionsMsg);
        } else {
            Thread.currentThread().setContextClassLoader(loader);
            setLoggingProperties(args);

            try {
                // Only set property if it has not been set explicitly
                if (System.getProperty(HEADLESS_MODE_PROPERTY) == null && shouldBeHeadless(args)) {
                    System.setProperty(HEADLESS_MODE_PROPERTY, "true");
                }
                Class<?> initialClass = loader.loadClass("org.apache.jmeter.JMeter");
                Object instance = initialClass.getDeclaredConstructor().newInstance();
                Method startup = initialClass.getMethod("start", PressureConstants.pressureEngineParamsInstance.getClass());
                //modify by 李鹏 将start方法入参改为pressureEngineParams
                startup.invoke(instance, PressureConstants.pressureEngineParamsInstance);
                //正常启动后不需要上报cloud已正常启动，在receive接口会上报started标记
            }
            //  We want to log home directory in case of exception
            catch (Throwable e) {
                e.printStackTrace(); //   No logger at this step
                //  Intentional System.err use
                System.err.println("JMeter home directory was detected as: " + JMETER_INSTALLATION_DIRECTORY);
                //add by 李鹏 错误信息上报cloud
                HttpNotifyTroCloudUtils.notifyTroCloud(PressureConstants.pressureEngineParamsInstance, PressureConstants.ENGINE_STATUS_FAILED, throwableToString(e));
            }
        }
    }

    /**
     * 获取引擎参数信息
     *
     * @param args 参数
     * @return 引擎参数
     */
    private static PressureEngineParams getPressureEngineParams(String[] args) {
        PressureEngineParams result = new PressureEngineParams();
        //sceneId
        String sceneId = System.getProperty("SceneId", "0");
        result.setSceneId(sceneId);
        System.setProperty("SCENE_ID", sceneId + "");
        //reportId
        long reportId = Long.parseLong(System.getProperty("ReportId", "0"));
        result.setResultId(reportId);
        System.setProperty("__ENGINE_REPORT_ID__", reportId + "");
        //customerId
        long customerId = Long.parseLong(System.getProperty("CustomerId", "0"));
        result.setCustomerId(customerId);
        //采样率
        int samplingInterval = Integer.parseInt(System.getProperty("SamplingInterval", "0"));
        result.setSamplingInterval(samplingInterval);
        //podNumber
        String podNumber = System.getProperty("pod.number", "1");
        result.setPodNumber(podNumber);
        System.setProperty("pod.number", podNumber);
        //callbackUrl
        result.setCallbackUrl(System.getProperty("CallbackUrl"));
        //jmeter args
        result.setJmeterArgs(args);
        return result;
    }

    /**
     * @param throwable List of {@link Throwable}
     * @return String
     */
    private static String throwableToString(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        builder.append(stringWriter)
            .append("\r\n");
        return builder.toString();
    }

    /**
     * @param exceptionsInInit List of {@link Exception}
     * @return String
     */
    private static String exceptionsToString(List<Exception> exceptionsInInit) {
        StringBuilder builder = new StringBuilder();
        for (Exception exception : exceptionsInInit) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            builder.append(stringWriter)
                .append("\r\n");
        }
        return builder.toString();
    }

    /*
     * Set logging related system properties.
     */
    private static void setLoggingProperties(String[] args) {
        String jmLogFile = getCommandLineArgument(args, 'j', "jmeterlogfile");
        if (jmLogFile != null && !jmLogFile.isEmpty()) {
            jmLogFile = replaceDateFormatInFileName(jmLogFile);
            System.setProperty(JMETER_LOGFILE_SYSTEM_PROPERTY, jmLogFile);
        } else if (System.getProperty(JMETER_LOGFILE_SYSTEM_PROPERTY) == null) {
            System.setProperty(JMETER_LOGFILE_SYSTEM_PROPERTY, "jmeter.log");
        }

        String jmLogConf = getCommandLineArgument(args, 'i', "jmeterlogconf");
        File logConfFile = null;

        if (jmLogConf != null && !jmLogConf.isEmpty()) {
            logConfFile = new File(jmLogConf);
        } else if (System.getProperty("log4j.configurationFile") == null) {
            logConfFile = new File("log4j2.xml");
            if (!logConfFile.isFile()) {
                logConfFile = new File(JMETER_INSTALLATION_DIRECTORY, "bin" + File.separator + "log4j2.xml");
            }
        }

        if (logConfFile != null) {
            System.setProperty("log4j.configurationFile", logConfFile.toURI().toString());
        }
    }

    private static boolean shouldBeHeadless(String[] args) {
        for (String arg : args) {
            if ("-n".equals(arg) || "-s".equals(arg) || "-g".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Find command line argument option value by the id and name.
     */
    private static String getCommandLineArgument(String[] args, int id, String name) {
        final String shortArgName = "-" + ((char)id);
        final String longArgName = "--" + name;

        String value = null;

        for (int i = 0; i < args.length; i++) {
            if ((shortArgName.equals(args[i]) && i < args.length - 1)
                || longArgName.equals(args[i])) {
                if (!args[i + 1].startsWith("-")) {
                    value = args[i + 1];
                }
                break;
            } else if (!shortArgName.equals(args[i]) && args[i].startsWith(shortArgName)) {
                value = args[i].substring(shortArgName.length());
                break;
            }
        }

        return value;
    }

    /*
     * If the fileName contains at least one set of paired single-quotes, reformat using DateFormat
     */
    private static String replaceDateFormatInFileName(String fileName) {
        try {
            StringBuilder builder = new StringBuilder();

            final Date date = new Date();
            int fromIndex = 0;
            int begin = fileName.indexOf('\'', fromIndex);
            int end;

            String format;
            SimpleDateFormat dateFormat;

            while (begin != -1) {
                builder.append(fileName, fromIndex, begin);

                fromIndex = begin + 1;
                end = fileName.indexOf('\'', fromIndex);
                if (end == -1) {
                    throw new IllegalArgumentException("Invalid pairs of single-quotes in the file name: " + fileName);
                }

                format = fileName.substring(begin + 1, end);
                dateFormat = new SimpleDateFormat(format);
                builder.append(dateFormat.format(date));

                fromIndex = end + 1;
                begin = fileName.indexOf('\'', fromIndex);
            }

            if (fromIndex < fileName.length() - 1) {
                builder.append(fileName.substring(fromIndex));
            }

            return builder.toString();
        } catch (Exception ex) {
            System.err.println("Error replacing date format in file name:" + fileName + ", error:" + ex.getMessage());
        }

        return fileName;
    }
}
