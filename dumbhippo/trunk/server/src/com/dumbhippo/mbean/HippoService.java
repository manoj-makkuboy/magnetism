package com.dumbhippo.mbean;

import org.jboss.system.ServiceMBeanSupport;
import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.live.LiveState;

// The point of this extremely simple MBean is to get notification
// when our application is loaded and unloaded; in particular, we
// need to clean up some resources on unleoad. ServiceMBeanSupport
// has skeleton implementations of the ServiceMBean methods;
// we just need to provide the code to run on start and shutdown.
public class HippoService extends ServiceMBeanSupport implements HippoServiceMBean {
	private static final Logger logger = GlobalSetup.getLogger(HippoService.class);
	
	@Override
	protected void startService() {
		logger.info("Starting HippoService MBean");
    }
	
    @Override
	protected void stopService() {
		logger.info("Stopping HippoService MBean");
		LiveState.getInstance().shutdown();
   }
}
