package dk.rohdef.alarm.server;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import dk.rohdef.alarm.server.events.CallingFinishedListener;

public abstract class Caller extends Thread {
	private Logger log = Logger.getLogger(Caller.class);
	private int phone;
	private ArrayList<CallingFinishedListener> callingFinishedListeners;
	
	public Caller(int phone) {
		this.setPhone(phone);
		callingFinishedListeners = new ArrayList<>();
		setName("Caller-"+phone);
	}

	public int getPhone() {
		return phone;
	}

	public void setPhone(int phone) {
		this.phone = phone;
	}
	
	public void addCallingFinishedListener(CallingFinishedListener listener) {
		callingFinishedListeners.add(listener);
	}
	
	public void fireCallingFinishedListeners() {
		log.debug("Calling finished listeners");
		for (CallingFinishedListener listener : callingFinishedListeners)
			listener.onFinished();
	}
}
