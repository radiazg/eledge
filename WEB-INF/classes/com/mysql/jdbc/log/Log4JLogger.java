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

/**
 * Implementation of log interface for Apache Log4j
 * 
 * @author Mark Matthews
 * 
 * @version $Id: Log4JLogger.java,v 1.1.2.1 2003/09/06 16:38:13 mmatthew Exp $
 */
public class Log4JLogger implements Log {

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#isDebugEnabled()
	 */
	public boolean isDebugEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#isErrorEnabled()
	 */
	public boolean isErrorEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#isFatalEnabled()
	 */
	public boolean isFatalEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#isInfoEnabled()
	 */
	public boolean isInfoEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#isTraceEnabled()
	 */
	public boolean isTraceEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#isWarnEnabled()
	 */
	public boolean isWarnEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logDebug(java.lang.Object)
	 */
	public void logDebug(Object msg) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logDebug(java.lang.Object, java.lang.Throwable)
	 */
	public void logDebug(Object msg, Throwable thrown) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logError(java.lang.Object)
	 */
	public void logError(Object msg) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logError(java.lang.Object, java.lang.Throwable)
	 */
	public void logError(Object msg, Throwable thrown) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logFatal(java.lang.Object)
	 */
	public void logFatal(Object msg) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logFatal(java.lang.Object, java.lang.Throwable)
	 */
	public void logFatal(Object msg, Throwable thrown) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logInfo(java.lang.Object)
	 */
	public void logInfo(Object msg) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logInfo(java.lang.Object, java.lang.Throwable)
	 */
	public void logInfo(Object msg, Throwable thrown) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logTrace(java.lang.Object)
	 */
	public void logTrace(Object msg) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logTrace(java.lang.Object, java.lang.Throwable)
	 */
	public void logTrace(Object msg, Throwable thrown) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logWarn(java.lang.Object)
	 */
	public void logWarn(Object msg) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.mysql.jdbc.log.Log#logWarn(java.lang.Object, java.lang.Throwable)
	 */
	public void logWarn(Object msg, Throwable thrown) {
		// TODO Auto-generated method stub

	}

}
