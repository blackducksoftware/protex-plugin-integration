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

import static com.blackducksoftware.sdk.protex.common.BomRefreshMode.ASYNCHRONOUS;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.ComponentKey;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.project.bom.BomComponentRequest;

public class AddSubProjectTask extends AbstractTask<Void> {

    private final String parentProjectId;

    private final String subProjectId;

    public AddSubProjectTask(ProtexServerProxy proxy, String parentProjectId, String subProjectId) {
        super(proxy);
        this.parentProjectId = parentProjectId;
        this.subProjectId = subProjectId;
    }

    @Override
    protected Void execute() throws BuildToolIntegrationException {
        try {
            ComponentKey subProjectKey = new ComponentKey();
            subProjectKey.setComponentId(subProjectId);
            BomComponentRequest request = new BomComponentRequest();
            request.setComponentKey(subProjectKey);
            // TODO Should this be COMPONENT_MERELY_AGGREGATED or COMPONENT_MODULE?
            request.setUsageLevel(UsageLevel.COMPONENT_MERELY_AGGREGATED);

            proxy().getBomApi().addBomComponent(parentProjectId, request, ASYNCHRONOUS);
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        }
        return null;
    }

}
