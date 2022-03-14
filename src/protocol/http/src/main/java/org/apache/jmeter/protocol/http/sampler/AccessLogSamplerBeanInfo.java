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

package org.apache.jmeter.protocol.http.sampler;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.jmeter.protocol.http.util.accesslog.Filter;
import org.apache.jmeter.protocol.http.util.accesslog.LogParser;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.FileEditor;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.reflect.ClassFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessLogSamplerBeanInfo extends BeanInfoSupport {
    private static final Logger log = LoggerFactory.getLogger(AccessLogSamplerBeanInfo.class);
    private static final List<String> LOG_PARSER_CLASSES = logParsers();

    private static List<String> logParsers() {
        try {
            return ClassFinder.findClassesThatExtend(JMeterUtils.getSearchPaths(), new Class[] { LogParser.class });
        } catch (IOException e) {
            log.warn("Could not find log parsers.", e);
            return Collections.emptyList();
        }
    }

    public AccessLogSamplerBeanInfo() {
        super(AccessLogSampler.class);
        log.debug("Entered access log sampler bean info");
        try {
            createPropertyGroup("defaults", 
                    new String[] { "protocol", "domain", "portString", "imageParsing" });// $NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$ $NON-NLS-4$

            createPropertyGroup("plugins", 
                    new String[] { "parserClassName", "filterClassName" }); // $NON-NLS-1$ $NON-NLS-2$ $NON-NLS-3$

            createPropertyGroup("accesslogfile", 
                    new String[] { "logFile" });

            PropertyDescriptor p;

            p = property("parserClassName");
            p.setValue(NOT_UNDEFINED, Boolean.TRUE);
            p.setValue(DEFAULT, AccessLogSampler.DEFAULT_CLASS);
            p.setValue(NOT_OTHER, Boolean.TRUE);
            p.setValue(NOT_EXPRESSION, Boolean.TRUE);

            log.debug("found parsers: {}", LOG_PARSER_CLASSES);
            p.setValue(TAGS, LOG_PARSER_CLASSES.toArray(new String[LOG_PARSER_CLASSES.size()]));

            p = property("filterClassName");
            p.setValue(NOT_UNDEFINED, Boolean.FALSE);
            p.setValue(DEFAULT, "");
            p.setValue(NOT_EXPRESSION, Boolean.TRUE);
            List<String> classes = ClassFinder.findClassesThatExtend(JMeterUtils.getSearchPaths(),
                    new Class[] { Filter.class }, false);
            p.setValue(TAGS, classes.toArray(new String[classes.size()]));

            p = property("logFile");
            p.setValue(NOT_UNDEFINED, Boolean.TRUE);
            p.setValue(DEFAULT, "");
            p.setPropertyEditorClass(FileEditor.class);

            p = property("domain");
            p.setValue(NOT_UNDEFINED, Boolean.TRUE);
            p.setValue(DEFAULT, "");

            p = property("protocol");
            p.setValue(NOT_UNDEFINED, Boolean.TRUE);
            p.setValue(DEFAULT, "http");
            p.setValue(DEFAULT_NOT_SAVED, Boolean.TRUE);

            p = property("portString");
            p.setValue(NOT_UNDEFINED, Boolean.TRUE);
            p.setValue(DEFAULT, "");

            p = property("imageParsing");
            p.setValue(NOT_UNDEFINED, Boolean.TRUE);
            p.setValue(DEFAULT, Boolean.FALSE);
            p.setValue(NOT_OTHER, Boolean.TRUE);
        } catch (IOException e) {
            log.warn("couldn't find classes and set up properties", e);
            throw new RuntimeException("Could not find classes with class finder", e);
        }
        log.debug("Got to end of access log sampler bean info init");
    }

}
