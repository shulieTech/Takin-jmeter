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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

public interface FileService {


    void resetBase();

    void reserveFile(String filename);

    void reserveFile(String filename, String charsetName);

    void reserveFile(String filename, String charsetName, String alias);

    String reserveFile(String filename, String charsetName, String alias, boolean hasHeader);

    File resolveFileFromPath(String filename);

    String readLine(String filename) throws IOException;

    String readLine(String filename, boolean recycle, boolean ignoreFirstLine) throws IOException;

    String[] getParsedLine(String alias, boolean recycle, boolean ignoreFirstLine, char delim) throws IOException;

    BufferedReader createBufferedReader(FileServer.FileEntry fileEntry, boolean recycle) throws IOException;

    void write(String filename, String value) throws IOException;

    void closeFiles() throws IOException;

    void closeFile(String name) throws IOException;

    File getRandomFile(String basedir, String[] extensions);

    File getResolvedFile(String path);

    void setBasedir(String basedir);

     String getScriptName();

     void setScriptName(String scriptName);


    void reserveFile(long startPosition, long stopPosition, String filename);

    String reserveFile(long startPosition, long stopPosition, String filename, String charsetName, String alias, boolean hasHeader);



}
