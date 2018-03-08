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
