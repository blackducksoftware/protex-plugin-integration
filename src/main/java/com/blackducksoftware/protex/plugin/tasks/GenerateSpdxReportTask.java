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
