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

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;


/**
 * The representation (mapping) in the JavaTM programming language  of an SQL
 * BLOB value. An SQL BLOB is a built-in type that stores  a Binary Large
 * Object as a column value in a row of a database  table. The driver
 * implements Blob using an SQL locator(BLOB),  which means that a Blob object
 * contains a logical pointer to the  SQL BLOB data rather than the data
 * itself. A Blob object is valid  for the duration of the transaction in
 * which is was created.   Methods in the interfaces ResultSet,
 * CallableStatement, and  PreparedStatement, such as getBlob and setBlob
 * allow a programmer  to access an SQL BLOB value. The Blob interface
 * provides methods  for getting the length of an SQL BLOB (Binary Large
 * Object) value,  for materializing a BLOB value on the client, and for
 * determining  the position of a pattern of bytes within a BLOB value.   This
 * class is new in the JDBC 2.0 API.
 *
 * @author Mark Matthews
 *
 * @version $Id: BlobFromLocator.java,v 1.1.2.6 2004/01/20 22:50:57 mmatthew Exp $
 */
public class BlobFromLocator implements java.sql.Blob {
    //~ Instance fields --------------------------------------------------------

    private List primaryKeyColumns = null;
    private List primaryKeyValues = null;

    /** The ResultSet that created this BLOB */
    private ResultSet creatorResultSet;
    private String blobColumnName = null;
    private String tableName = null;
    
    private int numColsInResultSet = 0;
    private int numPrimaryKeys = 0;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates an updatable BLOB that can update in-place
     */
    BlobFromLocator(ResultSet creatorResultSetToSet, int blobColumnIndex)
        throws SQLException {
        this.creatorResultSet = creatorResultSetToSet;

        this.numColsInResultSet = this.creatorResultSet.fields.length;

        if (this.numColsInResultSet > 1) {
            this.primaryKeyColumns = new ArrayList();
            this.primaryKeyValues = new ArrayList();

            for (int i = 0; i < this.numColsInResultSet; i++) {
                if (this.creatorResultSet.fields[i].isPrimaryKey()) {
                    this.primaryKeyColumns.add(this.creatorResultSet.fields[i]
                        .getName());
                    this.primaryKeyValues.add(this.creatorResultSet.getString(i
                            + 1));
                }
            }
        } else {
            throw new SQLException("Emulated BLOB locators must come from "
                + "a ResultSet with only one table selected, and all primary "
                + "keys selected", SQLError.SQL_STATE_GENERAL_ERROR);
        }

        this.numPrimaryKeys = this.primaryKeyColumns.size();
        this.tableName = this.creatorResultSet.fields[0].getTableName();
        this.blobColumnName = this.creatorResultSet.getString(blobColumnIndex);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * @see Blob#setBinaryStream(long)
     */
    public OutputStream setBinaryStream(long indexToWriteAt)
        throws SQLException {
        throw new NotImplemented();
    }

    /**
     * Retrieves the BLOB designated by this Blob instance as a stream.
     *
     * @return this BLOB represented as a binary stream of bytes.
     *
     * @throws SQLException if a database error occurs
     */
    public java.io.InputStream getBinaryStream() throws SQLException {
        return new ByteArrayInputStream(getBytes(1L, (int) length()));
    }

    /**
     * @see Blob#setBytes(long, byte[], int, int)
     */
    public int setBytes(long writeAt, byte[] bytes, int offset, int length)
        throws SQLException {
        java.sql.PreparedStatement pStmt = null;

        if ((offset + length) > bytes.length) {
            length = bytes.length - offset;
        }

        byte[] bytesToWrite = new byte[length];
        System.arraycopy(bytes, offset, bytesToWrite, 0, length);

        // FIXME: Needs to use identifiers for column/table names
        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(this.tableName);
        query.append(" SET ");
        query.append(this.blobColumnName);
        query.append(" = INSERT(");
        query.append(this.blobColumnName);
        query.append(", ");
        query.append(writeAt);
        query.append(", ");
        query.append(length);
        query.append(", ?) WHERE ");

        query.append((String) this.primaryKeyColumns.get(0));
        query.append(" = ?");

        for (int i = 1; i < this.numPrimaryKeys; i++) {
            query.append(" AND ");
            query.append((String) this.primaryKeyColumns.get(i));
            query.append(" = ?");
        }

        try {
            // FIXME: Have this passed in instead
            pStmt = this.creatorResultSet.connection.prepareStatement(query
                    .toString());

            pStmt.setBytes(1, bytesToWrite);

            for (int i = 0; i < this.numPrimaryKeys; i++) {
                pStmt.setString(i + 2, (String) this.primaryKeyValues.get(i));
            }

            int rowsUpdated = pStmt.executeUpdate();

            if (rowsUpdated != 1) {
                throw new SQLException("BLOB data not found! Did primary keys change?",
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        } finally {
            if (pStmt != null) {
                try {
                    pStmt.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                pStmt = null;
            }
        }

        return (int) length();
    }

    /**
     * @see Blob#setBytes(long, byte[])
     */
    public int setBytes(long writeAt, byte[] bytes) throws SQLException {
        return setBytes(writeAt, bytes, 0, bytes.length);
    }

    /**
     * Returns as an array of bytes, part or all of the BLOB value that this
     * Blob object designates.
     *
     * @param pos where to start the part of the BLOB
     * @param length the length of the part of the BLOB you want returned.
     *
     * @return the bytes stored in the blob starting at position
     *         <code>pos</code> and having a length of <code>length</code>.
     *
     * @throws SQLException if a database error occurs
     */
    public byte[] getBytes(long pos, int length) throws SQLException {
        java.sql.ResultSet blobRs = null;
        java.sql.PreparedStatement pStmt = null;

        // FIXME: Needs to use identifiers for column/table names
        StringBuffer query = new StringBuffer("SELECT SUBSTRING(");
        query.append(this.blobColumnName);
        query.append(", ");
        query.append(pos);
        query.append(", ");
        query.append(length);
        query.append(") FROM ");
        query.append(this.tableName);
        query.append(" WHERE ");

        query.append((String) this.primaryKeyColumns.get(0));
        query.append(" = ?");

        for (int i = 1; i < this.numPrimaryKeys; i++) {
            query.append(" AND ");
            query.append((String) this.primaryKeyColumns.get(i));
            query.append(" = ?");
        }

        try {
            // FIXME: Have this passed in instead
            pStmt = this.creatorResultSet.connection.prepareStatement(query
                    .toString());

            for (int i = 0; i < this.numPrimaryKeys; i++) {
                pStmt.setString(i + 1, (String) this.primaryKeyValues.get(i));
            }

            blobRs = pStmt.executeQuery();

            if (blobRs.next()) {
                return blobRs.getBytes(1);
            } else {
                throw new SQLException("BLOB data not found! Did primary keys change?",
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        } finally {
            if (blobRs != null) {
                try {
                    blobRs.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                blobRs = null;
            }

            if (pStmt != null) {
                try {
                    pStmt.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                pStmt = null;
            }
        }
    }

    /**
     * Returns the number of bytes in the BLOB value designated by this Blob
     * object.
     *
     * @return the length of this blob
     *
     * @throws SQLException if a database error occurs
     */
    public long length() throws SQLException {
        java.sql.ResultSet blobRs = null;
        java.sql.PreparedStatement pStmt = null;

        // FIXME: Needs to use identifiers for column/table names
        StringBuffer query = new StringBuffer("SELECT LENGTH(");
        query.append(this.blobColumnName);
        query.append(") FROM ");
        query.append(this.tableName);
        query.append(" WHERE ");

        query.append((String) this.primaryKeyColumns.get(0));
        query.append(" = ?");

        for (int i = 1; i < this.numPrimaryKeys; i++) {
            query.append(" AND ");
            query.append((String) this.primaryKeyColumns.get(i));
            query.append(" = ?");
        }

     

        try {
            // FIXME: Have this passed in instead
            pStmt = this.creatorResultSet.connection.prepareStatement(query
                    .toString());

            for (int i = 0; i < this.numPrimaryKeys; i++) {
                pStmt.setString(i + 1, (String) this.primaryKeyValues.get(i));
            }

            blobRs = pStmt.executeQuery();

            if (blobRs.next()) {
                return blobRs.getLong(1);
            } else {
                throw new SQLException("BLOB data not found! Did primary keys change?",
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        } finally {
            if (blobRs != null) {
                try {
                    blobRs.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                blobRs = null;
            }

            if (pStmt != null) {
                try {
                    pStmt.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                pStmt = null;
            }
        }
    }

    /**
     * Finds the position of the given pattern in this BLOB.
     *
     * @param pattern the pattern to find
     * @param start where to start finding the pattern
     *
     * @return the position where the pattern is found in the BLOB, -1 if not
     *         found
     *
     * @throws SQLException if a database error occurs
     */
    public long position(java.sql.Blob pattern, long start)
        throws SQLException {
        return position(pattern.getBytes(0, (int) pattern.length()), start);
    }

    /**
     * @see java.sql.Blob#position(byte[], long)
     */
    public long position(byte[] pattern, long start) throws SQLException {
        java.sql.ResultSet blobRs = null;
        java.sql.PreparedStatement pStmt = null;

        // FIXME: Needs to use identifiers for column/table names
        StringBuffer query = new StringBuffer("SELECT LOCATE(");
        query.append("?, ");
        query.append(this.blobColumnName);
        query.append(", ");
        query.append(start);
        query.append(") FROM ");
        query.append(this.tableName);
        query.append(" WHERE ");

        query.append((String) this.primaryKeyColumns.get(0));
        query.append(" = ?");

        for (int i = 1; i < this.numPrimaryKeys; i++) {
            query.append(" AND ");
            query.append((String) this.primaryKeyColumns.get(i));
            query.append(" = ?");
        }

        try {
            // FIXME: Have this passed in instead
            pStmt = this.creatorResultSet.connection.prepareStatement(query
                    .toString());
            pStmt.setBytes(1, pattern);

            for (int i = 0; i < this.numPrimaryKeys; i++) {
                pStmt.setString(i + 2, (String) this.primaryKeyValues.get(i));
            }

            blobRs = pStmt.executeQuery();

            if (blobRs.next()) {
                return blobRs.getLong(1);
            } else {
                throw new SQLException("BLOB data not found! Did primary keys change?",
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        } finally {
            if (blobRs != null) {
                try {
                    blobRs.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                blobRs = null;
            }

            if (pStmt != null) {
                try {
                    pStmt.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                pStmt = null;
            }
        }
    }

    /**
     * @see Blob#truncate(long)
     */
    public void truncate(long length) throws SQLException {
        java.sql.PreparedStatement pStmt = null;

        // FIXME: Needs to use identifiers for column/table names
        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(this.tableName);
        query.append(" SET ");
        query.append(this.blobColumnName);
        query.append(" = LEFT(");
        query.append(this.blobColumnName);
        query.append(", ");
        query.append(length);
        query.append(") WHERE ");

        query.append((String) this.primaryKeyColumns.get(0));
        query.append(" = ?");

        for (int i = 1; i < this.numPrimaryKeys; i++) {
            query.append(" AND ");
            query.append((String) this.primaryKeyColumns.get(i));
            query.append(" = ?");
        }

        try {
            // FIXME: Have this passed in instead
            pStmt = this.creatorResultSet.connection.prepareStatement(query
                    .toString());

            for (int i = 0; i < this.numPrimaryKeys; i++) {
                pStmt.setString(i + 1, (String) this.primaryKeyValues.get(i));
            }

            int rowsUpdated = pStmt.executeUpdate();

            if (rowsUpdated != 1) {
                throw new SQLException("BLOB data not found! Did primary keys change?",
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        } finally {
            if (pStmt != null) {
                try {
                    pStmt.close();
                } catch (SQLException sqlEx) {
                    ; // do nothing
                }

                pStmt = null;
            }
        }
    }
}
