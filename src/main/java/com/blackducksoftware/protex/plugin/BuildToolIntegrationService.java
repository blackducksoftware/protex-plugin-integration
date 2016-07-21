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
package com.blackducksoftware.protex.plugin;

import java.io.File;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EventListener;

import com.blackducksoftware.sdk.protex.report.ReportTemplateRequest;
import com.blackducksoftware.sdk.protex.report.SpdxReportConfiguration;

/**
 * A service for interacting with a remote Protex server. This server provides all of the functionality required for a
 * build tool to provide integration with Protex.
 *
 * @author jgustie
 */
public interface BuildToolIntegrationService {

    // TODO Should we take ProtexProject instead of "externalId" just in case we have already looked up the project ID?

    /**
     * Resolves the build tool identifier into a Protex project identifier. Returns {@code null} if no mapping exists.
     */
    String lookupProjectId(String externalId) throws BuildToolIntegrationException;

    /**
     * Resolves the build tool identifier into a Protex component identifier. Returns {@code null} if no mappings
     * exists.
     */
    String lookupComponentId(String externalId) throws BuildToolIntegrationException;

    /**
     * Creates a new project from the supplied model. The project identifier of the supplied instance will be updated.
     */
    void createProject(ProtexProject project) throws BuildToolIntegrationException;

    /**
     * Updates an existing project from the supplied model. The project identifier of the supplied instance must be
     * pre-populated. Returns {@code true} if any changes were made.
     */
    boolean updateProject(ProtexProject project) throws BuildToolIntegrationException;

    /**
     * Configures a project to be a "sub-project" by adding it to the parent project's bill of materials. This will fail
     * if the parent project does not yet exist.
     */
    void addSubProject(ProtexProject parentProject, ProtexProject subProject) throws BuildToolIntegrationException;

    /**
     * Creates a new code printed component from the supplied model. The project identifier of the supplied instance
     * will be updated.
     */
    void createCodePrint(ProtexProject codePrint) throws BuildToolIntegrationException;

    /**
     * Performs an analysis on the project identified with the supplied build tool identifier.
     */
    // TODO Should this take a ProtexProject and use the source directory property?
    void analyze(String externalId, File directory, boolean force) throws BuildToolIntegrationException;

    /**
     * Generates a report.
     */
    Reader generateHtmlReport(String externalId, ReportTemplateRequest request) throws BuildToolIntegrationException;

    /**
     * Generates an SPDX report.
     */
    Reader generateHtmlReport(String externalId, SpdxReportConfiguration request) throws BuildToolIntegrationException;

    /**
     * Registers an arbitrary event listener with this service. Returns the service for chaining method calls.
     */
    BuildToolIntegrationService register(EventListener listener);

    /**
     * Generates a link resolved against the current Protex server. For example, to reference the Maven help
     * documentation FAQ for problems related to the Maven Site Plugin version, you would call
     * {@code generateLink("/mvn-docs/faq.html", "maven-site-plugin-30")}.
     */
    URL generateLink(String uri, String fragment) throws MalformedURLException;

}
