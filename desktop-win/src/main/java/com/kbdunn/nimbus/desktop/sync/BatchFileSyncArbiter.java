package com.kbdunn.nimbus.desktop.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class BatchFileSyncArbiter {

	private static final Logger log = LoggerFactory.getLogger(BatchFileSyncArbiter.class);
	
	private final Map<String, SyncFile> beforeState;
	private final Map<String, SyncFile> afterState;
	private final Map<String, SyncFile> networkState;
	
	public BatchFileSyncArbiter(List<SyncFile> networkState) {
		this.networkState = DesktopSyncFileUtil.buildMap(networkState);
		this.beforeState = SyncStateCache.instance().getLastPersistedSyncState();
		this.afterState = SyncStateCache.instance().getCurrentSyncState();
	}

	public Map<String, SyncFile> getNetworkSyncState() {
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
					if (network.getMd5().equals(before.getMd5())) {
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
				if (before != null && before.getMd5().equals(after.getMd5())) {
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
					(before == null || !before.getMd5().equals(after.getMd5()))) {
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
				} else if (!before.getMd5().equals(network.getMd5())) {
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
			if (before.getMd5().equals(after.getMd5())) {
				// File hasn't changed, skip
				continue;
			}
			
			network = networkState.get(after.getPath());
			if (network == null || network.isDirectory()) {
				// File isn't on the network or is a folder. Can't be updated
				continue;
			}
			
			// Check that the network has the same old version, otherwise it's a conflict
			if (before.getMd5().equals(network.getMd5())) {
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
			if (network.getMd5().equals(after.getMd5())) {
				// No change
				continue;
			}
			before = beforeState.get(network.getPath());
			
			// Don't update if there was a local modification to avoid data loss
			// If it wasn't here before it's a sync error
			// Basically only update if the file before matches what's on the network
			if (before != null && before.getMd5().equals(after.getMd5())) {
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
	public List<SyncFile> getVersionConflicts() {
		List<SyncFile> versionConficts = new ArrayList<>();
		
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
			if (after != null && !network.getMd5().equals(after.getMd5())) {
				// Check for a local add
				if (before == null) {
					log.debug("File '{}' was added during absense, but a different version exists on the network.", after);
					versionConficts.add(network);
				
				// Check for local modify
				} else if (!after.getMd5().equals(before.getMd5())) {
					log.debug("File '{}' was modified during absense, but a different version exists on the network.", after);
					versionConficts.add(network);
				}
			}
		}
		
		log.info("Found {} local files with version conflicts on the network.", versionConficts.size());
		return versionConficts;
	}
}