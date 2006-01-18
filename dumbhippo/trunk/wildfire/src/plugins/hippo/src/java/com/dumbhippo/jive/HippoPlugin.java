package com.dumbhippo.jive;

import java.io.File;

import org.jivesoftware.wildfire.IQRouter;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.component.InternalComponentManager;
import org.jivesoftware.wildfire.container.Plugin;
import org.jivesoftware.wildfire.container.PluginManager;
import org.jivesoftware.util.Log;
import org.xmpp.component.ComponentException;

import com.dumbhippo.jive.rooms.RoomHandler;
import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;
import com.dumbhippo.server.MessengerGlueRemote;
import com.dumbhippo.server.util.EJBUtil;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {
	
	private RoomHandler roomHandler;
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		try {
			Log.debug("Initializing Hippo plugin");
			GlobalSetup.disableLog4j();
			
			// this is a little broken since we have no connection tracking and thus
			// no clue when the server restarts and we need to call this again
			MessengerGlueRemote glue = EJBUtil.defaultLookup(MessengerGlueRemote.class);
			glue.serverStartup(System.currentTimeMillis());
			
			IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
			iqRouter.addHandler(new ClientMethodIQHandler());		
			iqRouter.addHandler(new ClientInfoIQHandler());
			Log.debug("Instantiating MySpace handler");		
			iqRouter.addHandler(new MySpaceIQHandler());					
			iqRouter.addHandler(new MusicIQHandler());
			Log.debug("Adding PresenceMonitor");
			SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
			sessionManager.registerListener(new PresenceMonitor());
					
			roomHandler = new RoomHandler();
			try {
				InternalComponentManager.getInstance().addComponent("rooms", roomHandler);
			} catch (ComponentException e) {
				throw new RuntimeException("Error adding Rooms component", e);
			}

			Log.debug("... done initializing Hippo plugin");
		} catch (Exception e) {
			Log.debug("Failed to init hippo plugin: " + ExceptionUtils.getRootCause(e).getMessage(), e);
		}
	}

	public void destroyPlugin() {
		Log.debug("Unloading Hippo plugin");
		
		Log.debug("Removing rooms route");
		InternalComponentManager.getInstance().removeComponent("rooms");

		Log.debug("... done unloading Hippo plugin");
	}
}
