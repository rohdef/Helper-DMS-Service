package dk.rohdef.alarm.server;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.NewAccountCodeEvent;
import org.asteriskjava.manager.event.NewChannelEvent;
import org.asteriskjava.manager.response.ManagerResponse;

import dk.rohdef.alarm.server.events.RfHmEvent;

class CallerImplementation extends Caller {
	private Logger log = Logger.getLogger(CallerImplementation.class);
	private boolean notAccepted, inCall;
	
	public CallerImplementation(int phone) {
		super(phone);
	}
	
	private List<Helper> findHelpers(int phone) {
		ArrayList<Helper> helpers = new ArrayList<>();
		if (phone != 21680621) return helpers;
		
		// REMOVED from public commit ;)
		
		return helpers;
	}
	
	@Override
	public void run() {
		log.debug("Caller thread '" + getId() +"' started.\n" +
				"Caller is: " + getPhone());
		
		ManagerConnection connection = createManagerConnection();
		try {
			try {
				connection.login();
				log.info("Logged in to asterisk");
			} catch (ConnectException e) {
				// FIXME Do something!! This is extremely critical
				log.fatal("Cannot connect to call server", e);
				throw e;
			}
				
			doCalling(connection);
			log.info("Logging off asterisk");
			connection.logoff();
		} catch (IllegalStateException e) {
			log.fatal("Illegal state: " + getPhone(), e);
		} catch (IOException e) {
			log.fatal("Io error: " + getPhone(), e);
		} catch (AuthenticationFailedException e) {
			log.fatal("Authentication failed: " + getPhone(), e);
		} catch (TimeoutException e) {
			log.fatal("Timed out: " + getPhone(), e);
		}
		
		fireCallingFinishedListeners();
	}

	private ManagerConnection createManagerConnection() {
		Configuration config = null;
		try {
			config = new XMLConfiguration("config.xml");
		} catch (ConfigurationException e) {
			log.fatal("Configuration not found", e);
		}
		String host = config.getString("asterisk.host");
		String username = config.getString("asterisk.username");
		String password = config.getString("asterisk.password");
		
		ManagerConnectionFactory connectionFactory =
				new ManagerConnectionFactory(host, username, password);
		
		log.info("Connecting to asterisk");
		ManagerConnection connection = connectionFactory.createManagerConnection();
		connection.addEventListener(new CallStatusListener());
		
		return connection;
	}

	private void doCalling(ManagerConnection connection)
			throws IOException, TimeoutException {
		OriginateAction action = new OriginateAction();
		action.setContext("DMS-voicemenu"); // Redirect it to the agi!!
		action.setExten("s");
		action.setPriority(1);
		action.setCallerId("+4544400621");
		action.setVariable("userPhone", ""+getPhone());
		
		notAccepted = true;
		List<Helper> helpers = findHelpers(getPhone());
		
		int index = 0;
		while (notAccepted) {
			Helper helper = helpers.get(index%helpers.size());
			index += 1;
			// TODO better solution needed, atm just an infinite loop prevention
			if (index > helpers.size()*5) break; // We've tried 5 times...
			
			action.setChannel("SIP/out/"+helper.getPhoneNumber());
			
			log.info("Calling");
			inCall = true;
			connection.registerUserEventClass(RfHmEvent.class);
			ManagerResponse response = connection.sendAction(action, 30000);
			log.debug("Response is: " + response.getResponse());
			if ("error".equalsIgnoreCase(response.getResponse()))
				inCall = false;
			while (inCall) {
				// Wait for call to end
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// Ignore interupts, we will just sleep again in a moment,
					// unless call is accepted.
				}
				
			}
		}
	}
	
	private class CallStatusListener implements ManagerEventListener {
		private Logger log = Logger.getLogger(CallStatusListener.class);
		@Override
		public void onManagerEvent(ManagerEvent event) {
			if (event instanceof NewChannelEvent)
				onEvent((NewChannelEvent) event);
			else if (event instanceof NewAccountCodeEvent)
				onEvent((NewAccountCodeEvent) event);
			else if (event instanceof HangupEvent) {
				onEvent((HangupEvent) event);
			} else if (event instanceof RfHmEvent) {
				onEvent((RfHmEvent) event);
			} else
				log.debug(event);
		}

		private void onEvent(RfHmEvent event) {
			log.info("Status event recieved");
			if (event.getStatus().equalsIgnoreCase("accepted"))
				notAccepted = false;
		}
		
		private void onEvent(HangupEvent event) {
			log.info("Caller hung up");
			inCall = false;
		}
		
		public void onEvent(NewChannelEvent event) {
			log.debug("New channel event");
			log.debug(event);
		}
		public void onEvent(NewAccountCodeEvent event) {
			log.debug("New account event");
			log.debug(event);
		}
	}
}