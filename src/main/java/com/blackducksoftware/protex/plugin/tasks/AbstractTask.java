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
