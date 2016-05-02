package com.kbdunn.nimbus.common.security;

import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.server.OAuthService;

public interface OAuthAPIService {
	
	public enum Type {
		GOOGLE(true, 
				"64996945795-flkfa0nd0kqi2oh7ejs83psg5qg8jopt.apps.googleusercontent.com", 
				"cs_DemiIwd1B3-URvLimp9z6",
				"https://www.googleapis.com/auth/gmail.compose https://www.googleapis.com/auth/gmail.send"), 
		YAHOO(false,
				"dj0yJmk9allmNzFQdjhoU1R2JmQ9WVdrOVJVcFVPREYwTkdrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD0xNg--",
				"1216a2d263390829ce03bcc20844bd39a7b1a76f",
				null);
		
		private final boolean isOAuth20;
		private final String clientKey;
		private final String clientSecret;
		private final String emailScope;
		
		private Type(boolean isOAuth20, String clientKey, String clientSecret, String emailScope) {
			this.isOAuth20 = isOAuth20;
			this.clientKey = clientKey;
			this.clientSecret = clientSecret;
			this.emailScope = emailScope;
		}
		
		public boolean isOAuth20() {
			return isOAuth20;
		}
		
		public String getClientKey() {
			return clientKey;
		}
		
		public String getClientSecret() {
			return clientSecret;
		}
		
		public String getEmailScope() {
			return emailScope;
		}
	}
	
	Type getType();
	OAuthCredential getOAuthCredential();
	boolean isEmailSupported();
	EmailTransport getEmailTransport();
	OAuthService getOAuthService();
}
