package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.common.security.OAuthAPIService.Type;
import com.kbdunn.nimbus.common.util.ComparatorUtil;

@JsonIgnoreProperties({
		"passwordDigest"
		, "hmacKey"
		, "apiToken"
})
public class NimbusUser implements NimbusRecord, Comparable<NimbusUser> {
	
	private Long id;
	protected String name;
	protected String email;
	protected String passwordDigest;
	protected String apiToken;
	protected String hmacKey;
	protected Boolean isPasswordTemporary;
	protected Boolean isAdministrator;
	protected Boolean isOwner;
	private OAuthAPIService.Type oAuthEmailService;
	private Date created;
	private Date updated;
	
	public NimbusUser() {
		isPasswordTemporary = false;
		isAdministrator = false;
		isOwner = false;
	}

	public NimbusUser(Long id, String name, String email, String passwordDigest, String apiToken, String hmacKey,
			Boolean isPasswordTemporary, Boolean isAdministrator, Boolean isOwner, Type oAuthEmailService, Date created,
			Date updated) {
		super();
		this.id = id;
		this.name = name;
		this.email = email;
		this.passwordDigest = passwordDigest;
		this.apiToken = apiToken;
		this.hmacKey = hmacKey;
		this.isPasswordTemporary = isPasswordTemporary;
		this.isAdministrator = isAdministrator;
		this.isOwner = isOwner;
		this.oAuthEmailService = oAuthEmailService;
		this.created = created;
		this.updated = updated;
	}

	public NimbusUser(NimbusUser user) {
		this.id = user.id;
		this.name = user.name;
		this.email = user.email;
		this.passwordDigest = user.passwordDigest;
		this.apiToken = user.apiToken;
		this.hmacKey = user.hmacKey;
		this.isPasswordTemporary = user.isPasswordTemporary;
		this.isAdministrator = user.isAdministrator;
		this.isOwner = user.isOwner;
		this.oAuthEmailService = user.oAuthEmailService;
		this.created = user.created;
		this.updated = user.updated;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getHmacKey() {
		return hmacKey;
	}
	
	public void setHmacKey(String hmacKey) {
		this.hmacKey = hmacKey;
	}
	
	public String getApiToken() {
		return apiToken;
	}
	
	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}
	
	public String getPasswordDigest() {
		return passwordDigest;
	}
	
	public void setPasswordDigest(String pwDigest) {
		this.passwordDigest = pwDigest;
	}
	
	public boolean isPasswordTemporary() {
		return isPasswordTemporary != null && isPasswordTemporary;
	}
	
	public void setPasswordTemporary(Boolean isPasswordTemporary) {
		this.isPasswordTemporary = isPasswordTemporary;
	}
	
	public boolean isAdministrator() {
		return isAdministrator != null && isAdministrator;
	}
	
	public void setAdministrator(Boolean isAdmin) {
		this.isAdministrator = isAdmin;
	}
	
	public boolean isOwner() {
		return isOwner != null && isOwner;
	}
	
	public void setOwner(Boolean isOwner) {
		this.isOwner = isOwner;
	}

	public OAuthAPIService.Type getEmailServiceName() {
		return oAuthEmailService;
	}

	public void setEmailServiceName(OAuthAPIService.Type oAuthEmailService) {
		this.oAuthEmailService = oAuthEmailService;
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
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((isAdministrator == null) ? 0 : isAdministrator.hashCode());
		result = prime * result + ((isOwner == null) ? 0 : isOwner.hashCode());
		result = prime * result + ((oAuthEmailService == null) ? 0 : oAuthEmailService.hashCode());
		result = prime
				* result
				+ ((isPasswordTemporary == null) ? 0 : isPasswordTemporary
						.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((passwordDigest == null) ? 0 : passwordDigest.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NimbusUser))
			return false;
		NimbusUser other = (NimbusUser) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isAdministrator == null) {
			if (other.isAdministrator != null)
				return false;
		} else if (!isAdministrator.equals(other.isAdministrator))
			return false;
		if (isOwner == null) {
			if (other.isOwner != null)
				return false;
		} else if (!isOwner.equals(other.isOwner))
			return false;
		if (oAuthEmailService == null) {
			if (other.oAuthEmailService != null)
				return false;
		} else if (!oAuthEmailService.equals(other.oAuthEmailService))
			return false;
		if (isPasswordTemporary == null) {
			if (other.isPasswordTemporary != null)
				return false;
		} else if (!isPasswordTemporary.equals(other.isPasswordTemporary))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (passwordDigest == null) {
			if (other.passwordDigest != null)
				return false;
		} else if (!passwordDigest.equals(other.passwordDigest))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "NimbusUser [id=" + id + ", name=" + name + ", email=" + email
				+ ", isPasswordTemporary=" + isPasswordTemporary
				+ ", isAdministrator=" + isAdministrator + ", isOwner="
				+ isOwner + ", oAuthEmailService=" + oAuthEmailService + "]";
	}

	@Override
	public int compareTo(NimbusUser o) {
		int i = ComparatorUtil.nullSafeBooleanComparator(o.isOwner, this.isOwner);
		i = i == 0 ? ComparatorUtil.nullSafeBooleanComparator(o.isAdministrator, this.isAdministrator) : i;
		i = i == 0 ? ComparatorUtil.nullSafeStringComparator(this.name, o.name) : i;
		return i;
	}
}
