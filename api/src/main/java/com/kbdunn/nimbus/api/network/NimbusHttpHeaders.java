package com.kbdunn.nimbus.api.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.TreeMap;

public class NimbusHttpHeaders {
	
	public interface Key {
		String PREFIX = "x-nmb-";
		String REQUESTOR = PREFIX + "requestor";
		String SIGNATURE = PREFIX + "signature";
		String TIMESTAMP = PREFIX + "timestamp";
	}
		
	private Map<String, String> headers;
	
	public NimbusHttpHeaders() {
		headers = new TreeMap<>();
	}
	
	public void put(String key, String value) {
		if (value == null) return;
		headers.put(key, value);
	}
	
	public String get(String key) {
		return headers.get(key);
	}
	
	public boolean containsKey(Key key) {
		return headers.containsKey(key);
	}
	
	public Map<String, String> getMap() {
		return Collections.unmodifiableMap(headers);
	}
	
	public Map<String, String> getStringMap() {
		Map<String, String> sm = new HashMap<>();
		for (Entry<String, String> e : headers.entrySet()) {
			sm.put(e.getKey().toString(), e.getValue());
		}
		return Collections.unmodifiableMap(sm);
	}
	
	/*public String getCanonicalString() {
		StringBuilder sb = new StringBuilder();
		Iterator<Entry<Key, String>> it = headers.entrySet().iterator();
		Entry<Key, String> e = null;
		while (it.hasNext()) {
			e = it.next();
			if (e.getValue() != null) sb.append(e.getKey() + ":" + e.getValue().replace("\n", " "));
			if (it.hasNext()) sb.append("\n");
		}
		
		return sb.toString();
	}*/
	
	public NimbusHttpHeaders coalesce(NimbusHttpHeaders secondary) {
		return coalesce(secondary.getStringMap());
	}
	
	public NimbusHttpHeaders coalesce(Map<String, String> secondary) {
		for (Entry<String, String> e : NimbusHttpHeaders.fromMap(secondary).getMap().entrySet()) {
			if (!headers.containsKey(e.getKey())) {
				headers.put(e.getKey(), e.getValue());
			}
		}
		return this;
	}
	
	/*public static CustomHttpHeaders fromCanonicalString(String string) {
		CustomHttpHeaders headers = new CustomHttpHeaders();
		String[] entry = null;
		for (String s : string.split("\n")) {
			entry = s.split(":");
			if (entry.length != 2 || entry[0].isEmpty() || entry[1].isEmpty() || Key.fromString(entry[0]) == null)
				throw new IllegalArgumentException("Input string is not in canonical format: " + string);
			headers.put(Key.fromString(entry[0]), entry[1]);
		}
		
		return headers;
	}*/
	
	// Case-insensitive keys
	public static NimbusHttpHeaders fromMap(Map<String, String> map) {
		NimbusHttpHeaders customHeaders = new NimbusHttpHeaders();
		for (Entry<String, String> e : map.entrySet()) {
			if (e.getKey().startsWith(Key.PREFIX) && e.getValue() != null && !e.getValue().isEmpty()) {
				customHeaders.put(e.getKey(), e.getValue());
			}
		}
		return customHeaders;
	}
}
