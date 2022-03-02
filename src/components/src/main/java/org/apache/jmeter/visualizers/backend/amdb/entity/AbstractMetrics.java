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

package org.apache.jmeter.visualizers.backend.amdb.entity;

import java.io.Serializable;

import org.apache.jmeter.shulie.util.DataUtil;

/**
 * @author shiyajian
 * create: 2020-10-10
 */
public abstract class AbstractMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 当前pod的序号
     */
    private String podNo = DataUtil.getPodNo();
    /**
     * 是返回数据还是事件数据
     */
    private String type;

    public String getPodNo() {
        return podNo;
    }

    public void setPodNo(String podNo) {
        this.podNo = podNo;
    }

    public AbstractMetrics(String type) {
        this.type = type;
        this.podNo = DataUtil.getPodNo();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
