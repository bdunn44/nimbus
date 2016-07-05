package com.kbdunn.nimbus.web.email;

import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.security.OAuthAPIService;

public enum EmailService {
	//AOL(Type.SMTP, "AOL", "smtp.aol.com", "465", true, "465", null),
	//ATT(Type.SMTP, "AT&T", "smtp.att.yahoo.com", "465", true, "465", null),
	//COMCAST(Type.SMTP, "Comcast", "smtp.comcast.net", "465", true, "465", null),
	GMAIL(Type.OAUTH2, "Gmail", null, null, false, null, OAuthAPIService.Type.GOOGLE),
	HOTMAIL(Type.SMTP, "Hotmail", "smtp.live.com", "465", true, "465", null),
	OFFICE_365(Type.SMTP, "Office365.com", "smtp.office365.com", "587", true, "587", null),
	OUTLOOK(Type.SMTP, "Outlook.com", "smtp.live.com", "587", true, "587", null),
	//VERIZON(Type.SMTP, "Verizon", "outgoing.verizon.net", "465", true, "465", null),
	YAHOO(Type.SMTP, "Yahoo Mail", "smtp.mail.yahoo.com", "465", true, "465", null),
	YAHOO_AU_NZ(Type.SMTP, "Yahoo AU/NZ", "smtp.mail.yahoo.com.au", "465", true, "465", null),
	YAHOO_PLUS(Type.SMTP, "Yahoo Mail Plus", "plus.smtp.mail.yahoo.com", "465", true, "465", null),
	YAHOO_UK(Type.SMTP, "Yahoo UK", "smtp.mail.yahoo.co.uk", "465", true, "465", null);
	
	public enum Type {
		SMTP, OAUTH2
	}
	
	private Type type;
	private String name, server, port, sslPort;
	private boolean sslEnabled;
	private OAuthAPIService.Type oAuthServiceType;
	
	EmailService(Type type, String name, String server, String port, boolean sslEnabled, String sslPort, OAuthAPIService.Type oAuthServiceType) {
		this.type = type;
		this.name = name;
		this.server = server;
		this.port = port;
		this.sslEnabled = sslEnabled;
		this.sslPort = sslPort;
		this.oAuthServiceType = oAuthServiceType;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public String getServer() {
		return server;
	}
	
	public String getPort() {
		return port;
	}
	
	public boolean sslEnabled() {
		return sslEnabled;
	}
	
	public String getSslPort() {
		return sslPort;
	}
	
	public OAuthAPIService.Type getOAuthServiceType() {
		return oAuthServiceType;
	}
	
	public static EmailService valueOf(SMTPSettings settings) {
		if (settings == null) return null;
		for (EmailService s : EmailService.values()) {
			if (s.getType() == Type.SMTP
					&& s.server.equals(settings.getSmtpServer())
					&& s.port.equals(settings.getSmtpPort())
					&& s.sslEnabled == settings.isSslEnabled()
					&& s.sslPort.equals(settings.getSslPort())) {
				return s;
			}
		}
		return null;
	}
	
	public static EmailService valueOf(OAuthAPIService.Type type) {
		if (type == null) return null;
		for (EmailService s : EmailService.values()) {
			if (s.getType() == Type.OAUTH2 && s.oAuthServiceType == type) {
				return s;
			}
		}
		return null;
	}
}
