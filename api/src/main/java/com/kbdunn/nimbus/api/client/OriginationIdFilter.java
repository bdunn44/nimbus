package com.kbdunn.nimbus.api.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OriginationIdFilter {
	
	private static final Logger log = LoggerFactory.getLogger(OriginationIdFilter.class);
	
	private static final long CACHE_TTL_SECONDS = 30;
	private final Map<String, Long> idCache = new HashMap<>();
	
	public OriginationIdFilter() {  }
	
	public OriginationIdFilter(NimbusClient client) {
		client.addOriginationIdListener((request) -> {
			addOriginationIdToFilter(request.getOriginationId());
		});
	}
	
	public void addOriginationIdToFilter(String originationId) {
		idCache.put(originationId, System.nanoTime());
		cleanCache();
	}
	
	public boolean isFiltered(String originationId) {
		return originationId != null && idCache.containsKey(originationId);
	}
	
	private void cleanCache() {
		long ts = System.nanoTime();
		Map.Entry<String, Long> entry;
		for (Iterator<Map.Entry<String, Long>> it = idCache.entrySet().iterator(); it.hasNext();) {
			entry = it.next();
			if (ts-entry.getValue() > TimeUnit.SECONDS.toNanos(CACHE_TTL_SECONDS)) {
				it.remove();
			}
		}
		log.debug("Origination ID filter cache contains {} ID(s)", idCache.size());
	}
}
