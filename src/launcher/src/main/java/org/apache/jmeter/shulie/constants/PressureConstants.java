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

package org.apache.jmeter.shulie.constants;

import org.apache.jmeter.shulie.model.PressureEngineParams;

/**
 * 常量类
 *
 * @author lipeng
 * @date 2021-05-11 2:05 下午
 */
public abstract class PressureConstants {

    //引擎启动状态  启动
    public static String ENGINE_STATUS_STARTED = "started";

    //引擎启动状态  启动失败
    public static String ENGINE_STATUS_FAILED = "startFail";

    //压测引擎参数信息实例
    public static PressureEngineParams pressureEngineParamsInstance;

    //当前ptl系统参数key
    public static final String CURRENT_JTL_FILE_NAME_SYSTEM_PROP_KEY = "__CURRENT_PTL_FILE_NAME__";

    //试跑模式code
    public static final String TRY_RUN_MODE_CODE = "5";

    //traceId key
    public static final String TRACE_ID_KEY = "pradarTraceId";
}
