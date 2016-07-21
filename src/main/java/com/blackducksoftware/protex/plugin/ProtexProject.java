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
package com.blackducksoftware.protex.plugin;

import static com.blackducksoftware.sdk.protex.license.LicenseCategory.PROPRIETARY;

import java.util.List;

import com.blackducksoftware.sdk.protex.license.LicenseCategory;
import com.blackducksoftware.sdk.protex.project.AnalysisSourceRepository;

/**
 * A model of a Protex project which includes only those fields which can be customized through the build tool
 * integration service.
 *
 * @author jgustie
 */
public final class ProtexProject {

    /**
     * The Protex project identifier. Initially {@code null} when creating new projects, but is populated as soon as the
     * server responds with the newly assigned identifier.
     */
    private String projectId;

    /**
     * The external identifier used by the build tool. For example, the Maven GAV(T) or Ant project name.
     */
    private String externalId;

    /**
     * The human readable project name.
     */
    private String name;

    /**
     * A brief description of the project.
     */
    private String description;

    /**
     * The source repository.
     */
    private AnalysisSourceRepository analysisSourceRepository;

    /**
     * The source path.
     */
    private String analysisSourcePath;

    /**
     * The source host name.
     */
    private String analysisSourceHostname;

    /**
     * The license category of the project. Currently this is always {@code PROPRIETARY}.
     */
    private LicenseCategory licenseCategory = PROPRIETARY;

    /**
     * The name of the organization the project belongs to. This is displayed in the code label and SPDX reports.
     */
    private String furnishedBy;

    /**
     * The URL used to obtain a distribution of the project. This is displayed in the code label and SPDX reports.
     */
    private String openSourceReferenceLocation;

    /**
     * The list of Protex user identifiers with access to this project.
     */
    private List<String> projectUsers;

    /**
     * Flag indicating that rapid identification should be enabled.
     */
    private Boolean rapidIdEnabled;

    /**
     * A list of patterns which should be ignored when analyzing this project.
     */
    private List<String> ignorePatterns;

    public ProtexProject() {
    }

    /**
     * Checks to see if this project model contains any code label specific information.
     */
    public boolean hasCodeLabelOptions() {
        return furnishedBy != null || openSourceReferenceLocation != null;
    }

    /**
     * Checks to see if this project model contains any source location specific information.
     */
    public boolean hasAnalysisSourceLocation() {
        return analysisSourceRepository != null || analysisSourcePath != null || analysisSourceHostname != null;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AnalysisSourceRepository getAnalysisSourceRepository() {
        return analysisSourceRepository;
    }

    public void setAnalysisSourceRepository(AnalysisSourceRepository analysisSourceRepository) {
        this.analysisSourceRepository = analysisSourceRepository;
    }

    public String getAnalysisSourcePath() {
        return analysisSourcePath;
    }

    public void setAnalysisSourcePath(String analysisSourcePath) {
        this.analysisSourcePath = analysisSourcePath;
    }

    public String getAnalysisSourceHostname() {
        return analysisSourceHostname;
    }

    public void setAnalysisSourceHostname(String analysisSourceHostname) {
        this.analysisSourceHostname = analysisSourceHostname;
    }

    public LicenseCategory getLicenseCategory() {
        return licenseCategory;
    }

    public void setLicenseCategory(LicenseCategory licenseCategory) {
        this.licenseCategory = licenseCategory;
    }

    public String getFurnishedBy() {
        return furnishedBy;
    }

    public void setFurnishedBy(String furnishedBy) {
        this.furnishedBy = furnishedBy;
    }

    public String getOpenSourceReferenceLocation() {
        return openSourceReferenceLocation;
    }

    public void setOpenSourceReferenceLocation(String openSourceReferenceLocation) {
        this.openSourceReferenceLocation = openSourceReferenceLocation;
    }

    public List<String> getProjectUsers() {
        return projectUsers;
    }

    public void setProjectUsers(List<String> projectUsers) {
        this.projectUsers = projectUsers;
    }

    public Boolean getRapidIdEnabled() {
        return rapidIdEnabled;
    }

    public void setRapidIdEnabled(Boolean rapidIdEnabled) {
        this.rapidIdEnabled = rapidIdEnabled;
    }

    public List<String> getIgnorePatterns() {
        return ignorePatterns;
    }

    public void setIgnorePatterns(List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

}
