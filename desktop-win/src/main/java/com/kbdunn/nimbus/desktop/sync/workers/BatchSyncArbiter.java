package com.kbdunn.nimbus.desktop.sync.workers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class BatchSyncArbiter {

	private static final Logger log = LoggerFactory.getLogger(BatchSyncArbiter.class);
	
	// Map<Path, SyncFile> - state snapshots
	private final Map<String, SyncFile> previousLocalState;
	private final Map<String, SyncFile> currentLocalState;
	private final Map<String, SyncFile> currentNetworkState;
	
	// Map<Path, SyncFile> - local events
	private final Map<String, SyncFile> addedLocal;
	private final Map<String, SyncFile> updatedLocal;
	private final Map<String, SyncFile> deletedLocal;
	private final Map<String, SyncFile> unchangedLocal;
	
	// Lists of sync decisions made
	private final List<SyncFile> toDeleteLocal;
	private final List<SyncFile> toDeleteRemote;
	private final List<SyncFile> toAddLocal;
	private final List<SyncFile> toAddRemote;
	private final List<SyncFile> toUpdateLocal;
	private final List<SyncFile> toUpdateRemote;
	private final List<SyncFile> syncConflicts;
	private final List<SyncFile> noAction;
	
	// Special cases where we can save bandwith by copying
	// instead of uploading/downloading
	private final List<FileCopyEvent> toCopyRemote;
	private final List<FileCopyEvent> toCopyLocal;
	
	public BatchSyncArbiter(List<SyncFile> currentLocalState, List<SyncFile> previousLocalState, List<SyncFile> currentNetworkState) {
		this.previousLocalState = DesktopSyncFileUtil.buildMap(previousLocalState);
		this.currentLocalState = DesktopSyncFileUtil.buildMap(currentLocalState);
		this.currentNetworkState = DesktopSyncFileUtil.buildMap(currentNetworkState);
		
		this.addedLocal = new HashMap<>();
		this.updatedLocal = new HashMap<>();
		this.deletedLocal = new HashMap<>();
		this.unchangedLocal = new HashMap<>();
		
		this.toDeleteLocal = new ArrayList<>();
		this.toDeleteRemote = new ArrayList<>();
		this.toAddLocal = new ArrayList<>();
		this.toAddRemote = new ArrayList<>();
		this.toUpdateLocal = new ArrayList<>();
		this.toUpdateRemote = new ArrayList<>();
		this.syncConflicts = new ArrayList<>();
		this.noAction = new ArrayList<>();
		
		this.toCopyRemote = new ArrayList<>();
		this.toCopyLocal = new ArrayList<>();
	}
	
	public List<SyncFile> getFilesToDeleteLocally() {
		return toDeleteLocal;
	}

	public List<SyncFile> getFilesToDeleteRemotely() {
		return toDeleteRemote;
	}

	public List<SyncFile> getFilesToAddLocally() {
		return toAddLocal;
	}

	public List<SyncFile> getFilesToAddRemotely() {
		return toAddRemote;
	}

	public List<SyncFile> getFilesToUpdateLocally() {
		return toUpdateLocal;
	}

	public List<SyncFile> getFilesToUpdateRemotely() {
		return toUpdateRemote;
	}

	public List<SyncFile> getSyncConflicts() {
		return syncConflicts;
	}

	public List<FileCopyEvent> getFilesToCopyRemotely() {
		return toCopyRemote;
	}

	public List<FileCopyEvent> getFilesToCopyLocally() {
		return toCopyLocal;
	}
	
	public void arbitrate() {
		long start = System.nanoTime();
		log.info("Processing file sync changes...");
		
		detectLocalEvents();
		decideOutcome();
		findCopyOpportunities();
		sort();
		
		log.info("Finished processing file sync changes in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		logResults();
	}

	private void detectLocalEvents() {
		SyncFile before = null;
		for (SyncFile after : currentLocalState.values()) {
			before = previousLocalState.get(after.getPath());
			
			// Check for a local add
			if (before == null) {
				addedLocal.put(after.getPath(), after);
			// Check for a local update
			} else if (!before.getMd5().equals(after.getMd5())) {
				updatedLocal.put(after.getPath(), after);
			// Otherwise it didn't change
			} else {
				unchangedLocal.put(after.getPath(), after);
			}
		}
		for (SyncFile before2 : previousLocalState.values()) {
			// Check for a local delete
			if (!currentLocalState.containsKey(before2.getPath())) {
				deletedLocal.put(before2.getPath(), before2);
			}
		}
	}
	
	private void decideOutcome() {
		SyncFile network = null;
		SyncFile before = null;
		
		// Decide what to do with local adds
		for (SyncFile added : addedLocal.values()) {
			network = currentNetworkState.get(added.getPath());
			
			if (network == null) {
				// Add it remotely
				// See if we can save bandwidth by copying instead
				
				toAddRemote.add(added);
			} else if (added.getMd5().equals(network.getMd5())) {
				noAction.add(added);
			} else {
				syncConflicts.add(added);
			}
		}
		
		// Decide what to do with local updates
		for (SyncFile updated : updatedLocal.values()) {
			network = currentNetworkState.get(updated.getPath());
			before = previousLocalState.get(updated.getPath());
			
			if (before == null) {
				throw new IllegalStateException("Detected local update does not exist in the previous snapshot");
			}
			
			if (network == null) {
				toAddRemote.add(updated);
			} else if (before.getMd5().equals(network.getMd5())) {
				toUpdateRemote.add(updated);
			} else if (updated.getMd5().equals(network.getMd5())) {
				noAction.add(updated);
			} else {
				syncConflicts.add(updated);
			}
		}
		
		// Decide what to do with local deletes
		for (SyncFile deleted : deletedLocal.values()) {
			network = currentNetworkState.get(deleted.getPath());
			
			if (network == null) {
				noAction.add(deleted);
			} else if (deleted.getMd5().equals(network.getMd5())) {
				toDeleteRemote.add(deleted);
			} else {
				toAddLocal.add(network);
			}
		}
		
		// Decide what to do with locally unchanged files
		for (SyncFile unchanged : unchangedLocal.values()) {
			network = currentNetworkState.get(unchanged.getPath());
			
			if (network == null) {
				toDeleteLocal.add(unchanged);
			} else if (unchanged.getMd5().equals(network.getMd5())) {
				noAction.add(unchanged);
			} else {
				toUpdateLocal.add(network);
			}
		}
		
		// Detect new remote files
		for (SyncFile network2 : currentNetworkState.values()) {
			if (!currentLocalState.containsKey(network2.getPath()) 
					&& !previousLocalState.containsKey(network2.getPath())) {
				toAddLocal.add(network2);
			}
		}
	}
	
	// Saves bandwith by copying exising files instead 
	// of uploading/downloading
	private void findCopyOpportunities() {
		// Check remote adds for copy scenarios
		SyncFile copyCandidate = null;
		SyncFile remoteAdd = null;
		for (Iterator<SyncFile> it = toAddRemote.iterator(); it.hasNext();) {
			remoteAdd = it.next();
			copyCandidate = findCopyCandidate(remoteAdd, currentNetworkState.values());
			if (copyCandidate != null) {
				toCopyRemote.add(new FileCopyEvent(copyCandidate, remoteAdd, false));
				it.remove();
			}
		}
		
		// Check remote updates for copy scenarios
		SyncFile remoteUpdate = null;
		for (Iterator<SyncFile> it = toUpdateRemote.iterator(); it.hasNext();) {
			remoteUpdate = it.next();
			copyCandidate = findCopyCandidate(remoteUpdate, currentNetworkState.values());
			if (copyCandidate != null) {
				toCopyRemote.add(new FileCopyEvent(copyCandidate, remoteUpdate, true));
				it.remove();
			}
		}
		
		// Check local adds for copy scenarios
		SyncFile localAdd = null;
		for (Iterator<SyncFile> it = toAddLocal.iterator(); it.hasNext();) {
			localAdd = it.next();
			copyCandidate = findCopyCandidate(localAdd, currentLocalState.values());
			if (copyCandidate != null) {
				toCopyLocal.add(new FileCopyEvent(copyCandidate, localAdd, false));
				it.remove();
			}
		}
		
		// Check local updates for copy scenarios
		SyncFile localUpdate = null;
		for (Iterator<SyncFile> it = toUpdateLocal.iterator(); it.hasNext();) {
			localUpdate = it.next();
			copyCandidate = findCopyCandidate(localUpdate, currentLocalState.values());
			if (copyCandidate != null) {
				toCopyLocal.add(new FileCopyEvent(copyCandidate, localUpdate, true));
				it.remove();
			}
		}
	}
	
	private SyncFile findCopyCandidate(SyncFile file, Collection<SyncFile> pool) {
		if (file.isDirectory()) return null;
		for (SyncFile candidate : pool) {
			if (file.getMd5().equals(candidate.getMd5())) {
				return candidate;
			}
		}
		return null;
	}
	
	private void sort() {
		SyncFileUtil.sortPreorder(toDeleteLocal);
		Collections.reverse(toDeleteLocal); // Delete from behind
		SyncFileUtil.sortPreorder(toDeleteRemote);
		Collections.reverse(toDeleteRemote); // Delete from behind
		SyncFileUtil.sortPreorder(toAddLocal);
		SyncFileUtil.sortPreorder(toAddRemote);
		SyncFileUtil.sortPreorder(toUpdateLocal);
		SyncFileUtil.sortPreorder(toUpdateRemote);
	}
	
	private void logResults() {		
		log.info("Processed {} previous file(s), {} current file(s), {} network file(s)", 
				previousLocalState.size(), currentLocalState.size(), currentNetworkState.size());
		log.info("Detected the following local events:");
		log.info("  {} file(s) added", addedLocal.size());
		log.info("  {} file(s) updated", updatedLocal.size());
		log.info("  {} file(s) deleted", deletedLocal.size());
		log.info("  {} file(s) were unchanged", unchangedLocal.size());
		log.info("Decided on the following outcomes:");
		log.info("  {} file(s) to add locally", toAddLocal.size());
		log.info("  {} file(s) to add remotely", toAddRemote.size());
		log.info("  {} file(s) to update locally", toUpdateLocal.size());
		log.info("  {} file(s) to update remotely", toUpdateRemote.size());
		log.info("  {} file(s) to copy locally", toCopyLocal.size());
		log.info("  {} file(s) to copy remotely", toCopyRemote.size());
		log.info("  {} file(s) to delete locally", toDeleteLocal.size());
		log.info("  {} file(s) to delete remotely", toDeleteRemote.size());
	}
	
	/**
	 * Returns a list of files that have been deleted from the disc during this client was offline
	 * 
	 * @return a list of files that has been deleted locally
	 *//*
	public List<SyncFile> getFilesToDeleteRemotely() {
		List<SyncFile> toDeleteRemotely = new ArrayList<>();
		
		SyncFile network = null;
		for (SyncFile before : previousLocalState.values()) {
			if (currentLocalState.containsKey(before.getPath())) {
				// Skip, the file is still here
				continue;
			} else {
				network = currentNetworkState.get(before.getPath());
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
	
	*//**
	 * Returns a list of files that have been deleted by another client during the absence of this client.
	 * 
	 * @return a list of files that has been deleted remotely
	 *//*
	public List<SyncFile> getFilesToDeleteLocally() {
		List<SyncFile> toDeleteLocally = new ArrayList<>();
		
		SyncFile before = null;
		for (SyncFile after : currentLocalState.values()) {
			// Check if File is on disk but deleted on the network
			if (!currentNetworkState.containsKey(after.getPath())) {
				// Check if file was modified locally. If so it should be added to the network (ignored here).
				before = previousLocalState.get(after.getPath());
				if (before != null && before.getMd5().equals(after.getMd5())) {
					log.debug("File '{}' should be deleted locally.", after);
					toDeleteLocally.add(after);
				}
			}
		}
		
		log.info("Found {} files/folders to delete locally.", toDeleteLocally.size());
		return toDeleteLocally;
	}

	*//**
	 * Returns the missing files that exist on disk but not in the file tree in the user profile. The list is
	 * in pre-order
	 * 
	 * @return a list of files that has been added locally
	 *//*
	public List<SyncFile> getFilesToAddRemotely() {
		List<SyncFile> toAddRemotely = new ArrayList<>();

		SyncFile before = null;
		for (SyncFile after : currentLocalState.values()) {
			before = previousLocalState.get(after.getPath());
			// Check that the file is not on the network was added or modified locally
			// If nothing changed before vs. after and it's not on the network it should be deleted locally
			if (!currentNetworkState.containsKey(after.getPath()) && 
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
	
	*//**
	 * Returns a list of files that are in the user profile but not on the local disk yet.
	 * 
	 * @return a list of files that has been added remotely
	 *//*
	public List<SyncFile> getFilesToAddLocally() {
		List<SyncFile> toAddLocally = new ArrayList<>();
		
		SyncFile before = null;
		for (SyncFile network : currentNetworkState.values()) {
			// Check that it's not on the disk now
			if (!currentLocalState.containsKey(network.getPath())) {
				before = previousLocalState.get(network.getPath());
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
	
	*//**
	 * Returns a list of files that already existed but have been modified by the client while he was offline.
	 * 
	 * @return a list of files that has been updated locally
	 *//*
	public List<SyncFile> getFilesToUpdateRemotely() {
		List<SyncFile> toUpdateRemotely = new ArrayList<>();

		SyncFile before = null;
		SyncFile network = null;
		for (SyncFile after : currentLocalState.values()) {
			
			before = previousLocalState.get(after.getPath());
			if (before == null) {
				// Wasn't here before, skip
				continue;
			}
			if (before.getMd5().equals(after.getMd5())) {
				// File hasn't changed, skip
				continue;
			}
			
			network = currentNetworkState.get(after.getPath());
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
	
	*//**
	 * Returns files that have been remotely modified while the client was offline
	 * 
	 * @return a list of files that has been updated remotely
	 *//*
	public List<SyncFile> getFilesToUpdateLocally() {
		List<SyncFile> toUpdateLocally = new ArrayList<>();

		SyncFile before = null;
		SyncFile after = null;
		for (SyncFile network : currentNetworkState.values()) {
			if (network.isDirectory()) {
				// Can't modify folders
				continue;
			}
			after = currentLocalState.get(network.getPath());
			if (after == null) {
				// Can't modify it, it doesn't exist
				continue;
			}
			// Check that current is different than the network
			if (network.getMd5().equals(after.getMd5())) {
				// No change
				continue;
			}
			before = previousLocalState.get(network.getPath());
			
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
	
	*//**
	 * Returns local files that have a version conflict with the network. This could be due to:
	 * <ul>
	 *    <li>File was modified locally, but a different version exists on the network.</li>
	 *    <li>File was added locally, but a different version exists on the network.</li>
	 * </ul>
	 * 
	 * @return a list of local files that have version conflicts
	 *//*
	public List<SyncFile> getVersionConflicts() {
		List<SyncFile> versionConficts = new ArrayList<>();
		
		SyncFile before = null;
		SyncFile after = null;
		for (SyncFile network : currentNetworkState.values()) {
			if (network.isDirectory()) {
				// Folders can't conflict
				continue;
			}
			before = previousLocalState.get(network.getPath());
			after = currentLocalState.get(network.getPath());
			
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
	}*/
}