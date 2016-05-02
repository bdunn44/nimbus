package com.kbdunn.nimbus.common.security;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.kbdunn.nimbus.common.model.OAuthCredential;

public abstract class OAuth20API implements OAuthAPIService {
	
	abstract public DefaultApi20 getScribeApi();
	
	protected final void refreshAccessTokenIfNeeded() {
		final OAuthCredential cred = getOAuthCredential();
		if (cred.getLastRefresh() + cred.getExpiresIn() > System.currentTimeMillis()) {
			getOAuthService().refreshAccessToken(this);
		}
	}
}
