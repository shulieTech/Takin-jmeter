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

package org.apache.jmeter.services;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.input.BOMInputStream;


/**
 * 文件指定位置流
 *
 * @author vincent
 * @see BOMInputStream
 */
public class PositionFileInputStream extends FileInputStream {

    private long available;

    /**
     * BOMInputStream fis = new BOMInputStream(new FileInputStream(fileEntry.file)); //NOSONAR
     *
     * @param startPosition
     */
    public PositionFileInputStream(long startPosition, long stopPosition, File file) throws FileNotFoundException {
        super(file);
        init(startPosition, stopPosition);
    }

    /**
     * 初始化
     *
     * @param startPosition
     * @param stopPosition
     */
    public void init(long startPosition, long stopPosition) throws PositionFileInputStreamException {
        try {
            //跳过起始位置
            long skip = super.skip(startPosition);
            if (skip != startPosition) {
                throw new PositionFileInputStreamException("Skip position failed.skip:{" + skip + "},startPosition:{" + startPosition + "}");
            }
        } catch (IOException e) {
            throw new PositionFileInputStreamException("Skip position failed.", e);
        }
//        try {
//            available = super.available();
//            //校验是否大于文件可用字节数
//            if ((stopPosition - startPosition) > this.available()) {
//                throw new PositionFileInputStreamException("Subtract startPosition{" + startPosition + "} from stopPosition{" + stopPosition + "} is larger than file available{" + available + "}.");
//            }
//            available = (int) (stopPosition - startPosition);
            available = stopPosition - startPosition;
//        } catch (IOException e) {
//            throw new PositionFileInputStreamException("Get file available failed.", e);
//        }
    }


    /**
     * Reads up to <code>len</code> bytes of data from this input stream
     * into an array of bytes. If <code>len</code> is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and <code>0</code> is returned.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the file has been reached.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if an I/O error occurs.
     */
    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (available < 0) {
            available = -1;
            return -1;
        }
        int num = super.read(b, off, len);
        if (available < num)
        {
            long tmpAvailable = available;
            available = available - num;
            if (available < 0)
            {
                available = -1;
            }
            return (int)tmpAvailable;
        }
        available = available - num;

        return num;
    }

    /**
     * Reads a byte of data from this input stream. This method blocks
     * if no input is yet available.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * file is reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public synchronized int read() throws IOException {
        if (available < 0) {
            available = -1;
            return -1;
        }
        int num = super.read();
        available = available - 1;
        if (available < 0) {
            available = -1;
            return -1;
        }
        return num;
    }

    /**
     * Reads up to <code>b.length</code> bytes of data from this input
     * stream into an array of bytes. This method blocks until some input
     * is available.
     *
     * @param b the buffer into which the data is read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the file has been reached.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Skips over and discards <code>n</code> bytes of data from the
     * input stream.
     *
     * <p>The <code>skip</code> method may, for a variety of
     * reasons, end up skipping over some smaller number of bytes,
     * possibly <code>0</code>. If <code>n</code> is negative, the method
     * will try to skip backwards. In case the backing file does not support
     * backward skip at its current position, an <code>IOException</code> is
     * thrown. The actual number of bytes skipped is returned. If it skips
     * forwards, it returns a positive value. If it skips backwards, it
     * returns a negative value.
     *
     * <p>This method may skip more bytes than what are remaining in the
     * backing file. This produces no exception and the number of bytes skipped
     * may include some number of bytes that were beyond the EOF of the
     * backing file. Attempting to read from the stream after skipping past
     * the end will result in -1 indicating the end of the file.
     *
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     * @throws IOException if n is negative, if the stream does not
     *                     support seek, or if an I/O error occurs.
     */
    @Override
    public synchronized long skip(long n) throws IOException {
        long skip = super.skip(n);
        available = available - skip;
        if (available < 0) {
            available = 0;
        }
        //去掉跳过的字节数
        return skip;
    }

    /**
     * Returns an estimate of the number of remaining bytes that can be read (or
     * skipped over) from this input stream without blocking by the next
     * invocation of a method for this input stream. Returns 0 when the file
     * position is beyond EOF. The next invocation might be the same thread
     * or another thread. A single read or skip of this many bytes will not
     * block, but may read or skip fewer bytes.
     *
     * <p> In some cases, a non-blocking read (or skip) may appear to be
     * blocked when it is merely slow, for example when reading large
     * files over slow networks.
     *
     * @return an estimate of the number of remaining bytes that can be read
     * (or skipped over) from this input stream without blocking.
     * @throws IOException if this file input stream has been closed by calling
     *                     {@code close} or an I/O error occurs.
     */
    @Override
    public int available() throws IOException {
        return (int)available;
    }

    /**
     * 防止超过int最大值，返回负数
     * @return
     * @throws IOException
     */
    public long longAvailable() throws IOException{
        return available;
    }
}
