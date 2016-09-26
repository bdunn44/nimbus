package com.kbdunn.nimbus.common.model.nimbusphere;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NimbusphereStatus {

	private Boolean accepted; // Specific to a heartbeat response
	private String token;
	private String ip;
	private String address;
	private Boolean verified;
	private Boolean deleted;
	
	public NimbusphereStatus() {}

	public NimbusphereStatus(Boolean accepted, String token, String ip, String address, Boolean verified, Boolean deleted) {
		this.accepted = accepted;
		this.token = token;
		this.ip = ip;
		this.address = address;
		this.verified = verified;
		this.deleted = deleted;
	}
	
	public Boolean isAccepted() {
		return accepted;
	}
	
	public String getToken() {
		return this.token;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public String getIp() {
		return ip;
	}
	
	public InetAddress getInetAddress() throws UnknownHostException {
		if (this.ip == null) return null;
		return InetAddress.getByName(this.ip);
	}

	public Boolean isVerified() {
		return verified;
	}

	public Boolean isDeleted() {
		return deleted;
	}

	@Override
	public String toString() {
		return "NimbusphereStatus [accepted=" + accepted + ", token=" + token + ", ip=" + ip + ", address=" + address
				+ ", verified=" + verified + ", deleted=" + deleted + "]";
	}
}
