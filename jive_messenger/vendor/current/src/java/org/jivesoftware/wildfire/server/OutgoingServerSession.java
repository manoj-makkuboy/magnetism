/**
 * $RCSfile$
 * $Revision: 1703 $
 * $Date: 2005-07-26 12:25:11 -0400 (Tue, 26 Jul 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.server;

import org.jivesoftware.messenger.*;
import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Server-to-server communication is done using two TCP connections between the servers. One
 * connection is used for sending packets while the other connection is used for receiving packets.
 * The <tt>OutgoingServerSession</tt> represents the connection to a remote server that will only
 * be used for sending packets.<p>
 *
 * Currently only the Server Dialback method is being used for authenticating with the remote
 * server. Use {@link #authenticateDomain(String, String)} to create a new connection to a remote
 * server that will be used for sending packets to the remote server from the specified domain.
 * Only the authenticated domains with the remote server will be able to effectively send packets
 * to the remote server. The remote server will reject and close the connection if a
 * non-authenticated domain tries to send a packet through this connection.<p>
 *
 * Once the connection has been established with the remote server and at least a domain has been
 * authenticated then a new route will be added to the routing table for this connection. For
 * optimization reasons the same outgoing connection will be used even if the remote server has
 * several hostnames. However, different routes will be created in the routing table for each
 * hostname of the remote server.
 *
 * @author Gaston Dombiak
 */
public class OutgoingServerSession extends Session {

    /**
     * Regular expression to ensure that the hostname contains letters.
     */
    private static Pattern pattern = Pattern.compile("[a-zA-Z]");

    private Collection<String> authenticatedDomains = new ArrayList<String>();
    private Collection<String> hostnames = new ArrayList<String>();
    private OutgoingServerSocketReader socketReader;

    /**
     * Creates a new outgoing connection to the specified hostname if no one exists. The port of
     * the remote server could be configured by setting the <b>xmpp.server.socket.remotePort</b> 
     * property or otherwise the standard port 5269 will be used. Either a new connection was
     * created or already existed the specified hostname will be authenticated with the remote
     * server. Once authenticated the remote server will start accepting packets from the specified
     * domain.<p>
     *
     * The Server Dialback method is currently the only implemented method for server-to-server
     * authentication. This implies that the remote server will ask the Authoritative Server
     * to verify the domain to authenticate. Most probably this server will act as the
     * Authoritative Server. See {@link IncomingServerSession} for more information.
     *
     * @param domain the local domain to authenticate with the remote server.
     * @param hostname the hostname of the remote server.
     * @return True if the domain was authenticated by the remote server.
     */
    public static boolean authenticateDomain(String domain, String hostname) {
        if (hostname == null || hostname.length() == 0 || hostname.trim().indexOf(' ') > -1) {
            // Do nothing if the target hostname is empty, null or contains whitespaces
            return false;
        }
        try {
            // Check if the remote hostname is in the blacklist
            if (!RemoteServerManager.canAccess(hostname)) {
                return false;
            }

            // Check if a session already exists to the desired hostname (i.e. remote server). If
            // no one exists then create a new session. The same session will be used for the same
            // hostname for all the domains to authenticate
            SessionManager sessionManager = SessionManager.getInstance();
            OutgoingServerSession session = sessionManager.getOutgoingServerSession(hostname);
            if (session == null) {
                // Try locating if the remote server has previously authenticated with this server
                IncomingServerSession incomingSession = sessionManager.getIncomingServerSession(
                        hostname);
                if (incomingSession != null) {
                    for (String otherHostname : incomingSession.getValidatedDomains()) {
                        session = sessionManager.getOutgoingServerSession(otherHostname);
                        if (session != null) {
                            // A session to the same remote server but with different hostname
                            // was found. Use this session and add the new hostname to the session
                            session.addHostname(hostname);
                            break;
                        }
                    }
                }
            }
            if (session == null) {
                int port = RemoteServerManager.getPortForServer(hostname);
                // No session was found to the remote server so make sure that only one is created
                synchronized (hostname.intern()) {
                    session = sessionManager.getOutgoingServerSession(hostname);
                    if (session == null) {
                        session =
                                new ServerDialback().createOutgoingSession(domain, hostname, port);
                        if (session != null) {
                            // Add the new hostname to the list of names that the server may have
                            session.addHostname(hostname);
                            // Add the validated domain as an authenticated domain
                            session.addAuthenticatedDomain(domain);
                            // Notify the SessionManager that a new session has been created
                            sessionManager.outgoingServerSessionCreated(session);
                            return true;
                        }
                        else {
                            // Ensure that the hostname is not an IP address (i.e. contains chars)
                            if (!pattern.matcher(hostname).find()) {
                                return false;
                            }
                            // Check if hostname is a subdomain of an existing outgoing session
                            for (String otherHost : sessionManager.getOutgoingServers()) {
                                if (hostname.contains(otherHost)) {
                                    session = sessionManager.getOutgoingServerSession(otherHost);
                                    // Add the new hostname to the found session
                                    session.addHostname(hostname);
                                    return true;
                                }
                            }
                            // Try to establish a connection to candidate hostnames. Iterate on the
                            // substring after the . and try to establish a connection. If a
                            // connection is established then the same session will be used for
                            // sending packets to the "candidate hostname" as well as for the
                            // requested hostname (i.e. the subdomain of the candidate hostname)
                            // This trick is useful when remote servers haven't registered in their
                            // DNSs an entry for their subdomains
                            int index = hostname.indexOf('.');
                            while (index > -1 && index < hostname.length()) {
                                String newHostname = hostname.substring(index + 1);
                                String serverName = XMPPServer.getInstance().getServerInfo()
                                        .getName();
                                if ("com".equals(newHostname) || "net".equals(newHostname) ||
                                        "org".equals(newHostname) ||
                                        "gov".equals(newHostname) ||
                                        "edu".equals(newHostname) ||
                                        serverName.equals(newHostname)) {
                                    return false;
                                }
                                session =
                                        new ServerDialback().createOutgoingSession(domain, newHostname, port);
                                if (session != null) {
                                    // Add the new hostname to the list of names that the server may have
                                    session.addHostname(hostname);
                                    // Add the validated domain as an authenticated domain
                                    session.addAuthenticatedDomain(domain);
                                    // Notify the SessionManager that a new session has been created
                                    sessionManager.outgoingServerSessionCreated(session);
                                    // Add the new hostname to the found session
                                    session.addHostname(newHostname);
                                    return true;
                                }
                                else {
                                    index = hostname.indexOf('.', index + 1);
                                }
                            }
                            return false;
                        }
                    }
                }
            }
            if (session.getAuthenticatedDomains().contains(domain)) {
                // Do nothing since the domain has already been authenticated
                return true;
            }
            // A session already exists so authenticate the domain using that session
            ServerDialback method = new ServerDialback(session.getConnection(), domain);
            if (method.authenticateDomain(session.socketReader, domain, hostname,
                    session.getStreamID().getID())) {
                // Add the validated domain as an authenticated domain
                session.addAuthenticatedDomain(domain);
                return true;
            }
        }
        catch (Exception e) {
            Log.error("Error authenticating domain with remote server: " + hostname, e);
        }
        return false;
    }

    OutgoingServerSession(String serverName, Connection connection,
            OutgoingServerSocketReader socketReader, StreamID streamID) {
        super(serverName, connection, streamID);
        this.socketReader = socketReader;
        socketReader.setSession(this);
    }

    public void process(Packet packet) throws UnauthorizedException, PacketException {
        if (conn != null && !conn.isClosed()) {
            try {
                conn.deliver(packet);
            }
            catch (Exception e) {
                Log.error(LocaleUtils.getLocalizedString("admin.error"), e);
            }
        }
    }

    /**
     * Returns a collection with all the domains, subdomains and virtual hosts that where
     * authenticated. The remote server will accept packets sent from any of these domains,
     * subdomains and virtual hosts.
     *
     * @return domains, subdomains and virtual hosts that where validated.
     */
    public Collection<String> getAuthenticatedDomains() {
        return Collections.unmodifiableCollection(authenticatedDomains);
    }

    /**
     * Adds a new authenticated domain, subdomain or virtual host to the list of
     * authenticated domains for the remote server. The remote server will accept packets
     * sent from this new authenticated domain.
     *
     * @param domain the new authenticated domain, subdomain or virtual host to add.
     */
    public void addAuthenticatedDomain(String domain) {
        authenticatedDomains.add(domain);
    }

    /**
     * Removes an authenticated domain from the list of authenticated domains. The remote
     * server will no longer be able to accept packets sent from the removed domain, subdomain or
     * virtual host.
     *
     * @param domain the domain, subdomain or virtual host to remove from the list of
     *               authenticated domains.
     */
    public void removeAuthenticatedDomain(String domain) {
        authenticatedDomains.remove(domain);
    }

    /**
     * Returns the list of hostnames related to the remote server. This tracking is useful for
     * reusing the same session for the same remote server even if the server has many names.
     *
     * @return the list of hostnames related to the remote server.
     */
    public Collection<String> getHostnames() {
        return Collections.unmodifiableCollection(hostnames);
    }

    /**
     * Adds a new hostname to the list of known hostnames of the remote server. This tracking is
     * useful for reusing the same session for the same remote server even if the server has
     * many names.
     *
     * @param hostname the new known name of the remote server
     */
    private void addHostname(String hostname) {
        if (hostnames.add(hostname)) {
            // Register the outgoing session in the SessionManager. If the session
            // was already registered nothing happens
            sessionManager.registerOutgoingServerSession(hostname, this);
            // Add a new route for this new session
            XMPPServer.getInstance().getRoutingTable().addRoute(new JID(hostname), this);
        }
    }

}
