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
package com.blackducksoftware.protex.plugin;

import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.EOF;
import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.EOT;
import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.ETX;
import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.RS;
import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.SOH;
import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.STX;
import static com.blackducksoftware.protex.plugin.BDSToolPlumbing.US;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.EOFException;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.blackducksoftware.protex.plugin.BDSToolPlumbing.BDSToolSink;
import com.blackducksoftware.protex.plugin.BDSToolPlumbing.ClientDriverNotifier;

/**
 * The source and sink implementations of the "porcelain" mode used to run {@code bdstool} in a separate process.
 *
 * @author jgustie
 */
public class BDSToolSource {

    /**
     * When JDWP suspends the JVM waiting for a remote debugger, this is what it prints to standard out.
     */
    private static final String JDWP_SUSPEND_MESSAGE = "Listening for transport dt_socket at address: ";

    /**
     * A stream used to send all of the output to another process in a machine readable way.
     */
    public static final class TransmissionStream extends FilterOutputStream implements BDSToolSink {
        private static final byte[] NULL = new byte[0];

        private final boolean delegateClose;

        public TransmissionStream() {
            super(System.out);
            delegateClose = false;
        }

        public TransmissionStream(OutputStream out) {
            super(out);
            delegateClose = true;
        }

        @Override
        public void close() {
            try {
                synchronized (out) {
                    out.write(EOT);
                    out.flush();
                    if (delegateClose) {
                        out.close();
                    }
                }
            } catch (IOException ignored) {
            }
        }

        @Override
        public void write(int b) {
            // Sure hope we are wrapped in a buffered implementation...
            message(ClientDriverNotifier.SYSOUT, ByteBuffer.allocate(1).put((byte) b).flip());
        }

        @Override
        public void write(byte[] b, int off, int len) {
            message(ClientDriverNotifier.SYSOUT, ByteBuffer.wrap(b, off, len));
        }

        @Override
        public void sysout(String str) {
            message(ClientDriverNotifier.SYSOUT, str);
        }

        @Override
        public void progress(Map<String, String> map) {
            message(ClientDriverNotifier.PROGRESS, map);
        }

        @Override
        public void message(Map<String, String> map) {
            message(ClientDriverNotifier.MESSAGE, map);
        }

        @Override
        public void output(String out) {
            message(ClientDriverNotifier.OUTPUT, out);
        }

        @Override
        public void scanLog(Map<String, String> map) {
            message(ClientDriverNotifier.SCAN_LOG, map);
        }

        @Override
        public void unknown(Map<String, String> map) {
            message(ClientDriverNotifier.UNKNOWN, map);
        }

        @Override
        public void unknown(String str) {
            message(ClientDriverNotifier.UNKNOWN, str);
        }

        private void message(ClientDriverNotifier id, Object body) {
            synchronized (out) {
                try {
                    out.write(SOH);
                    out.write(id.toInt() & 0xFF);
                    out.write(STX);
                    if (body instanceof Map) {
                        for (Entry<?, ?> r : ((Map<?, ?>) body).entrySet()) {
                            out.write(utf8(r.getKey()));
                            out.write(US);
                            out.write(utf8(r.getValue()));
                            out.write(RS);
                        }
                    } else if (body instanceof ByteBuffer) {
                        ByteBuffer buffer = (ByteBuffer) body;
                        out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.limit() - buffer.position());
                    } else {
                        out.write(utf8(body));
                    }
                    out.write(ETX);
                } catch (IOException ignored) {
                }
            }
        }

        private byte[] utf8(Object obj) {
            return obj != null ? obj.toString().getBytes(UTF_8) : NULL;
        }
    }

    /**
     * Inverse of the transmission stream, used to parse the output it produces.
     */
    public static void pump(InputStream in, BDSToolSink sink) throws IOException {
        final Map<String, String> mapBuffer = new LinkedHashMap<String, String>();
        final ByteBuffer buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();
        boolean firstLine = true;
        ClientDriverNotifier id = null;
        while (true) {
            int b = in.read();
            switch (b) {
            case SOH:
                id = ClientDriverNotifier.valueOf(in.read());
                break;
            case STX:
                mapBuffer.clear();
                buffer.clear().mark();
                break;
            case US:
                buffer.mark();
                break;
            case RS:
                String value = newString(buffer.limit(buffer.position()).reset(), mapBuffer.remove(null));
                mapBuffer.put(newString(buffer.flip(), null), value);
                buffer.clear().mark();
                break;
            case ETX:
                String overflow = mapBuffer.remove(null);
                id.funnel(mapBuffer.isEmpty() ? newString(buffer.flip(), overflow) : mapBuffer, sink);
                break;
            case EOT:
                return;
            case EOF:
                throw new EOFException();
            default:
                // Stash the overflow in the map buffer if necessary
                if (!buffer.hasRemaining()) {
                    mapBuffer.put(null, newString(buffer.reset(), mapBuffer.remove(null)));
                }
                buffer.put((byte) b);

                // The first line may be a JDWP message indicating the debugger can connect
                if (firstLine && b == '\n') {
                    firstLine = false;
                    final String line = newString(buffer.reset(), mapBuffer.remove(null));
                    if (line.startsWith(JDWP_SUSPEND_MESSAGE)) {
                        // Dump the raw message to standard error (which should pass through)
                        System.out.print(line);
                    } else {
                        // Nope, return the output to the overflow buffer
                        mapBuffer.put(null, line);
                    }
                }
            }
        }
    }

    /**
     * Creates a new string from the current state of the supplied buffer.
     */
    private static String newString(Buffer buffer, String overflow) {
        String string = new String((byte[]) buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.limit() - buffer.position(), UTF_8);
        return overflow != null ? overflow.concat(string) : string;
    }

}
