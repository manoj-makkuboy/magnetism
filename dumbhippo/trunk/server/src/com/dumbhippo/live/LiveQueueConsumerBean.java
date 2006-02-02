package com.dumbhippo.live;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.SessionContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;

import com.dumbhippo.ExceptionUtils;
import com.dumbhippo.GlobalSetup;

//
// Handles taking events queued via LiveState.queueUpdate and dispatching
// them to the appropriate "processor bean"
//

@MessageDriven(activateConfig =
{
  @ActivationConfigProperty(propertyName="destinationType",
    propertyValue="javax.jms.Queue"),
  @ActivationConfigProperty(propertyName="destination",
    propertyValue="queue/" + LiveEvent.QUEUE)
})
public class LiveQueueConsumerBean implements MessageListener {
	
	@Resource SessionContext context;

	static private final Logger logger = GlobalSetup.getLogger(LiveQueueConsumerBean.class);
	
	private void process(LiveEvent event) {
		// To find the right "processor bean" for this event, we have the 
		// processor beans register themselves in JDNI under their class name
		// using the JBoss @LocalBinding EJB3-annotation extension. There
		// is no standard way of finding a bean at runtime.
		
		Class<? extends LiveEventProcessor> clazz = event.getProcessorClass();
		LiveEventProcessor processor = (LiveEventProcessor)context.lookup(clazz.getCanonicalName());
		if (processor == null) {
			logger.warn("Could not lookup event processor bean " + clazz.getCanonicalName());
			return;
		}
		
		processor.process(LiveState.getInstance(), event);
	}

	public void onMessage(Message message) {
		try {
			logger.debug("Got message from " + LiveEvent.QUEUE + ": " + message);
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Object obj = objectMessage.getObject();
				
				logger.debug("Got object in " + LiveEvent.QUEUE + ": " + obj);
				
				if (obj instanceof LiveEvent) {
					process((LiveEvent) obj);
				} else {
					logger.warn("Got unknown object: " + obj);
				}
			} else {
				logger.warn("Got unknown JMS message: " + message);
			}
		} catch (JMSException e) {
			logger.warn("JMS exception", e);
		} catch (Exception e) {
			logger.warn("Exception processing JMS message: " + ExceptionUtils.getRootCause(e).getMessage(), e);
		}
	}
}
