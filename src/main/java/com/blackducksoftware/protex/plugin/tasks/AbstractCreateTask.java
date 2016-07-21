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
