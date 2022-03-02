package org.apache.jmeter.shulie.model;

/**
 * @Author: liyuanba
 * @Date: 2022/1/25 9:31 上午
 */
public class GroupTopicEnum extends AbstractEntry {
    /**
     * 组
     */
    private String group;
    /**
     * TOPIC
     */
    private String topic;

    public GroupTopicEnum() {
    }

    public GroupTopicEnum(String group, String topic) {
        this.group = group;
        this.topic = topic;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean equals(GroupTopicEnum obj) {
        boolean r = super.equals(obj);
        if (!r) {
            r = this.group.equals(obj.getGroup()) && this.topic.equals(obj.getTopic());
        }
        return r;
    }
}
