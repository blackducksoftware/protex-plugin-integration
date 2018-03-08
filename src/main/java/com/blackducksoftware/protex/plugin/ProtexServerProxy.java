/**
 * Protex Plugin Integration
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
 */
package com.blackducksoftware.protex.plugin;

/**
 * The Protex SDK server proxy instance, extended to accept the build tool integration server configuration. We also
 * change the login type to ensure access for those without an SDK license.
 *
 * @author jgustie
 */
public final class ProtexServerProxy extends com.blackducksoftware.sdk.protex.client.util.ProtexServerProxy {

    private final ProtexServer server;

    ProtexServerProxy(ProtexServer server) {
        super(server.getServerUrl(), server.getUsername(), server.getPassword().toString());
        this.server = server;
    }

    /**
     * Returns the URL of the Protex server this proxy is connected to.
     */
    public final String getServerUrl() {
        return server.getServerUrl();
    }

    /**
     * Returns the user name used to connect to the Protex server.
     */
    public final String getUsername() {
        return server.getUsername();
    }

    /**
     * Returns a mutable reference to the Protex server configuration. Changes to the server configuration will not be
     * reflected in SDK calls but may affect {@code bdstool} commands: DO NOT MODIFY THE RETURNED REFERENCE. Use
     * {@link #getServerUrl()} and {@link #getUsername()} unless the password and class loader of the server
     * configuration are required.
     */
    public final ProtexServer server() {
        return server;
    }

}
