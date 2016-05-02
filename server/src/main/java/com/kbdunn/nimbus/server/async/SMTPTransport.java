package com.kbdunn.nimbus.server.async;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.model.Email;
import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.util.EmailUtil;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.service.LocalUserService;

public class SMTPTransport implements EmailTransport {

	private final SMTPSettings settings;
	private final LocalUserService userService;
	
	public SMTPTransport(SMTPSettings settings) {
		this.settings = settings;
		this.userService = NimbusContext.instance().getUserService();
	}
	
	@Override
	public void send(Email email) throws NimbusException {
		try {
			Session session = Session.getInstance(EmailUtil.getSmtpProperties(settings), EmailUtil.getAuthenticator(settings));
			session.setDebug(NimbusContext.instance().getPropertiesService().isDevMode());
			Transport trans = session.getTransport("smtp");
			trans.connect();
			MimeMessage m = new MimeMessage(session);
			InternetAddress[] to = EmailUtil.getToAddresses(email);
			m.setFrom(email.getFrom().getEmail());
			m.setRecipients(Message.RecipientType.TO, to);
			m.setSubject(email.getSubject());
			m.setContent(email.getBody(), "text/html; charset=UTF-8");
			m.saveChanges();
			trans.sendMessage(m, to);
			trans.close();
		} catch (MessagingException e) {
			throw new NimbusException(e);
		}
	}

	@Override
	public String getEmailAddress() {
		return userService.getUserById(settings.getUserId()).getEmail();
	}

	@Override
	public boolean testConnection() {
		return EmailUtil.checkSMTPSettings(settings);
	}
}
