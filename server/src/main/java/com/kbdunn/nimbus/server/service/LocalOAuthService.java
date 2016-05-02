package com.kbdunn.nimbus.server.service;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.security.OAuth20API;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.common.security.OAuthAPIService.Type;
import com.kbdunn.nimbus.common.server.OAuthService;
import com.kbdunn.nimbus.common.util.OAuthUtil;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.dao.OAuthDAO;
import com.kbdunn.nimbus.server.google.GoogleAPIService;

public class LocalOAuthService implements OAuthService {
	
	private static final Logger log = LogManager.getLogger(LocalOAuthService.class);

	private LocalUserService userService;
	
	public LocalOAuthService() {  }
	
	public void initialize(NimbusContext container) {
		userService = container.getUserService();
	}
	
	@Override
	public OAuthCredential getOAuthCredential(NimbusUser user, OAuthAPIService.Type type) {
		if (user == null || user.getId() == null) throw new IllegalArgumentException("User ID cannot be null");
		if (type == null) throw new IllegalArgumentException("OAuthCredential Type cannot be null");
		return OAuthDAO.getCredential(user.getId(), type.toString());
	}

	@Override
	public List<OAuthCredential> getOAuthCredentials(NimbusUser user) {
		if (user == null || user.getId() == null) throw new IllegalArgumentException("User ID cannot be null");
		return OAuthDAO.getCredentials(user.getId());
	}

	@Override
	public OAuthAPIService getOAuthAPIService(NimbusUser user, Type type) {
		if (user == null || user.getId() == null) throw new IllegalArgumentException("User ID cannot be null");
		if (type == null) throw new IllegalArgumentException("OAuthCredential Type cannot be null");
		final OAuthCredential cred = getOAuthCredential(user, type);
		if (cred == null) return null;
		if (type == Type.GOOGLE) {
			return new GoogleAPIService(this, cred);
		}
		throw new UnsupportedOperationException("OAuth API " + type + " is not supported");
	}

	@Override
	public boolean save(OAuthCredential credential) {
		if (credential == null) throw new IllegalArgumentException("OAuthCredential cannot be null");
		if (credential.getUserId() == null) throw new IllegalArgumentException("User ID cannot be null");
		if (credential.getServiceName() == null) throw new IllegalArgumentException("OAuth service type cannot be null");
		final OAuthCredential dbr = OAuthDAO.getCredential(credential.getUserId(), credential.getServiceName().toString());
		if (dbr != null) {
			credential.setId(dbr.getId());
		}
		if (credential.getId() == null) {
			if (credential.getServiceName().isOAuth20() && credential.getLastRefresh() == null) {
				credential.setLastRefresh(System.currentTimeMillis());
			}
			return OAuthDAO.insert(credential);
		}
		return OAuthDAO.update(credential);
	}

	public boolean delete(OAuthCredential credential) {
		if (credential == null || credential.getId() == null) return true;
		boolean success = OAuthDAO.delete(credential.getId());
		if (success) {
			final NimbusUser user = userService.getUserById(credential.getUserId());
			if (user.getEmailServiceName() == credential.getServiceName()) {
				user.setEmailServiceName(null);
				try {
					return userService.save(user);
				} catch (UsernameConflictException | EmailConflictException | FileConflictException e) {
					log.error(e, e);
				}
			}
		}
		
		return success;
	}
	
	@Override
	public boolean delete(NimbusUser user, OAuthAPIService.Type type) {
		if (user == null || user.getId() == null) throw new IllegalArgumentException("User ID cannot be null");
		if (type == null) throw new IllegalArgumentException("OAuth Type cannot be null");
		return OAuthDAO.delete(user.getId(), type.toString());
	}

	@Override
	public OAuth2AccessToken refreshAccessToken(OAuth20API api) {
		if (api == null) throw new IllegalArgumentException("API cannot be null");
		if (api.getOAuthCredential() == null || api.getOAuthCredential().getRefreshToken() == null) 
			throw new IllegalArgumentException("OAuth refresh token cannot be null");
		
		final com.github.scribejava.core.oauth.OAuthService scribeService = OAuthUtil.getScribeOAuthService(api);
		final OAuth2AccessToken token = ((OAuth20Service) scribeService)
				.refreshAccessToken(api.getOAuthCredential().getRefreshToken());
		final OAuthCredential cred = api.getOAuthCredential();
		cred.setAccessToken(token.getAccessToken());
		cred.setExpiresIn(token.getExpiresIn());
		cred.setLastRefresh(System.currentTimeMillis());
		cred.setTokenType(token.getTokenType());
		cred.setScope(token.getScope());
		save(cred);
		return token;
	}
}
