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
