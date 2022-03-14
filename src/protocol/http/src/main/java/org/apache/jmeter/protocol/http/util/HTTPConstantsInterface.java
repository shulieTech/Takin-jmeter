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

package org.apache.jmeter.protocol.http.util;

/**
 * Constants used in HTTP, mainly header names.
 */
public interface HTTPConstantsInterface { // CHECKSTYLE IGNORE InterfaceIsType
    String SC_MOVED_PERMANENTLY = "301";
    String SC_MOVED_TEMPORARILY = "302";
    String SC_SEE_OTHER = "303";
    String SC_TEMPORARY_REDIRECT = "307";

    int DEFAULT_HTTPS_PORT = 443;
    String DEFAULT_HTTPS_PORT_STRING = "443";
    int DEFAULT_HTTP_PORT = 80;
    String DEFAULT_HTTP_PORT_STRING = "80";
    String PROTOCOL_HTTP = "http";
    String PROTOCOL_HTTPS = "https";
    String HEAD = "HEAD";
    String POST = "POST";
    String PUT = "PUT";
    String GET = "GET";
    String OPTIONS = "OPTIONS";
    String TRACE = "TRACE";
    String DELETE = "DELETE";
    String PATCH = "PATCH";
    String PROPFIND = "PROPFIND";
    String PROPPATCH = "PROPPATCH";
    String MKCOL = "MKCOL";
    String COPY = "COPY";
    String MOVE = "MOVE";
    String LOCK = "LOCK";
    String UNLOCK = "UNLOCK";
    String CONNECT = "CONNECT";
    String REPORT = "REPORT";
    String MKCALENDAR = "MKCALENDAR";
    String SEARCH = "SEARCH";
    String HEADER_AUTHORIZATION = "Authorization";
    String HEADER_COOKIE = "Cookie";
    String HEADER_COOKIE_IN_REQUEST = "Cookie:";
    String HEADER_CONNECTION = "Connection";
    String CONNECTION_CLOSE = "close";
    String KEEP_ALIVE = "keep-alive";
    // e.g. "Transfer-Encoding: chunked", which is processed automatically by the underlying protocol
    String TRANSFER_ENCODING = "transfer-encoding";
    String HEADER_CONTENT_ENCODING = "content-encoding";
    String HTTP_1_1 = "HTTP/1.1";
    String HEADER_SET_COOKIE = "set-cookie";
    // Brotli compression not supported yet by HC4 4.5.2 , but to be added
    String ENCODING_BROTLI = "br";
    String ENCODING_DEFLATE = "deflate";
    String ENCODING_GZIP = "gzip";

    String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    String HEADER_CONTENT_TYPE = "Content-Type";
    String HEADER_CONTENT_LENGTH = "Content-Length";
    String HEADER_HOST = "Host";
    String HEADER_LOCAL_ADDRESS = "X-LocalAddress"; // $NON-NLS-1$ pseudo-header for reporting Local Address
    String HEADER_LOCATION = "Location";
    String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
    String MULTIPART_FORM_DATA = "multipart/form-data";
    // For handling caching
    String IF_NONE_MATCH = "If-None-Match";
    String IF_MODIFIED_SINCE = "If-Modified-Since";
    String ETAG = "Etag";
    String LAST_MODIFIED = "Last-Modified";
    String EXPIRES = "Expires";
    String CACHE_CONTROL = "Cache-Control";  //e.g. public, max-age=259200
    String DATE = "Date";  //e.g. Date Header of response
    String VARY = "Vary";

}
