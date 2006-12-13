/**
 * $RCSfile$
 * $Revision: 1378 $
 * $Date: 2005-05-23 15:25:24 -0300 (Mon, 23 May 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.wildfire;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Represents a port on which the server will listen for connections.
 * Used to aggregate information that the rest of the system needs
 * regarding the port while hiding implementation details.
 *
 * @author Iain Shigeoka
 */
public class ServerPort {

    private int port;
    private String interfaceName;
    private ArrayList names;
    private String address;
    private boolean secure;
    private String algorithm;
    private Type type;

    public ServerPort(int port, String interfaceName, String name, String address,
            boolean isSecure, String algorithm, Type type)
    {
        this.port = port;
        this.interfaceName = interfaceName;
        this.names = new ArrayList(1);
        this.names.add(name);
        this.address = address;
        this.secure = isSecure;
        this.algorithm = algorithm;
        this.type = type;
    }

    /**
     * Returns the port number that is being used.
     *
     * @return the port number this server port is listening on.
     */
    public int getPort() {
        return port;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * Returns the logical domains for this server port. As multiple
     * domains may point to the same server, this helps to define what
     * the server considers "local".
     *
     * @return The server domain name(s) as Strings
     */
    public Iterator getDomainNames() {
        return names.iterator();
    }

    /**
     * Returns the dot separated IP address for the server.
     *
     * @return The dot separated IP address for the server
     */
    public String getIPAddress() {
        return address;
    }

    /**
     * Determines if the connection is secure.
     *
     * @return True if the connection is secure
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Returns the basic protocol/algorithm being used to secure
     * the port connections. An example would be "SSL" or "TLS".
     *
     * @return The protocol used or null if this is not a secure server port
     */
    public String getSecurityType() {
        return algorithm;
    }

    /**
     * Returns true if other servers can connect to this port for s2s communication.
     *
     * @return true if other servers can connect to this port for s2s communication.
     */
    public boolean isServerPort() {
        return type == Type.server;
    }

    /**
     * Returns true if clients can connect to this port.
     *
     * @return true if clients can connect to this port.
     */
    public boolean isClientPort() {
        return type == Type.client;
    }

    /**
     * Returns true if external components can connect to this port.
     *
     * @return true if external components can connect to this port.
     */
    public boolean isComponentPort() {
        return type == Type.component;
    }

    public static enum Type {
        client,

        server,

        component;
    }
}
