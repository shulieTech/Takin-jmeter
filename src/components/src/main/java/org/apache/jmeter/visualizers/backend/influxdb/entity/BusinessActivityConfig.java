package org.apache.jmeter.visualizers.backend.influxdb.entity;


import java.io.Serializable;

/**
 * @Author: liyuanba
 * @Date: 2021/11/8 2:09 下午
 */
public class BusinessActivityConfig implements Serializable {
    /**
     * 绑定关系
     */
    private String bindRef;
    /**
     * 业务活动名称
     */
    private String activityName;
    /**
     * 业务指标，目标rt
     */
    private Integer rt;
    /**
     * 业务指标，目标tps
     */
    private Integer tps;
    /**
     * 业务目标tps占总的tps百分比
     */
    private Double rate;

    public String getBindRef() {
        return bindRef;
    }

    public void setBindRef(String bindRef) {
        this.bindRef = bindRef;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public Integer getRt() {
        return rt;
    }

    public void setRt(Integer rt) {
        this.rt = rt;
    }

    public Integer getTps() {
        return tps;
    }

    public void setTps(Integer tps) {
        this.tps = tps;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }
}
