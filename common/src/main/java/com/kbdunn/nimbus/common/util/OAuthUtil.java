package com.kbdunn.nimbus.common.util;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;
import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.security.OAuth10aAPI;
import com.kbdunn.nimbus.common.security.OAuth20API;
import com.kbdunn.nimbus.common.security.OAuthAPIService;

public final class OAuthUtil {
	
	public static OAuth1AccessToken toOAuth1AccessToken(OAuthCredential credential) {
		return new OAuth1AccessToken(credential.getOAuth1PublicToken(), credential.getAccessToken());
	}
	
	public static OAuth2AccessToken toOAuth2AccessToken(OAuthCredential credential) {
		return new OAuth2AccessToken(credential.getAccessToken(), 
				credential.getTokenType(), 
				credential.getExpiresIn(), 
				credential.getRefreshToken(), 
				credential.getScope(), 
				null
			);
	}
	
	public static OAuth2AccessToken refreshAccessToken(OAuth20API service) {
		final OAuthService scribeService = getScribeOAuthService(service);
		return ((OAuth20Service) scribeService).refreshAccessToken(service.getOAuthCredential().getRefreshToken());
	}
	
	public static OAuthService getScribeOAuthService(OAuthAPIService service) {
		final ServiceBuilder sb =  new ServiceBuilder()
				.apiKey(service.getType().getClientKey())
				.apiSecret(service.getType().getClientSecret());
		OAuthService scribe = null;
		if (service instanceof OAuth20API) {
			scribe = sb.build(((OAuth20API) service).getScribeApi());
		} else {
			scribe = sb.build(((OAuth10aAPI) service).getScribeApi());
		}
		return scribe;
	}
	
	public static OAuthRequest signOAuthRequest(OAuthRequest request, OAuthAPIService service) {
		final OAuthService scribeService = getScribeOAuthService(service);
		Token accessToken = null;
		if (service instanceof OAuth20API) {
			accessToken = toOAuth2AccessToken(service.getOAuthCredential());
			((OAuth20Service) scribeService).signRequest((OAuth2AccessToken) accessToken, request);
		} else {
			accessToken = toOAuth1AccessToken(service.getOAuthCredential());
			((OAuth10aService) scribeService).signRequest((OAuth1AccessToken) accessToken, request);
		}
		return request;
	}
}
