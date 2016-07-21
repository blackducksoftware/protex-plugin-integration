/*
 * Protex Plugin Integration
 * Copyright (C) 2015 Black Duck Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
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
