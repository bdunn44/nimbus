package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.desktop.Application;

public class BatchFileSyncArbiter {

	private static final Logger log = LoggerFactory.getLogger(BatchFileSyncArbiter.class);
	
	private final HashMap<String, SyncFile> beforeState;
	private final HashMap<String, SyncFile> afterState;
	private final HashMap<String, SyncFile> networkState;
	
	/**
	 * @param rootDirectory the root Hive2Hive directory
	 * @param userProfile the current user profile
	 * @param before represents the file state at the last logout, before H2H was shutdown. The key of the map
	 *            is the path, the byte[] is the hash of the file content.
	 *            {@link BatchFileSyncArbiter#getCurrentSyncState(File)} can be used to generate this map.
	 * @param now represents the current file state. The key of the map is the path, the byte[] is the hash of
	 *            the file content. {@link BatchFileSyncArbiter#getCurrentSyncState(File)} can be used to generate this
	 *            map.
	 */
	public BatchFileSyncArbiter(List<SyncFile> network, List<SyncFile> before, List<SyncFile> now) {
		this.networkState = buildMap(network);
		this.beforeState = buildMap(before);
		this.afterState = buildMap(now);
	}
	
	public BatchFileSyncArbiter(List<SyncFile> network) throws IOException, ClassNotFoundException {
		this.networkState = buildMap(network);
		this.beforeState = buildMap(getPersistedSyncState());
		this.afterState = buildMap(getCurrentSyncState(Application.getSyncRootDirectory()));
	}
	
	private HashMap<String, SyncFile> buildMap(List<SyncFile> syncFiles) {
		HashMap<String, SyncFile> map = new HashMap<>();
		for (SyncFile syncFile : syncFiles) {
			log.debug("Adding {} to map", syncFile);
			map.put(syncFile.getPath(), syncFile);
		}
		return map;
	}
	
	/*private void buildNetworkFileState(SyncFile rootNode) {
		List<SyncFile> flatNodes = FileUtil.getFlatNodeList(rootNode, true, true);
		for (SyncFile node : flatNodes) {
			// Ignore root node
			if (!node.getPath().equals(".")) {
				network.put(node.getPath(), node);
				log.debug("Added {} to network state map.", node.getPath());
			}
		}
	}*/
	
	public Map<String, SyncFile> getBeforeSyncState() {
		return beforeState;
	}

	public Map<String, SyncFile> getAfterSyncState() {
		return afterState;
	}

	public HashMap<String, SyncFile> getNetworkSyncState() {
		return networkState;
	}
	
	/**
	 * Returns a list of files that have been deleted from the disc during this client was offline
	 * 
	 * @return a list of files that has been deleted locally
	 */
	public List<SyncFile> getFilesToDeleteRemotely() {
		List<SyncFile> toDeleteRemotely = new ArrayList<>();
		
		SyncFile network = null;
		for (SyncFile before : beforeState.values()) {
			if (afterState.containsKey(before.getPath())) {
				// Skip, the file is still here
				continue;
			} else {
				network = networkState.get(before.getPath());
				if (network == null) {
					// Skip, the file isn't on the network
					continue;
				}
				// File is on the network
				if (network.isDirectory()) {
					toDeleteRemotely.add(network);
					log.debug("Folder '{}' should be deleted remotely.", network);
				} else {
					// Check file hash, don't delete a file that has been modified remotely
					if (HashUtil.compare(network.getMd5(), before.getMd5())) {
						log.debug("File '{}' should be deleted remotely.", network);
						toDeleteRemotely.add(network);
					}
				}
			}
		}
		
		// Delete from behind
		SyncFileUtil.sortPreorder(toDeleteRemotely);
		Collections.reverseOrder();
		
		log.info("Found {} files/folders to delete remotely.", toDeleteRemotely.size());
		return toDeleteRemotely;
	}
	
	/**
	 * Returns a list of files that have been deleted by another client during the absence of this client.
	 * 
	 * @return a list of files that has been deleted remotely
	 */
	public List<SyncFile> getFilesToDeleteLocally() {
		List<SyncFile> toDeleteLocally = new ArrayList<>();
		
		SyncFile before = null;
		for (SyncFile after : afterState.values()) {
			// Check if File is on disk but deleted on the network
			if (!networkState.containsKey(after.getPath())) {
				// Check if file was modified locally. If so it should be added to the network (ignored here).
				before = beforeState.get(after.getPath());
				if (before != null && HashUtil.compare(before.getMd5(), after.getMd5())) {
					log.debug("File '{}' should be deleted locally.", after);
					toDeleteLocally.add(after);
				}
			}
		}
		
		log.info("Found {} files/folders to delete locally.", toDeleteLocally.size());
		return toDeleteLocally;
	}

	/**
	 * Returns the missing files that exist on disk but not in the file tree in the user profile. The list is
	 * in pre-order
	 * 
	 * @return a list of files that has been added locally
	 */
	public List<SyncFile> getFilesToAddRemotely() {
		List<SyncFile> toAddRemotely = new ArrayList<>();

		SyncFile before = null;
		for (SyncFile after : afterState.values()) {
			before = beforeState.get(after.getPath());
			// Check that the file is not on the network was added or modified locally
			// If nothing changed before vs. after and it's not on the network it should be deleted locally
			if (!networkState.containsKey(after.getPath()) && 
					(before == null || !HashUtil.compare(before.getMd5(), after.getMd5()))) {
				// Not in network, it has been added locally
				log.debug("File '{}' should be added remotely.", after);
				toAddRemotely.add(after);
			}
		}
		
		SyncFileUtil.sortPreorder(toAddRemotely);
		log.info("Found {} files/folders to add remotely.", toAddRemotely.size());
		return toAddRemotely;
	}
	
	/**
	 * Returns a list of files that are in the user profile but not on the local disk yet.
	 * 
	 * @return a list of files that has been added remotely
	 */
	public List<SyncFile> getFilesToAddLocally() {
		List<SyncFile> toAddLocally = new ArrayList<>();
		
		SyncFile before = null;
		for (SyncFile network : networkState.values()) {
			// Check that it's not on the disk now
			if (!afterState.containsKey(network.getPath())) {
				before = beforeState.get(network.getPath());
				// Check that is didn't exist before (wasn't deleted locally)
				if (before == null) {
					log.debug("File '{}' should be added locally.", network);
					toAddLocally.add(network);
					
				// Check if it was deleted locally, but a modified version is on the network
				} else if (!HashUtil.compare(before.getMd5(), network.getMd5())) {
					log.debug("File '{}' was deleted locally during absence, but a modified version exists on the network.", network);
					toAddLocally.add(network);
				}
			}
		}
		
		SyncFileUtil.sortPreorder(toAddLocally);
		log.info("Found {} files/folders to add locally.", toAddLocally.size());
		return toAddLocally;
	}
	
	/**
	 * Returns a list of files that already existed but have been modified by the client while he was offline.
	 * 
	 * @return a list of files that has been updated locally
	 */
	public List<SyncFile> getFilesToUpdateRemotely() {
		List<SyncFile> toUpdateRemotely = new ArrayList<>();

		SyncFile before = null;
		SyncFile network = null;
		for (SyncFile after : afterState.values()) {
			
			before = beforeState.get(after.getPath());
			if (before == null) {
				// Wasn't here before, skip
				continue;
			}
			if (HashUtil.compare(before.getMd5(), after.getMd5())) {
				// File hasn't changed, skip
				continue;
			}
			
			network = networkState.get(after.getPath());
			if (network == null || network.isDirectory()) {
				// File isn't on the network or is a folder. Can't be updated
				continue;
			}
			
			// Check that the network has the same old version, otherwise it's a conflict
			if (HashUtil.compare(before.getMd5(), network.getMd5())) {
				log.debug("File '{}' should be updated remotely.", after);
				toUpdateRemotely.add(after);
			}
		}
		
		SyncFileUtil.sortPreorder(toUpdateRemotely);
		log.info("Found {} files/folders to update remotely.", toUpdateRemotely.size());
		return toUpdateRemotely;
	}
	
	/**
	 * Returns files that have been remotely modified while the client was offline
	 * 
	 * @return a list of files that has been updated remotely
	 */
	public List<SyncFile> getFilesToUpdateLocally() {
		List<SyncFile> toUpdateLocally = new ArrayList<>();

		SyncFile before = null;
		SyncFile after = null;
		for (SyncFile network : networkState.values()) {
			if (network.isDirectory()) {
				// Can't modify folders
				continue;
			}
			after = afterState.get(network.getPath());
			if (after == null) {
				// Can't modify it, it doesn't exist
				continue;
			}
			// Check that current is different than the network
			if (HashUtil.compare(network.getMd5(), after.getMd5())) {
				// No change
				continue;
			}
			before = beforeState.get(network.getPath());
			
			// Don't update if there was a local modification to avoid data loss
			// If it wasn't here before it's a sync error
			// Basically only update if the file before matches what's on the network
			if (before != null && HashUtil.compare(before.getMd5(), after.getMd5())) {
				// Before and after hashes match and network differs. Update locally.
				log.debug("File '{}' should be updated locally.", network);
				toUpdateLocally.add(network);
			}
		}
		
		log.info("Found {} files/folders to update locally.", toUpdateLocally.size());
		return toUpdateLocally;
	}
	
	/**
	 * Returns local files that have a version conflict with the network. This could be due to:
	 * <ul>
	 *    <li>File was modified locally, but a different version exists on the network.</li>
	 *    <li>File was added locally, but a different version exists on the network.</li>
	 * </ul>
	 * 
	 * @return a list of local files that have version conflicts
	 */
	public List<SyncFile> getRemoteVersionConflicts() {
		List<SyncFile> remoteVersionConficts = new ArrayList<>();
		
		SyncFile before = null;
		SyncFile after = null;
		for (SyncFile network : networkState.values()) {
			if (network.isDirectory()) {
				// Folders can't conflict
				continue;
			}
			before = beforeState.get(network.getPath());
			after = afterState.get(network.getPath());
			
			// Check that file on disk is different vs. network
			if (after != null && !HashUtil.compare(network.getMd5(), after.getMd5())) {
				// Check for a local add
				if (before == null) {
					log.debug("File '{}' was added during absense, but a different version exists on the network.", after);
					remoteVersionConficts.add(network);
				
				// Check for local modify
				} else if (!HashUtil.compare(after.getMd5(), before.getMd5())) {
					log.debug("File '{}' was modified during absense, but a different version exists on the network.", after);
					remoteVersionConficts.add(network);
				}
			}
		}
		
		log.info("Found {} local files with version conflicts on the network.", remoteVersionConficts.size());
		return remoteVersionConficts;
	}

	public void persistSyncState() throws IOException {
		BatchFileSyncArbiter.persistSyncState(Lists.newArrayList(afterState.values()));
	}

	/**
	 * Visit all files recursively and calculate the hash of the file. Folders are also added to the result.
	 * 
	 * @param root the root folder
	 * @return a map where the key is the relative file path to the root and the value is the hash
	 * @throws IOException if hashing fails
	 */
	public static List<SyncFile> getCurrentSyncState(File root) throws IOException {
		List<SyncFile> currentState = new ArrayList<>();
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
				currentState.add(new SyncFile(path + "/", hash, true));
			} else {
				currentState.add(new SyncFile(path, hash, false));
			}
		}
		return currentState;
	}
	
	public synchronized static void persistCurrentSyncState() throws IOException {
		persistSyncState(getCurrentSyncState(Application.getSyncRootDirectory()));
	}
	
	private static File getSyncStatePersistenceFile() {
		return new File(Application.getInstallationDirectory(), "sync-state.dat");
	}
	
	synchronized static void persistSyncState(List<SyncFile> state) throws IOException {
		final List<String> lines = new ArrayList<>();
		for (SyncFile syncFile : state) {
			lines.add(syncFile.getPath() + "::" + StringUtil.bytesToHex(syncFile.getMd5()));
		}
		FileUtils.writeLines(getSyncStatePersistenceFile(), lines, false);
		/*try (
			final FileOutputStream fout = new FileOutputStream(getSyncStatePersistenceFile());
			final ObjectOutputStream oout = new ObjectOutputStream(fout);
		) {
			oout.writeObject(state);
		} catch (IOException e) {
			log.error("Unable to persist serialized sync state.", e);
			throw e;
		}*/
	}
	
	public synchronized List<SyncFile> getPersistedSyncState() throws IOException, ClassNotFoundException {
		final List<SyncFile> state = new ArrayList<>();
		final File stateFile = getSyncStatePersistenceFile();
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
					StringUtil.hexToBytes(line.substring(idx+2)),
					line.substring(0, idx).endsWith("/")
				));
		}
		return state;
		
		/*try (
			final FileInputStream fin = new FileInputStream(stateFile);
			final ObjectInputStream oin = new ObjectInputStream(fin);
		) {
			return (Map<String, byte[]>) oin.readObject();
		} catch (IOException | ClassNotFoundException e) {
			log.error("Unable to read persisted sync state.", e);
			throw e;
		}*/
	}
}