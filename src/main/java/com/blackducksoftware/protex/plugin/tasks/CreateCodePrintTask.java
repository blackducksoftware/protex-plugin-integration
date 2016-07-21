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
