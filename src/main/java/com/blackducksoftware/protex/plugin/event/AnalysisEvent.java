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
package com.blackducksoftware.protex.plugin.event;

import java.util.EventObject;
import java.util.Map;

/**
 * An event that occurred during analysis. Currently this just contains a map that can contain anything. Generally it
 * contains the contents of progress notifier map coming from the BDS client (see {@code BDSClientProgressNotifier}).
 *
 * @author jgustie
 */
public class AnalysisEvent extends EventObject {

    private final Map<String, ?> status;

    public AnalysisEvent(Map<String, ?> status) {
        super(status);
        this.status = status;
    }

    public Map<String, ?> status() {
        return status;
    }

}
