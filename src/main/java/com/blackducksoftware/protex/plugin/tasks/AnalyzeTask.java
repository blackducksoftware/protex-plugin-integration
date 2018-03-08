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

import java.io.File;

import com.blackducksoftware.protex.plugin.BlackDuckCommand;
import com.blackducksoftware.protex.plugin.BlackDuckCommandBuilder;
import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.protex.plugin.event.AnalysisListener;
import com.blackducksoftware.protex.plugin.event.ProgressObserver;

public class AnalyzeTask extends AbstractTask<Void> {

    private final String projectId;

    private final File directory;

    private final boolean force;

    private final Iterable<AnalysisListener> listeners;

    public AnalyzeTask(ProtexServerProxy proxy, String projectId, File directory, boolean force, Iterable<AnalysisListener> listeners) {
        super(proxy);
        this.projectId = projectId;
        this.directory = directory;
        this.force = force;
        this.listeners = listeners;
    }

    @Override
    protected Void execute() throws BuildToolIntegrationException {
        BlackDuckCommand analyzeCommand = BlackDuckCommandBuilder.analyze()
                .projectId(projectId)
                .directory(directory)
                .force(force)
                .connectedTo(proxy().server())
                .observingProgress(new ProgressObserver(listeners))
                .build();
        analyzeCommand.run();
        return null;
    }

}
