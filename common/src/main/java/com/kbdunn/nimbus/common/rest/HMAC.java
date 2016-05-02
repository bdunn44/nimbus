package com.kbdunn.nimbus.common.rest;

import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.rest.NimbusHttpHeaders.Key;

public class HMAC {
	
	private static final Logger log = LogManager.getLogger(HMAC.class.getName());
	
	private static final String ALGORITHM = "HmacSHA256"; // "HmacSHA512"
	private static final String CHARSET = "UTF-8";
	
	public String hmacDigestRequest(String key, String verb, String resource, String content, String contentType, 
			NimbusHttpHeaders headers, Map<String, String> queryParameters) throws Exception {
		if (verb == null) throw new IllegalArgumentException("HTTP verb cannot be null");
		if (resource == null) throw new IllegalArgumentException("REST resource cannot be null");
		if (headers == null) throw new IllegalArgumentException("Custom HTTP headers cannot be null");
		if (headers.get(NimbusHttpHeaders.Key.REQUESTOR) == null) throw new IllegalArgumentException(NimbusHttpHeaders.Key.REQUESTOR + " header cannot be null");
		if (headers.get(NimbusHttpHeaders.Key.TIMESTAMP) == null) throw new IllegalArgumentException(NimbusHttpHeaders.Key.TIMESTAMP + " header cannot be null");
		
		return hmacDigest(key, getStringToSign(headers, queryParameters, verb, resource, content, contentType));
	}
	
	public String hmacDigestResponse(String key, String httpStatus, String content, String contentType, NimbusHttpHeaders headers) throws Exception {
		if (httpStatus == null) throw new IllegalArgumentException("HTTP status cannot be null");
		if (headers == null) throw new IllegalArgumentException("Custom HTTP headers cannot be null");
		if (headers.get(NimbusHttpHeaders.Key.TIMESTAMP) == null) throw new IllegalArgumentException(NimbusHttpHeaders.Key.TIMESTAMP + " header cannot be null");
		
		return hmacDigest(key, getStringToSign(headers, httpStatus, content, contentType));
	}
	
	private String getStringToSign(NimbusHttpHeaders headers, Map<String, String> queryParameters, String...values) {
		if (queryParameters == null) return getStringToSign(headers, values);
		
		String[] newValues = new String[values.length + queryParameters.size()];
		System.arraycopy(values, 0, newValues, 0, values.length);
		int i = values.length;
		for (Entry<String, String> e : queryParameters.entrySet()) {
			newValues[i++] = e.getKey() + "=" + e.getValue();
		}
		return getStringToSign(headers, newValues);
	}
	
	private String getStringToSign(NimbusHttpHeaders headers, String... values) {
		
		StringBuilder sb = new StringBuilder();
		
		// Append values, delimited by newline. Append newline for null values.
		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				sb.append(values[i].replaceAll("[\n|\r\n]", " "));
			}
			sb.append(i != values.length - 1 ? "\n" : "");
		}
		
		// Iterate through custom headers. Append key:value pairs, delimited by newline. Do not append newline if header doesn't exist.
		Iterator<Entry<Key, String>> it = headers.getMap().entrySet().iterator();
		Entry<Key, String> e = null;
		
		// Add delimiter if iterator has values
		if (it.hasNext()) sb.append("\n");
		
		while (it.hasNext()) {
			e = it.next();
			if (e.getKey().equals(Key.SIGNATURE)) continue; // Ignore signature
			sb.append(e.getKey().toString().toLowerCase() + ":" + e.getValue().replaceAll("[\n|\r\n]", " "));
			if (it.hasNext()) sb.append("\n");
		}
		
		log.debug("String to sign: " + sb.toString().replace("\n", "\\n"));
		return sb.toString();
	}
	
	public String hmacDigest(String key, String message) throws Exception {
		if (key == null) throw new IllegalArgumentException("HMAC signing key cannot be null");
		if (message == null) throw new IllegalArgumentException("HMAC message cannot be null");
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(CHARSET), ALGORITHM);
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(signingKey);
			return toHex(mac.doFinal(message.getBytes(CHARSET)));
		} catch (Exception e) {
			throw e;
		}
	}
	
	public String generateSecretKey() {
		try {
			return toHex(KeyGenerator.getInstance(ALGORITHM).generateKey().getEncoded());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	
	private String toHex(byte[] bytes) {
		StringBuffer hash = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			String hex = Integer.toHexString(0xFF & bytes[i]);
			if (hex.length() == 1) {
				hash.append('0');
			}
			hash.append(hex);
		}
		return hash.toString();
	}
}
