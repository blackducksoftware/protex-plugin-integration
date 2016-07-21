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
package com.blackducksoftware.protex.plugin;

import static java.math.BigInteger.ZERO;

import java.io.File;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observer;

import org.apache.commons.lang.StringUtils;

import com.blackducksoftware.protex.plugin.BDSToolPlumbing.ClientDriverNotifier;

/**
 * A set of builders for creating {@link BlackDuckCommand} instances.
 * <p>
 * Subclasses are expected to override the {@link #arguments()} method.
 *
 * @author jgustie
 */
public abstract class BlackDuckCommandBuilder {

    /**
     * A collection of options for when the command is run in a separate process.
     */
    public static class ProcessOptions {

        /**
         * Flag indicating if the process should be launched in debug mode. When true, the Java Debug Wire Protocol
         * agent library should be enabled with default configuration; additionally class data sharing will be disabled.
         */
        private final boolean debug;

        /**
         * The maximum size in bytes of the Java heap.
         */
        private final long maxHeapSize;

        private ProcessOptions(boolean debug, long maxHeapSize) {
            this.debug = debug;
            this.maxHeapSize = maxHeapSize;
        }

        public boolean isDebug() {
            return debug;
        }

        public long getMaxHeapSize() {
            return maxHeapSize;
        }

        public String getProxyHost(String protocol, Proxy proxy) {
            String proxyHost = ProxyUtil.proxyHost(proxy);
            return proxyHost != null ? proxyHost : System.getProperty(protocol + ".proxyHost");
        }

        public int getProxyPort(String protocol, Proxy proxy) {
            int proxyPort = ProxyUtil.proxyPort(proxy);
            if ((protocol.equalsIgnoreCase("http") && proxyPort == 80)
                    || (protocol.equalsIgnoreCase("https") && proxyPort == 443)) {
                // Eat the default port
                return -1;
            } else if (proxyPort < 0) {
                return Integer.getInteger(protocol + ".proxyPort", -1);
            } else {
                return proxyPort;
            }
        }

        public String getNonProxyHosts() {
            return ProxyUtil.nonProxyHosts();
        }

        public PasswordAuthentication getProxyAuthentication(ProtexServer server) {
            try {
                Proxy proxy = server.getProxy();
                URL serverUrl = new URL(server.getServerUrl());
                String protocol = serverUrl.getProtocol();
                String host = getProxyHost(protocol, proxy);
                int port = getProxyPort(protocol, proxy);
                return Authenticator.requestPasswordAuthentication(host, null, port, protocol, null, null, serverUrl, RequestorType.PROXY);
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    /**
     * A builder for gathering arguments specific to the {@code analyze} command.
     */
    public static class AnalyzeCommandBuilder extends BlackDuckCommandBuilder {

        private String projectId;

        private File directory;

        private boolean force;

        private AnalyzeCommandBuilder() {
            super("analyze");
        }

        public AnalyzeCommandBuilder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public AnalyzeCommandBuilder directory(File directory) {
            this.directory = directory;
            return this;
        }

        public AnalyzeCommandBuilder force(boolean force) {
            this.force = force;
            return this;
        }

        @Override
        protected List<String> arguments() {
            List<String> arguments = super.arguments();
            arguments.add("--no-new-project");
            arguments.add("--pure-project");
            arguments.add(projectId);
            arguments.add("--path");
            arguments.add(directory.getAbsolutePath());
            if (force) {
                arguments.add("--force");
            }
            return arguments;
        }

    }

    /**
     * A builder for gathering arguments specific to the {@code codeprint} command.
     */
    public static class CodePrintCommandBuilder extends BlackDuckCommandBuilder {

        private String codePrintId;

        private String path;

        private CodePrintCommandBuilder() {
            super("codeprint");
        }

        public CodePrintCommandBuilder codePrintId(String codePrintId) {
            this.codePrintId = codePrintId;
            return this;
        }

        public CodePrintCommandBuilder path(String path) {
            this.path = path;
            return this;
        }

        @Override
        protected List<String> arguments() {
            List<String> arguments = super.arguments();
            arguments.add("--no-new-project");
            arguments.add("--pure-project");
            arguments.add(codePrintId);
            arguments.add("--path");
            arguments.add(path);
            return arguments;
        }
    }

    /**
     * The mapping of observers to register with the client driver.
     */
    private final Map<ClientDriverNotifier, List<Observer>> observers = new HashMap<ClientDriverNotifier, List<Observer>>();

    /**
     * The command being executed.
     */
    private final String command;

    /**
     * The connection details of the server to load and execute the client from/against.
     */
    private ProtexServer server;

    /**
     * The file to put the bdstool logs.
     */
    private File logFile;

    /**
     * The java executable to use to run the forked process.
     */
    private File java;

    /**
     * The BDSTOOLJAVAOPTIONS to be used in the forked process.
     */
    private String bdsToolJavaOptions;

    /**
     * Additional configuration used when running the Black Duck command in a separate process.
     * <p>
     * The default behavior is to fork a Java process with a 2g max heap; the permanent generation size (which is not
     * configurable) will be set to 256m.
     */
    private ProcessOptions processOptions = new ProcessOptions(false, (long) Math.pow(1024, 3) * 2L);

    protected BlackDuckCommandBuilder(String command) {
        this.command = command;
    }

    /**
     * Creates a new command builder for the {@code analyze} command.
     */
    public static AnalyzeCommandBuilder analyze() {
        return new AnalyzeCommandBuilder();
    }

    /**
     * Creates a new command builder for the {@code codeprint} command.
     */
    public static CodePrintCommandBuilder codePrint() {
        return new CodePrintCommandBuilder();
    }

    /**
     * Build a new command which can be run (once).
     */
    public final BlackDuckCommand build() throws BuildToolIntegrationException {
        ClassLoader loader = processOptions != null ? null : server.getClientLoader();
        return new BlackDuckCommand(java, arguments(), observers, loader);
    }

    /**
     * Specifies the server details the command should execute against.
     */
    public final BlackDuckCommandBuilder connectedTo(ProtexServer server) {
        this.server = server;
        return this;
    }

    /**
     * Specifies the file where to put the bdstool logs.
     */
    public final void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    /**
     * Specifies the java executable to use to run the forked process.
     */
    public final void setJava(File java) {
        this.java = java;
    }

    /**
     * Specifies the java executable to use to run the forked process.
     */
    public final void setBdsToolJavaOptions(String bdsToolJavaOptions) {
        this.bdsToolJavaOptions = bdsToolJavaOptions;
    }

    /**
     * Forces the Black Duck command to be run in a separate JVM with the specified maximum heap size.
     */
    public final BlackDuckCommandBuilder withMaxHeapSize(long maxHeapSize) {
        processOptions = new ProcessOptions(processOptions.isDebug(), maxHeapSize);
        return this;
    }

    /**
     * Forces the Black Duck command to be run in the current JVM. This has known issues with permanent generation leaks
     * in Java 6 and Java 7; the default behavior is to run the command in a separate JVM.
     */
    public final BlackDuckCommandBuilder runningInProcess() {
        processOptions = null;
        return this;
    }

    /**
     * Adds a progress observer to the resulting command.
     * <p>
     * Observers are expected to be thread-safe.
     */
    public final BlackDuckCommandBuilder observingProgress(Observer observer) {
        addObserver(ClientDriverNotifier.PROGRESS, observer);
        return this;
    }

    /**
     * Adds a message observer to the resulting command.
     * <p>
     * Observers are expected to be thread-safe.
     */
    public final BlackDuckCommandBuilder observingMessages(Observer observer) {
        addObserver(ClientDriverNotifier.MESSAGE, observer);
        return this;
    }

    /**
     * Adds an output observer to the resulting command.
     * <p>
     * Observers are expected to be thread-safe.
     */
    public final BlackDuckCommandBuilder observingOutput(Observer observer) {
        addObserver(ClientDriverNotifier.OUTPUT, observer);
        return this;
    }

    /**
     * Adds a scan log observer to the resulting command. Note that registering with this observable will require
     * compile time access to {@code bdstool} for the scan log entry class used in notifications.
     * <p>
     * Observers are expected to be thread-safe.
     */
    public final BlackDuckCommandBuilder observingScanLog(Observer observer) {
        addObserver(ClientDriverNotifier.SCAN_LOG, observer);
        return this;
    }

    private void addObserver(ClientDriverNotifier observableType, Observer observer) {
        if (!observers.containsKey(observableType)) {
            observers.put(observableType, new LinkedList<Observer>());
        }
        observers.get(observableType).add(observer);
    }

    /**
     * Starts building the argument list, sub-class are expected to override this.
     */
    protected List<String> arguments() {
        List<String> arguments = new LinkedList<String>();
        arguments.add(command);

        // Enable expert mode
        arguments.add("--expert-mode");

        // Reduce logging (use observers if you want that information!)
        arguments.add("--quiet");

        // Server URL
        arguments.add("--server");
        arguments.add(server.getServerUrl());

        // Specify user details
        arguments.add("--user");
        arguments.add(server.getUsername());
        arguments.add("--password");
        arguments.add(server.getPassword().toString());

        if (logFile != null) {
            arguments.add("--log");

            if (logFile.isDirectory()) {
                logFile = new File(logFile, "bdstool.log");
            }
            arguments.add(logFile.getAbsolutePath());
        }
        if (StringUtils.isNotBlank(bdsToolJavaOptions)) {
            parseBdsToolJavaOptions(arguments, bdsToolJavaOptions);
        }

        // Process options
        if (processOptions != null) {
            arguments.add("-J-XX:MaxPermSize=256m");
            if (processOptions.getMaxHeapSize() > 0) {
                arguments.add("-J-Xmx" + memory(processOptions.getMaxHeapSize()));
            }

            if (processOptions.isDebug()) {
                // Enable the socket server using an ephemeral port; suspend so you can actually connect
                arguments.add("-J-agentlib:jdwp=transport=dt_socket,server=y,address=0,suspend=y");
                arguments.add("-J-Xshare:off");
            }

            if (server.getTrustStore() != null) {
                arguments.add("-J-Djavax.net.ssl.trustStore=" + server.getTrustStore().getAbsolutePath());
            }

            // Use the same proxy override for both HTTP and HTTPS
            String protocol = URI.create(server.getServerUrl()).getScheme();
            Proxy proxy = server.getProxy();
            if (processOptions.getProxyHost(protocol, proxy) != null) {
                arguments.add("-J-D" + protocol + ".proxyHost=" + processOptions.getProxyHost(protocol, proxy));
            }
            if (processOptions.getProxyPort(protocol, proxy) > 0) {
                arguments.add("-J-D" + protocol + ".proxyPort=" + processOptions.getProxyPort(protocol, proxy));
            }
            if (processOptions.getNonProxyHosts() != null) {
                arguments.add("-J-Dhttp.nonProxyHosts=" + processOptions.getNonProxyHosts());
            }

            // Only ask for authentication once just in case it is interactive
            PasswordAuthentication proxyAuthentication = processOptions.getProxyAuthentication(server);
            if (proxyAuthentication != null) {
                arguments.add("-J-D" + protocol + ".proxyUser=" + proxyAuthentication.getUserName());
                arguments.add("-J-D" + protocol + ".proxyPassword=" + new String(proxyAuthentication.getPassword()));
            }
        }

        return arguments;
    }

    /**
     * We assume each argument starts with a '-', so when we run into this character
     * if the string so far is not empty we assume this is the start of the next argument.
     *
     * @param arguments
     *            The argument list that will be passed as the command.
     * @param bdsToolJavaOptions
     *            The BDSTOOLJAVAOPTIONS String
     */
    private void parseBdsToolJavaOptions(List<String> arguments, String bdsToolJavaOptions) {
        try {
            StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(bdsToolJavaOptions));
            tokenizer.quoteChar('"');
            tokenizer.quoteChar('\'');
            tokenizer.parseNumbers();
            StringBuilder argumentBuilder = new StringBuilder();
            int token = tokenizer.nextToken();
            while (token != StreamTokenizer.TT_EOF) {
                if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
                    argumentBuilder.append(tokenizer.sval);
                } else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
                    argumentBuilder.append(tokenizer.nval);
                } else if (tokenizer.ttype == StreamTokenizer.TT_EOL) {
                    // We ignore end of lines, we dont want them in the arguments
                } else {
                    if (StringUtils.isNotBlank(tokenizer.sval)) {
                        argumentBuilder.append(tokenizer.sval);
                    } else {
                        char c = (char) token;
                        if (c == '-' && argumentBuilder.length() > 1) {
                            // We assume each option starts with a '-' character
                            // So if we see a '-' and the optionBuilder is not empty, we
                            // add the string we have so far to
                            arguments.add("-J" + argumentBuilder.toString());

                            // We clear the StringBuilder
                            argumentBuilder = new StringBuilder();
                        }
                        argumentBuilder.append(c);
                    }
                }
                token = tokenizer.nextToken();
            }
            arguments.add("-J" + argumentBuilder.toString());
        } catch (IOException e) {
            throw new RuntimeException("Can not parse the BDSTOOLJAVAOPTIONS provided. ", e);
        }
    }

    /**
     * Helper to format byte counts for Java {@code -X} arguments.
     */
    protected static String memory(long bytes) {
        final String suffix = "km";
        final BigInteger unit = BigInteger.valueOf(1024);
        final BigInteger bigBytes = BigInteger.valueOf(bytes);
        for (int i = suffix.length(); i > 0; --i) {
            BigInteger size = bigBytes.divide(unit.pow(i));
            if (size.compareTo(ZERO) > 0) {
                return size.toString() + suffix.charAt(i - 1);
            }
        }
        return bigBytes.toString();
    }
}
