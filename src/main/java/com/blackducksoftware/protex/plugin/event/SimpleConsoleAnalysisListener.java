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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

import com.blackducksoftware.protex.plugin.StatusLogger;

public class SimpleConsoleAnalysisListener extends AbstractAnalysisListener {

    private static final String BUNDLE_BASE_NAME = "SimpleAnalysisListener";

    protected static final String OPERATION = "operation";

    protected static final String OPERATION_NUMBER = "operation_number";

    protected static final String OPERATION_NUMBER_MAX = "operation_number_max";

    protected static final String FILE = "file";

    private final StatusLogger logger;

    private final AtomicBoolean seenMaxOperation = new AtomicBoolean();

    /**
     * Creates a simple console analysis listener. The supplied console may be {@code null}.
     */
    public SimpleConsoleAnalysisListener(StatusLogger logger) {
        this.logger = logger;

        // There is a noticeable delay from when the analysis is invoked until we can actually start
        // receiving notifications; printing a message here is more reassuring that something is
        // actually about to happen (note: the delay seems to be caused by CXF).
        logger.status(message("initializing"));
    }

    /**
     * Returns a message for display in the status or log.
     */
    protected String message(String key) {
        return ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.getDefault(), getClass().getClassLoader()).getString(key);
    }

    @Override
    public void analysisInitiated(AnalysisEvent event) {
        // At this point we have a Protex project identifier but bdstool has not yet been called
        logger.status(message("starting"));
        seenMaxOperation.set(false);
    }

    @Override
    public void analysisStarted(AnalysisEvent event) {
        // This is the first notification produced by bdstool and should only be called once
        updateStatus(event);
        seenMaxOperation.set(false);
    }

    @Override
    public void analysisProgressed(AnalysisEvent event) {
        // We are now in a loop of scan progress, output the status with the file name
        updateStatus(event);

        Object operationNumber = event.status().get(OPERATION_NUMBER);
        Object operationNumberMax = event.status().get(OPERATION_NUMBER_MAX);
        if (operationNumber != null && operationNumber.equals(operationNumberMax) && !seenMaxOperation.getAndSet(true)) {
            // This is the last notification before bdstool asks the server to compute the BOM,
            // therefore we will see a longer then average pause until the server is done
            logger.status(message("computingBom"));
        }
    }

    /**
     * Updates the status with the current operation and file name (if available).
     */
    protected void updateStatus(AnalysisEvent event) {
        Object operation = event.status().get(OPERATION);
        Object operationNumber = event.status().get(OPERATION_NUMBER);
        Object operationNumberMax = event.status().get(OPERATION_NUMBER_MAX);
        Object file = event.status().get(FILE);
        if (operation != null) {
            // Display the file name if we have one ("assessing work" comes as the initial status)
            if (file != null && !file.equals("... assessing work ...")) {
                logger.status("%s (%s/%s) %s", operation, operationNumber, operationNumberMax, file);
            } else {
                logger.status("%s (%s/%s)", operation, operationNumber, operationNumberMax);
            }
        } else {
            // Sometimes we just get nulls and we don't want to show "null (null/null)"
            logger.status(message("working"));
        }
    }

    @Override
    public void analysisSucceeded(AnalysisEvent event) {
        // At this point bdstool has exited cleanly
        logger.status(message("done"));
        logger.info(message("success"));
        logger.info("");
    }

    @Override
    public void analysisFailed(AnalysisEvent event) {
        // A failure occurred either in bdstool or while resolving the project identifier
        logger.status(message("done"));
        logger.error(message("failure"));
        logger.info("");
    }

}
