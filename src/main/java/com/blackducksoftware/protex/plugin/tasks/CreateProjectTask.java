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
