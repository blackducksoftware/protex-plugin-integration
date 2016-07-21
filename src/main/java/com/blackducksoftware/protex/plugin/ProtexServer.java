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

import java.io.File;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.security.auth.Destroyable;

/**
 * Configuration of a Protex server configuration. This class contains the server URL and credentials necessary for
 * establishing a connection to the Protex server, additionally it maintains a lazily initialized class loader which can
 * be used to load {@code BDSClientDriver} instances.
 *
 * @author jgustie
 */
public class ProtexServer implements Destroyable {

    /**
     * The relative paths to the remote code source locations. Note that the class loader will follow relative paths in
     * the manifest's class path so not every individual JAR needs to be listed here.
     */
    private static final String[] CODE_SOURCE_PATHS = { "repo/lib/bdsclient.jar" };

    /**
     * A view of the password. When the enclosing instance is destroyed, all methods will throw
     * {@code IllegalStateException}.
     */
    private class PasswordWrapper implements CharSequence {

        @Override
        public int length() {
            checkDestroyed();
            return password.length;
        }

        @Override
        public char charAt(int index) {
            checkDestroyed();
            return password[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            checkDestroyed();
            if (start == 0 && end == password.length) {
                return this;
            } else {
                throw new UnsupportedOperationException("cannot sub-sequence passwords");
            }
        }

        @Override
        public String toString() {
            // WARNING: This creates a copy of the password array that we cannot control
            return new String(password);
        }
    }

    private String serverUrl;

    private String username;

    private char[] password;

    private ClassLoader clientLoader;

    // TODO We need the location of the trust store in case Protex is using HTTPS with a private certificate
    private File trustStore;

    /**
     * The proxy override, {@code null} means use the system defaults and is different from {@code Proxy.NO_PROXY}.
     */
    private Proxy proxy;

    public ProtexServer(CharSequence password) {
        char[] passwordClone = new char[password.length()];
        for (int i = 0; i < passwordClone.length; ++i) {
            passwordClone[i] = password.charAt(i);
        }
        this.password = passwordClone;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        // Clear the client loader whenever the server URL changes
        if (this.serverUrl != null && !this.serverUrl.equals(serverUrl)) {
            clientLoader = null;
        }
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public CharSequence getPassword() {
        checkDestroyed();
        return new PasswordWrapper();
    }

    /**
     * Returns a class loader for BDS Client on this Protex server.
     */
    public ClassLoader getClientLoader() throws BuildToolIntegrationException {
        if (clientLoader == null) {
            try {
                String baseUrl = serverUrl;
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/";
                }

                URL[] urls = new URL[CODE_SOURCE_PATHS.length];
                int index = 0;
                for (String codeSourcePath : CODE_SOURCE_PATHS) {
                    urls[index++] = ProxyUtil.forceProxy(new URL(baseUrl + codeSourcePath), proxy);
                }

                clientLoader = URLClassLoader.newInstance(urls, getClass().getClassLoader());
            } catch (MalformedURLException e) {
                throw BuildToolIntegrationException.invalidServerUrl(serverUrl).initCause(e);
            }
        }
        return clientLoader;
    }

    /**
     * Returns the trust store used for establishing TLS/SSL connections with this server.
     */
    public File getTrustStore() {
        return trustStore;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    /**
     * Returns the proxy server used for establishing HTTP(S) connections to this server.
     */
    public Proxy getProxy() {
        return proxy;
    }

    @Override
    public void destroy() {
        checkDestroyed();
        Arrays.fill(password, '\0');
        password = null;
        clientLoader = null;
        proxy = null;
    }

    @Override
    public boolean isDestroyed() {
        return password == null;
    }

    private void checkDestroyed() {
        if (isDestroyed()) {
            throw new IllegalStateException("destroyed");
        }
    }

}
