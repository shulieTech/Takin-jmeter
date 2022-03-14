package org.apache.jmeter.shulie.consts;

/**
 * 吞吐量常量类
 *
 * @author 李鹏
 */
@SuppressWarnings("unused")
public class ThroughputConstants {
    /**
     * REDIS 业务活动吞吐量百分比 KEY 格式化串
     */
    public static final String REDIS_ACTIVITY_PERCENTAGE_KEY_FORMAT = "__REDIS_TPS_LIMIT_KEY_%s_%s_%s_%s_";
    /**
     * REDIS TPS KEY
     */
    public static String redisActivityPercentageKey;
    /**
     * 是否TPS模式
     */
    public static boolean IS_TPS_MODE = false;
    /**
     * TPS模式 值
     */
    public static String ENGINE_PRESSURE_MODE_TPS_VALUE = "1";
    /**
     * 脚本中testName中的xpathMd5的分割符
     */
    public static String TEST_NAME_MD5_SPLIT = "@MD5:";

}
