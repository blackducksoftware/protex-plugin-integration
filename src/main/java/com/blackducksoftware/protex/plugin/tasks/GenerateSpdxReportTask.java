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
package com.blackducksoftware.protex.plugin.tasks;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.report.Report;
import com.blackducksoftware.sdk.protex.report.SpdxReportConfiguration;
import com.blackducksoftware.sdk.protex.report.SpdxReportFormat;
import com.blackducksoftware.sdk.protex.user.User;

public class GenerateSpdxReportTask extends AbstractTask<Reader> {

    /**
     * SPDX constant indicating the creator of the report is anonymous.
     * <p>
     * See section 3.1.1 of the SPDX 1.1 specification.
     */
    private static final String SPDX_ANONYMOUS_CREATOR = "anonymous";

    private static final Charset REPORT_ENCODING = Charset.forName("UTF-8");

    private final String projectId;

    private final SpdxReportConfiguration request;

    public GenerateSpdxReportTask(ProtexServerProxy proxy, String projectId, SpdxReportConfiguration request) {
        super(proxy);
        this.projectId = projectId;
        this.request = request;
    }

    @Override
    protected Reader execute() throws BuildToolIntegrationException {
        // Resolve the user information if necessary
        if (request.getCreatedBy() == null) {
            try {
                User user = proxy().getUserApi().getUserByEmail(proxy().getUsername());
                // I18N ~cringe~: hopefully this produces the right output since it is all we have
                request.setCreatedBy(user.getFirstName() + " " + user.getLastName());
                request.setCreatedByEMail(user.getEmail());
            } catch (SdkFault fault) {
                request.setCreatedBy(null);
            }
        }

        // Make sure the creator is set to something
        if (request.getCreatedBy() == null) {
            request.setCreatedBy(SPDX_ANONYMOUS_CREATOR);
            request.setCreatedByEMail(null);
        }

        try {
            // Overwrite the server URL with the real value
            request.setProtexUrl(proxy().getServerUrl());

            Report report = proxy().getReportApi().generateSpdxReport(projectId, request, SpdxReportFormat.HTML);
            return new InputStreamReader(report.getFileContent().getInputStream(), REPORT_ENCODING);
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        } catch (IOException e) {
            throw BuildToolIntegrationException.reportReadFailure().initCause(e);
        }
    }
}
