package com.kbdunn.nimbus.common.model;

public class SMTPSettings {
	
	private Long userId;
	private String smtpServer;
	private String smtpPort;
	private String sslPort;
	private String username;
	private String password;
	private Boolean sslEnabled;
	
	public SMTPSettings(Long userId) { 
		this.userId = userId;
	}
	
	public SMTPSettings(Long userId, String smtpServer, String smtpPort,
			String sslPort, String username, String password,
			Boolean sslEnabled) {
		super();
		this.userId = userId;
		this.smtpServer = smtpServer;
		this.smtpPort = smtpPort;
		this.sslPort = sslPort;
		this.username = username;
		this.password = password;
		this.sslEnabled = sslEnabled;
	}

	public Long getUserId() {
		return userId;
	}
	
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public String getSmtpServer() {
		return smtpServer;
	}
	
	public void setSmtpServer(String smtpServer) {
		this.smtpServer = smtpServer;
	}
	
	public String getSmtpPort() {
		return smtpPort;
	}
	
	public void setSmtpPort(String smtpPort) {
		this.smtpPort = smtpPort;
	}
	
	public String getSslPort() {
		return sslPort;
	}
	
	public void setSslPort(String sslPort) {
		this.sslPort = sslPort;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	public boolean isSslEnabled() {
		return sslEnabled != null && sslEnabled;
	}
	
	public void setSslEnabled(Boolean isSslEnabled) {
		this.sslEnabled = isSslEnabled;
	}
	
	// True if no attributes (other than username) are set
	public boolean noAttributesSet() {
		return smtpServer == null && smtpPort == null &&
				(sslEnabled == null || !sslEnabled) && sslPort == null && password == null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((sslEnabled == null) ? 0 : sslEnabled.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result
				+ ((smtpPort == null) ? 0 : smtpPort.hashCode());
		result = prime * result
				+ ((smtpServer == null) ? 0 : smtpServer.hashCode());
		result = prime * result + ((sslPort == null) ? 0 : sslPort.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		result = prime * result
				+ ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SMTPSettings))
			return false;
		SMTPSettings other = (SMTPSettings) obj;
		if (sslEnabled == null) {
			if (other.sslEnabled != null)
				return false;
		} else if (!sslEnabled.equals(other.sslEnabled))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (smtpPort == null) {
			if (other.smtpPort != null)
				return false;
		} else if (!smtpPort.equals(other.smtpPort))
			return false;
		if (smtpServer == null) {
			if (other.smtpServer != null)
				return false;
		} else if (!smtpServer.equals(other.smtpServer))
			return false;
		if (sslPort == null) {
			if (other.sslPort != null)
				return false;
		} else if (!sslPort.equals(other.sslPort))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SmtpSettings [userId=" + userId + ", smtpServer=" + smtpServer
				+ ", smtpPort=" + smtpPort + ", sslPort=" + sslPort
				+ ", username=" + username + ", password=" + password
				+ ", isSslEnabled=" + sslEnabled + "]";
	}
}
