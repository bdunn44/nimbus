package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Token;
import com.kbdunn.nimbus.common.security.OAuthAPIService;

public class OAuthCredential implements NimbusRecord {
	
	private Long id;
	private Long userId;
	private OAuthAPIService.Type service;
	private String accessToken;
	private String refreshToken;
	private Integer expiresIn;
	private Long lastRefresh;
	private String tokenType;
	private String oAuth1PublicToken;
	private String scope;
	private Date created;
	private Date updated;

	public OAuthCredential(Long id, Long userId, OAuthAPIService.Type service, String accessToken, String refreshToken, Integer expiresIn, Long lastRefresh,
			String tokenType, String oAuth1PublicToken, String scope, Date created, Date updated) {
		super();
		this.id = id;
		this.userId = userId;
		this.service = service;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.expiresIn = expiresIn;
		this.lastRefresh = lastRefresh;
		this.tokenType = tokenType;
		this.setOAuth1PublicToken(oAuth1PublicToken);
		this.setScope(scope);
		this.created = created;
		this.updated = updated;
	}

	public OAuthCredential(NimbusUser user, Token token, OAuthAPIService.Type type) {
		this.userId = user.getId();
		this.service = type;
		if (token instanceof OAuth1AccessToken) {
			this.oAuth1PublicToken = ((OAuth1AccessToken) token).getToken();
			this.accessToken = ((OAuth1AccessToken) token).getTokenSecret();
		} else if (token instanceof OAuth2AccessToken) {
			this.accessToken = ((OAuth2AccessToken) token).getAccessToken();
			this.refreshToken = ((OAuth2AccessToken) token).getRefreshToken();
			this.scope = ((OAuth2AccessToken) token).getScope();
			this.tokenType = ((OAuth2AccessToken) token).getTokenType();
			this.expiresIn = ((OAuth2AccessToken) token).getExpiresIn();
		}
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public OAuthAPIService.Type getServiceName() {
		return service;
	}

	public void setServiceName(OAuthAPIService.Type serviceName) {
		this.service = serviceName;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

	public Long getLastRefresh() {
		return lastRefresh;
	}

	public void setLastRefresh(Long lastRefresh) {
		this.lastRefresh = lastRefresh;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getOAuth1PublicToken() {
		return oAuth1PublicToken;
	}

	public void setOAuth1PublicToken(String oAuth1PublicToken) {
		this.oAuth1PublicToken = oAuth1PublicToken;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public void setCreated(Date createDate) {
		this.created = createDate;
	}

	@Override
	public Date getUpdated() {
		return updated;
	}

	@Override
	public void setUpdated(Date lastUpdateDate) {
		this.updated = lastUpdateDate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessToken == null) ? 0 : accessToken.hashCode());
		result = prime * result + ((expiresIn == null) ? 0 : expiresIn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((lastRefresh == null) ? 0 : lastRefresh.hashCode());
		result = prime * result + ((oAuth1PublicToken == null) ? 0 : oAuth1PublicToken.hashCode());
		result = prime * result + ((refreshToken == null) ? 0 : refreshToken.hashCode());
		result = prime * result + ((scope == null) ? 0 : scope.hashCode());
		result = prime * result + ((service == null) ? 0 : service.hashCode());
		result = prime * result + ((tokenType == null) ? 0 : tokenType.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof OAuthCredential))
			return false;
		OAuthCredential other = (OAuthCredential) obj;
		if (accessToken == null) {
			if (other.accessToken != null)
				return false;
		} else if (!accessToken.equals(other.accessToken))
			return false;
		if (expiresIn == null) {
			if (other.expiresIn != null)
				return false;
		} else if (!expiresIn.equals(other.expiresIn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (lastRefresh == null) {
			if (other.lastRefresh != null)
				return false;
		} else if (!lastRefresh.equals(other.lastRefresh))
			return false;
		if (oAuth1PublicToken == null) {
			if (other.oAuth1PublicToken != null)
				return false;
		} else if (!oAuth1PublicToken.equals(other.oAuth1PublicToken))
			return false;
		if (refreshToken == null) {
			if (other.refreshToken != null)
				return false;
		} else if (!refreshToken.equals(other.refreshToken))
			return false;
		if (scope == null) {
			if (other.scope != null)
				return false;
		} else if (!scope.equals(other.scope))
			return false;
		if (service == null) {
			if (other.service != null)
				return false;
		} else if (!service.equals(other.service))
			return false;
		if (tokenType == null) {
			if (other.tokenType != null)
				return false;
		} else if (!tokenType.equals(other.tokenType))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OAuthCredential [id=" + id + ", userId=" + userId + ", apiService=" + service + ", expiresIn="
				+ expiresIn + ", lastRefresh=" + lastRefresh + ", scope=" + scope + "]";
	}
}
