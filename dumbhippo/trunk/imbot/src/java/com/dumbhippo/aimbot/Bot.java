package com.dumbhippo.aimbot;

import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.aim.Buddy;
import com.dumbhippo.aim.Client;
import com.dumbhippo.aim.FilterException;
import com.dumbhippo.aim.Listener;
import com.dumbhippo.aim.RawListenerAdapter;
import com.dumbhippo.aim.ScreenName;
import com.dumbhippo.aim.TocError;
import com.dumbhippo.botcom.BotEvent;
import com.dumbhippo.botcom.BotEventToken;
import com.dumbhippo.botcom.BotTaskFailedException;
import com.dumbhippo.botcom.BotTaskMessage;
import com.dumbhippo.identity20.RandomToken;

class Bot implements Runnable {
	private static final Log logger = GlobalSetup.getLog(Bot.class);

	private static Timer timer;
	
	private static final String tokenRegex = "[a-f0-9]{" + RandomToken.STRING_LENGTH + "}";
	private static final Pattern tokenPattern = Pattern.compile(tokenRegex);
	
	private ScreenName name;
	private String pass;
	
	private Client aim;
	private Random random;
	private SelfPinger pinger;
	
	private Lock quitLock;
	private Condition quitCondition;
	private boolean quit;
	
	private Lock onlineLock;
	private Condition onlineCondition;
	
	private Set<BotListener> listeners;
	
	class SelfPinger extends TimerTask {
	    // check connection every "TIME_DELAY" milliseconds (5 mins)
	    private static final long TIME_DELAY = 5 * 60 * 1000;
	    private static final String PING = "PING";
	    
	    SelfPinger() {
	    	timer.schedule(this, TIME_DELAY, TIME_DELAY);
	    	
	    	aim.addRawListener(new RawListenerAdapter() {
	    		public void handleMessage(ScreenName buddy, String htmlMessage)
				throws FilterException {
	    			if (buddy.equals(aim.getName()) && htmlMessage.equals(PING)) {
	    				logger.debug("filtering out " + PING);
	    				throw new FilterException();
	    			}
	    		}
	    	});
	    }
	    
		@Override
		public void run() {
			if (aim != null && aim.getOnline()) {
				// clock setting back just works since we do a 
				// ping then which fixes last message timestamp
				
				long now = System.currentTimeMillis();
				long last = aim.getLastMessageTimestamp();

				// only ping if we've been idle
				if ((now - last) > TIME_DELAY) {
					
					logger.debug("Self-pinging at " + new Date(now));
					
					aim.sendMessageRaw(aim.getName(), PING);
					
					// set up a one-shot to verify results
					timer.schedule(new Ponger(), TIME_DELAY);
				}
            } else {
            	cancel();
            }
		}
		
		class Ponger extends TimerTask {

			@Override
			public void run() {
				// if we've now been idle for a while, sign us off, 
				// the connection is hosed
				if (aim != null && aim.getOnline()) {
					long now = System.currentTimeMillis();
					long last = aim.getLastMessageTimestamp();

					logger.debug("Ping check, last message at " + new Date(last));
					
					if ((now - last) > TIME_DELAY*2) {
						logger.debug("Last message too old, signing off");
						aim.signOff();
					}
				}
			}
		}
	}
	
	class ClientListener implements Listener {
		public void handleConnected() {
			logger.info(name + " connected");
		}
		
		public void handleDisconnected() {
			logger.info(name + " disconnected");
		}
		
		public void handleMessage(Buddy buddy, String messageHtml) {
			logger.info(name + " message from " + buddy.getName() + ": " + messageHtml);
			
			Client client = aim;
			if (client == null)
				return;		
			
			Matcher m = tokenPattern.matcher(messageHtml);
			if (m.find()) {
				sayBusyThinking(buddy);
				sendEvent(new BotEventToken(name.getNormalized(), buddy.getName().getNormalized(), 
						m.group()));
			} else {
				saySomethingRandom(buddy);
			}
		}
		
		public void handleWarning(Buddy buddy, int amount) {
			logger.info(name + " warning from " + buddy.getName());
	
			Client client = aim;
			if (client == null)
				return;
			logger.debug("retaliating by warning back");
			client.sendWarning(buddy);
		}
		
		public void handleBuddySignOn(Buddy buddy, String info) {
			logger.debug(name + " Buddy sign on " + buddy.getName());
		}
		
		public void handleBuddySignOff(Buddy buddy, String info) {
			logger.debug(name + " Buddy sign off " + buddy.getName());
		}
		
		public void handleError(TocError error, String message) {
			logger.warn(name + " error: " + message);
		}
		
		public void handleBuddyUnavailable(Buddy buddy, String message) {
			logger.debug(name + " buddy unavailable: " + buddy.getName() + " message: " + message);
		}
		
		public void handleBuddyAvailable(Buddy buddy, String message) {
			logger.debug(name + " buddy available: " + buddy.getName() + " message: " + message);
		}
	}
		
	Bot(ScreenName name, String pass) {
		// don't put things in constructor that need to be recreated 
		// for each run()
		
		synchronized(Bot.class) {
			if (timer == null)
				timer = new Timer(true); // daemon thread
		}
		
		random = new Random();
		
		this.name = name;
		this.pass = pass;
		
		quitLock = new ReentrantLock();
		quitCondition = quitLock.newCondition();
		onlineLock = new ReentrantLock();
		onlineCondition = onlineLock.newCondition();
		
		this.listeners = new HashSet<BotListener>();
	}

	private void saySomethingRandom(Buddy buddy) {
		Client client = aim;
		logger.debug("saying something random to " + buddy.getName());
		if (client == null)
			return;
		switch (random.nextInt(5)) {
		case 0:
			client.sendMessage(buddy, "You suck");
			break;
		case 1:
			client.sendMessage(buddy, "Blah blah blah blah blah blah blah");
			break;
		case 2:
			client.sendMessage(buddy, "Hippo Hippo Hooray");
			break;
		case 3:
			client.sendMessage(buddy, "Do I repeat myself often?");
			break;
		case 4:
			client.sendMessage(buddy, "I may be dumb, but I'm not stupid");
			break;
		}
	}

	private void sayBusyThinking(Buddy buddy) {
		Client client = aim;
		logger.debug("saying we're thinking to " + buddy.getName());
		if (client == null)
			return;
		switch (random.nextInt(6)) {
		case 0:
			client.sendMessage(buddy, "Crunch crunch crunch");
			break;
		case 1:
			client.sendMessage(buddy, "Hmmmm...");
			break;
		case 2:
			client.sendMessage(buddy, "I'll get back to you on that in a minute");
			break;
		case 3:
			client.sendMessage(buddy, "........");
			break;
		case 4:
			client.sendMessage(buddy, "Your feeble commands insult my infinite bot mind");
			break;
		case 5:
			client.sendMessage(buddy, "Got it! Right on it!");
			break;
		}
	}
	
	private void signOn() {
		if (aim != null)
			throw new IllegalStateException("can't sign on when you're already running");

		logger.debug("Bot signing on...");
		
		/*
		if (false || true)
			return; // simulate signon failure
		*/
		/*
		if ((new Random()).nextBoolean()) {
			logger.debug("Randomly generated signon failure!");
			return;
		}
		*/
		
		Client client = new Client(name, pass, "I am DUMB HIPPO BOT",
				"Hmm, who are you?", true /*auto-add everyone as buddy*/);
		client.addListener(new ClientListener());
		
		client.signOn();
		
		if (client.getOnline()) {
			aim = client;
		} else {
			logger.error("Bot failed to sign on");
		}
	}

	private void signOff() {
		if (aim != null) {
			aim.signOff();
		}
	}
	
	private void signalOnlineMaybeChanged() {
		onlineLock.lock();
		try {
			onlineCondition.signalAll();
		} finally {
			onlineLock.unlock();
		}
	}
	
	public void waitForOnlineMaybeChanged() {
		onlineLock.lock();
		try {
			onlineCondition.await();
		} catch (InterruptedException e) {
			// nothing to do, it's fine if online didn't really change
		} finally {
			onlineLock.unlock();
		}
	}
	
	private void setQuit(boolean value) {
		quitLock.lock();
		try {
			if (quit != value) {
				quit = value;
				quitCondition.notifyAll();
			}
		} finally {
			quitLock.unlock();
		}
	}
	
	public void run() {
		final long FAILED_IF_LESS_THAN_MS = 15000;
		final int MAX_FAILURES = 11;
				
		int failures = 0;
		
		setQuit(false);
		
		while (true) {
			quitLock.lock();
			try {
				if (quit) {
					logger.debug("Bot quitting");
					// in case someone is waiting on this (e.g. if we never connected even once, we won't have called it yet)
					signalOnlineMaybeChanged();
					return;
				}
				
				if (failures > 0) {
					// don't go overboard
					if (failures > MAX_FAILURES) {
						failures = MAX_FAILURES;
					}
					// 2 seconds, 4 seconds, 8 seconds, ... 2048 seconds for MAX_FAILURES=11   
					long pauseSeconds = (long) Math.pow(2, failures);
					try {
						logger.debug("Waiting " + pauseSeconds + " seconds (" + pauseSeconds/60 + ") minutes to reconnect");
						quitCondition.await(pauseSeconds, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
					}
				}
				
			} finally {
				quitLock.unlock();
			}
					
			if (aim == null)
				signOn();
			
			if (aim != null) {
				long lastSignOn = 0;
				long lastSignOff = 0;
				
				pinger = new SelfPinger();
				
				logger.debug("New connection OK, waiting for events...");
				lastSignOn = System.currentTimeMillis();
				signalOnlineMaybeChanged(); // wake up people who want to see we're online
				
				aim.read();
				
				logger.debug("Bot connection closed");
				
				aim = null;
				
				pinger.cancel();
				pinger = null;
				
				lastSignOff = System.currentTimeMillis();
				
				if ((lastSignOff - lastSignOn) < FAILED_IF_LESS_THAN_MS) {
					logger.debug("We were signed on less than " + FAILED_IF_LESS_THAN_MS/1000 + " seconds, counting as failure to connect");
					++failures;
				} else {
					// reset the back-off
					failures = 0;
				}
				
				signalOnlineMaybeChanged(); // wake up people who want to see we're offline
			} else {
				++failures;
			}
		}
	}
	
	public void quit() {		
		setQuit(true);
		
		signOff();
	}
	
	/**
	 * Sees if we're currently authenticated. Note that this changes 
	 * over time as we'll disconnect and reconnect.
	 * @return true if we currently signed on
	 */
	public boolean getOnline() {
		return aim != null && aim.getOnline();
	}
	
	// Normally called from a separate thread
	public void doMessage(BotTaskMessage message) throws BotTaskFailedException {
		
		logger.debug("Bot " + name + " got message task for " + message.getRecipient());
		
		// we save a reference in case it gets set to null by the main thread
		Client client = aim;
		
		if (client == null) {
			throw new BotTaskFailedException("bot is not running");
		}
		
		ScreenName recipientName = new ScreenName(message.getRecipient());
		Buddy recipient = client.addBuddy(recipientName);
				
		client.sendMessage(recipient, message.getHtmlMessage());
		
		if (!client.getOnline()) {
			// most likely this means we failed (there's really no way to know reliably)
			throw new BotTaskFailedException("offline right when we sent the message");
		}
	}
	
	public void addListener(BotListener listener) {
		this.listeners.add(listener);
	}
	
	public void removeListener(BotListener listener) {
		this.listeners.remove(listener);
	}
	
	private void sendEvent(BotEvent event) {
		for (BotListener l : listeners) {
			try {
				l.onEvent(event);
			} catch (Exception e) {
				logger.warn("Bot listener exception", e);
			}
		}
	}
}
