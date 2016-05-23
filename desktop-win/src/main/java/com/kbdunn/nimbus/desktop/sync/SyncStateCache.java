package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class SyncStateCache {

	private static final Logger log = LoggerFactory.getLogger(SyncStateCache.class);
	private static SyncStateCache instance;

	private Map<String, SyncFile> lastPersistedState;
	private Map<String, SyncFile> currentLocalState;
	private CountDownLatch cacheReadyLatch;
	
	private SyncStateCache() {
		cacheReadyLatch = new CountDownLatch(1);
		// Build current and prior states in background thread
		Application.asyncExec(() -> {
			try {
				lastPersistedState = DesktopSyncFileUtil.buildMap(SyncStateCache.readPersistedSyncState());
				currentLocalState = DesktopSyncFileUtil.buildMap(SyncStateCache.buildCurrentSyncState());
			} catch (ClassNotFoundException|IOException e) {
				log.error("Error reading persisted sync state", e);
			} finally {
				if (currentLocalState == null) currentLocalState = Collections.emptyMap();
				if (lastPersistedState == null) lastPersistedState = Collections.emptyMap();
				log.info("Sync state cache initialized");
				cacheReadyLatch.countDown();
			}
		});
	}
	
	public Map<String, SyncFile> getLastPersistedSyncState() {
		return lastPersistedState;
	}
	
	public Map<String, SyncFile> getCurrentSyncState() {
		return currentLocalState;
	}
	
	public void updateLocalCache(SyncFile toUpdate) {
		currentLocalState.put(toUpdate.getPath(), toUpdate);
	}
	
	public void deleteLocalCache(SyncFile toDelete) {
		currentLocalState.remove(toDelete.getPath());
	}
	
	public void awaitCacheReady() {
		try {
			cacheReadyLatch.await();
		} catch (InterruptedException e) {
			log.error("Error waiting for sync state cache to be ready", e);
		}
	}
	
	public void disposeLastPersistedSyncState() {
		lastPersistedState = null; // Okay to GC
	}
	
	public static SyncStateCache instance() {
		return instance;
	}
	
	public static void initialize() {
		if (instance == null) {
			instance = new SyncStateCache();
		}
	}
	
	public synchronized void persist() throws IOException {
		long start = System.nanoTime();
		log.info("Persisting cached sync state");
		final List<SyncFile> state = Lists.newArrayList(currentLocalState.values());
		final List<String> lines = new ArrayList<>();
		for (SyncFile syncFile : state) {
			lines.add(syncFile.getPath() + "::" + syncFile.getMd5());
		}
		FileUtils.writeLines(getCachePersistenceFile(), lines, false);
		log.info("Finished persisting sync state in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
	}
	
	private static File getCachePersistenceFile() {
		return new File(Application.getInstallationDirectory(), "sync-state.dat");
	}
	
	public static List<SyncFile> buildCurrentSyncState() throws IOException {
		long start = System.nanoTime();
		log.info("Building current local sync state");
		List<SyncFile> currentState = new ArrayList<>();
		final File root = SyncPreferences.getSyncDirectory();
		Iterator<File> files = FileUtils.iterateFilesAndDirs(root, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
		while (files.hasNext()) {
			File file = files.next();
			if (file.equals(root)) {
				// skip root folder
				continue;
			}
			String path = file.getAbsolutePath().replace(root.getAbsolutePath(), "").replace("\\", "/");
			path = path.startsWith("/") ? path.substring(1) : path;
			byte[] hash = HashUtil.hash(file);
			if (file.isDirectory()) {
				currentState.add(new SyncFile(path + "/", StringUtil.bytesToHex(hash), true));
			} else {
				currentState.add(new SyncFile(path, StringUtil.bytesToHex(hash), false));
			}
		}
		log.info("Finished building local sync state in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		return currentState;
	}
	
	private static synchronized List<SyncFile> readPersistedSyncState() throws IOException, ClassNotFoundException {
		long start = System.nanoTime();
		log.info("Reading last persisted sync state");
		final List<SyncFile> state = new ArrayList<>();
		final File stateFile = getCachePersistenceFile();
		if (!stateFile.exists()) {
			log.error("Persisted sync state is not available.");
			return state;
		}
		for (String line : FileUtils.readLines(stateFile)) {
			int idx = line.indexOf("::");
			if (idx == -1) continue; // Corrupt!
			log.debug("Read {} from persisted state", line);
			state.add(new SyncFile(
					line.substring(0, idx), 
					line.substring(idx+2),
					line.substring(idx-1, idx).equals("/")
				));
		}
		log.info("Finished reading persisted sync state in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		return state;
	}
}
