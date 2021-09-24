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

import org.apache.commons.io.input.BOMInputStream;
import org.apache.jorphan.util.JOrphanUtils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 位置新的文件服务
 * @author vincent
 */
public class PositionFileServer extends FileServer {
    private static final PositionFileServer server = new PositionFileServer();

    /**
     * @return the singleton instance of the server.
     */
    public static PositionFileServer getFileServer() {
        return server;
    }

    public static ConcurrentHashMap<String, PositionFileInputStream> positionMap = new ConcurrentHashMap<>();


    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename - relative (to base) or absolute file name (must not be null)
     */
    @Override
    public void reserveFile(long startPosition, long stopPosition, String filename) {
        reserveFile(startPosition, stopPosition, filename, null);
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename    - relative (to base) or absolute file name (must not be null)
     * @param charsetName - the character set encoding to use for the file (may be null)
     */
    public void reserveFile(long startPosition, long stopPosition, String filename, String charsetName) {
        reserveFile(startPosition, stopPosition, filename, charsetName, filename, false);
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename    - relative (to base) or absolute file name (must not be null)
     * @param charsetName - the character set encoding to use for the file (may be null)
     * @param alias       - the name to be used to access the object (must not be null)
     */
    public void reserveFile(long startPosition, long stopPosition, String filename, String charsetName, String alias) {
        reserveFile(filename, charsetName, alias, false);
    }

    /**
     * Creates an association between a filename and a File inputOutputObject,
     * and stores it for later use - unless it is already stored.
     *
     * @param filename    - relative (to base) or absolute file name (must not be null or empty)
     * @param charsetName - the character set encoding to use for the file (may be null)
     * @param alias       - the name to be used to access the object (must not be null)
     * @param hasHeader   true if the file has a header line describing the contents
     * @return the header line; may be null
     * @throws IllegalArgumentException if header could not be read or filename is null or empty
     */
    @Override
    public synchronized String reserveFile(long startPosition, long stopPosition, String filename, String charsetName, String alias, boolean hasHeader) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename must not be null or empty");
        }
        if (alias == null) {
            throw new IllegalArgumentException("Alias must not be null");
        }
        FileEntry fileEntry = files.get(alias);
        if (fileEntry == null) {
            fileEntry = new FileEntry(resolveFileFromPath(filename), null, charsetName);

            fileEntry.startPosition = startPosition;
            fileEntry.stopPosition = stopPosition;

            if (filename.equals(alias)) {
                log.info("Stored: {}", filename);
            } else {
                log.info("Stored: {} Alias: {}", filename, alias);
            }
            files.put(alias, fileEntry);
            if (hasHeader) {
                try {
                    fileEntry.headerLine = readLine(alias, false);
                    if (fileEntry.headerLine == null) {
                        fileEntry.exception = new EOFException("File is empty: " + fileEntry.file);
                    }
                } catch (IOException | IllegalArgumentException e) {
                    fileEntry.exception = e;
                }
            }
        }
        if (hasHeader && fileEntry.headerLine == null) {
            throw new IllegalArgumentException("Could not read file header line for file " + filename,
                    fileEntry.exception);
        }
        return fileEntry.headerLine;
    }

    @Override
    public BufferedReader createBufferedReader(FileEntry fileEntry, boolean recycle) throws IOException {
        if (!fileEntry.file.canRead() || !fileEntry.file.isFile()) {
            throw new IllegalArgumentException("File " + fileEntry.file.getName() + " must exist and be readable");
        }
        PositionFileInputStream positionFileInputStream = positionMap.get(fileEntry.file.getName());
        if (recycle || null == positionFileInputStream) {
            positionFileInputStream = new PositionFileInputStream(fileEntry.startPosition, fileEntry.stopPosition, fileEntry.file);
            positionMap.put(fileEntry.file.getName(),positionFileInputStream);
        }
        BOMInputStream fis = new BOMInputStream(positionFileInputStream); //NOSONAR
        InputStreamReader isr = null;
        // If file encoding is specified, read using that encoding, otherwise use default platform encoding
        String charsetName = fileEntry.charSetEncoding;
        if (!JOrphanUtils.isBlank(charsetName)) {
            isr = new InputStreamReader(fis, charsetName);
        } else if (fis.hasBOM()) {
            isr = new InputStreamReader(fis, fis.getBOM().getCharsetName());
        } else {
            isr = new InputStreamReader(fis);
        }
        return new BufferedReader(isr);
    }

    @Override
    public synchronized void closeFiles() throws IOException {
        super.closeFiles();
    }
}
