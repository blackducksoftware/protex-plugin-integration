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
