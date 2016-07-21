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
