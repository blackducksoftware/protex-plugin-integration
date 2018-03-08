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

import java.io.PrintStream;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.blackducksoftware.protex.plugin.BDSToolPlumbing.ClientDriverNotifier;
import com.blackducksoftware.protex.plugin.BDSToolPlumbing.ObserverPump;
import com.blackducksoftware.protex.plugin.BDSToolSource.TransmissionStream;

/**
 * This class is used to invoke the tool driver from a separate JVM. For example, the original process should fork a
 * {@code java} process that runs this code.
 *
 * @author jgustie
 */
public class BDSToolRemoteRunner {

    private static final String DRIVER_CLASS_NAME = "com.blackducksoftware.bdsclient.BDSClientDriver";

    public static void main(String[] args) {
        int status = 0;
        boolean closeSystemOut = false;
        try {
            ClassLoader loader = loader(args);
            Thread.currentThread().setContextClassLoader(loader);

            // Create the client driver
            Class<?> clientDriverClass = loader.loadClass(DRIVER_CLASS_NAME);
            Object clientDriver = clientDriverClass.newInstance();

            // Porcelain implementation, we are going to wrap everything into the transmission stream
            List<String> argList = new ArrayList<String>(Arrays.asList(args));
            if (argList.remove("--porcelain")) {
                final TransmissionStream transmitter = new TransmissionStream();
                closeSystemOut = true;
                System.setOut(new PrintStream(transmitter));
                Observer observer = new ObserverPump(transmitter);
                for (ClientDriverNotifier notifier : ClientDriverNotifier.values()) {
                    if (!notifier.methodName().isEmpty()) {
                        ((Observable) clientDriverClass.getMethod(notifier.methodName()).invoke(clientDriver)).addObserver(observer);
                    }
                }
            }

            Object clientDriverArgs = argList.toArray(new String[argList.size()]);
            if (clientDriverClass.getMethod("execute", String[].class).invoke(clientDriver, clientDriverArgs) != null) {
                status = 1;
            }
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            status = 1;
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            status = 1;
        } catch (Throwable e) {
            e.printStackTrace();
            status = 1;
        } finally {
            try {
                // Sleep so the pump has time to pump the error messages
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignore interrupted
            }
            // If we created our system out, close it (sends an EOT to the consumer)
            if (closeSystemOut) {
                System.out.close();
            }

            // Report failure status
            if (status != 0) {
                System.exit(status);
            }
        }
    }

    /**
     * Creates the class loader.
     */
    private static ClassLoader loader(String[] args) {
        try {
            return new URLClassLoader(new URL[] { new URL(server(args) + "repo/lib/bdsclient.jar") });
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to construct classpath URL", e);
        }
    }

    /**
     * Finds the {@code --server} option and ensures it ends with a "/".
     */
    private static String server(String[] args) {
        for (int i = 1; i < args.length; ++i) {
            if (args[i - 1].equals("--server")) {
                String server = args[i];
                if (!server.endsWith("/")) {
                    server += "/";
                }
                return server;
            }
        }
        throw new IllegalStateException("No server, unable to execute remote tool driver");
    }

    static {
        // This authenticator will get overridden by bdstool with it's own version.
        // We need this to get through the proxy to download bdstool though

        // http://rolandtapken.de/blog/2012-04/java-process-httpproxyuser-and-httpproxypassword
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() == RequestorType.PROXY) {
                    // Only look at the http settings (not the HTTPS settings)
                    String user = System.getProperty("http.proxyUser");
                    String password = System.getProperty("http.proxyPassword");
                    if (user != null && password != null) {
                        String protocol = getRequestingProtocol().toLowerCase();
                        String host = System.getProperty(protocol + ".proxyHost", "");
                        String port = System.getProperty(protocol + ".proxyPort", "0");
                        if (getRequestingHost().toLowerCase().equals(host.toLowerCase())) {
                            if (Integer.parseInt(port) == getRequestingPort()) {
                                // Seems to be OK.
                                return new PasswordAuthentication(user, password.toCharArray());
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

}
