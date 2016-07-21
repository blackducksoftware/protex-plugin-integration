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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import nu.validator.htmlparser.sax.HtmlParser;
import nu.validator.htmlparser.sax.XmlSerializer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServerProxy;
import com.blackducksoftware.protex.plugin.xml.ForwardingContentHandler.SimpleForwardingContentHandler;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.report.Report;
import com.blackducksoftware.sdk.protex.report.ReportFormat;
import com.blackducksoftware.sdk.protex.report.ReportSection;
import com.blackducksoftware.sdk.protex.report.ReportSectionType;
import com.blackducksoftware.sdk.protex.report.ReportTemplateRequest;

public class GenerateProtexReportTask extends AbstractTask<Reader> {

    /**
     * Character encoding for generated reports. The SDK generates the report with the default character set, which for
     * Protex servers, should be explicitly set to UTF-8. Let's hope that is the case.
     */
    private static final Charset REPORT_ENCODING = Charset.forName("UTF-8");

    /**
     * The resource name of the stylesheet used to clean up Protex reports.
     */
    private static final String STYLESHEET_NAME = "/protex-report.xslt";

    /**
     * Different reports have different content.
     */
    private enum ReportDocumentType {
        XHTML, HTML
    }

    /**
     * A file reader which deletes the underlying file when it is closed. This is returned from the API to clean up
     * temporary files created during report generation.
     */
    private static class DeleteOnCloseFileReader extends InputStreamReader {
        private final File file;

        private DeleteOnCloseFileReader(File file, Charset charset) throws FileNotFoundException {
            super(new FileInputStream(file), charset);
            this.file = file;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    /**
     * A SAX content handler which produces XML output with no namespaces on the elements. This is used to simplify the
     * XPath expressions in the XSLT used to clean up the reports.
     */
    private static class NoNamespaceXmlSerializer extends SimpleForwardingContentHandler {
        private NoNamespaceXmlSerializer(OutputStream out) {
            super(new XmlSerializer(out));
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement("", localName, qName, atts);
        }
    }

    private final String projectId;

    private final ReportTemplateRequest request;

    public GenerateProtexReportTask(ProtexServerProxy proxy, String projectId, ReportTemplateRequest request) {
        super(proxy);
        this.projectId = projectId;
        this.request = request;
    }

    @Override
    protected Reader execute() throws BuildToolIntegrationException {
        // The license texts report will contain HTML instead of XHTML
        ReportDocumentType documentType = ReportDocumentType.XHTML;
        for (ReportSection section : request.getSections()) {
            if (section.getSectionType() == ReportSectionType.LICENSE_TEXTS) {
                documentType = ReportDocumentType.HTML;
                break;
            }
        }

        try {
            // Run the report, download it, and transform it into nice clean XHTML
            final Report report = proxy().getReportApi().generateAdHocProjectReport(projectId, request, ReportFormat.HTML, false);
            final File downloadFile = downloadReport(report, documentType);
            final File reportFile = transformReport(downloadFile);

            // Delete the temporary download file and return a reader which will delete the report file
            downloadFile.delete();
            return new DeleteOnCloseFileReader(reportFile, REPORT_ENCODING);
        } catch (SdkFault fault) {
            switch (fault.getFaultInfo().getErrorCode()) {
            default:
                throw handleSdkFault(fault);
            }
        } catch (IOException e) {
            throw BuildToolIntegrationException.reportReadFailure().initCause(e);
        }
    }

    /**
     * Downloads the report, normalizing to XHTML if necessary.
     */
    private File downloadReport(Report report, ReportDocumentType documentType) throws BuildToolIntegrationException, IOException {
        final File reportDownload = File.createTempFile(report.getFileName(), ".download");
        final OutputStream reportDownloadOut = new BufferedOutputStream(new FileOutputStream(reportDownload));

        // Just to be safe...
        reportDownload.deleteOnExit();

        boolean threw = true;
        try {
            if (documentType == ReportDocumentType.XHTML) {
                // Without this processing instruction, XML 1.1 entities will cause parser errors
                reportDownloadOut.write("<?xml version='1.1' encoding='UTF-8'?>\n".getBytes(REPORT_ENCODING));
                report.getFileContent().writeTo(reportDownloadOut);
            } else {
                // Create an HTML parser which forwards parse events to an XML serializer
                final HtmlParser parser = new HtmlParser();
                parser.setContentHandler(new NoNamespaceXmlSerializer(reportDownloadOut));

                // Parse the HTML report, generating XHTML as we go
                final Reader reportReader = new InputStreamReader(report.getFileContent().getInputStream(), REPORT_ENCODING);
                try {
                    parser.parse(new InputSource(reportReader));
                } catch (SAXException e) {
                    throw BuildToolIntegrationException.reportReadFailure().initCause(e);
                } finally {
                    reportReader.close();
                }
            }
            threw = false;
        } catch (IOException e) {
            throw BuildToolIntegrationException.reportReadFailure().initCause(e);
        } finally {
            try {
                reportDownloadOut.close();
            } catch (IOException e) {
                threw = true;
                throw e;
            } finally {
                if (threw) {
                    reportDownload.delete();
                }
            }
        }
        return reportDownload;
    }

    /**
     * Performs an XSLT transformation on the report.
     */
    private File transformReport(final File inFile) throws BuildToolIntegrationException, IOException {
        final File outFile = File.createTempFile("clean-" + inFile.getName(), ".xml");
        try {
            Source xslt = new StreamSource(getClass().getResourceAsStream(STYLESHEET_NAME));
            Transformer transformer = TransformerFactory.newInstance().newTransformer(xslt);
            transformer.transform(new StreamSource(inFile), new StreamResult(outFile));
        } catch (TransformerFactoryConfigurationError e) {
            throw BuildToolIntegrationException.reportProcessingFailure().initCause(e);
        } catch (TransformerConfigurationException e) {
            throw BuildToolIntegrationException.reportProcessingFailure().initCause(e);
        } catch (TransformerException e) {
            throw BuildToolIntegrationException.reportProcessingFailure().initCause(e);
        }
        return outFile;
    }

}
