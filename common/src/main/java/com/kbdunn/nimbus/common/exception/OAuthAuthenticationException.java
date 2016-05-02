package com.kbdunn.nimbus.common.exception;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.security.OAuthAPIService;

public class OAuthAuthenticationException extends AuthenticationException {

	private static final long serialVersionUID = -6831295925174943734L;
	
	public OAuthAuthenticationException(NimbusUser user, OAuthAPIService.Type serviceType) {
		super(user.getName() + " does not have a valid OAuth access token for " + serviceType.toString());
	}
	
	public OAuthAuthenticationException(Long userId, OAuthAPIService.Type serviceType) {
		super("User ID " + userId + " does not have a valid OAuth access token for " + serviceType.toString());
	}
}
