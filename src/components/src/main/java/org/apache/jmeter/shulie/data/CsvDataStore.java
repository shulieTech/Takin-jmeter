package org.apache.jmeter.shulie.data;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

/**
 * @author 李鹏
 * @date 2021-05-26 1:33 下午
 */
@SuppressWarnings("unused")
public class CsvDataStore {

    /**
     * csv 数据队列
     */
    public static LinkedBlockingQueue<String> csvDataQueue = new LinkedBlockingQueue<>(120);

    /**
     * 文件是否分区
     */
    public static ConcurrentHashMap<String, Boolean> hasPartitionMap = new ConcurrentHashMap<>();

    /**
     * csv 分区队列数据
     */
    private static final ConcurrentHashMap<String, List<LinkedBlockingQueue<String>>> CSV_FILE_CONSUMER_MAP
        = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, String> csvFileConsumerExists = new ConcurrentHashMap<>();

    /**
     * 分区队列是否存在
     *
     * @param filename 文件名称
     * @return 是否存在
     */
    public static boolean isPartitionQueueExists(String filename) {
        return CSV_FILE_CONSUMER_MAP.containsKey(filename);
    }

    /**
     * 添加分区队列
     *
     * @param filename       文件名
     * @param partitionQueue 分区队列
     */
    public static void addPartitionQueue(String filename, LinkedBlockingQueue<String> partitionQueue) {
        //获取分区数据
        List<LinkedBlockingQueue<String>> partitionQueues = CSV_FILE_CONSUMER_MAP.get(filename);
        partitionQueues = Objects.isNull(partitionQueues) ? new ArrayList<>() : partitionQueues;
        partitionQueues.add(partitionQueue);
        CSV_FILE_CONSUMER_MAP.put(filename, partitionQueues);
    }

    /**
     * 消费队列数据
     *
     * @param fileName 文件名
     * @param indexInt 索引
     * @return 数据
     */
    public static String peekPartitionValue(String fileName, AtomicInteger indexInt) {
        //获取分区数据
        List<LinkedBlockingQueue<String>> partitionQueues = CSV_FILE_CONSUMER_MAP.get(fileName);
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
