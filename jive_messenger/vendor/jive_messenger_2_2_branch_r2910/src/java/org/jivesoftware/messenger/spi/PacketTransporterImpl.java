/**
 * $RCSfile$
 * $Revision: 624 $
 * $Date: 2004-12-05 00:38:08 -0500 (Sun, 05 Dec 2004) $
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.spi;

import org.jivesoftware.messenger.container.BasicModule;
import org.jivesoftware.util.Log;
import org.jivesoftware.messenger.*;
import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.messenger.transport.TransportHandler;
import org.xmpp.packet.Packet;

/**
 * In-memory implementation of the packet transporter service.
 *
 * @author Iain Shigeoka
 */
public class PacketTransporterImpl extends BasicModule  {

    /**
     * The handler that does the actual delivery (could be a channel instead)
     */
    private TransportHandler transportHandler;

    /**
     * deliverer for xmpp server
     */
    private PacketDeliverer deliverer;

    /**
     * xmpp server
     */
    private XMPPServer xmppServer;

    /**
     * This is a singleton, you can't create one. Be very careful not to do anything
     * that refers back to the factory's create method. Do initialization in the init()
     * method if at all possible.
     */
    public PacketTransporterImpl() {
        super("XMPP Packet Transporter");
    }

    /**
     * Obtain the transport handler that this transporter uses for delivering
     * transport packets.
     *
     * @return The transport handler instance used by this transporter
     */
    public TransportHandler getTransportHandler() {
        return transportHandler;
    }

    /**
     * Delivers the given packet based on packet recipient and sender. The
     * deliverer defers actual routing decisions to other classes.
     * <h2>Warning</h2>
     * Be careful to enforce concurrency DbC of concurrent by synchronizing
     * any accesses to class resources.
     *
     * @param packet The packet to route
     * @throws NullPointerException If the packet is null or the
     *                              packet could not be routed
     */
    public void deliver(Packet packet) throws UnauthorizedException, PacketException {
        if (packet == null) {
            throw new NullPointerException();
        }

        if (xmppServer != null && xmppServer.isLocal(packet.getTo())) {
            deliverer.deliver(packet);
        }
        else if (transportHandler != null) {
            transportHandler.process(packet);
        }
        else {
            Log.warn("Could not deliver message: no deliverer available "
                    + packet.toString());
        }
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        xmppServer = server;
        deliverer = server.getPacketDeliverer();
        transportHandler = server.getTransportHandler();
    }
}
