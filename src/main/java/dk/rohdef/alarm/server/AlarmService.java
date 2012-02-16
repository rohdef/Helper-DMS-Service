package dk.rohdef.alarm.server;

public class AlarmService {
	public boolean fireAlarm(int phoneNumber, String uuid, String signature) {
		
		if (phoneNumber == 21680621 &&  signature.equals(phoneNumber + signature))
			return true;
		else
			return false;
	}
}
