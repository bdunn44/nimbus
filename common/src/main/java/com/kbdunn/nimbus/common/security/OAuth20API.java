package com.kbdunn.nimbus.common.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.kbdunn.nimbus.common.model.OAuthCredential;

public abstract class OAuth20API implements OAuthAPIService {
	
	abstract public DefaultApi20 getScribeApi();
	
	protected final void refreshAccessTokenIfNeeded() throws IOException {
		final OAuthCredential cred = getOAuthCredential();
		if (cred.getLastRefresh() + TimeUnit.SECONDS.toMillis(cred.getExpiresIn()) < System.currentTimeMillis()) {
			getOAuthService().refreshAccessToken(this);
		}
	}
}
