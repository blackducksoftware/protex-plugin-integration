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
package com.blackducksoftware.protex.plugin;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.soap.SOAPFaultException;

import com.blackducksoftware.sdk.fault.SdkFault;

/**
 * A wrapper around various Protex SDK and Black Duck Command errors.
 * <p>
 * Instances must be created using the factory methods, you can use {@link #initCause} to supply a cause for the
 * exception (for the "unknown" factory methods which accept a cause, this is not applicable).
 * <p>
 * New errors can be added by adding a new factory method with a corresponding message in the {@value #BUNDLE_BASE_NAME}
 * bundle. The key format is "exception." plus the factory method name.
 *
 * @author jgustie
 */
public final class BuildToolIntegrationException extends Exception {

    private static final String BUNDLE_BASE_NAME = "PluginIntegration";

    /**
     * Mapping of Java class version number to Java version numbers. Why isn't this baked in somewhere?
     */
    private static final Map<String, String> JAVA_CLASS_VERSIONS;
    static {
        Map<String, String> javaClassVersions = new HashMap<String, String>();
        javaClassVersions.put("49.0", "1.5");
        javaClassVersions.put("50.0", "1.6");
        javaClassVersions.put("51.0", "1.7");
        javaClassVersions.put("52.0", "1.8");
        JAVA_CLASS_VERSIONS = Collections.unmodifiableMap(javaClassVersions);
    }

    /**
     * Helper to extract the version number from an unsupported class version error.
     */
    private static String findJavaClassVersion(String string) {
        if (string != null) {
            Matcher matcher = Pattern.compile("\\W(\\d\\d\\.\\d)\\W").matcher(string);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "51.0"; // Default to Java 7, because that is what is causing the issue right ${now}
    }

    public static BuildToolIntegrationException duplicateProjectName(String artifactId, String name) {
        return new BuildToolIntegrationException(true, artifactId, name);
    }

    public static BuildToolIntegrationException noSuchProject(String externalId) {
        return new BuildToolIntegrationException(true, externalId);
    }

    public static BuildToolIntegrationException missingParentProject(String externalId) {
        return new BuildToolIntegrationException(false, externalId);
    }

    public static BuildToolIntegrationException remoteClientDriverNotFound() {
        return new BuildToolIntegrationException(true);
    }

    public static BuildToolIntegrationException remoteClientUnsupportedClassVersion(UnsupportedClassVersionError error) {
        final String expectedJavaClassVersion = findJavaClassVersion(error.getMessage());
        final String expectedJavaVersion = JAVA_CLASS_VERSIONS.get(expectedJavaClassVersion);
        final String javaClassVersion = System.getProperty("java.class.version");
        final String javaVersion = System.getProperty("java.version");
        return new BuildToolIntegrationException(true,
                expectedJavaVersion, expectedJavaClassVersion, javaVersion, javaClassVersion).initCause(error);
    }

    public static BuildToolIntegrationException invalidServerUrl(String serverUrl) {
        return new BuildToolIntegrationException(true, serverUrl);
    }

    public static BuildToolIntegrationException invalidCredentials() {
        return new BuildToolIntegrationException(true);
    }

    public static BuildToolIntegrationException unknownHost(String host) {
        return new BuildToolIntegrationException(true, host);
    }

    public static BuildToolIntegrationException connectionFailed(String serverUrl) {
        return new BuildToolIntegrationException(true, serverUrl);
    }

    public static BuildToolIntegrationException malformedReport() {
        return new BuildToolIntegrationException(true);
    }

    public static BuildToolIntegrationException reportProcessingFailure() {
        return new BuildToolIntegrationException(true);
    }

    public static BuildToolIntegrationException reportReadFailure() {
        return new BuildToolIntegrationException(true);
    }

    // These "unknowns" are used to handle errors that we don't specifically have a message for

    public static BuildToolIntegrationException unknownCommandFailure(String[] messages) {
        // TODO Is there a better way to do this?
        StringBuilder message = new StringBuilder();
        for (String m : messages) {
            message.append(m).append('\n');
        }
        return new BuildToolIntegrationException(new Exception(message.toString()), true);
    }

    public static BuildToolIntegrationException unknownSoapFault(SOAPFaultException fault) {
        return new BuildToolIntegrationException(fault, true);
    }

    public static BuildToolIntegrationException unknownSdkFault(SdkFault fault) {
        return new BuildToolIntegrationException(fault, true);
    }

    public static BuildToolIntegrationException unknownException(Throwable throwable) {
        return new BuildToolIntegrationException(throwable, true);
    }

    /**
     * Flag indicating if this exception is fatal or not. Some build systems will allow errors to be reported without
     * failing the build.
     */
    private final boolean fatal;

    /**
     * The key in the {@value #BUNDLE_BASE_NAME} bundle to use for the exception message.
     */
    private final String key;

    /**
     * The arguments used to format the message. If empty, no formatting is necessary.
     */
    private final Object[] arguments;

    private BuildToolIntegrationException(boolean fatal, Object... arguments) {
        super();
        this.fatal = fatal;
        this.arguments = arguments;
        key = "exception." + getStackTrace()[0].getMethodName();
    }

    private BuildToolIntegrationException(Throwable cause, boolean fatal) {
        super(cause);
        this.fatal = fatal;
        arguments = new Object[0];
        key = "exception.unknown";
    }

    /**
     * Checks to see if this exception is fatal and should stop the build immediately. If {@code false}, callers should
     * make an effort to clean up and allow the build to finish normally.
     */
    public boolean isFatal() {
        return fatal;
    }

    @Override
    public BuildToolIntegrationException initCause(Throwable cause) {
        // Just cast the result to allow fluent invocations
        return (BuildToolIntegrationException) super.initCause(cause);
    }

    @Override
    public String getMessage() {
        // Use the root locale (i.e. the bundle which does not have any locale information)
        return getMessage(Locale.ROOT);
    }

    @Override
    public String getLocalizedMessage() {
        // Use the default locale (which is hopefully set by the OS or build tool)
        return getMessage(Locale.getDefault());
    }

    private String getMessage(Locale locale) {
        String pattern = ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale).getString(key);
        return arguments.length > 0 ? MessageFormat.format(pattern, arguments) : pattern;
    }

}
