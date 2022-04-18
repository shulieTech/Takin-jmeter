package org.apache.jmeter.shulie.constants;

import org.apache.jmeter.shulie.model.PressureEngineParams;

/**
 * 常量类
 *
 * @author 李鹏
 */
@SuppressWarnings("unused")
public class PressureConstants {

    /**
     * 引擎启动状态
     * <strong>启动</strong>
     */
    public static String ENGINE_STATUS_STARTED = "started";

    /**
     * 引擎启动状态
     * <strong>启动失败</strong>
     */
    public static String ENGINE_STATUS_FAILED = "startFail";

    /**
     * 压测引擎参数信息实例
     */
    public static PressureEngineParams pressureEngineParamsInstance;

    /**
     * 当前ptl系统参数key
     */
    public static final String CURRENT_JTL_FILE_NAME_SYSTEM_PROP_KEY = "__CURRENT_PTL_FILE_NAME__";

    /**
     * 调试模式code
     */
    public static final String INSPECTION_MODE_CODE = "4";

    /**
     * 试跑模式code
     */
    public static final String TRY_RUN_MODE_CODE = "5";

    /**
     * traceId key
     */
    public static final String TRACE_ID_KEY = "pradarTraceId";
}
