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
	private final Map<String, SyncFile> syncErrors;
	
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
	
	public BatchSyncArbiter(Map<String, SyncFile> currentLocalState, Map<String, SyncFile> previousLocalState, 
			List<SyncFile> currentNetworkState, Map<String, SyncFile> syncErrors) {
		this.previousLocalState = previousLocalState;
		this.currentLocalState = currentLocalState;
		this.currentNetworkState = DesktopSyncFileUtil.buildMap(currentNetworkState);
		this.syncErrors = syncErrors;
		
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
	
	/*public BatchSyncArbiter(List<SyncFile> currentLocalState, List<SyncFile> previousLocalState, List<SyncFile> currentNetworkState, List<SyncFile> syncErrors) {
		this.previousLocalState = DesktopSyncFileUtil.buildMap(previousLocalState);
		this.currentLocalState = DesktopSyncFileUtil.buildMap(currentLocalState);
		this.currentNetworkState = DesktopSyncFileUtil.buildMap(currentNetworkState);
		this.syncErrors = DesktopSyncFileUtil.buildMap(syncErrors);
		
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
	}*/
	
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
	
	// TODO: Smarter outcomes using last modified timestamp and sync errors
	private void decideOutcome() {
		SyncFile network = null;
		SyncFile before = null;
		
		// Decide what to do with local adds
		for (SyncFile added : addedLocal.values()) {
			network = currentNetworkState.get(added.getPath());
			
			if (network == null) {
				// Add it remotely
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
		SyncFile error = null;
		for (SyncFile unchanged : unchangedLocal.values()) {
			network = currentNetworkState.get(unchanged.getPath());
			error = syncErrors.get(unchanged.getPath());
			
			if (network == null) {
				if (error == null) {
					toDeleteLocal.add(unchanged);
				} else {
					// If there was an error try to add again remotely
					toAddRemote.add(unchanged);
				}
			} else if (!unchanged.getMd5().equals(network.getMd5())) {
				// TODO: Change logic if there's a sync error? 
				// Use the modifed date to determine survivor?
				toUpdateLocal.add(network);
			} else {
				noAction.add(unchanged);
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
}