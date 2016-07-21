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

import static com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectType.COMPONENT;
import static com.blackducksoftware.sdk.protex.policy.externalid.ProtexObjectType.PROJECT;

import java.io.File;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.List;

import com.blackducksoftware.protex.plugin.event.AnalysisEvent;
import com.blackducksoftware.protex.plugin.event.AnalysisListener;
import com.blackducksoftware.protex.plugin.tasks.AddSubProjectTask;
import com.blackducksoftware.protex.plugin.tasks.AnalyzeTask;
import com.blackducksoftware.protex.plugin.tasks.CreateCodePrintTask;
import com.blackducksoftware.protex.plugin.tasks.CreateProjectTask;
import com.blackducksoftware.protex.plugin.tasks.GenerateProtexReportTask;
import com.blackducksoftware.protex.plugin.tasks.GenerateSpdxReportTask;
import com.blackducksoftware.protex.plugin.tasks.LookupIdTask;
import com.blackducksoftware.protex.plugin.tasks.UpdateProjectTask;
import com.blackducksoftware.sdk.protex.policy.externalid.ExternalNamespace;
import com.blackducksoftware.sdk.protex.report.ReportTemplateRequest;
import com.blackducksoftware.sdk.protex.report.SpdxReportConfiguration;

/**
 * Default implementation of the build tool integration service. Most of the actual logic is actually captured in
 * separate tasks, however some common identifier mapping and error checking happens here.
 *
 * @author jgustie
 */
public class BuildToolIntegrationServiceImpl implements BuildToolIntegrationService {

    /**
     * A helper for setting the current thread context class loader (and restoring it) around every interface method
     * invocation. This is necessary primarily for Maven 2 Site invocation which does not set the context class loader,
     * but other build systems (like Jenkins) also appear to have this problem.
     * <p>
     * Without this fix in place, the most likely failure is that CXF will fail to initialize. You end up getting
     * "Could not resolve a binding for null" exceptions caused by
     * "No binding factory for namespace http://schemas.xmlsoap.org/soap/ registered."
     */
    private static class ContextClassLoaderHandler implements InvocationHandler {
        // TODO Should we generalize this further and put it in a public place?

        private static final Class<?>[] INTERFACES = new Class[] { BuildToolIntegrationService.class };

        private final BuildToolIntegrationService delegate;

        private final ClassLoader loader;

        private ContextClassLoaderHandler(BuildToolIntegrationService delegate, ClassLoader loader) {
            this.delegate = delegate;
            this.loader = loader;
        }

        private BuildToolIntegrationService newProxyInstance() {
            return (BuildToolIntegrationService) Proxy.newProxyInstance(loader, INTERFACES, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(loader);
                return method.invoke(delegate, args);
            } finally {
                Thread.currentThread().setContextClassLoader(originalLoader);
            }
        }
    }

    /**
     * The namespace for mapping external identifiers.
     */
    private final ExternalNamespace namespace;

    /**
     * The SDK helper for accessing the individual Protex APIs.
     */
    private final ProtexServerProxy proxy;

    /**
     * The event listeners registered with this service.
     */
    private final Collection<EventListener> listeners = new ArrayList<EventListener>();

    /**
     * @deprecated This constructor may lead to class loader issues with CXF in certain environments where the context
     *             class loader does not have access to the Protex Plugin Integration dependencies. You should use
     *             {@link #newInstance(ProtexServer, ExternalNamespace)} instead. This constructor will be removed in
     *             version 1.1.
     */
    @Deprecated
    public BuildToolIntegrationServiceImpl(ProtexServer server, ExternalNamespace namespace) {
        this(namespace, new ProtexServerProxy(server));
    }

    private BuildToolIntegrationServiceImpl(ExternalNamespace namespace, ProtexServerProxy proxy) {
        this.namespace = namespace;
        this.proxy = proxy;
    }

    /**
     * Creates a new instance of the build tool integration service.
     */
    public static BuildToolIntegrationService newInstance(ProtexServer server, ExternalNamespace namespace) {
        BuildToolIntegrationService service = new BuildToolIntegrationServiceImpl(namespace, new ProtexServerProxy(server));

        // Using a relative path, look for one our resources in the context class loader. If it's
        // not there, assume that the context class loader is not the correct class loader and wrap
        // the service implementation with a dynamic proxy which will adjust the context class
        // loader before and after each method call.
        if (Thread.currentThread().getContextClassLoader().getResource("protex-report.xslt") == null) {
            ClassLoader loader = BuildToolIntegrationServiceImpl.class.getClassLoader();
            service = new ContextClassLoaderHandler(service, loader).newProxyInstance();
        }

        return service;
    }

    @Override
    public void createProject(ProtexProject project) throws BuildToolIntegrationException {
        new CreateProjectTask(proxy, project, namespace).call();
    }

    @Override
    public boolean updateProject(ProtexProject project) throws BuildToolIntegrationException {
        return new UpdateProjectTask(proxy, project).call();
    }

    @Override
    public void addSubProject(ProtexProject parentProject, ProtexProject subProject) throws BuildToolIntegrationException {
        final String parentProjectId = getProjectId(parentProject);
        final String subProjectId = getProjectId(subProject);
        if (parentProjectId == null) {
            throw BuildToolIntegrationException.missingParentProject(parentProject.getExternalId());
        }
        new AddSubProjectTask(proxy, parentProjectId, subProjectId).call();
    }

    /**
     * Retrieves the existing project identifier before attempting to look it up. May return {@code null}.
     */
    private String getProjectId(ProtexProject project) throws BuildToolIntegrationException {
        String projectId = project.getProjectId();
        if (projectId == null) {
            projectId = lookupProjectId(project.getExternalId());
        }
        return projectId;
    }

    @Override
    public String lookupProjectId(String externalId) throws BuildToolIntegrationException {
        final String namespaceKey = namespace.getExternalNamespaceKey();
        return new LookupIdTask(proxy, namespaceKey, PROJECT, externalId).call();
    }

    @Override
    public String lookupComponentId(String externalId) throws BuildToolIntegrationException {
        final String namespaceKey = namespace.getExternalNamespaceKey();
        return new LookupIdTask(proxy, namespaceKey, COMPONENT, externalId).call();
    }

    @Override
    public void createCodePrint(ProtexProject codePrint) throws BuildToolIntegrationException {
        new CreateCodePrintTask(proxy, codePrint, namespace).call();
    }

    @Override
    public void analyze(String externalId, File directory, boolean force) throws BuildToolIntegrationException {
        // Isolate the analysis listeners
        List<AnalysisListener> analysisListeners = new ArrayList<AnalysisListener>(listeners.size());
        for (EventListener listener : listeners) {
            if (listener instanceof AnalysisListener) {
                analysisListeners.add((AnalysisListener) listener);
            }
        }

        // Make sure we can map to a valid project identifier
        String projectId;
        try {
            projectId = ensureProjectId(externalId);
        } catch (BuildToolIntegrationException e) {
            // We need to manually notify the listeners since this occurred before command execution
            AnalysisEvent event = new AnalysisEvent(Collections.singletonMap("exception", e));
            for (AnalysisListener listener : analysisListeners) {
                listener.analysisFailed(event);
            }
            throw e;
        }

        // Execute the command (which will also notify the listeners on failure)
        new AnalyzeTask(proxy, projectId, directory, force, analysisListeners).call();
    }

    @Override
    public Reader generateHtmlReport(String externalId, ReportTemplateRequest request) throws BuildToolIntegrationException {
        final String projectId = ensureProjectId(externalId);
        return new GenerateProtexReportTask(proxy, projectId, request).call();
    }

    @Override
    public Reader generateHtmlReport(String externalId, SpdxReportConfiguration request) throws BuildToolIntegrationException {
        final String projectId = ensureProjectId(externalId);
        return new GenerateSpdxReportTask(proxy, projectId, request).call();
    }

    @Override
    public URL generateLink(String uri, String fragment) throws MalformedURLException {
        try {
            URI serverUri = new URI(proxy.server().getServerUrl());
            URI resolvedUri = serverUri.resolve(uri);
            if (fragment != null) {
                resolvedUri = new URI(resolvedUri.getScheme(), resolvedUri.getSchemeSpecificPart(), fragment);
            }
            return resolvedUri.toURL();
        } catch (URISyntaxException e) {
            final String message = "Failed to resolve '" + uri + "' against '" + proxy.server().getServerUrl() + "'";
            throw (MalformedURLException) new MalformedURLException(message).initCause(e);
        }
    }

    /**
     * Like {@link #lookupProjectId(String)} except it fails if the mapping does not exist.
     */
    private String ensureProjectId(String externalId) throws BuildToolIntegrationException {
        String projectId = lookupProjectId(externalId);
        if (projectId == null) {
            throw BuildToolIntegrationException.noSuchProject(externalId);
        }
        return projectId;
    }

    @Override
    public BuildToolIntegrationServiceImpl register(EventListener listener) {
        listeners.add(listener);
        return this;
    }

}
