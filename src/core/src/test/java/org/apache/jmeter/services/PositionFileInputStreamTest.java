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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author vincent
 */
public class PositionFileInputStreamTest {

    private File file;
    private String text;

    @BeforeEach
    void setUp() throws IOException {
        file = new File("/tmp/PositionFileInputStreamTest.txt");
        text = "abcdefghijklmnopqrstuvwxyz\n12\r\n3456\n7890";
        IOUtils.write(text, new FileOutputStream(file), Charset.forName("UTF-8"));

    }

    @AfterEach
    void tearDown() {
        file.deleteOnExit();
    }

    @Test
    /**
     * 跳过
     */
    void skip() throws IOException {

        PositionFileInputStream positionFileInputStream = new PositionFileInputStream(0, text.length(), file);

        Assert.assertEquals(10, positionFileInputStream.skip(10));

        Assert.assertEquals(30, positionFileInputStream.skip(30));

        Assert.assertEquals(0, positionFileInputStream.skip(40));

    }

    @Test
    /**
     * 位置流初始化
     */
    void init() throws IOException {

        PositionFileInputStream positionFileInputStream = new PositionFileInputStream(0, text.length(), file);
        Assert.assertEquals(40, positionFileInputStream.available());

        positionFileInputStream = new PositionFileInputStream(10, text.length(), file);
        Assert.assertEquals(30, positionFileInputStream.available());

        positionFileInputStream = new PositionFileInputStream(10, 20, file);
        Assert.assertEquals(20, positionFileInputStream.available());
    }

    @Test
    void read() throws IOException {
        PositionFileInputStream positionFileInputStream = new PositionFileInputStream(0, text.length(), file);

        Assert.assertEquals((char) positionFileInputStream.read(), 'a');

        Assert.assertEquals((char) positionFileInputStream.read(), 'b');

        positionFileInputStream.skip(23);

        Assert.assertEquals((char) positionFileInputStream.read(), 'z');
        positionFileInputStream.skip(14);

        Assert.assertEquals(positionFileInputStream.read(), -1);


    }

    @Test
    void testRead() throws IOException {

        PositionFileInputStream positionFileInputStream = new PositionFileInputStream(10, text.length(), file);
        text = "klmnopqrstuvwxyz\n12\r\n3456\n7890";
        byte[] bytes = new byte[text.length()];
        int retrun = positionFileInputStream.read(bytes);
        Assert.assertEquals(text, new String(bytes));

        Assert.assertEquals(30, retrun);

        Assert.assertEquals(-1, positionFileInputStream.read());
    }

    @Test
    void testRead1() {
    }

    @Test
    void available() {
    }


}
