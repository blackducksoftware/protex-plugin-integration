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

import java.io.Console;

import org.apache.commons.lang.SystemUtils;

/**
 * A logger which provides support for maintaining a status line. The status line is typically the last line in use on
 * the console, we use ANSI control sequences to overwrite this line. In addition to status, there is basic support for
 * other log levels. The default implementations always just prints out to the console, subclasses can forward these
 * methods to more robust loggers if desired.
 *
 * @author jgustie
 */
public class StatusLogger {

    /**
     * The default log message format.
     */
    private static final String LOG = "%-7s %s%n";

    /**
     * The status log message format to use in ANSI mode.
     */
    private static final String ANSI_STATUS_LOG = "\u001B[F\u001B[J%s%n";

    /**
     * The status log message format to use in non-ANSI mode.
     */
    private static final String NON_ANSI_STATUS_LOG = "[STATUS] %s%n";

    /**
     * A reference to the console.
     */
    private final Console console;

    /**
     * Flag indicating we should skip sending standard log messages to the console.
     */
    private final boolean skipLog;

    /**
     * Flag indicating it is safe to use ANSI control sequences.
     */
    private final boolean useAnsi;

    /**
     * Flag indicating we have printed a status message. The first "status" message that gets printed
     * does not clear the current line. This must get reset when log messages are printed so we don't
     * overwrite them.
     */
    private boolean hasStatus = false;

    /**
     * Creates a status logger. The supplied console may be {@code null} in which case no output will be
     * produced.
     */
    public StatusLogger(Console console) {
        this(console, false);
    }

    /**
     * Creates a status logger, optionally ignoring calls to the standard log methods {@code debug}, {@code info},
     * {@code warn} and {@code error}.
     */
    public StatusLogger(Console console, boolean skipLog) {
        this.console = console;
        this.skipLog = skipLog;
        useAnsi = (!SystemUtils.IS_OS_WINDOWS);
    }

    /**
     * Check to see there is a console available. Calls to {@link #console()} will fail if this returns false.
     */
    protected final boolean hasConsole() {
        return console != null;
    }

    /**
     * Returns the current console, failing if the console is not available.
     */
    protected final Console console() {
        if (console != null) {
            return console;
        } else {
            throw new IllegalStateException("no console available");
        }
    }

    public void debug(String message, Object... args) {
        log("[DEBUG]", message, args);
    }

    public void info(String message, Object... args) {
        log("[INFO]", message, args);
    }

    public void warn(String message, Object... args) {
        log("[WARNING]", message, args);
    }

    public void error(String message, Object... args) {
        log("[ERROR]", message, args);
    }

    private void log(String level, String message, Object... args) {
        if (console != null) {
            console.flush();
            if (hasStatus && useAnsi) {
                console.format("\u001B[F\u001B[J");
            }
            if (!skipLog) {
                console.format(LOG, level, String.format(message, args));
            }
            console.flush();
        }
        hasStatus = false;
    }

    public void status(String message, Object... args) {
        if (console != null) {
            final String log = (hasStatus || !useAnsi ? "" : "%n") + (useAnsi ? ANSI_STATUS_LOG : NON_ANSI_STATUS_LOG);
            console.format(log, String.format(message, args));
            console.flush();
        }
        hasStatus = true;
    }
}
