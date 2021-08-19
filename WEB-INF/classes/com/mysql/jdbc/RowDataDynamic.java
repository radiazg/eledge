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

import java.sql.SQLException;

import com.mysql.jdbc.profiler.ProfileEventSink;
import com.mysql.jdbc.profiler.ProfilerEvent;


/**
 * Allows streaming of MySQL data.
 *
 * @author dgan
 * @version $Id: RowDataDynamic.java,v 1.8.4.13 2004/02/06 17:11:36 mmatthew Exp $
 */
public class RowDataDynamic implements RowData {
    //~ Instance fields --------------------------------------------------------

    private MysqlIO io;
    private Object[] nextRow;
    private boolean isAfterEnd = false;
    private boolean isAtEnd = false;
    private boolean streamerClosed = false;
    private int columnCount;
    private int index = -1;
    private boolean isBinaryEncoded = false;
    private Field[] fields;
    private ResultSet owner;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new RowDataDynamic object.
     *
     * @param io the connection to MySQL that this data is coming from
     * @param fields the fields that describe this data
     * @param isBinaryEncoded is this data in native format?
     * @param colCount the number of columns
     * @throws SQLException if the next record can not be found
     */
    public RowDataDynamic(MysqlIO io, int colCount, Field[] fields, boolean isBinaryEncoded) throws SQLException {
        this.io = io;
        this.columnCount = colCount;
        this.isBinaryEncoded = isBinaryEncoded;
        this.fields = fields;
        nextRecord();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Returns true if we got the last element.
     *
     * @return true if after last row
     * @throws SQLException if a database error occurs
     */
    public boolean isAfterLast() throws SQLException {
        return this.isAfterEnd;
    }

    /**
     * Only works on non dynamic result sets.
     *
     * @param index row number to get at
     * @return row data at index
     * @throws SQLException if a database error occurs
     */
    public Object[] getAt(int ind) throws SQLException {
        notSupported();
        
        return null;
    }

    /**
     * Returns if iteration has not occured yet.
     *
     * @return true if before first row
     * @throws SQLException if a database error occurs
     */
    public boolean isBeforeFirst() throws SQLException {
        return this.index < 0;
    }

    /**
      * Moves the current position in the result set to
      * the given row number.
      *
      * @param rowNumber row to move to
      * @throws SQLException if a database error occurs
      */
    public void setCurrentRow(int rowNumber) throws SQLException {
        notSupported();
    }

    /**
     * Returns the current position in the result set as
     * a row number.
     *
     * @return the current row number
     * @throws SQLException if a database error occurs
     */
    public int getCurrentRowNumber() throws SQLException {
        notSupported();

        return -1;
    }

    /**
     * Returns true if the result set is dynamic.
     *
     * This means that move back and move forward won't work
     * because we do not hold on to the records.
     *
     * @return true if this result set is streaming from the server
     */
    public boolean isDynamic() {
        return true;
    }

    /**
     * Has no records.
     *
     * @return true if no records
     * @throws SQLException if a database error occurs
     */
    public boolean isEmpty() throws SQLException {
        notSupported();

        return false;
    }

    /**
     * Are we on the first row of the result set?
     *
     * @return true if on first row
     * @throws SQLException if a database error occurs
     */
    public boolean isFirst() throws SQLException {
        notSupported();

        return false;
    }

    /**
     * Are we on the last row of the result set?
     *
     * @return true if on last row
     * @throws SQLException if a database error occurs
     */
    public boolean isLast() throws SQLException {
        notSupported();

        return false;
    }

    /**
     * Adds a row to this row data.
     *
     * @param row the row to add
     * @throws SQLException if a database error occurs
     */
    public void addRow(byte[][] row) throws SQLException {
        notSupported();
    }

    /**
     * Moves to after last.
     *
     * @throws SQLException if a database error occurs
     */
    public void afterLast() throws SQLException {
        notSupported();
    }

    /**
     * Moves to before first.
     *
     * @throws SQLException if a database error occurs
     */
    public void beforeFirst() throws SQLException {
        notSupported();
    }

    /**
     * Moves to before last so next el is the last el.
     *
     * @throws SQLException if a database error occurs
     */
    public void beforeLast() throws SQLException {
        notSupported();
    }

    /**
     * We're done.
     *
     * @throws SQLException if a database error occurs
     */
    public void close() throws SQLException {
    	
    	boolean hadMore = false;
    	int howMuchMore = 0;
    	
        //drain the rest of the records.
        while (this.hasNext()) {
            this.next();
            hadMore = true;
            howMuchMore++;
            
            if (howMuchMore % 100 == 0) {
            	Thread.yield();
            }
        }
        
		if (this.owner != null) {
			Connection conn = this.owner.connection;
    		
			if (conn != null && conn.getUseUsageAdvisor()) {
				if (hadMore) {
					
					ProfileEventSink eventSink = ProfileEventSink.getInstance(conn);


					eventSink.consumeEvent(new ProfilerEvent(
											ProfilerEvent.TYPE_WARN, "",
											this.owner.owningStatement == null ? "N/A" : this.owner.owningStatement.currentCatalog,
											conn.getId(), this.owner.owningStatement == null ? -1 : this.owner.owningStatement.getId(),
											-1, System.currentTimeMillis(), 0,
											null, null, 
										"WARN: Possible incomplete traversal of result set. Streaming result set had " 
										+  howMuchMore + " rows left to read when it was closed."
										+ "\n\nYou should consider re-formulating your query to "
										+ "return only the rows you are interested in using."
										+ "\n\nResultSet was created at: "
										+ this.owner.pointOfOrigin
										));
				}
			}
		}
				
        this.fields = null;
        this.owner = null;
    }

    /**
     * Returns true if another row exsists.
     *
     * @return true if more rows
     * @throws SQLException if a database error occurs
     */
    public boolean hasNext() throws SQLException {
        boolean hasNext = (this.nextRow != null);

        if (!hasNext && !this.streamerClosed) {
            this.io.closeStreamer(this);
            this.streamerClosed = true;
        }

        return hasNext;
    }

    /**
     * Moves the current position relative 'rows' from
     * the current position.
     *
     * @param rows the relative number of rows to move
     * @throws SQLException if a database error occurs
     */
    public void moveRowRelative(int rows) throws SQLException {
        notSupported();
    }

    /**
     * Returns the next row.
     *
     * @return the next row value
     * @throws SQLException if a database error occurs
     */
    public Object[] next() throws SQLException {
        this.index++;

        Object[] ret = this.nextRow;
        nextRecord();

        return ret;
    }

    /**
     * Removes the row at the given index.
     *
     * @param index the row to move to
     * @throws SQLException if a database error occurs
     */
    public void removeRow(int ind) throws SQLException {
        notSupported();
    }

    /**
     * Only works on non dynamic result sets.
     *
     * @return the size of this row data
     */
    public int size() {
        return RESULT_SET_SIZE_UNKNOWN;
    }

    private void nextRecord() throws SQLException {

        try {
            if (!this.isAtEnd) {
                this.nextRow = (Object[]) this.io.nextRow(this.fields, (int) this.columnCount, this.isBinaryEncoded, java.sql.ResultSet.CONCUR_READ_ONLY);

                if (this.nextRow == null) {
                    this.isAtEnd = true;
                }
            } else {
                this.isAfterEnd = true;
            }
        } catch (SQLException sqlEx) {
			// don't wrap SQLExceptions
			throw sqlEx; 
		} catch (Exception ex) {
			String exceptionType = ex.getClass().getName();
			String exceptionMessage = ex.getMessage();

			exceptionMessage += "\n\nNested Stack Trace:\n";
			exceptionMessage += Util.stackTraceToString(ex);

			throw new java.sql.SQLException(
				"Error retrieving record: Unexpected Exception: "
				+ exceptionType + " message given: " + exceptionMessage, SQLError.SQL_STATE_GENERAL_ERROR);
		}
    }

    private void notSupported() throws SQLException {
        throw new OperationNotSupportedException();
    }

    //~ Inner Classes ----------------------------------------------------------

    class OperationNotSupportedException extends SQLException {
        OperationNotSupportedException() {
            super("Operation not supported for streaming result sets", SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
        }
    }
    
	/**
	 * @see com.mysql.jdbc.RowData#setOwner(com.mysql.jdbc.ResultSet)
	 */
	public void setOwner(ResultSet rs) {
		this.owner = rs;
	}
	
	/**
	 * @see com.mysql.jdbc.RowData#getOwner()
	 */
	public ResultSet getOwner() {
		return this.owner;
	}

}
