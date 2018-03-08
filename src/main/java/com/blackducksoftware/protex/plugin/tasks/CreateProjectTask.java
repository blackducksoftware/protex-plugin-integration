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

import static com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectType.PROJECT;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexProject;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.CodeLabelOption;
import com.blackducksoftware.sdk.protex.common.FileDiscoveryPattern;
import com.blackducksoftware.sdk.protex.common.FileType;
import com.blackducksoftware.sdk.protex.policy.externalid.ExternalNamespace;
import com.blackducksoftware.sdk.protex.project.AnalysisSourceLocation;
import com.blackducksoftware.sdk.protex.project.ProjectRequest;
import com.blackducksoftware.sdk.protex.project.RapidIdentificationMode;

public class CreateProjectTask extends AbstractCreateTask<Void> {

    private final ProtexProject project;

    public CreateProjectTask(ProtexServerProxy proxy, ProtexProject project, ExternalNamespace namespace) {
        super(proxy, project, namespace);
        this.project = project;
    }

    @Override
    protected Void execute() throws BuildToolIntegrationException {
        remoteCreateProject();
        remoteCreateExternalIdMapping(PROJECT);
        remoteCreateCodeLabelOptions();
        remoteAddProjectUsers();
        remoteCreateRapidIdentificationMode();
        remoteCreateIgnorePatterns();
        return null;
    }

    private void remoteCreateProject() throws BuildToolIntegrationException {
        try {
            ProjectRequest request = new ProjectRequest();
            request.setName(project.getName());
            request.setDescription(project.getDescription());
            request.setAnalysisSourceLocation(new AnalysisSourceLocation());
            request.getAnalysisSourceLocation().setRepository(project.getAnalysisSourceRepository());
            request.getAnalysisSourceLocation().setSourcePath(project.getAnalysisSourcePath());
            request.getAnalysisSourceLocation().setHostname(project.getAnalysisSourceHostname());
            project.setProjectId(proxy().getProjectApi().createProject(request, project.getLicenseCategory()));
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            case DUPLICATE_PROJECT_NAME:
                throw BuildToolIntegrationException.duplicateProjectName(project.getExternalId(), project.getName());
            default:
                throw handleSdkFault(fault);
            }
        }
    }

    private void remoteCreateCodeLabelOptions() throws BuildToolIntegrationException {
        if (project.hasCodeLabelOptions()) {
            try {
                CodeLabelOption codeLabelOptions = new CodeLabelOption();
                codeLabelOptions.setFurnishedBy(project.getFurnishedBy());
                codeLabelOptions.setOpenSourceReferenceLocation(project.getOpenSourceReferenceLocation());

                proxy().getProjectApi().updateCodeLabelOption(project.getProjectId(), codeLabelOptions);
            } catch (SdkFault fault) {
                switch (fault.getFaultInfo().getErrorCode()) {
                default:
                    throw handleSdkFault(fault);
                }
            }
        }
    }

    private void remoteAddProjectUsers() throws BuildToolIntegrationException {
        if (project.getProjectUsers() != null) {
            for (String projectUser : project.getProjectUsers()) {
                try {
                    proxy().getProjectApi().addProjectUser(project.getProjectId(), projectUser);
                } catch (SdkFault fault) {
                    switch (fault.getFaultInfo().getErrorCode()) {
                    case USER_NOT_FOUND:
                        break;
                    case DUPLICATE_USER_ASSIGNMENT:
                        break;
                    default:
                        throw handleSdkFault(fault);
                    }
                }
            }
        }
    }

    private void remoteCreateRapidIdentificationMode() throws BuildToolIntegrationException {
        if (project.getRapidIdEnabled() != null) {
            try {
                RapidIdentificationMode mode;
                if (project.getRapidIdEnabled()) {
                    mode = RapidIdentificationMode.AUTOMATIC_INCLUDE_GLOBAL_CONFIGURATIONS;
                } else {
                    mode = RapidIdentificationMode.DISABLED;
                }
                proxy().getProjectApi().updateRapidIdentificationMode(project.getProjectId(), mode);
            } catch (SdkFault fault) {
                switch (fault.getFaultInfo().getErrorCode()) {
                default:
                    throw handleSdkFault(fault);
                }
            }
        }
    }

    private void remoteCreateIgnorePatterns() throws BuildToolIntegrationException {
        if (project.getIgnorePatterns() != null) {
            for (String ignorePattern : project.getIgnorePatterns()) {
                try {
                    FileDiscoveryPattern fileDiscoveryPattern = new FileDiscoveryPattern();
                    fileDiscoveryPattern.setPattern(ignorePattern);
                    fileDiscoveryPattern.setFileType(FileType.IGNORED);
                    proxy().getProjectApi().addFileDiscoveryPattern(project.getProjectId(), fileDiscoveryPattern);
                } catch (SdkFault fault) {
                    switch (fault.getFaultInfo().getErrorCode()) {
                    default:
                        throw handleSdkFault(fault);
                    }
                }
            }
        }
    }

}
