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

import com.mysql.jdbc.profiler.ProfileEventSink;
import com.mysql.jdbc.profiler.ProfilerEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import java.math.BigDecimal;

import java.net.URL;

import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * JDBC Interface for MySQL-4.1 and newer server-side PreparedStatements.
 *
 * @author Mark Matthews
 * @version $Id: ServerPreparedStatement.java,v 1.1.2.33 2004/02/10 04:08:55 mmatthew Exp $
 */
public class ServerPreparedStatement extends PreparedStatement {
    private Buffer outByteBuffer;

    /** If this statement has been marked invalid, what was the reason? */
    private SQLException invalidationException;
    private BindValue[] parameterBindings;
    private Field[] parameterFields;
    private Field[] resultFields;

    /** Has this prepared statement been marked invalid? */
    private boolean invalid = false;

    /** Does this query modify data? */
    private boolean isSelectQuery;

    /** Do we need to send/resend types to the server? */
    private boolean sendTypesToServer = false;

    /**
     * The number of fields in the result set (if any) for this
     * PreparedStatement.
     */
    private int fieldCount;

    /** The ID that the server uses to identify this PreparedStatement */
    private long serverStatementId;
    
    /** 
     * Flag indicating whether or not the long parameters have been
     * 'switched' back to normal parameters. We can not execute() if
     * clearParameters() hasn't been called in this case.
     */
    private boolean detectedLongParameterSwitch = false;

    /**
     * Creates a new ServerPreparedStatement object.
     *
     * @param conn the connection creating us.
     * @param sql the SQL containing the statement to prepare.
     * @param catalog the catalog in use when we were created.
     *
     * @throws SQLException If an error occurs
     */
    public ServerPreparedStatement(Connection conn, String sql, String catalog)
        throws SQLException {
        super(conn, catalog);

        checkNullOrEmptyQuery(sql);

        this.isSelectQuery = StringUtils.startsWithIgnoreCaseAndWs(sql, "SELECT");

        this.useTrueBoolean = this.connection.getIO().versionMeetsMinimum(3,
                21, 23);
        this.hasLimitClause = (sql.toUpperCase().indexOf("LIMIT") != -1);
        this.firstCharOfStmt = StringUtils.firstNonWsCharUc(sql);
        this.originalSql = sql;

        try {
            serverPrepare(sql);
        } catch (SQLException sqlEx) {
            // don't wrap SQLExceptions
            throw sqlEx;
        } catch (Exception ex) {
            throw new SQLException(ex.toString(),
                SQLError.SQL_STATE_GENERAL_ERROR);
        }
    }

    /**
     * @see java.sql.PreparedStatement#setArray(int, java.sql.Array)
     */
    public void setArray(int i, Array x) throws SQLException {
        throw new NotImplemented();
    }

    /**
     * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream,
     *      int)
     */
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
        throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.BINARY);
        } else {
            BindValue binding = getBinding(parameterIndex, true);
            setType(binding, MysqlDefs.FIELD_TYPE_BLOB);

            binding.value = x;
            binding.isNull = false;
            binding.isLongData = true;

            if (this.connection.getUseStreamLengthsInPrepStmts()) {
                binding.bindLength = length;
            } else {
                binding.bindLength = -1;
            }

            serverLongData(parameterIndex, binding);
        }
    }

    /**
     * @see java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)
     */
    public void setBigDecimal(int parameterIndex, BigDecimal x)
        throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.DECIMAL);
        } else {
            setString(parameterIndex, fixDecimalExponent(x.toString()));
        }
    }

    /**
     * @see java.sql.PreparedStatement#setBinaryStream(int,
     *      java.io.InputStream, int)
     */
    public void setBinaryStream(int parameterIndex, InputStream x, int length)
        throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.BINARY);
        } else {
            BindValue binding = getBinding(parameterIndex, true);
            setType(binding, MysqlDefs.FIELD_TYPE_BLOB);

            binding.value = x;
            binding.isNull = false;
            binding.isLongData = true;

            if (this.connection.getUseStreamLengthsInPrepStmts()) {
                binding.bindLength = length;
            } else {
                binding.bindLength = -1;
            }

            serverLongData(parameterIndex, binding);
        }
    }

    /**
     * @see java.sql.PreparedStatement#setBlob(int, java.sql.Blob)
     */
    public void setBlob(int parameterIndex, Blob x) throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.BINARY);
        } else {
            BindValue binding = getBinding(parameterIndex, true);
            setType(binding, MysqlDefs.FIELD_TYPE_BLOB);

            binding.value = x;
            binding.isNull = false;
            binding.isLongData = true;

            if (this.connection.getUseStreamLengthsInPrepStmts()) {
                binding.bindLength = x.length();
            } else {
                binding.bindLength = -1;
            }

            serverLongData(parameterIndex, binding);
        }
    }

    /**
     * @see java.sql.PreparedStatement#setBoolean(int, boolean)
     */
    public void setBoolean(int parameterIndex, boolean x)
        throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_TINY);

        Byte val = new Byte((x ? (byte) 1 : (byte) 0));

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setByte(int, byte)
     */
    public void setByte(int parameterIndex, byte x) throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_TINY);

        Byte val = new Byte(x);

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setBytes(int, byte)
     */
    public void setBytes(int parameterIndex, byte[] x)
        throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.BINARY);
        } else {
            BindValue binding = getBinding(parameterIndex, false);
            setType(binding, MysqlDefs.FIELD_TYPE_BLOB);

            binding.value = x;
            binding.isNull = false;
            binding.isLongData = false;
        }
    }

    /**
     * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader,
     *      int)
     */
    public void setCharacterStream(int parameterIndex, Reader reader, int length)
        throws SQLException {
        checkClosed();

        if (reader == null) {
            setNull(parameterIndex, java.sql.Types.BINARY);
        } else {
            BindValue binding = getBinding(parameterIndex, true);
            setType(binding, MysqlDefs.FIELD_TYPE_BLOB);

            binding.value = reader;
            binding.isNull = false;
            binding.isLongData = true;

            if (this.connection.getUseStreamLengthsInPrepStmts()) {
                binding.bindLength = length;
            } else {
                binding.bindLength = -1;
            }

            serverLongData(parameterIndex, binding);
        }
    }

    /**
     * @see java.sql.PreparedStatement#setClob(int, java.sql.Clob)
     */
    public void setClob(int parameterIndex, Clob x) throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.BINARY);
        } else {
            BindValue binding = getBinding(parameterIndex, true);
            setType(binding, MysqlDefs.FIELD_TYPE_BLOB);

            binding.value = x.getCharacterStream();
            binding.isNull = false;
            binding.isLongData = true;

            if (this.connection.getUseStreamLengthsInPrepStmts()) {
                binding.bindLength = x.length();
            } else {
                binding.bindLength = -1;
            }

            serverLongData(parameterIndex, binding);
        }
    }

    /**
     * Set a parameter to a java.sql.Date value.  The driver converts this to a
     * SQL DATE value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the calendar to interpret the date with
     *
     * @exception SQLException if a database-access error occurs.
     */
    public void setDate(int parameterIndex, Date x, Calendar cal)
        throws SQLException {
        if (x == null) {
            setNull(parameterIndex, java.sql.Types.DATE);
        } else {
            BindValue binding = getBinding(parameterIndex, false);
            setType(binding, MysqlDefs.FIELD_TYPE_DATE);

            binding.value = x;
            binding.isNull = false;
            binding.isLongData = false;
        }
    }

    /**
     * Set a parameter to a java.sql.Date value.  The driver converts this to a
     * SQL DATE value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     *
     * @exception SQLException if a database-access error occurs.
     */
    public void setDate(int parameterIndex, Date x) throws SQLException {
        setDate(parameterIndex, x, null);
    }

    /**
     * @see java.sql.PreparedStatement#setDouble(int, double)
     */
    public void setDouble(int parameterIndex, double x)
        throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_DOUBLE);

        Double val = new Double(x);

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setFloat(int, float)
     */
    public void setFloat(int parameterIndex, float x) throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_FLOAT);

        Float val = new Float(x);

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setInt(int, int)
     */
    public void setInt(int parameterIndex, int x) throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_LONG);

        Integer val = new Integer(x);

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setLong(int, long)
     */
    public void setLong(int parameterIndex, long x) throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_LONGLONG);

        Long val = new Long(x);

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#getMetaData()
     */
    public java.sql.ResultSetMetaData getMetaData() throws SQLException {
        checkClosed();

        return new ResultSetMetaData(this.resultFields);
    }

    /**
     * @see java.sql.PreparedStatement#setNull(int, int, java.lang.String)
     */
    public void setNull(int parameterIndex, int sqlType, String typeName)
        throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);

        //
        // Don't re-set types, but use something if this
        // parameter was never specified
        //
        if (binding.bufferType == 0) {
            setType(binding, MysqlDefs.FIELD_TYPE_NULL);
        }

        binding.value = null;
        binding.isNull = true;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setNull(int, int)
     */
    public void setNull(int parameterIndex, int sqlType)
        throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);

        //
        // Don't re-set types, but use something if this
        // parameter was never specified
        //
        if (binding.bufferType == 0) {
            setType(binding, MysqlDefs.FIELD_TYPE_NULL);
        }

        binding.value = null;
        binding.isNull = true;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#getParameterMetaData()
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new NotImplemented();
    }

    /**
     * @see java.sql.PreparedStatement#setRef(int, java.sql.Ref)
     */
    public void setRef(int i, Ref x) throws SQLException {
        throw new NotImplemented();
    }

    /**
     * @see java.sql.PreparedStatement#setShort(int, short)
     */
    public void setShort(int parameterIndex, short x) throws SQLException {
        checkClosed();

        BindValue binding = getBinding(parameterIndex, false);
        setType(binding, MysqlDefs.FIELD_TYPE_SHORT);

        Short val = new Short(x);

        binding.value = val;
        binding.isNull = false;
        binding.isLongData = false;
    }

    /**
     * @see java.sql.PreparedStatement#setString(int, java.lang.String)
     */
    public void setString(int parameterIndex, String x)
        throws SQLException {
        checkClosed();

        if (x == null) {
            setNull(parameterIndex, java.sql.Types.CHAR);
        } else {
            BindValue binding = getBinding(parameterIndex, false);
            setType(binding, MysqlDefs.FIELD_TYPE_STRING);

            binding.value = x;
            binding.isNull = false;
            binding.isLongData = false;
        }
    }

    /**
     * Set a parameter to a java.sql.Time value.  The driver converts this to a
     * SQL TIME value when it sends it to the database, using the given
     * timezone.
     *
     * @param parameterIndex the first parameter is 1...));
     * @param x the parameter value
     * @param cal the timezone to use
     *
     * @throws SQLException if a database access error occurs
     */
    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal)
        throws SQLException {
        setTimeInternal(parameterIndex, x, cal.getTimeZone());
    }

    /**
     * Set a parameter to a java.sql.Time value.
     *
     * @param parameterIndex the first parameter is 1...));
     * @param x the parameter value
     *
     * @throws SQLException if a database access error occurs
     */
    public void setTime(int parameterIndex, java.sql.Time x)
        throws SQLException {
        setTimeInternal(parameterIndex, x, TimeZone.getDefault());
    }

    /**
     * Set a parameter to a java.sql.Time value.  The driver converts this to a
     * SQL TIME value when it sends it to the database, using the given
     * timezone.
     *
     * @param parameterIndex the first parameter is 1...));
     * @param x the parameter value
     * @param tz the timezone to use
     *
     * @throws SQLException if a database access error occurs
     */
    public void setTimeInternal(int parameterIndex, java.sql.Time x, TimeZone tz)
        throws SQLException {
        if (x == null) {
            setNull(parameterIndex, java.sql.Types.TIME);
        } else {
            BindValue binding = getBinding(parameterIndex, false);
            setType(binding, MysqlDefs.FIELD_TYPE_TIME);

            binding.value = TimeUtil.changeTimezone(this.connection, x, tz,
                    this.connection.getServerTimezoneTZ());
            binding.isNull = false;
            binding.isLongData = false;
        }
    }

    /**
     * Set a parameter to a java.sql.Timestamp value.  The driver converts this
     * to a SQL TIMESTAMP value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     *
     * @throws SQLException if a database-access error occurs.
     */
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x)
        throws SQLException {
        setTimestampInternal(parameterIndex, x, TimeZone.getDefault());
    }

    /**
     * Set a parameter to a java.sql.Timestamp value.  The driver converts this
     * to a SQL TIMESTAMP value when it sends it to the database.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @param x the parameter value
     * @param cal the timezone to use
     *
     * @throws SQLException if a database-access error occurs.
     */
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x,
        Calendar cal) throws SQLException {
        setTimestampInternal(parameterIndex, x, cal.getTimeZone());
    }

    /**
     * @see java.sql.PreparedStatement#setURL(int, java.net.URL)
     */
    public void setURL(int parameterIndex, URL x) throws SQLException {
        checkClosed();

        setString(parameterIndex, x.toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @param parameterIndex DOCUMENT ME!
     * @param x DOCUMENT ME!
     * @param length DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     * @throws NotImplemented DOCUMENT ME!
     *
     * @see java.sql.PreparedStatement#setUnicodeStream(int,
     *      java.io.InputStream, int)
     * @deprecated
     */
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
        throws SQLException {
        checkClosed();

        throw new NotImplemented();
    }

    /**
     * JDBC 2.0 Add a set of parameters to the batch.
     *
     * @exception SQLException if a database-access error occurs.
     *
     * @see Statement#addBatch
     */
    public synchronized void addBatch() throws SQLException {
        checkClosed();

        if (this.batchedArgs == null) {
            this.batchedArgs = new ArrayList();
        }

        this.batchedArgs.add(new BatchedBindValues(this.parameterBindings));
    }

    /**
     * @see java.sql.PreparedStatement#clearParameters()
     */
    public void clearParameters() throws SQLException {
        clearParametersInternal(true);
    }

    /**
     * @see java.sql.Statement#close()
     */
    public void close() throws SQLException {
        realClose(true);
    }

    /**
     * @see java.sql.Statement#executeBatch()
     */
    public synchronized int[] executeBatch() throws SQLException {
        if (this.connection.isReadOnly()) {
            throw new SQLException("Connection is read-only. "
                + "Queries leading to data modification are not allowed",
                SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
        }

        checkClosed();

        synchronized (this.connection.getMutex()) {
            clearWarnings();

            // Store this for later, we're going to 'swap' them out
            // as we execute each batched statement...
            BindValue[] oldBindValues = this.parameterBindings;

            try {
                int[] updateCounts = null;

                if (this.batchedArgs != null) {
                    int nbrCommands = this.batchedArgs.size();
                    updateCounts = new int[nbrCommands];

                    for (int i = 0; i < nbrCommands; i++) {
                        updateCounts[i] = -3;
                    }

                    SQLException sqlEx = null;

                    int commandIndex = 0;

                    for (commandIndex = 0; commandIndex < nbrCommands;
                            commandIndex++) {
                        Object arg = this.batchedArgs.get(commandIndex);

                        if (arg instanceof String) {
                            updateCounts[commandIndex] = executeUpdate((String) arg);
                        } else {
                            this.parameterBindings = ((BatchedBindValues) arg).batchedParameterValues;

                            try {
                                updateCounts[commandIndex] = executeUpdate();
                            } catch (SQLException ex) {
                                updateCounts[commandIndex] = EXECUTE_FAILED;

                                if (this.connection.getContinueBatchOnError()) {
                                    sqlEx = ex;
                                } else {
                                    int[] newUpdateCounts = new int[commandIndex];
                                    System.arraycopy(updateCounts, 0,
                                        newUpdateCounts, 0, commandIndex);

                                    throw new java.sql.BatchUpdateException(ex
                                        .getMessage(), ex.getSQLState(),
                                        ex.getErrorCode(), newUpdateCounts);
                                }
                            }
                        }
                    }

                    if (sqlEx != null) {
                        throw new java.sql.BatchUpdateException(sqlEx
                            .getMessage(), sqlEx.getSQLState(),
                            sqlEx.getErrorCode(), updateCounts);
                    }
                }

                return (updateCounts != null) ? updateCounts : new int[0];
            } finally {
                this.parameterBindings = oldBindValues;

                clearBatch();
            }
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "com.mysql.jdbc.ServerPreparedStatement["
        + this.serverStatementId + "]";
    }

    protected void setTimestampInternal(int parameterIndex,
        java.sql.Timestamp x, TimeZone tz) throws SQLException {
        if (x == null) {
            setNull(parameterIndex, java.sql.Types.TIMESTAMP);
        } else {
            BindValue binding = getBinding(parameterIndex, false);
            setType(binding, MysqlDefs.FIELD_TYPE_DATETIME);

            binding.value = TimeUtil.changeTimezone(this.connection, x, tz,
                    this.connection.getServerTimezoneTZ());
            binding.isNull = false;
            binding.isLongData = false;
        }
    }

    /* (non-Javadoc)
     * @see com.mysql.jdbc.Statement#checkClosed()
     */
    protected void checkClosed() throws SQLException {
        if (this.invalid) {
            throw this.invalidationException;
        } else {
            super.checkClosed();
        }
    }

    /**
     * @see com.mysql.jdbc.PreparedStatement#executeInternal(int,
     *      com.mysql.jdbc.Buffer, boolean, boolean)
     */
    protected com.mysql.jdbc.ResultSet executeInternal(int maxRowsToRetrieve,
        Buffer sendPacket, boolean createStreamingResultSet,
        boolean queryIsSelectOnly, boolean unpackFields)
        throws SQLException {
        this.numberOfExecutions++;

        // We defer to server-side execution
        try {
            return serverExecute(maxRowsToRetrieve, createStreamingResultSet);
        } catch (SQLException sqlEx) {
            // don't wrap SQLExceptions
            throw sqlEx;
        } catch (Exception ex) {
            throw new SQLException(ex.toString(),
                SQLError.SQL_STATE_GENERAL_ERROR);
        }
    }

    /**
     * @see com.mysql.jdbc.PreparedStatement#fillSendPacket()
     */
    protected Buffer fillSendPacket() throws SQLException {
        return null; // we don't use this type of packet
    }

    /**
     * @see com.mysql.jdbc.PreparedStatement#fillSendPacket(byte,
     *      java.io.InputStream, boolean, int)
     */
    protected Buffer fillSendPacket(byte[][] batchedParameterStrings,
        InputStream[] batchedParameterStreams, boolean[] batchedIsStream,
        int[] batchedStreamLengths) throws SQLException {
        return null; // we don't use this type of packet
    }

    /**
     * Used by Connection when auto-reconnecting to retrieve 'lost' prepared
     * statements.
     *
     * @throws SQLException if an error occurs.
     */
    protected void rePrepare() throws SQLException {
        this.invalidationException = null;

        try {
            serverPrepare(this.originalSql);
        } catch (SQLException sqlEx) {
            // don't wrap SQLExceptions
            this.invalidationException = sqlEx;
        } catch (Exception ex) {
            this.invalidationException = new SQLException(ex.toString(),
                    SQLError.SQL_STATE_GENERAL_ERROR);
        }

        if (this.invalidationException != null) {
            this.invalid = true;

            this.parameterBindings = null;

            this.parameterFields = null;
            this.resultFields = null;

            if (this.results != null) {
                try {
                    this.results.close();
                } catch (Exception ex) {
                    ;
                }
            }

            if (this.connection != null) {
                if (this.maxRowsChanged) {
                    this.connection.unsetMaxRows(this);
                }

                this.connection.unregisterStatement(this);
            }
        }
    }

    /**
     * Closes this connection and frees all resources.
     *
     * @param calledExplicitly was this called from close()?
     *
     * @throws SQLException if an error occurs
     */
    protected void realClose(boolean calledExplicitly)
        throws SQLException {
        if (this.isClosed) {
            return;
        }

        SQLException exceptionDuringClose = null;

        try {
            synchronized (this.connection.getMutex()) {
                MysqlIO mysql = this.connection.getIO();

                Buffer packet = mysql.getSharedSendPacket();

                packet.writeByte((byte) MysqlDefs.COM_CLOSE_STATEMENT);
                packet.writeLong(this.serverStatementId);
            }
        } catch (SQLException sqlEx) {
            exceptionDuringClose = sqlEx;
        }

        clearParametersInternal(false);
        this.parameterBindings = null;

        this.parameterFields = null;
        this.resultFields = null;

        super.realClose(calledExplicitly);

        if (exceptionDuringClose != null) {
            throw exceptionDuringClose;
        }
    }

    /**
     * @see com.mysql.jdbc.PreparedStatement#getBytes(int)
     */
    synchronized byte[] getBytes(int parameterIndex) throws SQLException {
        BindValue bindValue = getBinding(parameterIndex, false);

        if (bindValue.isNull) {
            return null;
        } else if (bindValue.isLongData) {
            throw new NotImplemented();
        } else {
            if (this.outByteBuffer == null) {
                this.outByteBuffer = Buffer.allocateNew(this.connection
                        .getNetBufferLength(), false);
            }

            this.outByteBuffer.clear();

            int originalPosition = this.outByteBuffer.getPosition();

            storeBinding(this.outByteBuffer, bindValue);

            int newPosition = this.outByteBuffer.getPosition();

            int length = newPosition - originalPosition;

            byte[] valueAsBytes = new byte[length];

            System.arraycopy(this.outByteBuffer.getByteBuffer(),
                originalPosition, valueAsBytes, 0, length);

            return valueAsBytes;
        }
    }

    /**
     * @see com.mysql.jdbc.PreparedStatement#isNull(int)
     */
    boolean isNull(int paramIndex) {
        throw new IllegalArgumentException(
            "Not supported for server-side prepared statements.");
    }

    private BindValue getBinding(int parameterIndex, boolean forLongData) throws SQLException {
        if (this.parameterBindings.length == 0) {
            throw new SQLException("No parameters defined during prepareCall()",
                SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
        }

        parameterIndex--;

        if ((parameterIndex < 0)
                || (parameterIndex >= this.parameterBindings.length)) {
            throw new SQLException("Parameter index out of bounds. "
                + (parameterIndex + 1) + " is not between valid values of 1 and "
                + this.parameterBindings.length,
                SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
        }

        if (this.parameterBindings[parameterIndex] == null) {
            this.parameterBindings[parameterIndex] = new BindValue();
        } else {
        	if (this.parameterBindings[parameterIndex].isLongData &&
        			!forLongData) {
        		this.detectedLongParameterSwitch = true;
        	}
        }

        return this.parameterBindings[parameterIndex];
    }

    private void setType(BindValue oldValue, int bufferType) {
        if (oldValue.bufferType != bufferType) {
            this.sendTypesToServer = true;
        }

        oldValue.bufferType = bufferType;
    }

    private void clearParametersInternal(boolean clearServerParameters)
        throws SQLException {
        
        boolean hadLongData = false;
        
        if (this.parameterBindings != null) {
            for (int i = 0; i < this.parameterCount; i++) {
            	if (this.parameterBindings[i] != null &&
            			this.parameterBindings[i].isLongData) {
            		hadLongData = true;
            	}
            	
                this.parameterBindings[i] = null;
            }
        }
        
		if (clearServerParameters && hadLongData) {
			serverResetStatement();
			
			this.detectedLongParameterSwitch = false;
		}
    }

    /**
     * Tells the server to execute this prepared statement with the current
     * parameter bindings.
     * <pre>
     * 
     *   -   Server gets the command 'COM_EXECUTE' to execute the
     *       previously         prepared query. If there is any param markers;
     * then client will send the data in the following format:
     * 
     * [COM_EXECUTE:1]
     * [STMT_ID:4]
     * [NULL_BITS:(param_count+7)/8)]
     * [TYPES_SUPPLIED_BY_CLIENT(0/1):1]
     * [[length]data]
     * [[length]data] .. [[length]data].
     * 
     * (Note: Except for string/binary types; all other types will not be
     * supplied with length field)
     * 
     * </pre>
     *
     * @param maxRowsToRetrieve DOCUMENT ME!
     * @param createStreamingResultSet DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws SQLException
     */
    private com.mysql.jdbc.ResultSet serverExecute(int maxRowsToRetrieve,
        boolean createStreamingResultSet) throws SQLException {
        synchronized (this.connection.getMutex()) {
        	if (this.detectedLongParameterSwitch) {
        		throw new SQLException("Driver can not re-execute prepared statement when a parameter has been changed " +
        				"from a streaming type to an intrinsic data type without calling clearParameters() first.", SQLError.SQL_STATE_DRIVER_NOT_CAPABLE);
        	}
        	
            // Check bindings
            for (int i = 0; i < this.parameterCount; i++) {
                if (this.parameterBindings[i] == null) {
                    throw new SQLException("Statement parameter " + (i + 1)
                        + " not set.", SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
                }
            }

            MysqlIO mysql = this.connection.getIO();

            Buffer packet = mysql.getSharedSendPacket();

            packet.clear();
            packet.writeByte((byte) MysqlDefs.COM_EXECUTE);
            packet.writeLong(this.serverStatementId);

            /* Reserve place for null-marker bytes */
            int nullCount = (this.parameterCount + 7) / 8;

            int nullBitsPosition = packet.getPosition();

            for (int i = 0; i < nullCount; i++) {
                packet.writeByte((byte) 0);
            }

            byte[] nullBitsBuffer = new byte[nullCount];

            /* In case if buffers (type) altered, indicate to server */
            packet.writeByte(this.sendTypesToServer ? (byte) 1 : (byte) 0);

            if (this.sendTypesToServer) {
                /*
                      Store types of parameters in first in first package
                      that is sent to the server.
                */
                for (int i = 0; i < this.parameterCount; i++) {
                    packet.writeInt(this.parameterBindings[i].bufferType);
                }
            }

            //
            // store the parameter values
            //
            for (int i = 0; i < this.parameterCount; i++) {
                if (!this.parameterBindings[i].isLongData) {
                    if (!this.parameterBindings[i].isNull) {
                        storeBinding(packet, this.parameterBindings[i]);
                    } else {
                        nullBitsBuffer[i / 8] |= (1 << (i & 7));
                    }
                }
            }

            //
            // Go back and write the NULL flags
            // to the beginning of the packet
            //
            int endPosition = packet.getPosition();
            packet.setPosition(nullBitsPosition);
            packet.writeBytesNoNull(nullBitsBuffer);
            packet.setPosition(endPosition);

            try {
                long begin = 0;

                if (this.connection.getProfileSql()) {
                    begin = System.currentTimeMillis();
                }

                Buffer resultPacket = mysql.sendCommand(MysqlDefs.COM_EXECUTE,
                        null, packet, false, null);

                if (this.connection.getProfileSql()) {
                    this.eventSink = ProfileEventSink.getInstance(this.connection);

                    this.eventSink.consumeEvent(new ProfilerEvent(
                            ProfilerEvent.TYPE_EXECUTE, "",
                            this.currentCatalog, this.connection.getId(),
                            this.statementId, -1, System.currentTimeMillis(),
                            (int) (System.currentTimeMillis() - begin), null,
                            new Throwable(), null));
                }

                com.mysql.jdbc.ResultSet rs = mysql.readAllResults(this, maxRowsToRetrieve,
                        this.resultSetType, this.resultSetConcurrency,
                        createStreamingResultSet, this.currentCatalog,
                        resultPacket, true, this.fieldCount, true);

                this.sendTypesToServer = false;
                this.results = rs;

                return rs;
            } catch (SQLException sqlEx) {
                throw sqlEx;
            } catch (Exception ex) {
                throw new SQLException(ex.toString(),
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        }
    }
    
    /**
     * Sends stream-type data parameters to the server.
     * <pre>
     * Long data handling:
     * 
     * - Server gets the long data in pieces with command type 'COM_LONG_DATA'.
     * - The packet recieved will have the format as:
     *   [COM_LONG_DATA:     1][STMT_ID:4][parameter_number:2][type:2][data]
     * - Checks if the type is specified by client, and if yes reads the type,
     *   and  stores the data in that format.
     * - It's up to the client to check for read data ended. The server doesn't
     *   care;  and also server doesn't notify to the client that it got the
     *   data  or not; if there is any error; then during execute; the error
     *   will  be returned
     * </pre>
     *
     * @param parameterIndex DOCUMENT ME!
     * @param longData DOCUMENT ME!
     *
     * @throws SQLException if an error occurs.
     */
    private void serverLongData(int parameterIndex, BindValue longData)
        throws SQLException {
        try {
            synchronized (this.connection.getMutex()) {
                MysqlIO mysql = this.connection.getIO();

                Buffer packet = mysql.getSharedSendPacket();

                packet.clear();
                packet.writeByte((byte) MysqlDefs.COM_LONG_DATA);
                packet.writeLong(this.serverStatementId);
                packet.writeInt((parameterIndex - 1));

                Object value = longData.value;

                if (value instanceof byte[]) {
                    packet.writeBytesNoNull((byte[]) longData.value);
                } else if (value instanceof InputStream) {
                    storeStream(packet, (InputStream) value);
                } else if (value instanceof java.sql.Blob) {
                    storeStream(packet,
                        ((java.sql.Blob) value).getBinaryStream());
                } else if (value instanceof Reader) {
                    storeReader(packet, (Reader) value);
                } else {
                    throw new SQLException("Unknown LONG DATA type '"
                        + value.getClass().getName() + "'",
                        SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
                }

                mysql.sendCommand(MysqlDefs.COM_LONG_DATA, null, packet, true,
                    null);
            }
        } catch (SQLException sqlEx) {
            // don't wrap SQLExceptions
            throw sqlEx;
        } catch (Exception ex) {
            throw new SQLException(ex.toString(),
                SQLError.SQL_STATE_GENERAL_ERROR);
        }
    }

    private void serverPrepare(String sql) throws Exception {
        MysqlIO mysql = this.connection.getIO();

        long begin = 0;

        if (StringUtils.startsWithIgnoreCaseAndWs(sql, "LOAD DATA")) {
            this.isLoadDataQuery = true;
        } else {
            this.isLoadDataQuery = false;
        }

        if (this.connection.getProfileSql()) {
            begin = System.currentTimeMillis();
        }

        String characterEncoding = null;
        String connectionEncoding = this.connection.getEncoding();

        if (!this.isLoadDataQuery && this.connection.getUseUnicode()
                && (connectionEncoding != null)) {
            characterEncoding = connectionEncoding;
        }

        Buffer prepareResultPacket = mysql.sendCommand(MysqlDefs.COM_PREPARE,
                sql, null, false, characterEncoding);

        if (mysql.versionMeetsMinimum(4, 1, 1)) {
            // 4.1.1 and newer use the first byte
            // as an 'ok' or 'error' flag, so move
            // the buffer pointer past it to
            // start reading the statement id.
            prepareResultPacket.setPosition(1);
        } else {
            // 4.1.0 doesn't use the first byte as an 
            // 'ok' or 'error' flag
            prepareResultPacket.setPosition(0);
        }

        this.serverStatementId = prepareResultPacket.readLong();
        this.fieldCount = prepareResultPacket.readInt();
        this.parameterCount = prepareResultPacket.readInt();
        this.parameterBindings = new BindValue[this.parameterCount];

        if (this.connection.getProfileSql()) {
            this.eventSink = ProfileEventSink.getInstance(this.connection);

            this.eventSink.consumeEvent(new ProfilerEvent(
                    ProfilerEvent.TYPE_PREPARE, "", this.currentCatalog,
                    this.connection.getId(), this.statementId, -1,
                    System.currentTimeMillis(),
                    (int) (System.currentTimeMillis() - begin), null,
                    new Throwable(), sql));
        }

        if (this.fieldCount > 0) {
            this.resultFields = new Field[this.fieldCount];

            Buffer fieldPacket = mysql.readPacket();

            int i = 0;

            // Read in the result set column information
            while (!fieldPacket.isLastDataPacket()) {
                this.resultFields[i++] = mysql.unpackField(fieldPacket, false);
                fieldPacket = mysql.readPacket();
            }
        }
    }

    private void serverResetStatement() throws SQLException {
        synchronized (this.connection.getMutex()) {
            MysqlIO mysql = this.connection.getIO();

            Buffer packet = mysql.getSharedSendPacket();

            packet.clear();
            packet.writeByte((byte) MysqlDefs.COM_RESET_STMT);
            packet.writeLong(this.serverStatementId);

            try {
                Buffer resultPacket = mysql.sendCommand(MysqlDefs.COM_RESET_STMT,
                        null, packet, true, null);
            } catch (SQLException sqlEx) {
                throw sqlEx;
            } catch (Exception ex) {
                throw new SQLException(ex.toString(),
                    SQLError.SQL_STATE_GENERAL_ERROR);
            }
        }
    }

    /**
     * Method storeBinding.
     *
     * @param packet
     * @param bindValue
     *
     * @throws SQLException DOCUMENT ME!
     */
    private void storeBinding(Buffer packet, BindValue bindValue)
        throws SQLException {
        try {
            Object value = bindValue.value;

            if (value instanceof Byte) {
                packet.writeByte(((Byte) value).byteValue());
            } else if (value instanceof Short) {
                packet.ensureCapacity(2);
                packet.writeInt(((Short) value).shortValue());
            } else if (value instanceof Integer) {
                packet.ensureCapacity(4);
                packet.writeLong(((Integer) value).intValue());
            } else if (value instanceof Long) {
                packet.ensureCapacity(8);
                packet.writeLongLong(((Long) value).longValue());
            } else if (value instanceof Float) {
                packet.ensureCapacity(4);
                packet.writeFloat(((Float) value).floatValue());
            } else if (value instanceof Double) {
                packet.ensureCapacity(8);
                packet.writeDouble(((Double) value).doubleValue());
            } else if (value instanceof String) {
                if (!this.isLoadDataQuery) {
                    packet.writeLenString((String) value, this.charEncoding,
                        this.charConverter);
                } else {
                    packet.writeLenBytes(((String) value).getBytes());
                }
            } else if (value instanceof byte[]) {
                packet.writeLenBytes((byte[]) value);
            } else if (value instanceof Time) {
                // NOTE: We need to check for Time instances first,
                // as they inherit from java.util.Date, but are stored
                // differently with MySQL....
                storeTime(packet, (Time) value);
            } else if (value instanceof java.util.Date) {
                storeDateTime(packet, (java.util.Date) value);
            }
        } catch (UnsupportedEncodingException uEE) {
            throw new SQLException("Unsupported character encoding '"
                + this.connection.getEncoding() + "'",
                SQLError.SQL_STATE_GENERAL_ERROR);
        }
    }

    private Calendar dateTimeBindingCal = null;
    
    private void storeDateTime(Buffer intoBuf, java.util.Date dt)
        throws SQLException {
    	// This is synchronized on the connection by callers, so it is
    	// safe to lazily-instantiate this...
    	
    	if (dateTimeBindingCal == null) {
			dateTimeBindingCal = Calendar.getInstance();
    	}
    	
		dateTimeBindingCal.setTime(dt);

        intoBuf.ensureCapacity(8);
        intoBuf.writeByte((byte) 7); // length

        int year = dateTimeBindingCal.get(Calendar.YEAR);
        int month = dateTimeBindingCal.get(Calendar.MONTH) + 1;
        int date = dateTimeBindingCal.get(Calendar.DATE);
        
        intoBuf.writeInt(year);
        intoBuf.writeByte((byte) month);
        intoBuf.writeByte((byte) date);

        if (dt instanceof java.sql.Date) {
            intoBuf.writeByte((byte) 0);
            intoBuf.writeByte((byte) 0);
            intoBuf.writeByte((byte) 0);
        } else {
            intoBuf.writeByte((byte) dateTimeBindingCal.get(Calendar.HOUR_OF_DAY));
            intoBuf.writeByte((byte) dateTimeBindingCal.get(Calendar.MINUTE));
            intoBuf.writeByte((byte) dateTimeBindingCal.get(Calendar.SECOND));
        }
    }

    //
    // TO DO: Investigate using NIO to do this faster
    //
    private void storeReader(Buffer packet, Reader inStream)
        throws SQLException {
        char[] buf = new char[4096];
        StringBuffer valueAsString = new StringBuffer();

        int numRead = 0;

        try {
            while ((numRead = inStream.read(buf)) != -1) {
                valueAsString.append(buf, 0, numRead);
            }
        } catch (IOException ioEx) {
            throw new SQLException("Error while reading binary stream: "
                + ioEx.toString(), SQLError.SQL_STATE_GENERAL_ERROR);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioEx) {
                    ; // ignore
                }
            }
        }

        byte[] valueAsBytes = StringUtils.getBytes(valueAsString.toString(),
                this.connection.getEncoding());

        packet.writeBytesNoNull(valueAsBytes);
    }

    private void storeStream(Buffer packet, InputStream inStream)
        throws SQLException {
        byte[] buf = new byte[4096];

        int numRead = 0;

        try {
            while ((numRead = inStream.read(buf)) != -1) {
                packet.writeBytesNoNull(buf, 0, numRead);
            }
        } catch (IOException ioEx) {
            throw new SQLException("Error while reading binary stream: "
                + ioEx.toString(), SQLError.SQL_STATE_GENERAL_ERROR);
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ioEx) {
                    ; // ignore
                }
            }
        }
    }

    private static void storeTime(Buffer intoBuf, Time tm)
        throws SQLException {
        intoBuf.ensureCapacity(9);
        intoBuf.writeByte((byte) 8); // length
        intoBuf.writeByte((byte) 0); // neg flag
        intoBuf.writeLong(0); // tm->day, not used

        Calendar cal = Calendar.getInstance();
        cal.setTime(tm);
        intoBuf.writeByte((byte) cal.get(Calendar.HOUR_OF_DAY));
        intoBuf.writeByte((byte) cal.get(Calendar.MINUTE));
        intoBuf.writeByte((byte) cal.get(Calendar.SECOND));

        //intoBuf.writeLongInt(0); // tm-second_part
    }

    class BatchedBindValues {
        BindValue[] batchedParameterValues;

        BatchedBindValues(BindValue[] paramVals) {
            int numParams = paramVals.length;

            this.batchedParameterValues = new BindValue[numParams];

            for (int i = 0; i < numParams; i++) {
                this.batchedParameterValues[i] = new BindValue(paramVals[i]);
            }
        }
    }

    class BindValue {
        Object value; /* The value to store */
        boolean isLongData; /* long data indicator */
        boolean isNull; /* NULL indicator */
        int bufferType; /* buffer type */
        long bindLength; /* Default length of data */

        BindValue() {
        }

        BindValue(BindValue copyMe) {
            this.value = copyMe.value;
            this.isLongData = copyMe.isLongData;
            this.isNull = copyMe.isNull;
            this.bufferType = copyMe.bufferType;
            this.bindLength = copyMe.bindLength;
        }
    }
}
