package com.kbdunn.nimbus.server.google;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.model.Message;
import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.exception.OAuthAuthenticationException;
import com.kbdunn.nimbus.common.model.Email;
import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.security.OAuth20API;
import com.kbdunn.nimbus.common.server.OAuthService;
import com.kbdunn.nimbus.common.util.EmailUtil;
import com.kbdunn.nimbus.common.util.OAuthUtil;

public class GoogleAPIService extends OAuth20API implements EmailTransport {

    private static final Logger log = LogManager.getLogger(GoogleAPIService.class);
    private static final ObjectMapper JACKSON = new ObjectMapper();
    private static final String GMAIL_SEND_ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/messages/send";
    private static final String GMAIL_PROFILE_ENDPOINT = "https://www.googleapis.com/gmail/v1/users/me/profile";
    
    private final OAuthService localOAuthService;
    private final OAuthCredential credential;
    
    public GoogleAPIService(OAuthService localOAuthService, OAuthCredential credential) {
    	this.localOAuthService = localOAuthService;
    	this.credential = credential;
    }

	@Override
	public Type getType() {
		return Type.GOOGLE;
	}
    
	@Override
	public GoogleApi20 getScribeApi() {
		return GoogleApi20.instance();
	}

	@Override
	public OAuthCredential getOAuthCredential() {
		return credential;
	}

	@Override
	public OAuthService getOAuthService() {
		return localOAuthService;
	}
	
	@Override
	public boolean isEmailSupported() {
		return true;
	}

	@Override
	public EmailTransport getEmailTransport() {
		return this;
	}

	@Override
	public void send(Email email) throws NimbusException {
		super.refreshAccessTokenIfNeeded();
		
		// Build a new authorized API client service.
        log.info("Sending Gmail email");
        Message message;
        try {
	        message = createMessageWithEmail(email);
        } catch (IOException|MessagingException e) {
        	throw new NimbusException(e);
        }
        final OAuth20Service service = (OAuth20Service) OAuthUtil.getScribeOAuthService(this);
        final OAuthRequest request = new OAuthRequest(Verb.POST, GMAIL_SEND_ENDPOINT, service);
        request.addHeader("Content-Type", "application/json");
        try {
			request.addPayload(JACKSON.writeValueAsString(message));
		} catch (JsonProcessingException e) {
			throw new NimbusException(e);
		}
        OAuthUtil.signOAuthRequest(request, this);
        final Response response = request.send();
        if (Math.floorDiv(response.getCode(), 100) != 2) {
        	log.debug("send() recieved HTTP " + response.getCode() + ". Raw response: \n" + response.getBody());
        	throw new NimbusException("Error sending email through Gmail!");
        }
        log.info("Sent email. Response code " + response.getCode());
	}
    
    public Message createMessageWithEmail(Email email) throws MessagingException, IOException {
    	Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		
		MimeMessage mimeMessage = new MimeMessage(session);
		
		mimeMessage.setFrom(new InternetAddress(email.getFrom().getEmail()));
		mimeMessage.setRecipients(javax.mail.Message.RecipientType.TO, EmailUtil.getToAddresses(email));
		mimeMessage.setSubject(email.getSubject());
		mimeMessage.setContent(email.getBody(), "text/html; charset=UTF-8");
		
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		mimeMessage.writeTo(bytes);
		String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
		Message message = new Message();
		message.setRaw(encodedEmail);
		return message;
	}

	@Override
	public String getEmailAddress() throws NimbusException {
		super.refreshAccessTokenIfNeeded();
		
        final OAuth20Service service = (OAuth20Service) OAuthUtil.getScribeOAuthService(this);
        final OAuthRequest request = new OAuthRequest(Verb.GET, GMAIL_PROFILE_ENDPOINT, service);
        OAuthUtil.signOAuthRequest(request, this);
        final Response response = request.send();
        if (Math.floorDiv(response.getCode(), 100) != 2) {
        	log.debug("getEmailAddress() recieved HTTP " + response.getCode() + ". Raw response: \n" + response.getBody());
        	throw new OAuthAuthenticationException(credential.getUserId(), credential.getServiceName());
        }
        try {
			return JACKSON.readTree(response.getBody()).get("emailAddress").textValue();
		} catch (IOException e) {
			throw new NimbusException(e);
		}
	}

	@Override
	public boolean testConnection() throws NimbusException {
		return getEmailAddress() != null;
	}
}
