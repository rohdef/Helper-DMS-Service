package dk.rohdef.alarm.server.events;

import org.asteriskjava.manager.event.UserEvent;

public class RfHmEvent extends UserEvent {
	private static final long serialVersionUID = -6629624929632503002L;
	private String status;
	public RfHmEvent(Object source) {
		super(source);
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}