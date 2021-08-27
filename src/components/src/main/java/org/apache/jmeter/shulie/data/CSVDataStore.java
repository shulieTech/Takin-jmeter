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

package org.apache.jmeter.shulie.data;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lipeng
 * @date 2021-05-26 1:33 下午
 */
public class CSVDataStore {

    //csv 数据队列
    public static LinkedBlockingQueue<String> csvDataQuene = new LinkedBlockingQueue<>(120);

    //文件是否分区
    public static ConcurrentHashMap<String,Boolean> hasPartitionMap = new ConcurrentHashMap<>();

    //csv 分区队列数据
    private static ConcurrentHashMap<String, List<LinkedBlockingQueue<String>>> csvFileConsumerMap = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> csvFileConsumerExists = new ConcurrentHashMap<>();

    /**
     * 分区队列是否存在
     *
     * @param filename
     * @return
     */
    public static boolean isPartitionQueneExists(String filename) {
        return csvFileConsumerMap.containsKey(filename);
    }

    /**
     * 添加分区队列
     *
     * @param filename
     * @param partitionQuene
     */
    public static void addPartitionQuene(String filename, LinkedBlockingQueue<String> partitionQuene) {
        //获取分区数据
        List<LinkedBlockingQueue<String>> partitionQuenes = csvFileConsumerMap.get(filename);
        partitionQuenes = Objects.isNull(partitionQuenes) ? new ArrayList<>() : partitionQuenes;
        partitionQuenes.add(partitionQuene);
        csvFileConsumerMap.put(filename,partitionQuenes);
    }

    /**
     * 消费队列数据
     *
     * @param fileName
     * @return
     */
    public static String peekPartitionValue(String fileName, AtomicInteger indexInt) {
        //获取分区数据
        List<LinkedBlockingQueue<String>> partitionQueues = csvFileConsumerMap.get(fileName);
        if (partitionQueues != null && partitionQueues.size() > 0) {
            int size = partitionQueues.size();
            int preIndex = indexInt.get();
            int index = (preIndex + 1) % size;
            LinkedBlockingQueue<String> queue = partitionQueues.get(index);
            indexInt.getAndSet(index);
            while (queue == null || queue.size() <= 0) {
                peekPartitionValue(fileName, indexInt);
            }
            String line = queue.poll();
            while (StringUtils.isBlank(line)) {
                //如果当前队列中没有数据，等待一下之后再去获取其他队列的数据
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                peekPartitionValue(fileName, indexInt);
            }
            return line;
        }
        return null;
    }

}
