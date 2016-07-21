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
