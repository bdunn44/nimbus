package com.kbdunn.nimbus.common.util;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.kbdunn.nimbus.common.model.Email;
import com.kbdunn.nimbus.common.model.SMTPSettings;

public abstract class EmailUtil {

	public static boolean checkSMTPSettings(SMTPSettings settings) {
		try {
			Session session = Session.getInstance(getSmtpProperties(settings), getAuthenticator(settings));
			Transport trans = session.getTransport("smtp");
			trans.connect();
			trans.close();
		} catch (MessagingException e) {
			return false;
		} 
		return true;
	}
	
	public static Properties getSmtpProperties(SMTPSettings settings) {
		Properties props = new Properties();
		
		props.put("mail.smtp.host", settings.getSmtpServer());
		props.put("mail.smtp.port", settings.getSmtpPort());
		if (settings.isSslEnabled()) {
			props.put("mail.smtp.socketFactory.port", settings.getSslPort());
			props.put("mail.smtp.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.starttls.enable", "true");
		}
		props.put("mail.smtp.auth", "true");
		//props.put("mail.debug", "true");
		//props.put("mail.user", settings.getUsername());
		//props.put("mail.password", settings.getPassword());
		
		return props;
	}
	
	public static javax.mail.Authenticator getAuthenticator(SMTPSettings settings) {
		return new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(settings.getUsername(), settings.getPassword());
				}
			};
	}
	
	public static InternetAddress[] getToAddresses(Email email) throws AddressException {
		if (email.getRecipients() == null) return new InternetAddress[0];
		InternetAddress[] result = new InternetAddress[email.getRecipients().size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = new InternetAddress(email.getRecipients().get(i));
		}
		return result;
	}
}
