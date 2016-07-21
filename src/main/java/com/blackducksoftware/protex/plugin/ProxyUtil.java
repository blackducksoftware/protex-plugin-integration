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
package com.blackducksoftware.protex.plugin;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Some helpers for network proxies.
 *
 * @author jgustie
 */
class ProxyUtil {

    /**
     * Default value of the non-proxy hosts option, used primary for suppressing built-in defaults.
     */
    private static final String DEFAULT_NON_PROXY_HOSTS = "local|*.local|169.254/16|*.169.254/16";

    /**
     * A dummy stream handler that forces a URL to connect through a proxy. This is dumb game to need to play.
     */
    private static class ForcedProxyURLStreamHandler extends URLStreamHandler {
        private final URL url;

        private final Proxy proxy;

        private ForcedProxyURLStreamHandler(URL url, Proxy proxy) {
            this.url = url;
            this.proxy = proxy;
        }

        /**
         * Ensures the URL being passed in is the same instance this stream handler was originally created with.
         */
        private void checkUrl(URL u) {
            if (!url.equals(u)) {
                throw new IllegalArgumentException("expected: " + url + "; got: " + u);
            }
        }

        /**
         * Ensures the proxy being passed is equivalent to one this stream handler was originally created with.
         */
        private void checkProxy(Proxy p) {
            if (!proxy.equals(p)) {
                throw new IllegalArgumentException("expected: " + proxy + "; got: " + p);
            }
        }

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return openConnection(u, proxy);
        }

        @Override
        protected URLConnection openConnection(URL u, Proxy p) throws IOException {
            checkUrl(u);
            checkProxy(p);
            return url.openConnection(proxy);
        }

        @Override
        protected int getDefaultPort() {
            return url.getDefaultPort();
        }

        @Override
        protected boolean equals(URL u1, URL u2) {
            checkUrl(u1);
            return u1.equals(u2);
        }

        @Override
        protected int hashCode(URL u) {
            checkUrl(u);
            return url.hashCode();
        }

        @Override
        protected boolean sameFile(URL u1, URL u2) {
            checkUrl(u1);
            return url.sameFile(u2);
        }

        @Override
        protected String toExternalForm(URL u) {
            checkUrl(u);
            return url.toExternalForm();
        }

        @Override
        protected void parseURL(URL u, String spec, int start, int limit) {
            checkUrl(u);

            // Only let the arguments from "forceProxy" pass through
            if (!spec.equals("") || start != 0 || limit != 0) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        protected synchronized InetAddress getHostAddress(URL u) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected boolean hostsEqual(URL u1, URL u2) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void setURL(URL u, String protocol, String host, int port, String file, String ref) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Constructs a new URL that will use the specified proxy when using {@code openConnection}.
     */
    static URL forceProxy(URL url, Proxy proxy) {
        if (proxy != null && proxy.type() != Type.DIRECT) {
            try {
                return new URL(url, "", new ForcedProxyURLStreamHandler(url, proxy));
            } catch (MalformedURLException e) {
                throw new IllegalStateException("empty string is malformed", e);
            }
        } else {
            return url;
        }
    }

    /**
     * Returns the proxy host name or {@code null} if it is not available.
     */
    static String proxyHost(Proxy proxy) {
        String result = null;
        if (proxy != null) {
            SocketAddress address = proxy.address();
            if (address instanceof InetSocketAddress) {
                InetAddress addr = ((InetSocketAddress) address).getAddress();
                if (addr == null || !addr.isAnyLocalAddress()) {
                    result = ((InetSocketAddress) address).getHostString();
                }
            }
        }
        return result;
    }

    /**
     * Returns the proxy port or {@code -1} if it is not available.
     */
    static int proxyPort(Proxy proxy) {
        int result = -1;
        if (proxy != null) {
            SocketAddress address = proxy.address();
            if (address instanceof InetSocketAddress) {
                result = ((InetSocketAddress) address).getPort();
            }
        }
        return result;
    }

    /**
     * Returns the non-proxy hosts for the system or {@code null} if not available.
     */
    static String nonProxyHosts() {
        String result = System.getProperty("http.nonProxyHosts", DEFAULT_NON_PROXY_HOSTS);
        return !result.equals(DEFAULT_NON_PROXY_HOSTS) ? result : null;
    }

}
