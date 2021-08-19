/*
   Copyright (C) 2003 MySQL AB

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

import com.mysql.jdbc.log.Log;
import com.mysql.jdbc.log.StandardLogger;

import java.io.UnsupportedEncodingException;

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Properties;

import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;


/**
 * Represents configurable properties for Connections and DataSources. Can also
 * expose properties as JDBC DriverPropertyInfo if required as well.
 *
 * @author Mark Matthews
 * @version $Id: ConnectionProperties.java,v 1.1.2.22 2004/02/06 18:41:16 mmatthew Exp $
 */
public class ConnectionProperties {
    private static final ArrayList PROPERTY_LIST = new ArrayList();

    static {
        try {
            java.lang.reflect.Field[] declaredFields = ConnectionProperties.class
                .getDeclaredFields();

            for (int i = 0; i < declaredFields.length; i++) {
                if (ConnectionProperties.ConnectionProperty.class
                        .isAssignableFrom(declaredFields[i].getType())) {
                    PROPERTY_LIST.add(declaredFields[i]);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    private BooleanConnectionProperty allowLoadLocalInfile = new BooleanConnectionProperty("allowLoadLocalInfile",
            true,
            "Should the driver allow use of 'LOAD DATA LOCAL INFILE...' (defaults to 'true').",
            "3.0.3");
    private BooleanConnectionProperty allowMultiQueries = new BooleanConnectionProperty("allowMultiQueries",
            false,
            "Allow the use of ';' to delimit multiple queries during one statement (true/false, defaults to 'false'",
            "3.1.1");
    private BooleanConnectionProperty autoReconnect = new BooleanConnectionProperty("autoReconnect",
            false, "Should the driver try to re-establish bad connections?",
            "1.1");
    private BooleanConnectionProperty autoReconnectForPools = new BooleanConnectionProperty("autoReconnectForPools",
            false,
            "Use a reconnection strategy appropriate for connection pools (defaults to 'false')",
            "3.0.8");
    private BooleanConnectionProperty cacheResultSetMetadata = new BooleanConnectionProperty("cacheResultSetMetadata",
            false,
            "Should the driver cache ResultSetMetaData for Statements and PreparedStatements? (Req. JDK-1.4+, true/false, default 'false')",
            "3.1.1");
    private BooleanConnectionProperty cachePreparedStatements = new BooleanConnectionProperty("cachePrepStmts", false, "Should the driver cache the parsing stage of PreparedStatements?", "3.0.10");

    private IntegerConnectionProperty preparedStatementCacheSize = new IntegerConnectionProperty("prepStmtCacheSize", 25, 0, Integer.MAX_VALUE, "If prepared statement caching is enabled, "
            + "how many prepared statements should be cached?", "3.0.10");
    
    private IntegerConnectionProperty preparedStatementCacheSqlLimit = new IntegerConnectionProperty("prepStmtCacheSqlLimit", 256, 1, Integer.MAX_VALUE, "If prepared statement caching is enabled, "
            + "what's the largest SQL the driver will cache the parsing for?", "3.0.10");
    
    private BooleanConnectionProperty capitalizeTypeNames = new BooleanConnectionProperty("capitalizeTypeNames",
            false,
            "Capitalize type names in DatabaseMetaData? (usually only useful when using WebObjects, true/false, defaults to 'false')",
            "2.0.7");
    private BooleanConnectionProperty clobberStreamingResults = new BooleanConnectionProperty("clobberStreamingResults",
            false,
            "This will cause a 'streaming' ResultSet to be automatically closed, "
            + "and any oustanding data still streaming from the server to be discarded if another query is executed "
            + "before all the data has been read from the server.", "3.0.9");
    private BooleanConnectionProperty continueBatchOnError = new BooleanConnectionProperty("continueBatchOnError",
            true,
            "Should the driver continue processing batch commands if "
            + "one statement fails. The JDBC spec allows either way (defaults to 'true').",
            "3.0.3");
    private BooleanConnectionProperty detectServerPreparedStmts = new BooleanConnectionProperty("useServerPrepStmts",
            true,
            "Use server-side prepared statements if the server supports them? (defaults to 'true').",
            "3.1.0");
    private BooleanConnectionProperty emulateLocators = new BooleanConnectionProperty("emulateLocators",
            false, "N/A", "3.1.0");
    private BooleanConnectionProperty ignoreNonTxTables = new BooleanConnectionProperty("ignoreNonTxTables",
            false,
            "Ignore non-transactional table warning for rollback? (defaults to 'false').",
            "3.0.9");
    private BooleanConnectionProperty isInteractiveClient = new BooleanConnectionProperty("interactiveClient",
            false,
            "Set the CLIENT_INTERACTIVE flag, which tells MySQL "
            + "to timeout connections based on INTERACTIVE_TIMEOUT instead of WAIT_TIMEOUT",
            "3.1.0");
    private BooleanConnectionProperty paranoid = new BooleanConnectionProperty("paranoid",
            false,
            "Take measures to prevent exposure sensitive information in error messages and clear "
            + "data structures holding sensitive data when possible? (defaults to 'false')",
            "3.0.1");
    private BooleanConnectionProperty pedantic = new BooleanConnectionProperty("pedantic",
            false, "Follow the JDBC spec to the letter.", "3.0.0");
    private BooleanConnectionProperty profileSQL = new BooleanConnectionProperty("profileSQL",
            false,
            "Trace queries and their execution/fetch times to the configured logger (true/false) defaults to 'false'",
            "3.1.0");
    private BooleanConnectionProperty reconnectAtTxEnd = new BooleanConnectionProperty("reconnectAtTxEnd",
            false,
            "If autoReconnect is set to true, should the driver attempt reconnections"
            + "at the end of every transaction?", "3.0.10");
    private BooleanConnectionProperty relaxAutoCommit = new BooleanConnectionProperty("relaxAutoCommit",
            false,
            "If the version of MySQL the driver connects to does not support transactions, still allow calls to commit(), rollback() and setAutoCommit() (true/false, defaults to 'false')?",
            "2.0.13");
    private BooleanConnectionProperty requireSSL = new BooleanConnectionProperty("requireSSL",
            false,
            "Require SSL connection if useSSL=true? (defaults to 'false').",
            "3.1.0");
    private BooleanConnectionProperty strictFloatingPoint = new BooleanConnectionProperty("strictFloatingPoint",
            false, "Used only in older versions of compliance test", "3.0.0");
    private BooleanConnectionProperty strictUpdates = new BooleanConnectionProperty("strictUpdates",
            true,
            "Should the driver do strict checking (all primary keys selected) of updatable result sets (true, false, defaults to 'true')?",
            "3.0.4");
    private BooleanConnectionProperty useCompression = new BooleanConnectionProperty("useCompression",
            false,
            "Use zlib compression when communicating with the server (true/false)? Defaults to 'false'.",
            "3.1.0");
    private BooleanConnectionProperty useHostsInPrivileges = new BooleanConnectionProperty("useHostsInPrivileges",
            true,
            "Add '@hostname' to users in DatabaseMetaData.getColumn/TablePrivileges() (true/false), defaults to 'true'.",
            "3.0.2");
    private BooleanConnectionProperty useNewIo = new BooleanConnectionProperty("useNewIO",
            false,
            "Should the driver use the java.nio.* interfaces for network communication (true/false), defaults to 'false'",
            "3.1.0");
    private BooleanConnectionProperty useSSL = new BooleanConnectionProperty("useSSL",
            false,
            "Use SSL when communicating with the server (true/false), defaults to 'false'",
            "3.0.2");
    private BooleanConnectionProperty useStreamLengthsInPrepStmts = new BooleanConnectionProperty("useStreamLengthsInPrepStmts",
            true,
            "Honor stream length parameter in "
            + "PreparedStatement/ResultSet.setXXXStream() method calls (true/false, defaults to 'true')?",
            "3.0.2");
    private BooleanConnectionProperty useTimezone = new BooleanConnectionProperty("useTimezone",
            false,
            "Convert time/date types between client and server timezones (true/false, defaults to 'false')?",
            "3.0.2");
    private BooleanConnectionProperty useUltraDevWorkAround = new BooleanConnectionProperty("ultraDevHack",
            false,
            "Create PreparedStatements for prepareCall() when required, because UltraDev "
            + " is broken and issues a prepareCall() for _all_ statements? (true/false, defaults to 'false')",
            "2.0.3");
    private BooleanConnectionProperty useUnbufferedInput = new BooleanConnectionProperty("useUnbufferedInput",  true, "Don't use BufferedInputStream for reading data from the server", "3.0.11");
    private BooleanConnectionProperty useUnicode = new BooleanConnectionProperty("useUnicode",
            false,
            "Should the driver use Unicode character encodings when handling strings? Should only be used when the driver can't determine the character set mapping, or you are trying to 'force' the driver to use a character set that MySQL either doesn't natively support (such as UTF-8), true/false, defaults to 'true'",
            "1.1g");
    private BooleanConnectionProperty useUsageAdvisor = new BooleanConnectionProperty("useUsageAdvisor",
            false,
            "Should the driver issue 'usage' warnings advising proper and efficient usage of JDBC and MySQL Connector/J to the log (true/false, defaults to 'false')?",
            "3.1.1");
    private IntegerConnectionProperty connectTimeout = new IntegerConnectionProperty("connectTimeout",
            0, 0, Integer.MAX_VALUE,
            "Timeout for socket connect (in milliseconds), with 0 being no timeout. "
            + "Only works on JDK-1.4 or newer. Defaults to '0'.", "3.0.1");
    private IntegerConnectionProperty initialTimeout = new IntegerConnectionProperty("initialTimeout",
            2, 1, Integer.MAX_VALUE,
            "If autoReconnect is enabled, the"
            + " initial time to wait between"
            + " re-connect attempts (in seconds, defaults to '2').", "1.1");
    private IntegerConnectionProperty maxReconnects = new IntegerConnectionProperty("maxReconnects",
            3, 1, Integer.MAX_VALUE,
            "Maximum number of reconnects to attempt if autoReconnect is true, default is '3'.",
            "1.1");
    private IntegerConnectionProperty maxRows = new IntegerConnectionProperty("maxRows",
            -1, -1, Integer.MAX_VALUE,
            "The maximum number of rows to return "
            + " (0, the default means return all rows).", "all versions");
    private IntegerConnectionProperty metadataCacheSize = new IntegerConnectionProperty("metadataCacheSize",
            50, 1, Integer.MAX_VALUE,
            "The number of queries to cache"
            + "ResultSetMetadata for if cacheResultSetMetaData is set to 'true' (default 50)",
            "3.1.1");
    private IntegerConnectionProperty queriesBeforeRetryMaster = new IntegerConnectionProperty("queriesBeforeRetryMaster",
            50, 1, Integer.MAX_VALUE,
            "Number of queries to issue before falling back to master when failed over "
            + "(when using multi-host failover). Whichever condition is met first, "
            + "'queriesBeforeRetryMaster' or 'secondsBeforeRetryMaster' will cause an "
            + "attempt to be made to reconnect to the master. Defaults to 50.",
            "3.0.2");
    private IntegerConnectionProperty secondsBeforeRetryMaster = new IntegerConnectionProperty("secondsBeforeRetryMaster",
            30, 1, Integer.MAX_VALUE,
            "How long should the driver wait, when failed over, before attempting "
            + "to reconnect to the master server? Whichever condition is met first, "
            + "'queriesBeforeRetryMaster' or 'secondsBeforeRetryMaster' will cause an "
            + "attempt to be made to reconnect to the master. Time in seconds, defaults to 30",
            "3.0.2");
    private IntegerConnectionProperty socketTimeout = new IntegerConnectionProperty("socketTimeout",
            0, 0, Integer.MAX_VALUE,
            "Timeout on network socket operations (0, the default means no timeout).",
            "3.0.1");
    private String characterEncodingAsString = null;
    private StringConnectionProperty characterEncoding = new StringConnectionProperty("characterEncoding",
            null,
            "If 'useUnicode' is set to true, what character encoding should the driver use when dealing with strings? (defaults is to 'autodetect')",
            "1.1g");
    private StringConnectionProperty loggerClassName = new StringConnectionProperty("logger",
            StandardLogger.class.getName(),
            "The name of a class that implements '" + Log.class.getName()
            + "' that will be used to log messages to." + "(default is '"
            + StandardLogger.class.getName() + "', which " + "logs to STDERR)",
            "3.1.1");
    private StringConnectionProperty profileSql = new StringConnectionProperty("profileSql",
            null,
            "Deprecated, use 'profileSQL' instead. Trace queries and their execution/fetch times on STDERR (true/false) defaults to 'false'",
            "2.0.14");
    private StringConnectionProperty serverTimezone = new StringConnectionProperty("serverTimezone",
            null,
            "Override detection/mapping of timezone. Used when timezone from server doesn't map to Java timezone",
            "3.0.2");
    private StringConnectionProperty socketFactoryClassName = new StringConnectionProperty("socketFactory",
            StandardSocketFactory.class.getName(),
            "The name of the class that the driver should use for creating socket connections to the server. This class must implement the interface 'com.mysql.jdbc.SocketFactory' and have public no-args constructor.",
            "3.0.3");
    private boolean autoReconnectForPoolsAsBoolean = false;
    private boolean cacheResultSetMetaDataAsBoolean;
    private boolean highAvailabilityAsBoolean = false;
    private boolean profileSQLAsBoolean = false;
    private boolean reconnectTxAtEndAsBoolean = false;

    // Cache these values, they are 'hot'
    private boolean useUnicodeAsBoolean = true;
    private boolean useUsageAdvisorAsBoolean = false;
    private int maxRowsAsInt = -1;

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setAllowLoadLocalInfile(boolean property) {
        this.allowLoadLocalInfile.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getAllowLoadLocalInfile() {
        return this.allowLoadLocalInfile.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setAllowMultiQueries(boolean property) {
        this.allowMultiQueries.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getAllowMultiQueries() {
        return this.allowMultiQueries.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setAutoReconnectForConnectionPools(boolean property) {
        this.autoReconnectForPools.setValue(property);
        this.autoReconnectForPoolsAsBoolean = this.autoReconnectForPools
            .getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getAutoReconnectForPools() {
        return this.autoReconnectForPoolsAsBoolean;
    }

    /**
     * Sets whether or not we should cache result set metadata.
     *
     * @param property
     */
    public void setCacheResultSetMetadata(boolean property) {
        this.cacheResultSetMetadata.setValue(property);
        this.cacheResultSetMetaDataAsBoolean = this.cacheResultSetMetadata.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean getCacheResultSetMetadata() {
        return this.cacheResultSetMetaDataAsBoolean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setCapitalizeDBMDTypes(boolean property) {
        this.capitalizeTypeNames.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getCapitalizeTypeNames() {
        return this.capitalizeTypeNames.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param flag The clobberStreamingResults to set.
     */
    public void setClobberStreamingResults(boolean flag) {
        this.clobberStreamingResults.setValue(flag);
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the clobberStreamingResults.
     */
    public boolean getClobberStreamingResults() {
        return this.clobberStreamingResults.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param timeoutMs
     */
    public void setConnectTimeout(int timeoutMs) {
        this.connectTimeout.setValue(timeoutMs);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getConnectTimeout() {
        return this.connectTimeout.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setContinueBatchOnError(boolean property) {
        this.continueBatchOnError.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getContinueBatchOnError() {
        return this.continueBatchOnError.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setDetectServerPreparedStmts(boolean property) {
        this.detectServerPreparedStmts.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setEmulateLocators(boolean property) {
        this.emulateLocators.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getEmulateLocators() {
        return this.emulateLocators.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setEncoding(String property) {
        this.characterEncoding.setValue(property);
        this.characterEncodingAsString = this.characterEncoding
            .getValueAsString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    protected String getEncoding() {
        return this.characterEncodingAsString;
    }

 


    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setIgnoreNonTxTables(boolean property) {
        this.ignoreNonTxTables.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getIgnoreNonTxTables() {
        return this.ignoreNonTxTables.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setInitialTimeout(int property) {
        this.initialTimeout.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getInitialTimeout() {
        return this.initialTimeout.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getInteractiveClient() {
        return this.isInteractiveClient.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setIsInteractiveClient(boolean property) {
        this.isInteractiveClient.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setLogger(String property) {
        this.loggerClassName.setValueAsObject(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public String getLogger() {
        Object loggerValue = this.loggerClassName.getValueAsObject();

        return ((loggerValue == null) ? null : loggerValue.toString());
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setMaxReconnects(int property) {
        this.maxReconnects.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getMaxReconnects() {
        return this.maxReconnects.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setMaxRows(int property) {
        this.maxRows.setValue(property);
        this.maxRowsAsInt = this.maxRows.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getMaxRows() {
        return this.maxRowsAsInt;
    }

    /**
     * Sets the number of queries that metadata can be cached if caching is
     * enabled.
     *
     * @param value the number of queries to cache metadata for.
     */
    public void setMetadataCacheSize(int value) {
        this.metadataCacheSize.setValue(value);
    }

    /**
     * Returns the number of queries that metadata can be cached if caching is
     * enabled.
     *
     * @return the number of queries to cache metadata for.
     */
    public int getMetadataCacheSize() {
        return this.metadataCacheSize.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setParanoid(boolean property) {
        this.paranoid.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getParanoid() {
        return this.paranoid.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setPedantic(boolean property) {
        this.pedantic.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getPedantic() {
        return this.pedantic.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setProfileSql(boolean property) {
        this.profileSQL.setValue(property);
        this.profileSQLAsBoolean = this.profileSQL.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getProfileSql() {
        return this.profileSQLAsBoolean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setQueriesBeforeRetryMaster(int property) {
        this.queriesBeforeRetryMaster.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getQueriesBeforeRetryMaster() {
        return this.queriesBeforeRetryMaster.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setReconnectAtTxEnd(boolean property) {
        this.reconnectAtTxEnd.setValue(property);
        this.reconnectTxAtEndAsBoolean = this.reconnectAtTxEnd
		.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getReconnectAtTxEnd() {
        return this.reconnectTxAtEndAsBoolean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setRelaxAutoCommit(boolean property) {
        this.relaxAutoCommit.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getRelaxAutoCommit() {
        return this.relaxAutoCommit.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setRequireSSL(boolean property) {
        this.requireSSL.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getRequireSSL() {
        return this.requireSSL.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setSecondsBeforeRetryMaster(int property) {
        this.secondsBeforeRetryMaster.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getSecondsBeforeRetryMaster() {
        return this.secondsBeforeRetryMaster.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property DOCUMENT ME!
     */
    public void setServerTimezone(String property) {
        this.serverTimezone.setValue(property);
    }

    /**
     * Returns the 'serverTimezone' property.
     *
     * @return the configured server timezone property.
     */
    public String getServerTimezone() {
        return this.serverTimezone.getValueAsString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setSocketFactoryClassName(String property) {
        this.socketFactoryClassName.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public String getSocketFactoryClassName() {
        return this.socketFactoryClassName.getValueAsString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setSocketTimeout(int property) {
        this.socketTimeout.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public int getSocketTimeout() {
        return this.socketTimeout.getValueAsInt();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setStrictFloatingPoint(boolean property) {
        this.strictFloatingPoint.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getStrictFloatingPoint() {
        return this.strictFloatingPoint.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setStrictUpdates(boolean property) {
        this.strictUpdates.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getStrictUpdates() {
        return this.strictUpdates.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseCompression(boolean property) {
        this.useCompression.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseCompression() {
        return this.useCompression.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseHostsInPrivileges(boolean property) {
        this.useHostsInPrivileges.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseHostsInPrivileges() {
        return this.useHostsInPrivileges.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseNewIo(boolean property) {
        this.useNewIo.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseNewIo() {
        return this.useNewIo.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseSSL(boolean property) {
        this.useSSL.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseSSL() {
        return this.useSSL.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseServerPreparedStmts() {
        return this.detectServerPreparedStmts.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseStreamLengthsInPrepStmts(boolean property) {
        this.useStreamLengthsInPrepStmts.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseStreamLengthsInPrepStmts() {
        return this.useStreamLengthsInPrepStmts.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseTimezone(boolean property) {
        this.useTimezone.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseTimezone() {
        return this.useTimezone.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param property
     */
    public void setUseUltraDevWorkAround(boolean property) {
        this.useUltraDevWorkAround.setValue(property);
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseUltraDevWorkAround() {
        return this.useUltraDevWorkAround.getValueAsBoolean();
    }

    /**
     * DOCUMENT ME!
     *
     * @return
     */
    public boolean getUseUnicode() {
        return ((Boolean) this.useUnicode.getValueAsObject()).booleanValue();
    }

    /**
     * Sets whether or not the driver advises of proper usage.
     *
     * @param useUsageAdvisorFlag whether or not the driver advises of proper
     *        usage.
     */
    public void setUseUsageAdvisor(boolean useUsageAdvisorFlag) {
        this.useUsageAdvisor.setValue(useUsageAdvisorFlag);
        this.useUsageAdvisorAsBoolean = this.useUsageAdvisor.getValueAsBoolean();
    }

    /**
     * Returns whether or not the driver advises of proper usage.
     *
     * @return the value of useUsageAdvisor
     */
    public boolean getUseUsageAdvisor() {
        return this.useUsageAdvisorAsBoolean;
    }

    /**
     * Returns a description of the connection properties as an XML document.
     *
     * @return the connection properties as an XML document.
     *
     * @throws SQLException if an error occurs.
     */
    public String exposeAsXml() throws SQLException {
        StringBuffer xmlBuf = new StringBuffer();
        xmlBuf.append("<ConnectionProperties>");

        int numPropertiesToSet = PROPERTY_LIST.size();

        for (int i = 0; i < numPropertiesToSet; i++) {
            java.lang.reflect.Field propertyField = (java.lang.reflect.Field) PROPERTY_LIST
                .get(i);

            try {
                ConnectionProperty propToGet = (ConnectionProperty) propertyField
                    .get(this);
                propToGet.syncDriverPropertyInfo();

                xmlBuf.append("\n  <Property name=\"");
                xmlBuf.append(propToGet.getPropertyName());
                xmlBuf.append("\" required=\"");
                xmlBuf.append(propToGet.required);

                xmlBuf.append("\" default=\"");

                if (propToGet.getDefaultValue() != null) {
                    xmlBuf.append(propToGet.getDefaultValue());
                }

                xmlBuf.append("\" since=\"");
                xmlBuf.append(propToGet.sinceVersion);
                xmlBuf.append("\">\n");
                xmlBuf.append("    ");
                xmlBuf.append(propToGet.description);
                xmlBuf.append("\n  </Property>");
            } catch (IllegalAccessException iae) {
                throw new SQLException("Internal properties failure", SQLError.SQL_STATE_GENERAL_ERROR);
            }
        }

        xmlBuf.append("\n</ConnectionProperties>");

        return xmlBuf.toString();
    }

    /**
     * Exposes all ConnectionPropertyInfo instances as DriverPropertyInfo
     *
     * @param info the properties to load into these ConnectionPropertyInfo
     *        instances
     * @param slotsToReserve the number of DPI slots to reserve for 'standard'
     *        DPI properties (user, host, password, etc)
     *
     * @return a list of all ConnectionPropertyInfo instances, as
     *         DriverPropertyInfo
     *
     * @throws SQLException if an error occurs
     */
    protected static DriverPropertyInfo[] exposeAsDriverPropertyInfo(
        Properties info, int slotsToReserve) throws SQLException {
        return (new ConnectionProperties() {
            }).exposeAsDriverPropertyInfoInternal(info, slotsToReserve);
    }

    protected DriverPropertyInfo[] exposeAsDriverPropertyInfoInternal(
        Properties info, int slotsToReserve) throws SQLException {
        initializeProperties(info);

        int numProperties = PROPERTY_LIST.size();

        int listSize = numProperties + slotsToReserve;

        DriverPropertyInfo[] driverProperties = new DriverPropertyInfo[listSize];

        for (int i = slotsToReserve; i < listSize; i++) {
            java.lang.reflect.Field propertyField = (java.lang.reflect.Field) PROPERTY_LIST
                .get(i - slotsToReserve);

            try {
                ConnectionProperty propToExpose = (ConnectionProperty) propertyField
                    .get(this);

                if (info != null) {
                    propToExpose.initializeFrom(info);
                }

                propToExpose.syncDriverPropertyInfo();
                driverProperties[i] = propToExpose;
            } catch (IllegalAccessException iae) {
                throw new SQLException("Internal properties failure", SQLError.SQL_STATE_GENERAL_ERROR);
            }
        }

        return driverProperties;
    }

    protected Properties exposeAsProperties(Properties info)
        throws SQLException {
        if (info == null) {
            info = new Properties();
        }

        int numPropertiesToSet = PROPERTY_LIST.size();

        for (int i = 0; i < numPropertiesToSet; i++) {
            java.lang.reflect.Field propertyField = (java.lang.reflect.Field) PROPERTY_LIST
                .get(i);

            try {
                ConnectionProperty propToGet = (ConnectionProperty) propertyField
                    .get(this);

                Object propValue = propToGet.getValueAsObject();

                if (propValue != null) {
                    info.setProperty(propToGet.getPropertyName(),
                        propValue.toString());
                }
            } catch (IllegalAccessException iae) {
                throw new SQLException("Internal properties failure", SQLError.SQL_STATE_GENERAL_ERROR);
            }
        }

        return info;
    }

    /**
     * Initializes driver properties that come from a JNDI reference (in the
     * case of a javax.sql.DataSource bound into some name service that
     * doesn't handle Java objects directly).
     *
     * @param ref The JNDI Reference that holds RefAddrs for all properties
     *
     * @throws SQLException DOCUMENT ME!
     */
    protected void initializeFromRef(Reference ref) throws SQLException {
        int numPropertiesToSet = PROPERTY_LIST.size();

        for (int i = 0; i < numPropertiesToSet; i++) {
            java.lang.reflect.Field propertyField = (java.lang.reflect.Field) PROPERTY_LIST
                .get(i);

            try {
                ConnectionProperty propToSet = (ConnectionProperty) propertyField
                    .get(this);

                if (ref != null) {
                    propToSet.initializeFrom(ref);
                }
            } catch (IllegalAccessException iae) {
                throw new SQLException("Internal properties failure", SQLError.SQL_STATE_GENERAL_ERROR);
            }
        }

        postInitialization();
    }

    /**
     * Initializes driver properties that come from URL or properties passed to
     * the driver manager.
     *
     * @param info DOCUMENT ME!
     *
     * @throws SQLException DOCUMENT ME!
     */
    protected void initializeProperties(Properties info)
        throws SQLException {
        if (info != null) {
 
            // For backwards-compatibility
            String profileSqlLc = info.getProperty("profileSql");

            if (profileSqlLc != null) {
                info.put("profileSQL", profileSqlLc);
            }

            Properties infoCopy = (Properties) info.clone();

            infoCopy.remove("HOST");
            infoCopy.remove("user");
            infoCopy.remove("password");
            infoCopy.remove("DBNAME");
            infoCopy.remove("PORT");
            infoCopy.remove("profileSql");

            int numPropertiesToSet = PROPERTY_LIST.size();

            for (int i = 0; i < numPropertiesToSet; i++) {
                java.lang.reflect.Field propertyField = (java.lang.reflect.Field) PROPERTY_LIST
                    .get(i);

                try {
                    ConnectionProperty propToSet = (ConnectionProperty) propertyField
                        .get(this);

                    propToSet.initializeFrom(infoCopy);
                } catch (IllegalAccessException iae) {
                    throw new SQLException(
                        "Unable to initialize driver properties due to "
                        + iae.toString(), SQLError.SQL_STATE_GENERAL_ERROR);
                }
            }

            // TODO -- Not yet
            /*
            int numUnknownProperties = infoCopy.size();

            if (numUnknownProperties > 0) {
                StringBuffer errorMessageBuf = new StringBuffer(
                        "Unknown connection ");
                errorMessageBuf.append((numUnknownProperties == 1)
                    ? "property " : "properties ");

                Iterator propNamesItor = infoCopy.keySet().iterator();

                errorMessageBuf.append("'");
                errorMessageBuf.append(propNamesItor.next().toString());
                errorMessageBuf.append("'");

                while (propNamesItor.hasNext()) {
                    errorMessageBuf.append(", '");
                    errorMessageBuf.append(propNamesItor.next().toString());
                    errorMessageBuf.append("'");
                }

                throw new SQLException(errorMessageBuf.toString(), SQLError.SQL_STATE_INVALID_CONNECTION_ATTRIBUTE);
            }
            */

            postInitialization();
        }
    }

    protected void postInitialization() throws SQLException {
        // Support 'old' profileSql capitalization
        if (this.profileSql.getValueAsObject() != null) {
            this.profileSQL.initializeFrom(this.profileSql.getValueAsObject()
                                                          .toString());
        }

        this.reconnectTxAtEndAsBoolean = ((Boolean) this.reconnectAtTxEnd
            .getValueAsObject()).booleanValue();

        // Adjust max rows
        if (this.getMaxRows() == 0) {
            // adjust so that it will become MysqlDefs.MAX_ROWS
            // in execSQL()
            this.maxRows.setValueAsObject(new Integer(-1));
        }

        //
        // Check character encoding
        //
        String testEncoding = this.getEncoding();

        if (testEncoding != null) {
            //	Attempt to use the encoding, and bail out if it
            // can't be used
            try {
                String testString = "abc";
                testString.getBytes(testEncoding);
            } catch (UnsupportedEncodingException UE) {
                throw new SQLException("Unsupported character " + "encoding '"
                    + testEncoding + "'.", "0S100");
            }
        }

        // Metadata caching is only supported on JDK-1.4 and newer
        // because it relies on LinkedHashMap being present.
        // Check (and disable) if not supported
        if (((Boolean) this.cacheResultSetMetadata.getValueAsObject())
                .booleanValue()) {
            try {
                Class.forName("java.util.LinkedHashMap");
            } catch (ClassNotFoundException cnfe) {
                this.cacheResultSetMetadata.setValue(false);
            }
        }

        this.cacheResultSetMetaDataAsBoolean = ((Boolean) this.cacheResultSetMetadata
            .getValueAsObject()).booleanValue();
        this.useUnicodeAsBoolean = ((Boolean) this.useUnicode.getValueAsObject())
            .booleanValue();
        this.characterEncodingAsString = ((String) this.characterEncoding
            .getValueAsObject());
        this.highAvailabilityAsBoolean = ((Boolean) this.autoReconnect
            .getValueAsObject()).booleanValue();
        this.autoReconnectForPoolsAsBoolean = ((Boolean) this.autoReconnectForPools
            .getValueAsObject()).booleanValue();
        this.maxRowsAsInt = ((Integer) this.maxRows.getValueAsObject())
            .intValue();
        this.profileSQLAsBoolean = ((Boolean) this.profileSQL.getValueAsObject())
            .booleanValue();
        this.useUsageAdvisorAsBoolean = ((Boolean) this.useUsageAdvisor
            .getValueAsObject()).booleanValue();
    }

    protected void storeToRef(Reference ref) throws SQLException {
        int numPropertiesToSet = PROPERTY_LIST.size();

        for (int i = 0; i < numPropertiesToSet; i++) {
            java.lang.reflect.Field propertyField = (java.lang.reflect.Field) PROPERTY_LIST
                .get(i);

            try {
                ConnectionProperty propToStore = (ConnectionProperty) propertyField
                    .get(this);

                if (ref != null) {
                    propToStore.storeTo(ref);
                }
            } catch (IllegalAccessException iae) {
                throw new SQLException("Huh?");
            }
        }
    }

    abstract class ConnectionProperty extends DriverPropertyInfo {
        Object defaultValue;
        Object valueAsObject;
        String propertyName;
        String sinceVersion;
        String[] allowableValues;
        int lowerBound;
        int upperBound;

        ConnectionProperty(String propertyNameToSet, Object defaultValueToSet,
            String[] allowableValuesToSet, int lowerBoundToSet,
            int upperBoundToSet, String descriptionToSet,
            String sinceVersionToSet) {
            super(propertyNameToSet, null);

            this.description = descriptionToSet;
            this.propertyName = propertyNameToSet;
            this.defaultValue = defaultValueToSet;
            this.valueAsObject = defaultValueToSet;
            this.allowableValues = allowableValuesToSet;
            this.lowerBound = lowerBoundToSet;
            this.upperBound = upperBoundToSet;
            this.required = false;
            this.sinceVersion = sinceVersionToSet;
        }

        String[] getAllowableValues() {
            return this.allowableValues;
        }

        int getLowerBound() {
            return this.lowerBound;
        }

        int getUpperBound() {
            return this.upperBound;
        }

        void initializeFrom(Properties extractFrom) throws SQLException {
            String extractedValue = extractFrom.getProperty(getPropertyName());
            extractFrom.remove(getPropertyName());
            initializeFrom(extractedValue);
        }

        void initializeFrom(Reference ref) throws SQLException {
            RefAddr refAddr = ref.get(getPropertyName());

            if (refAddr != null) {
                String refContentAsString = (String) refAddr.getContent();

                initializeFrom(refContentAsString);
            }
        }

        abstract void initializeFrom(String extractedValue)
            throws SQLException;

        Object getDefaultValue() {
            return this.defaultValue;
        }

        String getPropertyName() {
            return this.propertyName;
        }

        abstract boolean isRangeBased();

        abstract boolean hasValueConstraints();

        void setValueAsObject(Object obj) {
            this.valueAsObject = obj;
        }

        Object getValueAsObject() {
            return this.valueAsObject;
        }

        void storeTo(Reference ref) {
            if (getValueAsObject() != null) {
                ref.add(new StringRefAddr(getPropertyName(),
                        getValueAsObject().toString()));
            }
        }

        /**
         * Synchronizes the state of a ConnectionProperty so that it can be
         * exposed as a DriverPropertyInfo instance.
         */
        void syncDriverPropertyInfo() {
            this.choices = getAllowableValues();
            this.value = (this.valueAsObject != null)
                ? this.valueAsObject.toString() : null;
        }

        void validateStringValues(String valueToValidate)
            throws SQLException {
            String[] validateAgainst = getAllowableValues();

            if (valueToValidate == null) {
                return;
            }

            if ((validateAgainst == null) || (validateAgainst.length == 0)) {
                return;
            }

            for (int i = 0; i < validateAgainst.length; i++) {
                if ((validateAgainst[i] != null)
                        && validateAgainst[i].equalsIgnoreCase(valueToValidate)) {
                    return;
                }
            }

            StringBuffer errorMessageBuf = new StringBuffer();

            errorMessageBuf.append("The connection property '");
            errorMessageBuf.append(getPropertyName());
            errorMessageBuf.append("' only accepts values of the form: ");

            if (validateAgainst.length != 0) {
                errorMessageBuf.append("'");
                errorMessageBuf.append(validateAgainst[0]);
                errorMessageBuf.append("'");

                for (int i = 1; i < (validateAgainst.length - 1); i++) {
                    errorMessageBuf.append(", ");
                    errorMessageBuf.append("'");
                    errorMessageBuf.append(validateAgainst[i]);
                    errorMessageBuf.append("'");
                }

                errorMessageBuf.append(" or '");
                errorMessageBuf.append(validateAgainst[validateAgainst.length
                    - 1]);
                errorMessageBuf.append("'");
            }

            errorMessageBuf.append(". The value '");
            errorMessageBuf.append(valueToValidate);
            errorMessageBuf.append("' is not in this set.");

            throw new SQLException(errorMessageBuf.toString(), SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
        }
    }

    class BooleanConnectionProperty extends ConnectionProperty {
        /**
         * DOCUMENT ME!
         *
         * @param propertyNameToSet
         * @param defaultValueToSet
         * @param descriptionToSet DOCUMENT ME!
         * @param sinceVersionToSet DOCUMENT ME!
         */
        BooleanConnectionProperty(String propertyNameToSet,
            boolean defaultValueToSet, String descriptionToSet,
            String sinceVersionToSet) {
            super(propertyNameToSet, new Boolean(defaultValueToSet), null, 0,
                0, descriptionToSet, sinceVersionToSet);
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#getAllowableValues()
         */
        String[] getAllowableValues() {
            return new String[] { "true", "false", "yes", "no" };
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#isRangeBased()
         */
        boolean isRangeBased() {
            return false;
        }

        void setValue(boolean valueFlag) {
            this.valueAsObject = new Boolean(valueFlag);
        }
        
        boolean getValueAsBoolean() {
        	return ((Boolean) this.valueAsObject).booleanValue();
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#hasValueConstraints()
         */
        boolean hasValueConstraints() {
            return true;
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#initializeFrom(java.util.Properties)
         */
        void initializeFrom(String extractedValue) throws SQLException {
            if (extractedValue != null) {
                validateStringValues(extractedValue);

                this.valueAsObject = new Boolean(extractedValue
                        .equalsIgnoreCase("TRUE")
                        || extractedValue.equalsIgnoreCase("YES"));
            } else {
                this.valueAsObject = this.defaultValue;
            }
        }
    }

    class IntegerConnectionProperty extends ConnectionProperty {
        /**
         * DOCUMENT ME!
         *
         * @param propertyNameToSet
         * @param defaultValueToSet
         * @param descriptionToSet
         * @param sinceVersionToSet DOCUMENT ME!
         */
        IntegerConnectionProperty(String propertyNameToSet,
            int defaultValueToSet, String descriptionToSet,
            String sinceVersionToSet) {
            this(propertyNameToSet, defaultValueToSet, 0, 0, descriptionToSet,
                sinceVersionToSet);
        }

        IntegerConnectionProperty(String propertyNameToSet,
            int defaultValueToSet, int lowerBoundToSet, int upperBoundToSet,
            String descriptionToSet, String sinceVersionToSet) {
            super(propertyNameToSet, new Integer(defaultValueToSet), null,
                lowerBoundToSet, upperBoundToSet, descriptionToSet,
                sinceVersionToSet);
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#getAllowableValues()
         */
        String[] getAllowableValues() {
            return null;
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#getLowerBound()
         */
        int getLowerBound() {
            return this.lowerBound;
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#isRangeBased()
         */
        boolean isRangeBased() {
            return getUpperBound() != getLowerBound();
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#getUpperBound()
         */
        int getUpperBound() {
            return this.upperBound;
        }

        void setValue(int valueFlag) {
            this.valueAsObject = new Integer(valueFlag);
        }
        
        int getValueAsInt() {
        	return ((Integer)this.valueAsObject).intValue();
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#hasValueConstraints()
         */
        boolean hasValueConstraints() {
            return false;
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#initializeFrom(java.lang.String)
         */
        void initializeFrom(String extractedValue) throws SQLException {
            if (extractedValue != null) {
                try {
                    // Parse decimals, too
                    int intValue = Double.valueOf(extractedValue).intValue();

                    /*
                    if (isRangeBased()) {

                    if ((intValue < getLowerBound())
                    || (intValue > getUpperBound())) {
                    throw new SQLException("The connection property '"
                    + getPropertyName()
                    + "' only accepts integer values in the range of "
                    + getLowerBound() + " - " + getUpperBound()
                    + ", the value '" + extractedValue
                    + "' exceeds this range.", SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
                    }
                    }*/
                    this.valueAsObject = new Integer(intValue);
                } catch (NumberFormatException nfe) {
                    throw new SQLException("The connection property '"
                        + getPropertyName()
                        + "' only accepts integer values. The value '"
                        + extractedValue
                        + "' can not be converted to an integer.", SQLError.SQL_STATE_ILLEGAL_ARGUMENT);
                }
            } else {
                this.valueAsObject = this.defaultValue;
            }
        }
    }

    class StringConnectionProperty extends ConnectionProperty {
        /**
         * DOCUMENT ME!
         *
         * @param propertyNameToSet
         * @param defaultValueToSet
         * @param allowableValuesToSet
         * @param descriptionToSet
         * @param sinceVersionToSet DOCUMENT ME!
         */
        StringConnectionProperty(String propertyNameToSet,
            String defaultValueToSet, String[] allowableValuesToSet,
            String descriptionToSet, String sinceVersionToSet) {
            super(propertyNameToSet, defaultValueToSet, allowableValuesToSet,
                0, 0, descriptionToSet, sinceVersionToSet);
        }

        StringConnectionProperty(String propertyNameToSet,
            String defaultValueToSet, String descriptionToSet,
            String sinceVersionToSet) {
            this(propertyNameToSet, defaultValueToSet, null, descriptionToSet,
                sinceVersionToSet);
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#isRangeBased()
         */
        boolean isRangeBased() {
            return false;
        }

        void setValue(String valueFlag) {
            this.valueAsObject = valueFlag;
        }
        
		String getValueAsString() {
			return (String) this.valueAsObject;
		}

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#hasValueConstraints()
         */
        boolean hasValueConstraints() {
            return (this.allowableValues != null)
            && (this.allowableValues.length > 0);
        }

        /**
         * @see com.mysql.jdbc.ConnectionProperties.ConnectionProperty#initializeFrom(java.util.Properties)
         */
        void initializeFrom(String extractedValue) throws SQLException {
            if (extractedValue != null) {
                validateStringValues(extractedValue);

                this.valueAsObject = extractedValue;
            } else {
                this.valueAsObject = this.defaultValue;
            }
        }
    }
	/**
	 * @return Returns the cachePreparedStatements.
	 */
	public boolean getCachePreparedStatements() {
		return ((Boolean) this.cachePreparedStatements.getValueAsObject()).booleanValue();
	}

	/**
	 * @param cachePreparedStatements The cachePreparedStatements to set.
	 */
	public void setCachePreparedStatements(boolean flag) {
		this.cachePreparedStatements.setValue(flag);
	}

	/**
	 * @return Returns the preparedStatementCacheSize.
	 */
	public int getPreparedStatementCacheSize() {
		return ((Integer) this.preparedStatementCacheSize.getValueAsObject()).intValue();
	}

	/**
	 * @param preparedStatementCacheSize The preparedStatementCacheSize to set.
	 */
	public void setPreparedStatementCacheSize(int cacheSize) {
		this.preparedStatementCacheSize.setValue(cacheSize);
	}

	/**
	 * @return Returns the preparedStatementCacheSqlLimit.
	 */
	public int getPreparedStatementCacheSqlLimit() {
		return ((Integer) this.preparedStatementCacheSqlLimit.getValueAsObject()).intValue();
	}

	/**
	 * @param preparedStatementCacheSqlLimit The preparedStatementCacheSqlLimit to set.
	 */
	public void setPreparedStatementCacheSqlLimit(int cacheSqlLimit) {
		this.preparedStatementCacheSqlLimit.setValue(cacheSqlLimit);
	}

	/**
	 * @return Returns the useUnbufferedInput.
	 */
	protected boolean useUnbufferedInput() {
		return ((Boolean)useUnbufferedInput.getValueAsObject()).booleanValue();
	}



	/**
	 * @param detectServerPreparedStmts The detectServerPreparedStmts to set.
	 */
	public void setUseServerPreparedStmts(boolean flag) {
		this.detectServerPreparedStmts.setValue(flag);
	}

	/**
	 * @return Returns the isInteractiveClient.
	 */
	public boolean getIsInteractiveClient() {
		return this.isInteractiveClient.getValueAsBoolean();
	}


	/**
	 * @return Returns the loggerClassName.
	 */
	public String getLoggerClassName() {
		return this.loggerClassName.getValueAsString();
	}

	/**
	 * @param loggerClassName The loggerClassName to set.
	 */
	public void setLoggerClassName(String className) {
		this.loggerClassName.setValue(className);
	}

	/**
	 * @return Returns the profileSQL flag
	 */
	public boolean getProfileSQL() {
		return this.profileSQL.getValueAsBoolean();
	}

	/**
	 * @param profileSQL The profileSQL to set.
	 */
	public void setProfileSQL(boolean flag) {
		this.profileSQL.setValue(flag);
	}

	/**
	 * @return Returns the useUnbufferedInput.
	 */
	public boolean getUseUnbufferedInput() {
		return this.useUnbufferedInput.getValueAsBoolean();
	}

	/**
	 * @param useUnbufferedInput The useUnbufferedInput to set.
	 */
	public void setUseUnbufferedInput(boolean flag) {
		this.useUnbufferedInput.setValue(flag);
	}

	/**
	 * @param autoReconnect The autoReconnect to set.
	 */
	public void setAutoReconnect(boolean flag) {
		this.autoReconnect.setValue(flag);
	}

	/**
	 * @param autoReconnectForPools The autoReconnectForPools to set.
	 */
	public void setAutoReconnectForPools(boolean flag) {
		this.autoReconnectForPools.setValue(flag);
	}

	/**
	 * @param capitalizeTypeNames The capitalizeTypeNames to set.
	 */
	public void setCapitalizeTypeNames(boolean flag) {
		this.capitalizeTypeNames.setValue(flag);
	}

	/**
	 * @param characterEncoding The characterEncoding to set.
	 */
	public void setCharacterEncoding(String encoding) {
		this.characterEncoding.setValue(encoding);
	}

	/**
	 * @param useUnicode The useUnicode to set.
	 */
	public void setUseUnicode(boolean flag) {
		this.useUnicode.setValue(flag);
	}
	
	/**
	 * DOCUMENT ME!
	 *
	 * @return
	 */
	protected boolean getHighAvailability() {
		return this.highAvailabilityAsBoolean;
	}


	/**
	 * DOCUMENT ME!
	 *
	 * @param property
	 */
	protected void setDoUnicode(boolean property) {
		this.useUnicode.setValue(property);
	}
	
	/**
	 * DOCUMENT ME!
	 *
	 * @param property
	 */
	protected void setHighAvailability(boolean property) {
		this.autoReconnect.setValue(property);
		this.highAvailabilityAsBoolean = this.autoReconnect.getValueAsBoolean();
	}
}
