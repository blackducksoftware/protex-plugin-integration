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
package com.blackducksoftware.protex.plugin.tasks;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;

/**
 * An abstract task which provides some basic error handling.
 *
 * @author jgustie
 */
public abstract class AbstractTask<V> implements Callable<V> {

    private final ProtexServerProxy proxy;

    AbstractTask(ProtexServerProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public final V call() throws BuildToolIntegrationException {
        try {
            return execute();
        } catch (SOAPFaultException e) {
            // Wrap as an unknown exception
            throw BuildToolIntegrationException.unknownSoapFault(e);
        } catch (WebServiceException e) {
            // Look for common causes before falling through to an unknown exception
            if (e.getCause() instanceof UnknownHostException) {
                UnknownHostException uhe = (UnknownHostException) e.getCause();
                while (uhe.getCause() instanceof UnknownHostException) {
                    uhe = (UnknownHostException) uhe.getCause();
                }
                throw BuildToolIntegrationException.unknownHost(uhe.getMessage()).initCause(e);
            } else if (e.getCause() instanceof ConnectException) {
                throw BuildToolIntegrationException.connectionFailed(proxy.getServerUrl()).initCause(e);
            } else if (e.getCause() instanceof MalformedURLException) {
                throw BuildToolIntegrationException.invalidServerUrl(proxy.getServerUrl()).initCause(e);
            } else {
                throw BuildToolIntegrationException.unknownException(e);
            }
        }
    }

    protected abstract V execute() throws BuildToolIntegrationException;

    /**
     * Returns an SDK proxy instance.
     */
    protected final ProtexServerProxy proxy() {
        return proxy;
    }

    /**
     * Handle some common SDK faults. Use this method as a fallback when handling {@code SdkFault}.
     *
     * <pre>
     * try {
     *     // ...
     * } catch (SdkFault fault) {
     *     switch (fault.getFaultInfo().getErrorCode()) {
     *     // ...
     *     default:
     *         throw handleSdkFault(fault);
     *     }
     * }
     * </pre>
     */
    protected final BuildToolIntegrationException handleSdkFault(SdkFault fault) {
        switch (fault.getFaultInfo().getErrorCode()) {
        case INVALID_CREDENTIALS:
            return BuildToolIntegrationException.invalidCredentials();
        default:
            return BuildToolIntegrationException.unknownSdkFault(fault);
        }
    }

}
