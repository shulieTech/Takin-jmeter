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

package org.apache.jmeter.shulie.model;

/**
 * ClassName:    FilePostion
 * Package:    org.apache.jmeter.shulie.model
 * Description:
 * Datetime:    2022/5/11   16:27
 * Author:   chenhongqiao@shulie.com
 */
public class CsvFilePosition {
    private String taskId;        //任务ID
    private String fileName;    //csv文件
    private String podNum;     //pod标记
    private Long startPosition; //开始位置
    private Long readPosition;  //当前读取的位置
    private Long endPosition;   //结束位置

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPodNum() {
        return podNum;
    }

    public void setPodNum(String podNum) {
        this.podNum = podNum;
    }

    public Long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Long startPosition) {
        this.startPosition = startPosition;
    }

    public Long getReadPosition() {
        return readPosition;
    }

    public void setReadPosition(Long readPosition) {
        this.readPosition = readPosition;
    }

    public Long getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Long endPosition) {
        this.endPosition = endPosition;
    }
}
