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
package com.mysql.jdbc;

import java.net.BindException;

import java.sql.SQLException;

/**
 * An exception to represent communications errors with the database.
 * 
 * Attempts to provide 'friendler' error messages to end-users, including
 * last time a packet was sent to the database, what the client-timeout
 * is set to, and whether the idle time has been exceeded.
 * 
 * @author Mark Matthews
 * 
 * @version $Id: CommunicationsException.java,v 1.1.2.4 2004/02/06 17:17:28 mmatthew Exp $
 */
public class CommunicationsException extends SQLException {
	
	private static final long DEFAULT_WAIT_TIMEOUT_SECONDS = 28800;
	private static final int DUE_TO_TIMEOUT_FALSE = 0;
	private static final int DUE_TO_TIMEOUT_TRUE = 1;
	private static final int DUE_TO_TIMEOUT_MAYBE = 2;
	private String exceptionMessage;
	
	public CommunicationsException(Connection conn, long lastPacketSentTimeMs, Exception underlyingException) {
		
		long serverTimeoutSeconds = 0;
		boolean isInteractiveClient = false;
		
		if (conn != null) {
			isInteractiveClient = conn.getInteractiveClient();
			
			String serverTimeoutSecondsStr = null;
			
			if (isInteractiveClient) {
				serverTimeoutSecondsStr = conn.getServerVariable("interactive_timeout");
			} else {
				serverTimeoutSecondsStr = conn.getServerVariable("wait_timeout");
			}
			
			if (serverTimeoutSecondsStr != null) {
				try {
					serverTimeoutSeconds = Long.parseLong(serverTimeoutSecondsStr);
				} catch (NumberFormatException nfe) {
					serverTimeoutSeconds = 0;
				}
			}
		}
		
		StringBuffer exceptionMessageBuf = new StringBuffer();
		
		if (lastPacketSentTimeMs == 0) {
			lastPacketSentTimeMs = System.currentTimeMillis();
		}
		
		long timeSinceLastPacket = (System.currentTimeMillis() - lastPacketSentTimeMs) / 1000;
		
		int dueToTimeout = DUE_TO_TIMEOUT_FALSE;
		
		StringBuffer timeoutMessageBuf = null;
		
		
		if (serverTimeoutSeconds != 0) {
			if (timeSinceLastPacket > serverTimeoutSeconds) {
				dueToTimeout = DUE_TO_TIMEOUT_TRUE;
				
				timeoutMessageBuf = new StringBuffer();
				
				timeoutMessageBuf.append(" is longer than the server configured value of ");
				
				if (!isInteractiveClient) {
					timeoutMessageBuf.append("'wait_timeout'");
				} else {
					timeoutMessageBuf.append("'interactive_timeout'");
				}
				
				
			}
		} else if (timeSinceLastPacket > DEFAULT_WAIT_TIMEOUT_SECONDS){
			dueToTimeout = DUE_TO_TIMEOUT_MAYBE;
			
			timeoutMessageBuf = new StringBuffer();
			
			timeoutMessageBuf.append("may or may not be greater than the server-side timeout ");
			timeoutMessageBuf.append("(the driver was unable to determine the value of either the ");
			timeoutMessageBuf.append("'wait_timeout' or 'interactive_timeout' configuration values from ");
			timeoutMessageBuf.append("the server.");
		}
		
		if (dueToTimeout == DUE_TO_TIMEOUT_TRUE || dueToTimeout == DUE_TO_TIMEOUT_MAYBE) {
			
			exceptionMessageBuf.append("The last communications with the server was ");
			exceptionMessageBuf.append(timeSinceLastPacket);
			exceptionMessageBuf.append(" seconds ago, which ");
					
			if (timeoutMessageBuf != null) {
				exceptionMessageBuf.append(timeoutMessageBuf);
			}
			
			exceptionMessageBuf.append(". You should consider either expiring and/or testing connection validity ");
			exceptionMessageBuf.append("before use in your application, increasing the server configured values for client timeouts, ");
			exceptionMessageBuf.append("or using the Connector/J connection property 'autoReconnect=true' to avoid this problem.");
			
		} else {
			//
			// Attempt to determine the reason for the underlying exception
			// (we can only make a best-guess here)
			//
		
			if (underlyingException instanceof BindException) {
				// too many client connections???
				exceptionMessageBuf.append("The driver was unable to create a connection due to ");
				exceptionMessageBuf.append("an inability to establish the client portion of a socket.\n\n");
				exceptionMessageBuf.append("This is usually caused by a limit on the number of sockets imposed by ");
				exceptionMessageBuf.append("the operating system. This limit is usually configurable. \n\n");
				exceptionMessageBuf.append("For Unix-based platforms, see the manual page for the 'ulimit' command. Kernel or system reconfiguration may also be required.");
				exceptionMessageBuf.append("\n\nFor Windows-based platforms, see Microsoft Knowledge Base Article 196271 (Q196271).");
			}
		}
		
		if (exceptionMessageBuf.length() == 0) {
			// We haven't figured out a good reason, so copy it.
			exceptionMessageBuf.append("Communications link failure");
			
			if (underlyingException != null) {
				exceptionMessageBuf.append(" due to underlying exception: ");
				exceptionMessageBuf.append(Util.stackTraceToString(underlyingException));
			}
		}
		
		this.exceptionMessage = exceptionMessageBuf.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.sql.SQLException#getSQLState()
	 */
	public String getSQLState() {
		return SQLError.SQL_STATE_COMMUNICATION_LINK_FAILURE;
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage() {
		return this.exceptionMessage;
	}

}
