/*
   Copyright (C) 2004 MySQL AB
   
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
package com.mysql.jdbc.jdbc2.optional;

import java.sql.SQLException;

import com.mysql.jdbc.SQLError;

/**
 * Base class for all wrapped instances created by LogicalHandle
 * 
 * @author Mark matthews
 * 
 * @version $Id: WrapperBase.java,v 1.1.4.1 2004/02/13 18:27:04 mmatthew Exp $
 */
abstract class WrapperBase {
	protected MysqlPooledConnection pooledConnection;
	
	/**
	 * Fires connection error event if required, before re-throwing exception
	 * 
	 * @param sqlEx the SQLException that has ocurred
	 * @throws SQLException (rethrown)
	 */
	protected void checkAndFireConnectionError(SQLException sqlEx) throws SQLException {
		if (this.pooledConnection != null) {
			if (SQLError.SQL_STATE_COMMUNICATION_LINK_FAILURE.equals(sqlEx.getSQLState())) {
				this.pooledConnection.callListener(MysqlPooledConnection.CONNECTION_ERROR_EVENT, sqlEx);
			}
		}
		
		throw sqlEx;
	}
}