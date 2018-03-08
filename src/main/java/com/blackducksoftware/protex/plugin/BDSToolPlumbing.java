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

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

/**
 * Plumbing for handling {@code bdstool} in a separate process.
 * <p>
 * The protocol is just:
 * 
 * <pre>
 * STREAM := ( SOH &lt;OBSERVER_ID&gt; STX &lt;OBSERVER_CONTENT&gt; ETX )* EOT
 * OBSERVER_ID := BYTE
 * OBSERVER_CONTENT := &lt;STRING&gt; | &lt;MAP&gt;
 * MAP := ( &lt;STRING&gt; US &lt;STRING&gt; &lt;RS&gt; )*
 * STRING := UTF-8
 * </pre>
 * 
 * @author jgustie
 */
public class BDSToolPlumbing {

    /**
     * Types of output we can expect from {@code bdstool}.
     */
    public interface BDSToolSink {
        void sysout(String str);

        void progress(Map<String, String> map);

        void message(Map<String, String> map);

        void output(String out);

        void scanLog(Map<String, String> map);

        void unknown(Map<String, String> map);

        void unknown(String str);
    }

    /**
     * Interface for funneling stuff into the sink.
     */
    public interface BDSToolFunnel {
        void funnel(Object from, BDSToolSink into);
    }

    /**
     * The supported {@code BDSClientDriver} notifiers.
     */
    public enum ClientDriverNotifier implements BDSToolFunnel {
        UNKNOWN(-1, "", "") {
            @Override
            public void funnel(Object from, BDSToolSink into) {
                if (from instanceof Map) {
                    into.unknown(toMap(from));
                } else {
                    into.unknown(toString(from));
                }
            }
        },
        SYSOUT(0, "", "") {
            @Override
            public void funnel(Object from, BDSToolSink into) {
                into.sysout(toString(from));
            }
        },
        PROGRESS(1, "BDSClientProgressNotifier", "getProgressNotifier") {
            @Override
            public void funnel(Object from, BDSToolSink into) {
                into.progress(toMap(from));
            }
        },
        MESSAGE(2, "BDSClientMessageNotifier", "getMessageNotifier") {
            @Override
            public void funnel(Object from, BDSToolSink into) {
                into.message(toMap(from));
            }
        },
        OUTPUT(3, "BDSClientOutputNotifier", "getOutputNotifier") {
            @Override
            public void funnel(Object from, BDSToolSink into) {
                into.output(toString(from));
            }
        },
        SCAN_LOG(4, "BDSClientScanLogNotifier", "getScanLogNotifier") {
            @Override
            public void funnel(Object from, BDSToolSink into) {
                into.scanLog(toMap(from));
            }
        };

        /**
         * Used in the transmission protocol to identify the notification type.
         */
        private final int identifier;

        /**
         * Used by observers to determine who is doing the notification.
         */
        private final String notifierClassSimpleName;

        /**
         * The name of the method on the client driver to get the observable instance.
         */
        private final String driverMethodName;

        private ClientDriverNotifier(int identifier, String notifierClassSimpleName, String driverMethodName) {
            this.identifier = identifier;
            this.notifierClassSimpleName = notifierClassSimpleName;
            this.driverMethodName = driverMethodName;
        }

        /**
         * Converts an object to a non-{@code null} string.
         */
        protected static String toString(Object from) {
            return from != null ? from.toString() : "";
        }

        /**
         * Copies a map into a non-{@code null} map of strings.
         */
        protected static Map<String, String> toMap(Object from) {
            Map<String, String> map;
            if (from instanceof Map) {
                Map<?, ?> input = (Map<?, ?>) from;
                map = new LinkedHashMap<String, String>(input.size());
                for (Entry<?, ?> e : input.entrySet()) {
                    map.put(toString(e.getKey()), toString(e.getValue()));
                }
            } else {
                map = new LinkedHashMap<String, String>();
            }
            return map;
        }

        public String methodName() {
            return driverMethodName;
        }

        public int toInt() {
            return identifier;
        }

        public static ClientDriverNotifier valueOf(int identifier) {
            for (ClientDriverNotifier notifier : values()) {
                if (notifier.identifier == identifier) {
                    return notifier;
                }
            }
            return UNKNOWN;
        }

        public static ClientDriverNotifier valueOf(Observable observable) {
            if (observable instanceof BDSToolForkedObservable) {
                return ((BDSToolForkedObservable) observable).notifier();
            } else {
                final String classSimpleName = observable.getClass().getSimpleName();
                for (ClientDriverNotifier notifier : values()) {
                    if (notifier.notifierClassSimpleName.equals(classSimpleName)) {
                        return notifier;
                    }
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * An observer that can be used to observe any of {@code BDSClientDriver}'s notifiers. All notifications are
     * "pumped" into the sink.
     */
    public static class ObserverPump implements Observer {
        private final BDSToolSink sink;

        public ObserverPump(BDSToolSink sink) {
            this.sink = sink;
        }

        @Override
        public void update(Observable o, Object arg) {
            ClientDriverNotifier.valueOf(o).funnel(arg, sink);
        }
    }

    /**
     * The observable stand-in for when {@code bdstool} is running in a separate process. When run in-process, the
     * "real" observable instance (e.g. an instance of {@code BDSClientProgressNotifier}) is passed along to the
     * observers; we do not have those types available to us if it is running in a separate process so we use this
     * instead.
     */
    public static class BDSToolForkedObservable extends Observable {
        private final ClientDriverNotifier notifier;

        private BDSToolForkedObservable(ClientDriverNotifier notifier) {
            this.notifier = notifier;
        }

        public ClientDriverNotifier notifier() {
            return notifier;
        }

        @Override
        public void notifyObservers(Object arg) {
            // If we are notifying, assume it is because of a change
            setChanged();
            super.notifyObservers(arg);
        }
    }

    /**
     * A sink which creates intermediate observable instances for each notification type.
     */
    public static class ObservablesSink implements BDSToolSink {
        private final Map<ClientDriverNotifier, Observable> observables = new EnumMap<ClientDriverNotifier, Observable>(ClientDriverNotifier.class);

        @Override
        public void sysout(String str) {
            observable(ClientDriverNotifier.SYSOUT).notifyObservers(str);
        }

        @Override
        public void progress(Map<String, String> map) {
            observable(ClientDriverNotifier.PROGRESS).notifyObservers(map);
        }

        @Override
        public void message(Map<String, String> map) {
            observable(ClientDriverNotifier.MESSAGE).notifyObservers(map);
        }

        @Override
        public void output(String out) {
            observable(ClientDriverNotifier.OUTPUT).notifyObservers(out);
        }

        @Override
        public void scanLog(Map<String, String> map) {
            observable(ClientDriverNotifier.SCAN_LOG).notifyObservers(map);
        }

        @Override
        public void unknown(Map<String, String> map) {
            observable(ClientDriverNotifier.UNKNOWN).notifyObservers(map);
        }

        @Override
        public void unknown(String str) {
            observable(ClientDriverNotifier.UNKNOWN).notifyObservers(str);
        }

        public Observable observable(ClientDriverNotifier notifier) {
            synchronized (observables) {
                Observable observable = observables.get(notifier);
                if (observable == null) {
                    observable = new BDSToolForkedObservable(notifier);
                    observables.put(notifier, observable);
                }
                return observable;
            }
        }
    }

    // Fire up the time machine, we are going back to 1963!

    protected static final int EOF = -1;

    protected static final int SOH = 1;

    protected static final int STX = 2;

    protected static final int ETX = 3;

    protected static final int EOT = 4;

    protected static final int RS = 30;

    protected static final int US = 31;

}
