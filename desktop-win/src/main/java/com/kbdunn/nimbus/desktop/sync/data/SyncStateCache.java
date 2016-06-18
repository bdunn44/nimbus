package com.kbdunn.nimbus.desktop.sync.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.util.ComparatorUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.ApplicationProperties;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class SyncStateCache {

	private static final Logger log = LoggerFactory.getLogger(SyncStateCache.class);
	private static SyncStateCache instance;

	private ConcurrentHashMap<String, SyncFile> lastPersistedState;
	private ConcurrentHashMap<String, SyncFile> currentState;
	private CountDownLatch cacheReadyLatch;
	
	private SyncStateCache() {
		cacheReadyLatch = new CountDownLatch(1);
		// Build current and prior states in background thread
		Application.asyncExec(() -> {
			try {
				// Set both states to persisted
				lastPersistedState = currentState = new ConcurrentHashMap<>(DesktopSyncFileUtil.buildMap(readPersistedSyncState()));
				// Update the current state - this uses the persisted data to determine what needs to be re-hashed
				currentState = new ConcurrentHashMap<>(DesktopSyncFileUtil.buildMap(buildCurrentSyncState()));
			} catch (ClassNotFoundException|IOException e) {
				log.error("Error reading persisted sync state", e);
			} finally {
				if (currentState == null) currentState = new ConcurrentHashMap<>();
				if (lastPersistedState == null) lastPersistedState = new ConcurrentHashMap<>();
				log.info("Sync state cache initialized");
				cacheReadyLatch.countDown();
			}
		});
	}
	
	public Map<String, SyncFile> getLastPersistedSyncState() {
		return lastPersistedState;
	}
	
	public Map<String, SyncFile> getCurrentSyncState() {
		return currentState;
	}
	
	public void rebuildCurrentState() throws IOException {
		try {
			awaitCacheReady(); // Make sure we're not stepping on each other
			cacheReadyLatch = new CountDownLatch(1);
			currentState = new ConcurrentHashMap<>(DesktopSyncFileUtil.buildMap(buildCurrentSyncState()));
		} catch (IOException e) {
			log.error("Error reading persisted sync state", e);
		} finally {
			log.info("Sync state cache rebuilt");
			cacheReadyLatch.countDown();
		}
	}
	
	public SyncFile get(SyncFile cachedFile) {
		return currentState.get(cachedFile.getPath());
	}
	
	public SyncFile update(SyncFile toUpdate) {
		synchronized (currentState) {
			currentState.put(toUpdate.getPath(), toUpdate);
			return toUpdate;
		}
	}
	
	public SyncFile update(File file) throws IOException {
		return update(file, (file.exists() ? file.isDirectory() : false));
	}
	
	public SyncFile update(File file, boolean isDirectory) throws IOException {
		File syncRootDir = ApplicationProperties.instance().getSyncDirectory();
		SyncFile syncFile = new SyncFile(SyncFileUtil.getRelativeSyncFilePath(syncRootDir, file), "", isDirectory);
		if (!file.exists()) {
			delete(syncFile);
			return syncFile;
		} else if (isDirectory || file.isDirectory()) {
			update(syncFile);
			return syncFile;
		}
		syncFile = update(getUpdatedSyncFile(file, isDirectory));
		return syncFile;
	}
	
	public SyncFile getUpdatedSyncFile(File file, boolean isDirectory) throws IOException {
		File syncRootDir = ApplicationProperties.instance().getSyncDirectory();
		SyncFile syncFile = new SyncFile(SyncFileUtil.getRelativeSyncFilePath(syncRootDir, file), "", isDirectory);
		if (!file.exists()) {
			return null;
		} else {
			if (isDirectory != file.isDirectory()) {
				isDirectory = !isDirectory;
				syncFile = new SyncFile(SyncFileUtil.getRelativeSyncFilePath(syncRootDir, file), "", isDirectory);
			}
			if (isDirectory) return syncFile;
		}
		
		SyncFile cachedFile = get(syncFile);
		//log.debug("Cached file is {}", cachedFile);
		Long modified = Files.getLastModifiedTime(file.toPath()).toMillis();
		Long size = Files.size(file.toPath());
		/*log.debug("Current modified time is {}, size is {}", modified, size);
		if (cachedFile != null) {
			log.debug("Evaluating hash of {}", syncFile.getPath());
			log.debug("  Size: {} vs. {}", cachedFile.getSize(), size);
			log.debug("  Modified: {} vs. {}", cachedFile.getLastModified(), modified); 
			log.debug("  Last Hashed: {} vs. modified {}", cachedFile.getLastHashed(), modified);
			log.debug("  Evaluation: {}, {}, {}, {}",
					(cachedFile == null || cachedFile.getMd5() == null || cachedFile.getMd5().isEmpty()),
					(ComparatorUtil.nullSafeLongComparator(cachedFile.getSize(), size) != 0),
					(ComparatorUtil.nullSafeLongComparator(cachedFile.getLastModified(), modified) != 0),
					(ComparatorUtil.nullSafeLongComparator(cachedFile.getLastHashed(), modified) < 0));
		}*/
		if (cachedFile == null || cachedFile.getMd5() == null || cachedFile.getMd5().isEmpty() // We've never hashed it
				|| ComparatorUtil.nullSafeLongComparator(cachedFile.getSize(), size) != 0 // File size changed
				|| ComparatorUtil.nullSafeLongComparator(cachedFile.getLastModified(), modified) != 0 // Modified date has changed
				|| ComparatorUtil.nullSafeLongComparator(cachedFile.getLastHashed(), modified) < 0) {  // Haven't hashed since modification
			log.info("Calculating MD5 hash of " + syncFile);
			syncFile.setMd5(StringUtil.bytesToHex(HashUtil.hash(file)));
			syncFile.setLastHashed(System.currentTimeMillis());
		} else {
			syncFile.setMd5(cachedFile.getMd5());
			syncFile.setLastHashed(cachedFile.getLastHashed());
		}
		syncFile.setSize(size);
		syncFile.setLastModified(modified);
		
		return syncFile;
	}
	
	public void visit(SyncFile directory) throws IOException {
		List<SyncFile> visited = new ArrayList<>();
		final File root = DesktopSyncFileUtil.toFile(directory);
		if (root.exists()) {
			visited.add(update(root));
			if (root.isDirectory()) {
				Iterator<File> files = FileUtils.iterateFilesAndDirs(root, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
				while (files.hasNext()) {
					File file = files.next();
					if (file.equals(root)) {
						// skip root folder
						continue;
					}
					visited.add(update(file));
				}
			}
		}
		SyncFile cacheFile = null;
		for (Iterator<Map.Entry<String, SyncFile>> it = currentState.entrySet().iterator(); it.hasNext();) {
			cacheFile = it.next().getValue();
			if (cacheFile.getPath().startsWith(directory.getPath()) 
					&& !visited.contains(cacheFile)) {
				// The file wasn't visited, it doesn't exist
				it.remove();
			}
		}
	}
	
	public void delete(SyncFile toDelete) {
		synchronized (currentState) {
			currentState.remove(toDelete.getPath());
		}
	}
	
	public void awaitCacheReady() {
		try {
			cacheReadyLatch.await();
		} catch (InterruptedException e) {
			log.error("Error waiting for sync state cache to be ready", e);
		}
	}
	
	public static SyncStateCache instance() {
		return instance;
	}
	
	public static void initialize() {
		if (instance == null) {
			instance = new SyncStateCache();
		}
	}
	
	public void persistCurrentState() throws IOException {
		persist(currentState);
	}
	
	public void persist(Map<String, SyncFile> state) throws IOException {
		long start = System.nanoTime();
		log.info("Persisting cached sync state");
		final List<SyncFile> stateList = Lists.newArrayList(state.values());
		final List<String> lines = new ArrayList<>();
		for (SyncFile syncFile : stateList) {
			lines.add(DesktopSyncFileUtil.toCompositeString(syncFile));
		}
		FileUtils.writeLines(getCachePersistenceFile(), lines, false);
		lastPersistedState = new ConcurrentHashMap<>(state);
		log.info("Finished persisting sync state in {}ms. {} file state(s) persisted.", 
				TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), stateList.size());
	}
	
	private List<SyncFile> buildCurrentSyncState() throws IOException {
		long start = System.nanoTime();
		log.info("Building current local sync state");
		List<SyncFile> currentState = new ArrayList<>();
		final File root = ApplicationProperties.instance().getSyncDirectory();
		Iterator<File> files = FileUtils.iterateFilesAndDirs(root, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
		while (files.hasNext()) {
			File file = files.next();
			if (file.equals(root)) {
				// skip root folder
				continue;
			}
			currentState.add(getUpdatedSyncFile(file, file.isDirectory()));
		}
		log.info("Finished building local sync state in {}ms. {} file(s) detected.", 
				TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), currentState.size());
		return currentState;
	}
	
	private synchronized List<SyncFile> readPersistedSyncState() throws IOException, ClassNotFoundException {
		long start = System.nanoTime();
		log.info("Reading last persisted sync state");
		final List<SyncFile> state = new ArrayList<>();
		final File stateFile = getCachePersistenceFile();
		if (!stateFile.exists()) {
			log.error("Persisted sync state is not available.");
			return state;
		}
		for (String line : FileUtils.readLines(stateFile)) {
			state.add(DesktopSyncFileUtil.fromCompositeString(line));
		}
		log.info("Finished reading persisted sync state in {}ms. {} file state(s) read.", 
				TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), state.size());
		return state;
	}
	
	private static File getCachePersistenceFile() {
		return new File(ApplicationProperties.instance().getInstallDirectory(), "data/sync-state.dat");
	}
}
