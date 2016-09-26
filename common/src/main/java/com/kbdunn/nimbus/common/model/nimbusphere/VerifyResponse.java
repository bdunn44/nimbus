package com.kbdunn.nimbus.common.model.nimbusphere;

public class VerifyResponse {

	private String token;
	private String verifier;
	
	public VerifyResponse() {}

	public String getToken() {
		return token;
	}

	public String getVerifier() {
		return verifier;
	}

	@Override
	public String toString() {
		return "VerifyResponse [token=" + token + ", verifier=" + verifier + "]";
	}
}
