package com.kbdunn.nimbus.api.network.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import com.kbdunn.nimbus.common.util.StringUtil;

public class UriUtil {

	private UriUtil() {  }
	
	public static Map<String, String> splitQuery(URI uri) throws UnsupportedEncodingException {
	    Map<String, String> result = new LinkedHashMap<String, String>();
	    String query = uri.getQuery();
	    if (query == null || query.isEmpty()) return Collections.emptyMap();
	    String[] pairs = query.split("&");
	    for (String pair : pairs) {
	        int idx = pair.indexOf("=");
	        result.put(StringUtil.decodeUtf8(pair.substring(0, idx)), StringUtil.decodeUtf8(pair.substring(idx + 1)));
	    }
	    return result;
	}
	
	public static <K, V> Map<String, String> toSingleValuedStringMap(MultivaluedMap<K, V> multiMap) {
		Map<String, String> map = new HashMap<>();
		for (K key : multiMap.keySet()) {
			map.put(String.valueOf(key), String.valueOf(multiMap.getFirst(key)));
		}
		return map;
	}
}
