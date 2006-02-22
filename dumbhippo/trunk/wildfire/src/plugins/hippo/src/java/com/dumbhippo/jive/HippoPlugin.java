package com.dumbhippo.jive;

import java.io.File;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.IQRouter;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.component.InternalComponentManager;
import org.jivesoftware.wildfire.container.Plugin;
import org.jivesoftware.wildfire.container.PluginManager;
import org.xmpp.component.ComponentException;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.jive.rooms.RoomHandler;

/**
 * Our plugin for Jive Messenger
 */
public class HippoPlugin implements Plugin {
	
	private RoomHandler roomHandler;
	private PresenceMonitor presenceMonitor;
	
	public void initializePlugin(PluginManager pluginManager, File pluginDirectory) {
		try {
			Log.debug("Initializing Hippo plugin");
			
			IQRouter iqRouter = XMPPServer.getInstance().getIQRouter();
			iqRouter.addHandler(new ClientMethodIQHandler());		
			iqRouter.addHandler(new ClientInfoIQHandler());
			iqRouter.addHandler(new MySpaceIQHandler());					
			iqRouter.addHandler(new MusicIQHandler());
			iqRouter.addHandler(new PrefsIQHandler());
			iqRouter.addHandler(new HotnessIQHandler());		
			Log.debug("Adding PresenceMonitor");
			presenceMonitor = new PresenceMonitor();
			SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
			sessionManager.registerListener(presenceMonitor);
					
			roomHandler = new RoomHandler(presenceMonitor);			
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
		
		Log.debug("Shutting down presence monitor");
		presenceMonitor.shutdown();

		Log.debug("... done unloading Hippo plugin");
	}
}
