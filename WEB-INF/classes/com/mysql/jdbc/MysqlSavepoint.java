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

import java.rmi.server.UID;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Represents SQL SAVEPOINTS in MySQL.
 * 
 * @author Mark Matthews
 *
 * @version $Id: MysqlSavepoint.java,v 1.1.2.2 2004/01/20 22:50:50 mmatthew Exp $
 */
public class MysqlSavepoint implements Savepoint {
	private String savepointName;
	
	/**
	 * Creates an unnamed savepoint.
	 * @param conn
	 * 
	 * @throws SQLException if an error occurs
	 */
	MysqlSavepoint() throws SQLException {
		this(getUniqueId());
	}
	
	/**
	 * Creates a named savepoint
	 * @param name the name of the savepoint.
	 * 
	 * @throws SQLException if name == null or is empty.
	 */
	MysqlSavepoint(String name) throws SQLException {
		if (name == null || name.length() == 0) {
			throw new SQLException("Savepoint name can not be NULL or empty", SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
		}
		
		this.savepointName = name;
	}
	
	/**
	 * @see java.sql.Savepoint#getSavepointId()
	 */
	public int getSavepointId() throws SQLException {
		throw new SQLException("Only named savepoints are supported.", SQLError.SQL_STATE_DRIVER_NOT_CAPABLE);
	}

	/**
	 * @see java.sql.Savepoint#getSavepointName()
	 */
	public String getSavepointName() throws SQLException {
		return this.savepointName;
	}

	private static String getUniqueId() {
		// no need to re-invent the wheel here...
		String uidStr = new UID().toString();
		
		int uidLength = uidStr.length();
		
		StringBuffer safeString = new StringBuffer(uidLength);
		
		for (int i = 0; i < uidLength; i++) {
			char c = uidStr.charAt(i);
			
			if (Character.isLetter(c) || Character.isDigit(c)) {
				safeString.append(c);
			} else {
				safeString.append('_');
			}
		}
		
		return safeString.toString();
	}
}
