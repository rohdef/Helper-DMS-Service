package dk.rohdef.alarm.server;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.response.ManagerResponse;

public class AlarmService {
	private Logger log = Logger.getLogger(AlarmService.class);

	public boolean fireAlarm(int phoneNumber, String uuid, byte[] signature) {
		if (checkSignature(phoneNumber, uuid, signature)) {
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
			
			ManagerConnection connection = connectionFactory.createManagerConnection();
			OriginateAction action = new OriginateAction();
			action.setChannel("SIP/rulle-mob");
			action.setContext("voicemenu-custom-1");
			action.setExten("s");
			action.setPriority(1);
			action.setCallerId("Heeeeelp meeee");
			
			try {
				connection.login();
				ManagerResponse response = connection.sendAction(action, 30000);
				log.debug(response.getResponse());
				connection.logoff();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (AuthenticationFailedException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
			
			return true;
		} else {
			return false;
		}
	}

	private boolean checkSignature(int phoneNumber, String uuid, byte[] signature) {
		byte[] signatureBytes = null;
		try {
			signatureBytes = (phoneNumber + uuid).getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			log.fatal("Encoding", e);
		}
		byte[] cipherData = getCipherData(signature);
		
		return Arrays.equals(signatureBytes, cipherData);
	}
	
	private byte[] getCipherData(byte[] signature) {
		PublicKey key = readPublicKeyFromFile("21680621_public.der");
		byte[] cipherData = null;
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			cipherData = cipher.doFinal(signature);
		} catch (NoSuchAlgorithmException e) {
			log.fatal("Algorithm error", e);
		} catch (NoSuchPaddingException e) {
			log.fatal("Padding problem", e);
		} catch (InvalidKeyException e) {
			log.fatal("Invalid key", e);
		} catch (IllegalBlockSizeException e) {
			log.fatal("Block size is wrong", e);
		} catch (BadPaddingException e) {
			log.fatal("Padding problem", e);
		}
		
		return cipherData;
	}

	private PublicKey readPublicKeyFromFile(String path) {
		PublicKey publicKey = null;
		try {
			URL cert = new URL("http://localhost/"+path);
			URLConnection connection = cert.openConnection();
			DataInputStream input = new DataInputStream(
					connection.getInputStream());
			
			byte[] buffer = new byte[1024];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while(true) {
				int n = input.read(buffer);
				if (n<0) break;
				baos.write(buffer, 0, n);
			}
			
//			byte[] keyBytes = key.getBytes();
			
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(baos.toByteArray());
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			publicKey = keyFactory.generatePublic(keySpec);
		} catch (NoSuchAlgorithmException e) {
			log.fatal("Algorith not found", e);
		} catch (InvalidKeySpecException e) {
			log.fatal("Invalid key spec", e);
		} catch (MalformedURLException e) {
			log.fatal("URL malformed", e);
		} catch (IOException e) {
			log.fatal("IO problem", e);
		}
		
		
		return publicKey;
	}
}
