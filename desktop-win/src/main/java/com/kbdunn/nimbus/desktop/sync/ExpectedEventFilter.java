package com.kbdunn.nimbus.desktop.sync;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;

public class ExpectedEventFilter {

	private static final Logger log = LoggerFactory.getLogger(ExpectedEventFilter.class);
	private static final int CACHE_TTL_SECONDS = 10;
	
	private final ConcurrentHashMap<String, Long> expectedAdds;
	private final ConcurrentHashMap<String, Long> expectedUpdates;
	private final ConcurrentHashMap<String, Long> expectedDeletes;
	private long lastCacheClean = 0;
	
	public ExpectedEventFilter() {
		expectedAdds = new ConcurrentHashMap<>();
		expectedUpdates = new ConcurrentHashMap<>();
		expectedDeletes = new ConcurrentHashMap<>();
	}
	
	public void expectAdd(SyncFile added) {
		expectedAdds.put(added.getPath(), System.nanoTime());
		cleanCache();
	}
	
	public boolean filterAdd(SyncFile added) {
		return expectedAdds.remove(added.getPath()) != null;
	}
	
	public void expectUpdate(SyncFile updated) {
		expectedUpdates.put(updated.getPath(), System.nanoTime());
		cleanCache();
	}
	
	public boolean filterUpdate(SyncFile updated) {
		return expectedUpdates.remove(updated.getPath()) != null;
	}
	
	public void expectDelete(SyncFile deleted) {
		expectedDeletes.put(deleted.getPath(), System.nanoTime());
		cleanCache();
	}
	
	public boolean filterDelete(SyncFile deleted) {
		return expectedDeletes.remove(deleted.getPath()) != null;
	}
	
	private void cleanCache() {
		long ts = System.nanoTime();
		// Clean once per second max
		if (ts-lastCacheClean < TimeUnit.SECONDS.toNanos(1)) {
			return;
		}
		clean(expectedAdds);
		clean(expectedUpdates);
		clean(expectedDeletes);
		lastCacheClean = ts;
		log.debug("Expected event cache contains {} add(s), {} update(s), {} delete(s)", 
				expectedAdds.size(), expectedUpdates.size(), expectedDeletes.size());
	}
	
	private void clean(Map<String, Long> cache) {
		long ts = System.nanoTime();
		Map.Entry<String, Long> entry;
		for (Iterator<Map.Entry<String, Long>> it = cache.entrySet().iterator(); it.hasNext();) {
			entry = it.next();
			if (ts-entry.getValue() > TimeUnit.SECONDS.toNanos(CACHE_TTL_SECONDS)) {
				it.remove();
			}
		}
	}
}
