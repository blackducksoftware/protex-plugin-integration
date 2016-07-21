/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.protex.plugin.xml;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream wrapper that reads as if a prefix were present on the original byte stream.
 *
 * @author jgustie
 */
public class PrefixedInputStream extends InputStream {

    private int index;

    private final byte[] prefix;

    private final InputStream input;

    public PrefixedInputStream(byte[] prefix, InputStream input) {
        this.prefix = prefix;
        this.input = input;
    }

    @Override
    public int read() throws IOException {
        if (index < prefix.length) {
            return prefix[index++];
        } else {
            return input.read();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (index < prefix.length) {
            int read = Math.min(available(), len);
            System.arraycopy(prefix, index, b, off, read);
            index += read;
            return read;
        } else {
            return input.read(b, off, len);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        if (index < prefix.length) {
            long skip = Math.min(available(), n);
            index += skip;
            return skip;
        } else {
            return input.skip(n);
        }
    }

    @Override
    public int available() throws IOException {
        if (index <= prefix.length) {
            return prefix.length - index;
        } else {
            return input.available();
        }
    }

    @Override
    public void close() throws IOException {
        input.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
    }

    @Override
    public synchronized void reset() throws IOException {
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
