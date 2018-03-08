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

/**
 * An empty {@code AnalysisListener}, useful for when only a few methods need to be overridden.
 *
 * @author jgustie
 */
public abstract class AbstractAnalysisListener implements AnalysisListener {

    @Override
    public void analysisInitiated(AnalysisEvent event) {
    }

    @Override
    public void analysisStarted(AnalysisEvent event) {
    }

    @Override
    public void analysisProgressed(AnalysisEvent event) {
    }

    @Override
    public void analysisSucceeded(AnalysisEvent event) {
    }

    @Override
    public void analysisFailed(AnalysisEvent event) {
    }

}
