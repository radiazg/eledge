/*
 * MM JDBC Drivers for MySQL
 *
 * $Id: ResultSet.java,v 1.3 2002/05/15 03:16:55 mark_matthews Exp $
 *
 * Copyright (C) 1998 Mark Matthews <mmatthew@worldserver.com>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 *
 * See the COPYING file located in the top-level-directory of
 * the archive of this library for complete text of license.
 *
 * Some portions:
 *
 * Copyright (c) 1996 Bradley McLean / Jeffrey Medeiros
 * Modifications Copyright (c) 1996/1997 Martin Rode
 * Copyright (c) 1997 Peter T Mount
 */

/**
 * A ResultSet provides access to a table of data generated by executing a
 * Statement.  The table rows are retrieved in sequence.  Within a row its
 * column values can be accessed in any order.
 *
 * <P>A ResultSet maintains a cursor pointing to its current row of data.
 * Initially the cursor is positioned before the first row.  The 'next'
 * method moves the cursor to the next row.
 *
 * <P>The getXXX methods retrieve column values for the current row.  You can
 * retrieve values either using the index number of the column, or by using
 * the name of the column.  In general using the column index will be more
 * efficient.  Columns are numbered from 1.
 *
 * <P>For maximum portability, ResultSet columns within each row should be read
 * in left-to-right order and each column should be read only once.
 *
 *<P> For the getXXX methods, the JDBC driver attempts to convert the
 * underlying data to the specified Java type and returns a suitable Java
 * value.  See the JDBC specification for allowable mappings from SQL types
 * to Java types with the ResultSet getXXX methods.
 *
 * <P>Column names used as input to getXXX methods are case insenstive.  When
 * performing a getXXX using a column name, if several columns have the same
 * name, then the value of the first matching column will be returned.  The
 * column name option is designed to be used when column names are used in the
 * SQL Query.  For columns that are NOT explicitly named in the query, it is
 * best to use column numbers.  If column names were used there is no way for
 * the programmer to guarentee that they actually refer to the intended
 * columns.
 *
 * <P>A ResultSet is automatically closed by the Statement that generated it
 * when that Statement is closed, re-executed, or is used to retrieve the
 * next result from a sequence of multiple results.
 *
 * <P>The number, types and properties of a ResultSet's columns are provided by
 * the ResultSetMetaData object returned by the getMetaData method.
 *
 * @see ResultSetMetaData
 * @see java.sql.ResultSet
 * @author Mark Matthews <mmatthew@worldserver.com>
 * @version $Id: ResultSet.java,v 1.3 2002/05/15 03:16:55 mark_matthews Exp $
 */

package com.mysql.jdbc.jdbc1;

import java.io.*;
import java.math.*;
import java.text.*;
import java.util.*;
import java.sql.*;

public class ResultSet extends com.mysql.jdbc.ResultSet 
    implements java.sql.ResultSet
{
    /**
     * The numbers, types and properties of a ResultSet's columns are
     * provided by the getMetaData method
     *
     * @return a description of the ResultSet's columns
     * @exception java.sql.SQLException if a database access error occurs
     */
  
    public java.sql.ResultSetMetaData getMetaData() throws java.sql.SQLException
    {
	return new com.mysql.jdbc.jdbc1.ResultSetMetaData(_rows, _fields);
    }
  
    // ****************************************************************
    //
    //                       END OF PUBLIC INTERFACE
    //
    // ****************************************************************

    /**
     * Create a new ResultSet - Note that we create ResultSets to
     * represent the results of everything.
     *
     * @param fields an array of Field objects (basically, the
     *    ResultSet MetaData)
     * @param tuples Vector of the actual data
     * @param status the status string returned from the back end
     * @param updateCount the number of rows affected by the operation
     * @param cursor the positioned update/delete cursor name
     */

    public ResultSet(com.mysql.jdbc.Field[] Fields, Vector Tuples, com.mysql.jdbc.Connection Conn)
    {
	super(Fields, Tuples, Conn);
    }
	
    public ResultSet(com.mysql.jdbc.Field[] Fields, Vector Tuples)
    { 
	super(Fields, Tuples);
    }

    /**
     * Create a result set for an executeUpdate statement.
     *
     * @param updateCount the number of rows affected by the update
     */

    public ResultSet(long updateCount, long updateID)
    {
	super(updateCount, updateID);
    }

};
