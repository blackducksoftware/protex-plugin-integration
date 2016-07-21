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

import static com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectType.COMPONENT;

import java.util.Collections;

import com.blackducksoftware.protex.plugin.BlackDuckCommand;
import com.blackducksoftware.protex.plugin.BlackDuckCommandBuilder;
import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexProject;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.ComponentKey;
import com.blackducksoftware.sdk.protex.component.ComponentRequest;
import com.blackducksoftware.sdk.protex.component.custom.CustomComponentSettings;
import com.blackducksoftware.sdk.protex.policy.externalid.ExternalNamespace;
import com.blackducksoftware.sdk.protex.project.AnalysisSourceLocation;

public class CreateCodePrintTask extends AbstractCreateTask<Void> {

    private final ProtexProject codePrint;

    public CreateCodePrintTask(ProtexServerProxy proxy, ProtexProject codePrint, ExternalNamespace namespace) {
        super(proxy, codePrint, namespace);
        this.codePrint = codePrint;
    }

    @Override
    protected Void execute() throws BuildToolIntegrationException {
        remoteCreateCodePrint();
        remoteCreateExternalIdMapping(COMPONENT);
        BlackDuckCommand codePrintCommand = BlackDuckCommandBuilder.codePrint()
                .codePrintId(codePrint.getProjectId())
                .path(codePrint.getAnalysisSourcePath())
                .connectedTo(proxy().server())
                .build();
        codePrintCommand.run();
        return null;
    }

    private void remoteCreateCodePrint() throws BuildToolIntegrationException {
        try {
            ComponentRequest request = new ComponentRequest();
            request.setComponentName(codePrint.getName());
            request.setDescription(codePrint.getDescription());

            ComponentKey key = proxy().getComponentApi().createComponent(request);
            codePrint.setProjectId(key.getComponentId());

            CustomComponentSettings settings = new CustomComponentSettings();
            settings.setComponentKey(key);
            settings.setAnalysisSourceLocation(new AnalysisSourceLocation());
            settings.getAnalysisSourceLocation().setRepository(codePrint.getAnalysisSourceRepository());
            settings.getAnalysisSourceLocation().setSourcePath(codePrint.getAnalysisSourcePath());
            settings.getAnalysisSourceLocation().setHostname(codePrint.getAnalysisSourceHostname());
            proxy().getCustomComponentManagementApi().updateCustomComponentSettings(Collections.singletonList(settings));
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            case DUPLICATE_COMPONENT_NAME:
                throw BuildToolIntegrationException.duplicateProjectName(codePrint.getExternalId(), codePrint.getName());
            default:
                throw handleSdkFault(fault);
            }
        }
    }

}
