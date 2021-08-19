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

import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.Socket;
import java.net.SocketException;

import java.util.Properties;


/**
 * Socket factory for vanilla TCP/IP sockets (the standard)
 *
 * @author Mark Matthews
 */
public class StandardSocketFactory implements SocketFactory {
    //~ Instance fields --------------------------------------------------------

    /** The underlying TCP/IP socket to use */
    protected Socket rawSocket = null;

    /** The hostname to connect to */
    protected String host = null;

    /** The port number to connect to */
    protected int port = 3306;

    //~ Methods ----------------------------------------------------------------

    /**
     * Called by the driver after issuing the MySQL protocol handshake and
     * reading the results of the handshake.
     *
     * @throws SocketException if a socket error occurs
     * @throws IOException if an I/O error occurs
     *
     * @return The socket to use after the handshake
     */
    public Socket afterHandshake() throws SocketException, IOException {
        return this.rawSocket;
    }

    /**
     * Called by the driver before issuing the MySQL protocol handshake.
     * Should return the socket instance that should be used during
     * the handshake.
     *
     * @throws SocketException if a socket error occurs
     * @throws IOException if an I/O error occurs
     *
     * @return the socket to use before the handshake
     */
    public Socket beforeHandshake() throws SocketException, IOException {
        return this.rawSocket;
    }

    /**
     * @see com.mysql.jdbc.SocketFactory#createSocket(Properties)
     */
    public Socket connect(String hostname, Properties props)
        throws SocketException, IOException {
       	
        if (props != null) {
            this.host = hostname;

            String portStr = props.getProperty("PORT");

            if (portStr != null) {
                this.port = Integer.parseInt(portStr);
            }

            boolean hasConnectTimeoutMethod = false;

            Method connectWithTimeoutMethod = null;

            try {
                // Have to do this with reflection, otherwise older JVMs croak
                Class socketAddressClass = Class.forName(
                        "java.net.SocketAddress");

                connectWithTimeoutMethod = Socket.class.getMethod("connect",
                        new Class[] { socketAddressClass, Integer.TYPE });

                hasConnectTimeoutMethod = true;
            } catch (NoClassDefFoundError noClassDefFound) {
                hasConnectTimeoutMethod = false;
            } catch (NoSuchMethodException noSuchMethodEx) {
                hasConnectTimeoutMethod = false;
            } catch (Throwable catchAll) {
                hasConnectTimeoutMethod = false;
            }

            int connectTimeout = 0;

            String connectTimeoutStr = props.getProperty("connectTimeout");

            if (connectTimeoutStr != null) {
                try {
                    connectTimeout = Integer.parseInt(connectTimeoutStr);
                } catch (NumberFormatException nfe) {
                    throw new SocketException("Illegal value '"
                        + connectTimeoutStr + "' for connectTimeout");
                }
            }

            if (this.host != null) {
                if (connectWithTimeoutMethod == null || !hasConnectTimeoutMethod || (connectTimeout == 0)) {
                    this.rawSocket = new Socket(this.host, this.port);
                } else {
                    // must explicitly state this due to classloader issues
                    // when running on older JVMs :(
                    try {
                        Class inetSocketAddressClass = Class.forName(
                                "java.net.InetSocketAddress");
                        Constructor addrConstructor = inetSocketAddressClass
                            .getConstructor(new Class[] {
                                    String.class, Integer.TYPE
                                });

                        Object sockAddr = addrConstructor.newInstance(new Object[] {
                                    this.host, new Integer(this.port)
                                });

                        this.rawSocket = new Socket();
                        connectWithTimeoutMethod.invoke(this.rawSocket,
                            new Object[] { sockAddr, new Integer(connectTimeout) });
                    } catch (InvocationTargetException itEx) {
                    	Throwable cause = itEx.getCause();
                    	
                    	if (cause != null && cause instanceof SocketException) {
                    		throw (SocketException)cause;
                    	} else if (cause != null) {
                    		throw new SocketException(cause.toString());
                    	} else {
                    		throw new SocketException(itEx.toString());
                    	}
                    } catch (Throwable t) {
                        throw new SocketException(t.toString());
                    }
                }

                try {
                    this.rawSocket.setTcpNoDelay(true);
                } catch (Exception ex) {
                    /* Ignore */
                    ;
                }

                return this.rawSocket;
            }
        }

        throw new SocketException("Unable to create socket");
    }
}
