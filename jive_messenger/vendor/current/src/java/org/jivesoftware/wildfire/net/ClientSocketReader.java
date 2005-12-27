/**
 * $RCSfile$
 * $Revision: 1368 $
 * $Date: 2005-05-23 13:45:49 -0400 (Mon, 23 May 2005) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.net;

import org.dom4j.Element;
import org.jivesoftware.messenger.ClientSession;
import org.jivesoftware.messenger.PacketRouter;
import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

import java.io.IOException;
import java.net.Socket;

/**
 * A SocketReader specialized for client connections. This reader will be used when the open
 * stream contains a jabber:client namespace. Received packet will have their FROM attribute
 * overriden to avoid spoofing.
 *
 * @author Gaston Dombiak
 */
public class ClientSocketReader extends SocketReader {

    public ClientSocketReader(PacketRouter router, String serverName, Socket socket,
            SocketConnection connection) {
        super(router, serverName, socket, connection);
    }

    protected void processIQ(IQ packet) throws UnauthorizedException {
        // Overwrite the FROM attribute to avoid spoofing
        packet.setFrom(session.getAddress());
        super.processIQ(packet);
    }

    protected void processPresence(Presence packet) throws UnauthorizedException {
        // Overwrite the FROM attribute to avoid spoofing
        packet.setFrom(session.getAddress());
        super.processPresence(packet);
    }

    protected void processMessage(Message packet) throws UnauthorizedException {
        // Overwrite the FROM attribute to avoid spoofing
        packet.setFrom(session.getAddress());
        super.processMessage(packet);
    }

    /**
     * Only packets of type Message, Presence and IQ can be processed by this class. Any other
     * type of packet is unknown and thus rejected generating the connection to be closed.
     *
     * @param doc the unknown DOM element that was received
     * @return always false.
     */
    protected boolean processUnknowPacket(Element doc) {
        return false;
    }

    boolean createSession(String namespace) throws UnauthorizedException, XmlPullParserException,
            IOException {
        if ("jabber:client".equals(namespace)) {
            // The connected client is a regular client so create a ClientSession
            session = ClientSession.createSession(serverName, reader, connection);
            return true;
        }
        return false;
    }

}
