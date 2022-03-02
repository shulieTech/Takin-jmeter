package org.apache.jmeter.shulie.model;

/**
 * @Author: liyuanba
 * @Date: 2021/12/2 1:44 下午
 */
public class TkMessage extends AbstractEntry {
    /**
     * 组,topic
     */
    private GroupTopicEnum groupTopic;
    /**
     * 消息标签
     */
    private String tag;
    /**
     * 自定义key，用于去重
     */
    private String key;
    /**
     * 消息体
     */
    private String content;

    public GroupTopicEnum getGroupTopic() {
        return groupTopic;
    }

    public void setGroupTopic(GroupTopicEnum groupTopic) {
        this.groupTopic = groupTopic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        /**
         * 组,topic
         */
        private GroupTopicEnum groupTopic;
        /**
         * 消息标签
         */
        private String tag;
        /**
         * 自定义key，用于去重
         */
        private String key;
        /**
         * 消息体
         */
        private String content;

        public TkMessage build() {
            TkMessage message = new TkMessage();
            message.setGroupTopic(this.groupTopic);
            message.setTag(this.tag);
            message.setKey(this.key);
            message.setContent(this.content);
            return message;
        }

        public Builder setGroupTopic(GroupTopicEnum groupTopic) {
            this.groupTopic = groupTopic;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public GroupTopicEnum getGroupTopic() {
            return groupTopic;
        }

        public String getTag() {
            return tag;
        }

        public String getKey() {
            return key;
        }

        public String getContent() {
            return content;
        }
    }
}
