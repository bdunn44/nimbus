package com.kbdunn.nimbus.common.model.nimbusphere;

public class VerifyRequest {

	private String version;
	
	public VerifyRequest() {}
	
	public VerifyRequest(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "VerifyRequest [version=" + version + "]";
	}
}
