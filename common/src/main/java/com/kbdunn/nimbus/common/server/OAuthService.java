package com.kbdunn.nimbus.common.server;

import java.util.List;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.security.OAuth20API;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.common.security.OAuthAPIService.Type;

public interface OAuthService {
	OAuthCredential getOAuthCredential(NimbusUser user, OAuthAPIService.Type type);
	List<OAuthCredential> getOAuthCredentials(NimbusUser user);
	OAuthAPIService getOAuthAPIService(NimbusUser user, OAuthAPIService.Type type);
	OAuth2AccessToken refreshAccessToken(OAuth20API api);
	boolean save(OAuthCredential credential);
	boolean delete(OAuthCredential credential);
	boolean delete(NimbusUser user, Type type);
}
