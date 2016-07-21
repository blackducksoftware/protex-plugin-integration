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
import com.blackducksoftware.protex.plugin.ProtexProject;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.policy.externalid.ExternalIdMapping;
import com.blackducksoftware.sdk.protex.policy.externalid.ExternalNamespace;
import com.blackducksoftware.sdk.protex.policy.externalid.ProjectObjectKey;
import com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectKey;
import com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectType;

/**
 * A base class which helps with establishing an external namespace mapping.
 *
 * @author jgustie
 */
public abstract class AbstractCreateTask<V> extends AbstractTask<V> {

    private final ProtexProject project;

    private final ExternalNamespace namespace;

    AbstractCreateTask(ProtexServerProxy proxy, ProtexProject project, ExternalNamespace namespace) {
        super(proxy);
        this.project = project;
        this.namespace = namespace;
    }

    protected void remoteCreateExternalIdMapping(ProtexObjectType objectType) throws BuildToolIntegrationException {
        try {
            ensureExternalSystemId();

            ProtexObjectKey key = new ProjectObjectKey();
            key.setObjectType(objectType);
            key.setObjectId(project.getProjectId());

            ExternalIdMapping externalIdMapping = new ExternalIdMapping();
            externalIdMapping.setExternalObjectId(project.getExternalId());
            externalIdMapping.setProtexObjectKey(key);
            proxy().getExternalIdApi().createExternalIdMapping(namespace.getExternalNamespaceKey(), externalIdMapping);
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        }
    }

    /**
     * Makes sure the system namespace exists in the external mapping service.
     */
    private void ensureExternalSystemId() throws SdkFault {
        try {
            proxy().getExternalIdApi().getExternalNamespace(namespace.getExternalNamespaceKey());
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            case EXTERNAL_NAMESPACE_NOT_FOUND:
                // It wasn't found, try creating it
                proxy().getExternalIdApi().createExternalNamespace(namespace);
                break;
            default:
                // Rethrow the original SDK fault
                throw fault;
            }
        }
    }

}
