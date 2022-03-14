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

package org.apache.jmeter.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.jmeter.testbeans.TestBean;

/**
 * Parent class to handle common GUI design for JSR223 test elements
 */
public abstract class JSR223BeanInfoSupport extends ScriptingBeanInfoSupport {

    private static final String[] LANGUAGE_TAGS;

    /**
     * Will be removed in next version following 3.2
     * @deprecated use {@link JSR223BeanInfoSupport#getLanguageNames()}
     */
    @Deprecated
    public static final String[][] LANGUAGE_NAMES; // NOSONAR Kept for backward compatibility

    private static final String[][] CONSTANT_LANGUAGE_NAMES;

    static {
        Map<String, ScriptEngineFactory> nameMap = new HashMap<>();
        ScriptEngineManager sem = new ScriptEngineManager();
        final List<ScriptEngineFactory> engineFactories = sem.getEngineFactories();
        for(ScriptEngineFactory fact : engineFactories){
            List<String> names = fact.getNames();
            for(String shortName : names) {
                nameMap.put(shortName.toLowerCase(Locale.ENGLISH), fact);
            }
        }
        LANGUAGE_TAGS = nameMap.keySet().toArray(new String[nameMap.size()]);
        Arrays.sort(LANGUAGE_TAGS);
        CONSTANT_LANGUAGE_NAMES = new String[nameMap.size()][2];
        int i = 0;
        for(Entry<String, ScriptEngineFactory> me : nameMap.entrySet()) {
            final String key = me.getKey();
            CONSTANT_LANGUAGE_NAMES[i][0] = key;
            final ScriptEngineFactory fact = me.getValue();
            CONSTANT_LANGUAGE_NAMES[i++][1] = key +
                    "     ("
                    + fact.getLanguageName() + " " + fact.getLanguageVersion()
                    + " / "
                    + fact.getEngineName() + " " + fact.getEngineVersion()
                    + ")";
        }

        LANGUAGE_NAMES = getLanguageNames(); // NOSONAR Kept for backward compatibility
    }

    private static final ResourceBundle NAME_BUNDLE = new ListResourceBundle() {
        @Override
        protected Object[][] getContents() {
            return CONSTANT_LANGUAGE_NAMES;
        }
    };

    protected JSR223BeanInfoSupport(Class<? extends TestBean> beanClass) {
        super(beanClass, LANGUAGE_TAGS, NAME_BUNDLE);
    }

    /**
     * @return String array of 2 columns array containing Script engine short name / Script Language details
     */
    public static final String[][] getLanguageNames() {
        return CONSTANT_LANGUAGE_NAMES.clone();
    }

}
