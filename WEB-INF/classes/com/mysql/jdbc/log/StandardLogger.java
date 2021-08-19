/*
   Copyright (C) 2002 MySQL AB

      This program is free software; you can redistribute it and/or modify
      it under the terms of the GNU General Public License as published by
      the Free Software Foundation; either version 2 of the License, or
      (at your option) any later version.

      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.

      You should have received a copy of the GNU General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */
package com.mysql.jdbc.log;

import com.mysql.jdbc.Util;
import com.mysql.jdbc.profiler.ProfilerEvent;

import java.util.Date;

/**
 * Provides logging facilities for those platforms that
 * don't have built-in facilities. Simply logs messages
 * to STDERR.
 * 
 * @author Mark Matthews
 * 
 * @version $Id: StandardLogger.java,v 1.1.2.1 2003/04/18 21:32:53 mmatthew Exp $
 */
public class StandardLogger implements Log {
    private static final int FATAL = 0;
    private static final int ERROR = 1;
    private static final int WARN = 2;
    private static final int INFO = 3;
    private static final int DEBUG = 4;
    private static final int TRACE = 5;
    private static final String LINE_SEPARATOR = System.getProperty(
            "line.separator");
    private static final int LINE_SEPARATOR_LENGTH = LINE_SEPARATOR.length();

    /**
     * Creates a new StandardLogger object.
     *
     * @param name the name of the configuration to
     * use -- ignored
     */
    public StandardLogger(String name) {
    }

	/**
	 * @see com.mysql.jdbc.log.Log#isDebugEnabled()
	 */
	public boolean isDebugEnabled() {
		return true;
	}

	/**
	 * @see com.mysql.jdbc.log.Log#isErrorEnabled()
	 */
	public boolean isErrorEnabled() {
		return true;
	}

	/**
	 * @see com.mysql.jdbc.log.Log#isFatalEnabled()
	 */
	public boolean isFatalEnabled() {
		return true;
	}

	/**
	 * @see com.mysql.jdbc.log.Log#isInfoEnabled()
	 */
	public boolean isInfoEnabled() {
		return true;
	}

	/**
	 * @see com.mysql.jdbc.log.Log#isTraceEnabled()
	 */
	public boolean isTraceEnabled() {
		return true;
	}

	/**
	 * @see com.mysql.jdbc.log.Log#isWarnEnabled()
	 */
	public boolean isWarnEnabled() {
		return true;
	}

	/**
	 * Logs the given message instance using the 
	 * 'debug' level
	 * 
	 * @param message the message to log
	 */
	public void logDebug(Object message) {
		logInternal(DEBUG, message, null);
	}

	/**
	 * Logs the given message and Throwable at
	 * the 'debug' level.
	 * 
	 * @param message the message to log
	 * @param exception the throwable to log (may be null)
	 */
	public void logDebug(Object message, Throwable exception) {
		logInternal(DEBUG, message, exception);
	}

	/**
	 * Logs the given message instance using the 
	 * 'error' level
	 * 
	 * @param message the message to log
	 */
	public void logError(Object message) {
		logInternal(ERROR, message, null);
	}

	/**
	 * Logs the given message and Throwable at
	 * the 'error' level.
	 * 
	 * @param message the message to log
	 * @param exception the throwable to log (may be null)
	 */
	public void logError(Object message, Throwable exception) {
		logInternal(ERROR, message, exception);
	}

	/**
	 * Logs the given message instance using the 
	 * 'fatal' level
	 * 
	 * @param message the message to log
	 */
	public void logFatal(Object message) {
		logInternal(FATAL, message, null);
	}

	/**
	 * Logs the given message and Throwable at
	 * the 'fatal' level.
	 * 
	 * @param message the message to log
	 * @param exception the throwable to log (may be null)
	 */
	public void logFatal(Object message, Throwable exception) {
		logInternal(FATAL, message, exception);
	}

	/**
	 * Logs the given message instance using the 
	 * 'info' level
	 * 
	 * @param message the message to log
	 */
	public void logInfo(Object message) {
		logInternal(INFO, message, null);
	}

	/**
	 * Logs the given message and Throwable at
	 * the 'info' level.
	 * 
	 * @param message the message to log
	 * @param exception the throwable to log (may be null)
	 */
	public void logInfo(Object message, Throwable exception) {
		logInternal(INFO, message, exception);
	}

	/**
	 * Logs the given message instance using the 
	 * 'trace' level
	 * 
	 * @param message the message to log
	 */
	public void logTrace(Object message) {
		logInternal(TRACE, message, null);
	}

	/**
	 * Logs the given message and Throwable at
	 * the 'trace' level.
	 * 
	 * @param message the message to log
	 * @param exception the throwable to log (may be null)
	 */
	public void logTrace(Object message, Throwable exception) {
		logInternal(TRACE, message, exception);
	}

	/**
	 * Logs the given message instance using the 
	 * 'warn' level
	 * 
	 * @param message the message to log
	 */
	public void logWarn(Object message) {
		logInternal(WARN, message, null);
	}

	/**
	 * Logs the given message and Throwable at
	 * the 'warn' level.
	 * 
	 * @param message the message to log
	 * @param exception the throwable to log (may be null)
	 */
	public void logWarn(Object message, Throwable exception) {
		logInternal(WARN, message, exception);
	}

    private String findCallingClassAndMethod(Throwable t) {
        String stackTraceAsString = Util.stackTraceToString(t);

        String callingClassAndMethod = "Caller information not available";

        int endInternalMethods = stackTraceAsString.lastIndexOf(
                "com.mysql.jdbc");

        if (endInternalMethods != -1) {
            int endOfLine = -1;
            int compliancePackage = stackTraceAsString.indexOf("com.mysql.jdbc.compliance",
                    endInternalMethods);

            if (compliancePackage != -1) {
                endOfLine = compliancePackage - LINE_SEPARATOR_LENGTH;
            } else {
                endOfLine = stackTraceAsString.indexOf(LINE_SEPARATOR,
                        endInternalMethods);
            }

            if (endOfLine != -1) {
                int nextEndOfLine = stackTraceAsString.indexOf(LINE_SEPARATOR,
                        endOfLine + LINE_SEPARATOR_LENGTH);

                if (nextEndOfLine != -1) {
                    callingClassAndMethod = stackTraceAsString.substring(endOfLine
                            + LINE_SEPARATOR_LENGTH, nextEndOfLine);
                } else {
                    callingClassAndMethod = stackTraceAsString.substring(endOfLine
                            + LINE_SEPARATOR_LENGTH);
                }
            }
        }

        if (!callingClassAndMethod.startsWith("at ")) {
            return "at " + callingClassAndMethod;
        } else {
            return callingClassAndMethod;
        }
    }

    private void logInternal(int level, Object msg, Throwable exception) {
        StringBuffer msgBuf = new StringBuffer();
        msgBuf.append(new Date().toString());
        msgBuf.append(" ");

        switch (level) {
        case FATAL:
            msgBuf.append("FATAL: ");

            break;

        case ERROR:
            msgBuf.append("ERROR: ");

            break;

        case WARN:
            msgBuf.append("WARN: ");

            break;

        case INFO:
            msgBuf.append("INFO: ");

            break;

        case DEBUG:
            msgBuf.append("DEBUG: ");

            break;

        case TRACE:
            msgBuf.append("TRACE: ");

            break;
        }

        String callerMethodName = "N/A";
        String callerClassName = "N/A";

        if (msg instanceof ProfilerEvent) {
            ProfilerEvent evt = (ProfilerEvent) msg;

            Throwable locationException = evt.getEventCreationPoint();

            int lineNumber = 0;
            String fileName = "N/A";

            if (locationException == null) {
                locationException = new Throwable();
            }

            msgBuf.append("Profiler Event: [");

            switch (evt.getEventType()) {
            case ProfilerEvent.TYPE_EXECUTE:
                msgBuf.append("EXECUTE");

                break;

            case ProfilerEvent.TYPE_FETCH:
                msgBuf.append("FETCH");

                break;

            case ProfilerEvent.TYPE_OBJECT_CREATION:
                msgBuf.append("CONSTRUCT");

                break;

            case ProfilerEvent.TYPE_PREPARE:
                msgBuf.append("PREPARE");

                break;

            case ProfilerEvent.TYPE_QUERY:
                msgBuf.append("QUERY");

                break;

            case ProfilerEvent.TYPE_WARN:
                msgBuf.append("WARN");

                break;

            default:
                msgBuf.append("UNKNOWN");
            }

            msgBuf.append("] ");
            msgBuf.append(findCallingClassAndMethod(locationException));
            msgBuf.append(" duration: ");
            msgBuf.append(evt.getEventDurationMillis());
            msgBuf.append(" ms, connection-id: ");
            msgBuf.append(evt.getConnectionId());
            msgBuf.append(", statement-id: ");
            msgBuf.append(evt.getStatementId());
            msgBuf.append(", resultset-id: ");
            msgBuf.append(evt.getResultSetId());

            String evtMessage = evt.getMessage();

            if (evtMessage != null) {
                msgBuf.append(", message: ");
                msgBuf.append(evtMessage);
            }
        } else {
            Throwable locationException = new Throwable();
            msgBuf.append(findCallingClassAndMethod(locationException));
            msgBuf.append(" ");

            if (msg != null) {
                msgBuf.append(String.valueOf(msg));
            }
        }

        if (exception != null) {
            msgBuf.append(LINE_SEPARATOR);
            msgBuf.append(LINE_SEPARATOR);
            msgBuf.append("EXCEPTION STACK TRACE:");
            msgBuf.append(LINE_SEPARATOR);
            msgBuf.append(LINE_SEPARATOR);
            msgBuf.append(Util.stackTraceToString(exception));
        }

        String messageAsString = msgBuf.toString();

        System.err.println(messageAsString);
    }
}
