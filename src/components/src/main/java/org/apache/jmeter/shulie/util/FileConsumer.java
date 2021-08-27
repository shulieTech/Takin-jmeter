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

package org.apache.jmeter.shulie.util;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.shulie.data.CSVDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 零拷贝读取文件
 *
 * @author lipeng
 * @date 2021-05-25 2:37 下午
 */
public class FileConsumer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(FileConsumer.class);

    private static final Charset defaultCharset = StandardCharsets.UTF_8;

    private RandomAccessFile rAccessFile;

    //缓冲大小
    private int bufferSize;

    //开始结束
    private StartEndPair startEndPair;

    //字符集
    private Charset charset;

    //当前需读的总大小
    private long sliceSize;

    //是否循环读取
    private boolean recycle;

    //读取行数
    private int consumeCount;

    //分几个映射
    private int bufferCount;

    //内存映射缓冲
    private MappedByteBuffer[] mappedByteBuffers;

    /**
     * 字节缓冲
     */
    private byte[] byteBuffer;
    /**
     * 当前读到的映射
     */
    private int bufferCountIndex = 0;

    private ByteArrayOutputStream bos;

    /**
     * 分区
     */
    private String partition;

    private Boolean isSliceBigger;

    //分区队列
    private LinkedBlockingQueue<String> partitionQueue;

    public FileConsumer(String filePath, int bufferSize, StartEndPair startEndPair, String fileEncoding
            , boolean recycle, LinkedBlockingQueue<String> partitionQueue) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("csv文件未找到 [{}]", file.getAbsolutePath());
        }
        try {
            this.rAccessFile = new RandomAccessFile(file, "r");
            this.sliceSize = startEndPair.getEnd() - startEndPair.getStart() + 1;
            if (this.sliceSize >= Integer.MAX_VALUE ){
                isSliceBigger = true;
                this.bufferCount = (int) Math.ceil((double) this.sliceSize / (double) Integer.MAX_VALUE);
                this.mappedByteBuffers = new MappedByteBuffer[this.bufferCount];
                long preLength = startEndPair.getStart();
                long regionSize = Integer.MAX_VALUE;
                for (int i = 0; i < this.bufferCount; i++) {
                    if (startEndPair.getEnd() - preLength < Integer.MAX_VALUE) {
                        regionSize = startEndPair.getEnd() - preLength;
                    }
                    mappedByteBuffers[i] = this.rAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, preLength, regionSize);
                    preLength += regionSize;
                }
            }else {
                isSliceBigger = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.partition = startEndPair.getPartition();
        this.bufferSize = bufferSize;
        this.startEndPair = startEndPair;
        this.charset = StringUtils.isBlank(fileEncoding) ? defaultCharset : Charset.forName(fileEncoding);
        this.recycle = recycle;
        this.bos = new ByteArrayOutputStream();
        this.partitionQueue = partitionQueue;
    }

    /**
     * 不在内部循环
     *
     * @return
     */
    public synchronized int read() {
        if (bufferCountIndex >= bufferCount) {
            return -1;
        }
        int limit = mappedByteBuffers[bufferCountIndex].limit();
        int position = mappedByteBuffers[bufferCountIndex].position();

        int realSize = bufferSize;
        if (limit - position < bufferSize) {
            realSize = limit - position;
        }
        byteBuffer = new byte[realSize];
        mappedByteBuffers[bufferCountIndex].get(byteBuffer);

        //current fragment is end, goto next fragment start.
        if (realSize < bufferSize && bufferCountIndex < bufferCount) {
            bufferCountIndex++;
        }
        return realSize;
    }

    /**
     * 获取当前读到的字节
     *
     * @return
     */
    public synchronized byte[] getCurrentBytes() {
        return byteBuffer;
    }

    /**
     * 处理读到的数据
     */
    public synchronized void handleBytes() {
        try {
            for (byte tmp : getCurrentBytes()) {
                if (tmp == '\n' || tmp == '\r') {
                    handleLine(this.bos.toByteArray());
                    this.bos.reset();
                } else {
                    this.bos.write(tmp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 重置初始化参数
     *
     * @throws IOException
     */
    public synchronized void reset() throws IOException {
        this.bufferCountIndex = 0;
        long preLength = startEndPair.getStart();
        long regionSize = Integer.MAX_VALUE;
        for (int i = 0; i < this.bufferCount; i++) {
            if (startEndPair.getEnd() - preLength < Integer.MAX_VALUE) {
                regionSize = startEndPair.getEnd() - preLength;
            }
            mappedByteBuffers[i] = this.rAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY, preLength, regionSize);
            preLength += regionSize;
        }
    }

    /**
     * 较小的文件读取
     * @throws RuntimeException
     */
    public synchronized void consume() throws RuntimeException {
        if (this.sliceSize > Integer.MAX_VALUE) {
            throw new RuntimeException("需要处理的字节数量超出可处理的最大值");
        }
        //初始化buffer
        byte[] readBuff = new byte[this.bufferSize];
        ByteArrayOutputStream bos;
        try {
            MappedByteBuffer mapBuffer = this.rAccessFile.getChannel()
                    .map(FileChannel.MapMode.READ_ONLY, startEndPair.getStart(), this.sliceSize);
            bos = new ByteArrayOutputStream();
            for (int offset = 0; offset < this.sliceSize; ) {
                //如果队列长度高于50就先将队列消费，消费到低于50再往队列写数据
                if (CSVDataStore.csvDataQuene.size() > 50) {
                    TimeUnit.MILLISECONDS.sleep(200);
                    continue;
                }
                int readLength;
                //当前还没有读完
                if (offset + this.bufferSize <= this.sliceSize) {
                    readLength = this.bufferSize;
                }
                //读完
                else {
                    readLength = (int) (this.sliceSize - offset);
                }
                //mapbuffer中读取
                mapBuffer.get(readBuff, 0, readLength);
                //每个字节读取，读到一行为止
                for (int i = 0; i < readLength; i++) {
                    byte tmp = readBuff[i];
                    if (tmp == '\n' || tmp == '\r') {
                        handleLine(bos.toByteArray());
                        bos.reset();
                    } else {
                        bos.write(tmp);
                    }
                }
                //如果剩余未读完 处理掉
                if (bos.size() > 0) {
                    handleLine(bos.toByteArray());
                    bos.reset();
                }
                //设置offset 应该是+实际读取的长度
                offset += readLength;
                //如果读完文件并且设置了循环读取 需要重置offset和mapBuffer
                if (offset >= this.sliceSize && this.recycle) {
                    offset = 0;
                    mapBuffer = rAccessFile.getChannel().map(FileChannel.MapMode.READ_ONLY
                            , startEndPair.getStart(), this.sliceSize);
                }
            }
            log.info("处理完成【{}】条数据", this.consumeCount);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            close(rAccessFile);
        }
    }

    private synchronized void consume0() {
        //未读到指定的文件位置就继续读,这里设置循环读
        while (read() != -1) {
            handleBytes();
        }
        //处理最后一次读取到的内容
        if (this.bos.size() > 0) {
            handleLine(bos.toByteArray());
            bos.reset();
        }
        //如果是循环读取，需要重置初始化参数
        if (this.recycle) {
            try {
                reset();
            } catch (IOException e) {
                e.printStackTrace();
            }
            consume0();
        }
        this.bos = null;
        this.close(this.rAccessFile);
    }

    /**
     * 处理行数据
     *
     * @param bytes
     */
    private void handleLine(byte[] bytes) {
        //行数据
        String line = new String(bytes, this.charset);
        if (StringUtils.isNotBlank(line)) {
            if (StringUtils.isNotBlank(this.partition)) {
                line = line + "," + this.partition;
            }
            //添加数据到队列,阻塞方式
            try {
                this.partitionQueue.put(line);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.consumeCount++;
        }
    }

    /**
     *
     */
    public void close(Closeable... closes) {
        try {
            for (MappedByteBuffer mappedByteBuffer : mappedByteBuffers) {
                mappedByteBuffer.clear();
            }
            for (Closeable clo : closes) {
                clo.close();
            }
            byteBuffer = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (isSliceBigger) {
            consume0();
        }else {
            consume();
        }
    }
}
