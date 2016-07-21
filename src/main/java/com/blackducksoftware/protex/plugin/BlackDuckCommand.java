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
package com.blackducksoftware.protex.plugin;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import com.blackducksoftware.protex.plugin.BDSToolPlumbing.ClientDriverNotifier;
import com.blackducksoftware.protex.plugin.BDSToolPlumbing.ObservablesSink;

/**
 * An encapsulation of a {@code bdstool} invocation. There is a lot of junk in here mostly related to reflectively
 * calling the {@code BDSClientDriver} and using the Java {@code Observable} model for maintaining status.
 *
 * @author jgustie
 */
public final class BlackDuckCommand extends Observable {

    /**
     * Used for converting {@code List<String>} to {@code String[]}.
     */
    private static final String[] EMPTY_ARGUMENTS = new String[0];

    /**
     * The arguments to invoke the tool with.
     */
    private final List<String> arguments;

    /**
     * The mapping of observable types to observers for this command.
     */
    private final Map<ClientDriverNotifier, List<Observer>> observers;

    /**
     * The remote class laoder.
     */
    private final ClassLoader loader;

    /**
     * The java executable to use to run the forked process.
     */
    private File java;

    /**
     * The current state of this command.
     */
    private State state = State.NEW;

    BlackDuckCommand(List<String> arguments, Map<ClientDriverNotifier, List<Observer>> observers, ClassLoader loader) {
        this(null, arguments, observers, loader);
    }

    BlackDuckCommand(File java, List<String> arguments, Map<ClientDriverNotifier, List<Observer>> observers, ClassLoader loader) {
        this.arguments = arguments;
        this.observers = observers;
        this.loader = loader;
        this.java = java;

        // Add all of the observers to this command so they can participate in command lifecycle events as well
        for (List<Observer> commandObservers : observers.values()) {
            for (Observer observer : commandObservers) {
                addObserver(observer);
            }
        }
    }

    /**
     * Returns the current state of this command.
     */
    public State state() {
        return state;
    }

    /**
     * Executes this command.
     */
    public void run() throws BuildToolIntegrationException {
        notifyInitiated();
        try {
            ClientDriver driver = loader != null ? new ClassLoaderClientDriver(loader) : new ForkedClientDriver(java);

            for (Entry<ClientDriverNotifier, List<Observer>> entry : observers.entrySet()) {
                Observable observable = driver.getNotifier(entry.getKey());
                for (Observer observer : entry.getValue()) {
                    observable.addObserver(observer);
                }
            }

            driver.execute(arguments);
            notifySucceeded();
        } catch (BuildToolIntegrationException e) {
            notifyFailed(e);
            throw e;
        }
    }

    /**
     * Changes the state from an expected state. If the current state does not match the expected state, an exception is
     * thrown. This method also marks the command as {@linkplain #hasChanged() changed}.
     */
    private void changeState(State newState, State expectedState) {
        if (state == expectedState) {
            state = newState;
            setChanged();
        } else {
            throw new IllegalStateException(expectedState.toString());
        }
    }

    private void notifyInitiated() {
        changeState(State.INITIATED, State.NEW);
        notifyObservers();
    }

    private void notifySucceeded() {
        changeState(State.SUCCEEDED, State.INITIATED);
        notifyObservers();
    }

    private void notifyFailed(BuildToolIntegrationException cause) {
        changeState(State.FAILED, State.INITIATED);
        notifyObservers(cause);
    }

    /**
     * The possible states of a command.
     */
    public enum State {
        /**
         * The command has been created but the {@code run} method has not been invoked.
         */
        NEW,

        /**
         * The {@code run} method has been invoked.
         */
        INITIATED,

        /**
         * The {@code run} method completed normally.
         */
        SUCCEEDED,

        /**
         * The {@code run} method failed.
         */
        FAILED
    }

    /**
     * This exception is thrown when the reflective invocations against {@code BDSClientDriver} fail. Generally this
     * means that {@code BDSClientDriver} has changed and reflective calls need to be updated to reflect the new
     * contracts.
     */
    public static class BlackDuckCommandReflectionException extends RuntimeException {
        private BlackDuckCommandReflectionException(NoSuchMethodException e) {
            super(e);
        }

        private BlackDuckCommandReflectionException(IllegalAccessException e) {
            super(e);
        }

        private BlackDuckCommandReflectionException(SecurityException e) {
            super(e);
        }

        private BlackDuckCommandReflectionException(InstantiationException e) {
            super(e);
        }
    }

    /**
     * A wrapper around the remote reflective access to {@code BDSClientDriver}.
     */
    private static abstract class ClientDriver {
        protected abstract Observable getNotifier(ClientDriverNotifier observer);

        protected abstract void execute(List<String> arguments) throws BuildToolIntegrationException;
    }

    /**
     * A wrapper around an in process {@code BDSClientDriver}. This is known to cause perm gen issues because the class
     * loader can never be garbage collected due to lingering references.
     */
    private static final class ClassLoaderClientDriver extends ClientDriver {

        /**
         * The class name of the client driver.
         */
        private static final String DRIVER_CLASS_NAME = "com.blackducksoftware.bdsclient.BDSClientDriver";

        /**
         * The method name used to invoke the client driver.
         */
        private static final String EXECUTE_METHOD_NAME = "execute";

        /**
         * The instance of {@code BDSClientDriver} loaded from a remote class loader.
         */
        private final Object instance;

        /**
         * A reflective handle to the {@code BDSClientDriver#execute} method.
         */
        private final Method executeMethod;

        private ClassLoaderClientDriver(ClassLoader loader) throws BuildToolIntegrationException {
            try {
                Class<?> driverType = loader.loadClass(DRIVER_CLASS_NAME);
                instance = driverType.getConstructor(Boolean.TYPE).newInstance(false);
                executeMethod = driverType.getDeclaredMethod(EXECUTE_METHOD_NAME, String[].class);
            } catch (ClassNotFoundException e) {
                throw BuildToolIntegrationException.remoteClientDriverNotFound().initCause(e);
            } catch (UnsupportedClassVersionError e) {
                throw BuildToolIntegrationException.remoteClientUnsupportedClassVersion(e);
            } catch (SecurityException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (InstantiationException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (IllegalAccessException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (NoSuchMethodException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (InvocationTargetException e) {
                throw handle(e);
            }
        }

        @Override
        protected Observable getNotifier(ClientDriverNotifier observer) {
            try {
                Method notifierMethod = instance.getClass().getDeclaredMethod(observer.methodName());
                return (Observable) notifierMethod.invoke(instance);
            } catch (SecurityException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (IllegalAccessException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (NoSuchMethodException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (InvocationTargetException e) {
                throw handle(e);
            }
        }

        @Override
        protected void execute(List<String> arguments) throws BuildToolIntegrationException {
            try {
                // Strip any -J options before converting to an array
                for (Iterator<String> i = arguments.iterator(); i.hasNext();) {
                    if (i.next().startsWith("-J")) {
                        i.remove();
                    }
                }
                Object args = arguments.toArray(EMPTY_ARGUMENTS);

                String[] output = (String[]) executeMethod.invoke(instance, args);
                if (output != null) {
                    throw BuildToolIntegrationException.unknownCommandFailure(output);
                }
            } catch (IllegalAccessException e) {
                throw new BlackDuckCommandReflectionException(e);
            } catch (InvocationTargetException e) {
                throw handle(e);
            }
        }

        private RuntimeException handle(InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error) {
                // We cannot return this, so just re-throw it
                throw (Error) cause;
            } else if (!(cause instanceof RuntimeException)) {
                // BDSClientDriver does not have any checked exceptions
                cause = new UndeclaredThrowableException(cause);
            }
            return (RuntimeException) cause;
        }
    }

    /**
     * A wrapper around an execution of {@code bdstool} that delegates to a separate process.
     */
    private static final class ForkedClientDriver extends ClientDriver {

        private final File java;

        private final ObservablesSink notifiers = new ObservablesSink();

        private ForkedClientDriver() {
            this(null);
        }

        private ForkedClientDriver(File java) {
            if (java != null) {
                this.java = java;
            } else {
                this.java = new File(new File(System.getProperty("java.home"), "bin"), "java");
            }
        }

        @Override
        protected Observable getNotifier(ClientDriverNotifier notifier) {
            return notifiers.observable(notifier);
        }

        @Override
        protected void execute(List<String> arguments) throws BuildToolIntegrationException {
            // Build up the JVM command
            List<String> command = new ArrayList<String>();
            command.add(java.getAbsolutePath());
            command.add("-classpath");
            command.add(classpath());

            // Mimic javac's -J option
            for (Iterator<String> i = arguments.iterator(); i.hasNext();) {
                String arg = i.next();
                if (arg.startsWith("-J")) {
                    command.add(arg.substring(2));
                    i.remove();
                }
            }

            // Build up the bdstool command
            command.add(BDSToolRemoteRunner.class.getName());
            command.addAll(arguments);
            command.add("--porcelain");
            Process bdstool = null;
            ThreadErrorChecker errorChecker = null;
            ThreadPump pump = null;
            // Run it
            try {
                bdstool = new ProcessBuilder(command).redirectOutput(Redirect.PIPE).redirectError(Redirect.PIPE).start(); // .redirectError(INHERIT).start();

                // // check error in separate thread loop, dont throw exception store error in variable
                errorChecker = new ThreadErrorChecker(bdstool.getErrorStream());
                errorChecker.start();

                // pump the output in a separate thread, store exceptions
                pump = new ThreadPump(bdstool, notifiers);
                pump.start();

                synchronized (this) {
                    while (true) {
                        // wait 100 millseconds,
                        // if the main thread has been interrupted an InterruptedException will be thrown
                        this.wait(100);
                        try {
                            // check exit value of the process
                            int exitValue = bdstool.exitValue();
                            // process has finished, check for errors in the pump
                            if (exitValue != 0) {
                                // process finished unsuccessfully

                                boolean unsupportedClassVersionError = false;
                                String[] message = new String[0];
                                if (errorChecker.hasErrorMessage()) {
                                    String errorMessage = errorChecker.getErrorMessage();
                                    // We send the messages from the error stream to the output notifier
                                    notifiers.output(errorMessage);

                                    if (errorMessage.contains(UnsupportedClassVersionError.class.getName())) {
                                        // Check to see if "UnsupportedClassVersionError" was logged to the error stream
                                        unsupportedClassVersionError = true;
                                    }

                                    message = new String[] { errorChecker.getErrorMessage() };
                                }
                                if (pump.hasException() && !unsupportedClassVersionError) {
                                    // Only check the pump exception if there was no UnsupportedClassVersionError
                                    // printed already

                                    // otherwise we get a confusing EOF exception even though
                                    // we already logged the real error
                                    BuildToolIntegrationException failure = BuildToolIntegrationException.unknownException(pump.getException());
                                    failure.addSuppressed(new RuntimeException("Command failed: " + maskPasswordsInCommand(command)));
                                    throw failure;
                                }

                                if (!unsupportedClassVersionError) {
                                    // If there was no UnsupportedClassVersionError logged and no exception in the pump
                                    // then we throw this unknownCommandFailure exception
                                    throw BuildToolIntegrationException.unknownCommandFailure(message);
                                } else {
                                    // we already logged the error, just need to break now
                                    break;
                                }
                            } else {
                                break;
                            }
                        } catch (IllegalThreadStateException e) {
                            // process has not yet terminated, check for errors
                            if (errorChecker.hasErrorMessage()) {
                                String errorMessage = errorChecker.getErrorMessage();
                                // We send the messages from the error stream to the output notifier
                                // This should print
                                // "Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize" without
                                // failing the build
                                notifiers.output(errorMessage);
                            }
                            if (pump.hasException()) {
                                BuildToolIntegrationException failure = BuildToolIntegrationException.unknownException(pump.getException());
                                failure.addSuppressed(new RuntimeException("Command failed: " + maskPasswordsInCommand(command)));
                                throw failure;
                            }
                            // If no error messages continue waiting for the process to finish
                        }
                    }
                }
            } catch (IOException e) {
                // For debugging purposes, it would be useful to see what the command line options were
                BuildToolIntegrationException failure = BuildToolIntegrationException.unknownException(e);
                failure.addSuppressed(new RuntimeException("Command failed: " + maskPasswordsInCommand(command)));
                throw failure;
            } catch (InterruptedException e) {
                throw BuildToolIntegrationException.unknownException(e);
            } finally {
                // clean up threads, destroy process, etc.
                // if (errorChecker != null) {
                // errorChecker.interrupt();
                // }
                if (pump != null) {
                    pump.interrupt();
                }
                if (bdstool != null) {
                    bdstool.destroy();
                }
            }
        }

        protected class ThreadPump extends Thread {
            private final Process bdstool;

            private final ObservablesSink notifiers;

            private Throwable exception;

            protected ThreadPump(Process bdstool, ObservablesSink notifiers) {
                super("BDSTool Pump Thread");
                this.bdstool = bdstool;
                this.notifiers = notifiers;
            }

            public Throwable getException() {
                return exception;
            }

            public Boolean hasException() {
                return exception != null;
            }

            @Override
            public void run() {
                try {
                    BDSToolSource.pump(new BufferedInputStream(bdstool.getInputStream()), notifiers);
                } catch (IOException e) {
                    exception = e;
                }
            }
        }

        protected class ThreadErrorChecker extends Thread {
            private final InputStream errorStream;

            private StringBuilder errorMessage = new StringBuilder();

            public String getErrorMessage() {
                String errorMessageToReturn = errorMessage.toString();
                errorMessage = new StringBuilder(); // reset string builder so we dont re-print the error
                return errorMessageToReturn;
            }

            public boolean hasErrorMessage() {
                return errorMessage.length() > 0;
            }

            protected ThreadErrorChecker(InputStream errorStream) {
                super("Error Checker Thread");
                this.errorStream = errorStream;
            }

            @Override
            public void run() {
                BufferedReader stdError = new BufferedReader(new InputStreamReader(errorStream));
                String tmp = null;
                try {
                    while ((tmp = stdError.readLine()) != null) {
                        errorMessage.append(tmp + System.lineSeparator());
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        private String maskPasswordsInCommand(List<String> command) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < command.size(); i++) {
                if (i != 0) {
                    stringBuilder.append(", ");
                }
                if (command.get(i).matches("-D.*Password.*=.*")) {
                    // Ex : -Dhttps.proxyPassword=xxxxxxxx
                    String proxyPasswordCommand = command.get(i);
                    int startOfPassword = proxyPasswordCommand.indexOf("=") + 1;

                    proxyPasswordCommand = proxyPasswordCommand.substring(0, startOfPassword) + "*********";

                    stringBuilder.append(proxyPasswordCommand);

                } else if (command.get(i).matches("--.*password.*")) {
                    // Ex : --password, xxxxxxxxx
                    stringBuilder.append(command.get(i));
                    stringBuilder.append(", *********");
                    i++;
                } else {
                    stringBuilder.append(command.get(i));
                }
            }

            return stringBuilder.toString();
        }

        protected String classpath() {
            try {
                // The boot strap class path includes our own code source for the BDSToolRemoteRunner
                ProtectionDomain pd = getClass().getProtectionDomain();
                URL codeLocation = (pd != null && pd.getCodeSource() != null) ? pd.getCodeSource().getLocation() : null;

                // Not all class loaders supply a code source, work around is trying getResource()
                if (codeLocation == null) {
                    String classResourceName = BDSToolRemoteRunner.class.getName().replace('.', '/') + ".class";
                    URL classLocation = getClass().getClassLoader().getResource(classResourceName);
                    if (classLocation != null && classLocation.getProtocol().equalsIgnoreCase("JAR")) {
                        // Inside a JAR, extract the base location
                        String schemeSpecificPart = classLocation.toURI().getSchemeSpecificPart();
                        int pos = schemeSpecificPart.indexOf('!');
                        codeLocation = new URI(pos < 0 ? schemeSpecificPart : schemeSpecificPart.substring(0, pos)).toURL();
                    } else if (classLocation != null && classLocation.toString().endsWith(classResourceName)) {
                        // Just try stripping the class name part
                        String url = classLocation.toString();
                        codeLocation = new URL(url.substring(0, url.length() - classResourceName.length()));
                    }
                }

                // TODO Should we support "HTTP(S)" as well?
                if (codeLocation != null && codeLocation.getProtocol().equalsIgnoreCase("file")) {
                    return new File(codeLocation.toURI()).getAbsolutePath();
                }

                // Fall through, not much we can do
                return ".";
            } catch (MalformedURLException e) {
                return ".";
            } catch (URISyntaxException e) {
                return ".";
            }
        }
    }
}
