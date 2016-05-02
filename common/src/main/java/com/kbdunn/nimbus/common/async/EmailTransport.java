package com.kbdunn.nimbus.common.async;

import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.exception.OAuthAuthenticationException;
import com.kbdunn.nimbus.common.model.Email;

public interface EmailTransport {
	void send(Email email) throws NimbusException;
	String getEmailAddress() throws OAuthAuthenticationException, NimbusException;
	boolean testConnection() throws OAuthAuthenticationException, NimbusException;
}
