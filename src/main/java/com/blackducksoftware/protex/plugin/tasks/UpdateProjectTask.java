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

import static com.blackducksoftware.sdk.protex.common.PatternOriginType.LOCAL_CUSTOM;
import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexProject;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.CodeLabelOption;
import com.blackducksoftware.sdk.protex.common.FileDiscoveryPattern;
import com.blackducksoftware.sdk.protex.common.FileDiscoveryPatternPageFilter;
import com.blackducksoftware.sdk.protex.common.FileType;
import com.blackducksoftware.sdk.protex.project.AnalysisSourceLocation;
import com.blackducksoftware.sdk.protex.project.Project;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.project.ProjectRequest;
import com.blackducksoftware.sdk.protex.project.RapidIdentificationMode;

public class UpdateProjectTask extends AbstractTask<Boolean> {

    private final ProtexProject project;

    public UpdateProjectTask(ProtexServerProxy proxy, ProtexProject project) {
        super(proxy);
        this.project = project;
    }

    @Override
    protected Boolean execute() throws BuildToolIntegrationException {
        boolean dirty = false;
        dirty = remoteUpdateProject() || dirty;
        dirty = remoteUpdateCodeLabelOptions() || dirty;
        dirty = remoteUpdateRapidIdentificationMode() || dirty;
        dirty = remoteUpdateIgnorePatterns() || dirty;
        return dirty;
    }

    private boolean remoteUpdateProject() throws BuildToolIntegrationException {
        try {
            boolean dirty = false;

            Project remoteProject = proxy().getProjectApi().getProjectById(project.getProjectId());
            ProjectRequest projectRequest = new ProjectRequest();

            if (notNullAndNotEquals(project.getName(), remoteProject.getName())) {
                projectRequest.setName(project.getName());
                dirty = true;
            }

            if (notEquals(project.getDescription(), remoteProject.getDescription())) {
                projectRequest.setDescription(nullToEmpty(project.getDescription()));
                dirty = true;
            }

            AnalysisSourceLocation sourceLocation = remoteProject.getAnalysisSourceLocation();
            if (sourceLocation != null) {
                if (notNullAndNotEquals(project.getAnalysisSourceRepository(), sourceLocation.getRepository())) {
                    sourceLocation.setRepository(project.getAnalysisSourceRepository());
                    dirty = true;
                }

                if (notNullAndNotEquals(project.getAnalysisSourcePath(), sourceLocation.getSourcePath())) {
                    sourceLocation.setSourcePath(project.getAnalysisSourcePath());
                    dirty = true;
                }

                if (notNullAndNotEquals(project.getAnalysisSourceHostname(), sourceLocation.getHostname())) {
                    sourceLocation.setHostname(project.getAnalysisSourceHostname());
                    dirty = true;
                }
            } else if (project.hasAnalysisSourceLocation()) {
                sourceLocation = new AnalysisSourceLocation();
                sourceLocation.setRepository(project.getAnalysisSourceRepository());
                sourceLocation.setSourcePath(project.getAnalysisSourcePath());
                sourceLocation.setHostname(project.getAnalysisSourceHostname());
                dirty = true;
            }
            projectRequest.setAnalysisSourceLocation(sourceLocation);

            if (dirty) {
                proxy().getProjectApi().updateProject(project.getProjectId(), projectRequest);
            }
            return dirty;
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        }
    }

    private boolean remoteUpdateCodeLabelOptions() throws BuildToolIntegrationException {
        try {
            boolean dirty = false;

            CodeLabelOption codeLabelOptions = proxy().getProjectApi().getCodeLabelOption(project.getProjectId());
            if (codeLabelOptions == null) {
                codeLabelOptions = new CodeLabelOption();
            }

            if (notEquals(project.getFurnishedBy(), codeLabelOptions.getFurnishedBy())) {
                codeLabelOptions.setFurnishedBy(nullToEmpty(project.getFurnishedBy()));
                dirty = true;
            }

            if (notEquals(project.getOpenSourceReferenceLocation(), codeLabelOptions.getOpenSourceReferenceLocation())) {
                codeLabelOptions.setOpenSourceReferenceLocation(nullToEmpty(project.getOpenSourceReferenceLocation()));
                dirty = true;
            }

            if (dirty) {
                proxy().getProjectApi().updateCodeLabelOption(project.getProjectId(), codeLabelOptions);
            }
            return dirty;
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        }
    }

    private boolean remoteUpdateRapidIdentificationMode() throws BuildToolIntegrationException {
        boolean dirty = false;

        if (project.getRapidIdEnabled() != null) {
            try {
                RapidIdentificationMode mode;
                if (project.getRapidIdEnabled()) {
                    mode = RapidIdentificationMode.AUTOMATIC_INCLUDE_GLOBAL_CONFIGURATIONS;
                } else {
                    mode = RapidIdentificationMode.DISABLED;
                }
                proxy().getProjectApi().updateRapidIdentificationMode(project.getProjectId(), mode);

                // TODO This doesn't check to see if the value was actually changed...
                dirty = true;
            } catch (SdkFault fault) {
                switch (fault.getFaultInfo().getErrorCode()) {
                default:
                    throw handleSdkFault(fault);
                }
            }
        }

        return dirty;
    }

    private boolean remoteUpdateIgnorePatterns() throws BuildToolIntegrationException {
        boolean dirty = false;

        ProjectApi projectApi = proxy().getProjectApi();
        String projectId = project.getProjectId();
        Set<String> ignorePatterns;
        if (project.getIgnorePatterns() != null) {
            ignorePatterns = new LinkedHashSet<String>(project.getIgnorePatterns());
        } else {
            ignorePatterns = Collections.emptySet();
        }

        List<FileDiscoveryPattern> remotePatterns;
        try {
            remotePatterns = projectApi.getFileDiscoveryPatterns(projectId, singletonList(LOCAL_CUSTOM), new FileDiscoveryPatternPageFilter());
            if (remotePatterns == null) {
                remotePatterns = Collections.emptyList();
            }
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        }

        for (FileDiscoveryPattern pattern : remotePatterns) {
            if (pattern.getFileType() == FileType.IGNORED) {
                if (!ignorePatterns.remove(pattern.getPattern())) {
                    try {
                        projectApi.removeFileDiscoveryPattern(projectId, pattern.getPatternId());
                        dirty = true;
                    } catch (SdkFault fault) {
                        switch (fault.getFaultInfo().getErrorCode()) {
                        default:
                            throw handleSdkFault(fault);
                        }
                    }
                }
            }
        }

        for (String ignorePattern : ignorePatterns) {
            try {
                FileDiscoveryPattern pattern = new FileDiscoveryPattern();
                pattern.setPattern(ignorePattern);
                pattern.setFileType(FileType.IGNORED);
                projectApi.addFileDiscoveryPattern(projectId, pattern);
                dirty = true;
            } catch (SdkFault fault) {
                switch (fault.getFaultInfo().getErrorCode()) {
                default:
                    throw handleSdkFault(fault);
                }
            }

        }

        return dirty;
    }

    /**
     * Helper for testing equality (or lack thereof).
     */
    private static boolean notEquals(Object a, Object b) {
        return a != b && (a == null || !a.equals(b));
    }

    /**
     * Checks to see if {@code a} is not-{@code null} and not equal to {@code b}.
     */
    private static boolean notNullAndNotEquals(Object a, Object b) {
        return a != null && !a.equals(b);
    }

    /**
     * Helper for dealing with the fact that you can pass {@code null} values to the SDK to change something.
     */
    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

}
