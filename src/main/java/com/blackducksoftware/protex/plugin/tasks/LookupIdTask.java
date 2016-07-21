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
package com.blackducksoftware.protex.plugin.tasks;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectType;

public class LookupIdTask extends AbstractTask<String> {

    private final String namespaceKey;

    private final ProtexObjectType objectType;

    private final String externalId;

    public LookupIdTask(ProtexServerProxy proxy, String namespaceKey, ProtexObjectType objectType, String externalId) {
        super(proxy);
        this.namespaceKey = namespaceKey;
        this.objectType = objectType;
        this.externalId = externalId;
    }

    @Override
    protected String execute() throws BuildToolIntegrationException {
        try {
            return proxy().getExternalIdApi().getObjectIdByExternalId(namespaceKey, externalId, objectType).getObjectId();
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            case EXTERNAL_NAMESPACE_NOT_FOUND:
            case EXTERNAL_ID_MAPPING_NOT_FOUND:
                return null;
            default:
                throw handleSdkFault(fault);
            }
        }
    }

}
