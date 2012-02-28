package dk.rohdef.alarm.server;

import java.util.HashMap;

import org.apache.log4j.Logger;

import dk.rohdef.alarm.server.events.CallingFinishedListener;

public class CallerFactory {
	private static Logger log = Logger.getLogger(CallerFactory.class);
	public static void main(String[] args) {
		Caller caller = CallerFactory.getCallerFor(21680621);
		caller.start();
	}
	
	private static HashMap<Integer, CallerImplementation> callers = new HashMap<>();
	
	private CallerFactory() {
	}
	
	public static synchronized Caller getCallerFor(int phone) {
		if (callers.containsKey(phone)) {
			log.debug("Caller exists, returning");
			return callers.get(phone);
		}
		
		log.debug("Creating new caller");
		CallerImplementation caller = new CallerImplementation(phone);
		caller.addCallingFinishedListener(new RemoveCallerWhenFinishedListener(phone));
		callers.put(phone, caller);
		
		return caller;
	}
	
	private static class RemoveCallerWhenFinishedListener implements CallingFinishedListener {
		private int key;
		
		public RemoveCallerWhenFinishedListener(int key) {
			this.key = key;
		}

		@Override
		public void onFinished() {
			log.debug("Call ended, removing caller");
			callers.remove(key);
		}
	}
}
