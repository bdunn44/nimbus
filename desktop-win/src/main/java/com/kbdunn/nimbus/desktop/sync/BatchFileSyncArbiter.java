package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.sync.model.FileNode;
import com.kbdunn.nimbus.common.util.FileUtil;
import com.kbdunn.nimbus.desktop.Application;

public class BatchFileSyncArbiter {

	private static final Logger log = LoggerFactory.getLogger(BatchFileSyncArbiter.class);
	
	// Map<file-path, file-hash>
	private final Map<String, byte[]> before;
	private final Map<String, byte[]> now;

	// Map<file-path, FileNode>
	private final HashMap<String, FileNode> networkFiles;
	
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
	public BatchFileSyncArbiter(FileNode rootNode, Map<String, byte[]> before, Map<String, byte[]> now) {
		this.networkFiles = new HashMap<>();
		this.before = before;
		this.now = now;
		
		addFileNodesRecursively(rootNode, networkFiles);
	}
	
	public BatchFileSyncArbiter(FileNode rootNode) throws IOException, ClassNotFoundException {
		this.networkFiles = new HashMap<>();
		this.before = getPersistedSyncState();
		this.now = getCurrentSyncState(Application.getSyncRootDirectory());
		
		addFileNodesRecursively(rootNode, networkFiles);
	}
	
	public void addFileNodesRecursively(FileNode currentNode, HashMap<String, FileNode> flatList) {
		if (currentNode == null) return;
		
		flatList.put(currentNode.getPath(), currentNode);
		if (currentNode.isFolder()) {
			for (FileNode childNode : currentNode.getChildren()) {
				addFileNodesRecursively(childNode, flatList);
			}
		}
	}
	
	public Map<String, byte[]> getPreviousSyncState() {
		return before;
	}

	public Map<String, byte[]> getCurrentSyncState() {
		return now;
	}

	public HashMap<String, FileNode> getNetworkFiles() {
		return networkFiles;
	}
	
	/**
	 * Returns a list of files that have been deleted from the disc during this client was offline
	 * 
	 * @return a list of files that has been deleted locally
	 */
	public List<File> getFilesToDeleteRemotely() {
		List<File> deletedLocally = new ArrayList<>();

		for (String path : before.keySet()) {
			if (now.containsKey(path)) {
				// skip, this file is still here
				continue;
			} else {
				// Check if the file exists on the network
				FileNode node = networkFiles.get(path);
				if (node != null) {
					// File is on the network
					if (node.isFolder()) {
						deletedLocally.add(node.getFile());
						log.debug("Folder '{}' has been deleted locally during absence.", path);
					} else {
						// Check file hash, don't delete a file that has been modified remotely
						if (HashUtil.compare(node.getMd5(), before.get(path))) {
							log.debug("File '{}' has been deleted locally during absence.", path);
							deletedLocally.add(node.getFile());
						}
					}
				}
			}
		}
		
		// delete from behind
		sortFilesPreorder(deletedLocally);
		Collections.reverseOrder();
		
		log.info("Found {} files/folders that have been deleted locally during absence.", deletedLocally.size());
		return deletedLocally;
	}
	
	/**
	 * Returns a list of files that have been deleted by another client during the absence of this client.
	 * 
	 * @return a list of files that has been deleted remotely
	 */
	public List<File> getFilesToDeleteLocally() {
		List<File> deletedRemotely = new ArrayList<>();
		
		for (String p : now.keySet()) {
			// Check if File is on disk but deleted on the network
			if (networkFiles.get(p) == null) {
				// Check if file was modified locally. If so it should be added to the network (ignored here).
				if (HashUtil.compare(before.get(p), now.get(p))) {
					log.debug("File '{}' has been deleted remotely during absence.", p);
					deletedRemotely.add(new File(p));
				}
			}
		}

		log.debug("Found {} files/folders that have been deleted remotely during absence.", deletedRemotely.size());
		return deletedRemotely;
	}

	/**
	 * Returns the missing files that exist on disk but not in the file tree in the user profile. The list is
	 * in pre-order
	 * 
	 * @return a list of files that has been added locally
	 */
	public List<File> getFilesToAddRemotely() {
		List<File> addedLocally = new ArrayList<File>();

		for (String p : now.keySet()) {
			File file = new File(p);
			// Check that it was added locally and is not in the network (was not deleted remotely)
			if (!before.containsKey(p) || networkFiles.get(p) == null) {
				// Not in network, it has been added locally
				log.debug("File '{}' has been added locally during absence.", p);
				addedLocally.add(file);
			}
		}
		
		sortFilesPreorder(addedLocally);
		log.info("Found {} files/folders that have been added locally during absence.", addedLocally.size());
		return addedLocally;
	}
	
	/**
	 * Returns a list of files that are in the user profile but not on the local disk yet.
	 * 
	 * @return a list of files that has been added remotely
	 */
	public List<File> getFilesToAddLocally() {
		List<File> addedRemotely = new ArrayList<>();
		
		for (String p : networkFiles.keySet()) {
			// Check that it's not on the disk now
			if (!now.containsKey(p)) {
				// Check that is didn't exist before (wasn't deleted locally)
				if (!before.containsKey(p)) {
					log.debug("File '{}' has been added remotely during absence.", p);
					addedRemotely.add(networkFiles.get(p).getFile());
					
				// Check if it was deleted locally, but a modified version is on the network
				} else if (!HashUtil.compare(before.get(p), networkFiles.get(p).getMd5())) {
					log.debug("File '{}' was deleted locally during absence, but a modified version exists on the network.", p);
					addedRemotely.add(networkFiles.get(p).getFile());
				}
			}
		}
		
		sortFilesPreorder(addedRemotely);
		log.info("Found {} files/folders that have been added remotely during absence.", addedRemotely.size());
		return addedRemotely;
	}
	
	/**
	 * Returns a list of files that already existed but have been modified by the client while he was offline.
	 * 
	 * @return a list of files that has been updated locally
	 */
	public List<File> getFilesToUpdateRemotely() {
		List<File> updatedLocally = new ArrayList<File>();
		
		for (String p : now.keySet()) {
			if (!before.containsKey(p)) {
				// Wasn't here before, skip
				continue;
			}
			
			if (HashUtil.compare(before.get(p), now.get(p))) {
				// File hasn't changed, skip
				continue;
			}
			
			FileNode node = networkFiles.get(p);
			if (node == null || node.isFolder()) {
				// File isn't on the network or is a folder. Can't be updated
				continue;
			}
			
			File file = new File(p);
			
			// Check if modified 
			// Check before matches network hash. If not it was updated by another peer, and the network wins (don't update).
			if (HashUtil.compare(node.getMd5(), before.get(p)) && !HashUtil.compare(node.getMd5(), now.get(p))) {
				log.debug("File '{}' has been updated locally during absence.", p);
				updatedLocally.add(file);
			}
		}

		sortFilesPreorder(updatedLocally);
		log.info("Found {} files/folders that have been updated locally during absence.", updatedLocally.size());
		return updatedLocally;
	}
	
	/**
	 * Returns files that have been remotely modified while the client was offline
	 * 
	 * @return a list of files that has been updated remotely
	 */
	public List<File> getFilesToUpdateLocally() {
		List<File> updatedRemotely = new ArrayList<>();

		for (String p : networkFiles.keySet()) {
			FileNode node = networkFiles.get(p);
			if (node.isFolder()) {
				// Can't modify folders
				continue;
			}
			
			// Check before, after and network hashes
			// Don't update if before & after hashes differ to avoid the loss of a local modification.
			if (HashUtil.compare(before.get(p), now.get(p)) && !HashUtil.compare(node.getMd5(), now.get(p))) {
				// Before and after hashes match and network differs. Update locally.
				log.debug("File '{}' has been updated remotely during absence.", p);
				updatedRemotely.add(node.getFile());
			}
		}
		
		log.info("Found {} files/folders that have been updated remotely during absence.", updatedRemotely.size());
		return updatedRemotely;
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
	public List<File> getRemoteVersionConflicts() {
		List<File> versionConflicts = new ArrayList<>();
		
		for (String p : networkFiles.keySet()) {
			FileNode node = networkFiles.get(p);
			if (node.isFolder()) {
				continue;
			}
			
			// Check that file on disk is different vs. network
			if (!HashUtil.compare(node.getMd5(), now.get(p))) {
				// Check for a local add
				if (!before.containsKey(p)) {
					log.debug("File '{}' was added during absense, but a different version exists on the network.", p);
					versionConflicts.add(node.getFile());
				
				// Check for local modify
				} else if (HashUtil.compare(now.get(p), before.get(p))) {
					log.debug("File '{}' was modified during absense, but a different version exists on the network.", p);
					versionConflicts.add(node.getFile());
					
				}
			}
		}
		
		log.info("Found {} local files have version conflicts with the network.", versionConflicts.size());
		return versionConflicts;
	}
	
	/**
	 * Sorts a list of files in pre-order style
	 * 
	 * @param deletedLocally
	 */
	private void sortFilesPreorder(List<File> fileList) {
		Collections.sort(fileList, new Comparator<File>() {

			@Override
			public int compare(File file1, File file2) {
				return file1.compareTo(file2);
			}
		});
	}

	/**
	 * Visit all files recursively and calculate the hash of the file. Folders are also added to the result.
	 * 
	 * @param root the root folder
	 * @return a map where the key is the relative file path to the root and the value is the hash
	 * @throws IOException if hashing fails
	 */
	public static Map<String, byte[]> getCurrentSyncState(File root) throws IOException {
		Map<String, byte[]> digest = new HashMap<String, byte[]>();
		Iterator<File> files = FileUtils.iterateFilesAndDirs(root, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
		while (files.hasNext()) {
			File file = files.next();
			if (file.equals(root)) {
				// skip root folder
				continue;
			}
			String path = FileUtil.relativize(root, file).toString();
			byte[] hash = HashUtil.hash(file);
			if (file.isDirectory()) {
				digest.put(path + "/", hash);
			} else {
				digest.put(path, hash);
			}
		}
		return digest;
	}
	
	private static File getSyncStatePersistenceFile() {
		return new File(Application.getInstallationDirectory(), "sync-state.ser");
	}
	
	public synchronized static void persistCurrentSyncState() throws IOException {
		persistSyncState(getCurrentSyncState(Application.getSyncRootDirectory()));
	}
	
	public synchronized static void persistSyncState(Map<String, byte[]> state) throws IOException {
		try (
			final FileOutputStream fout = new FileOutputStream(getSyncStatePersistenceFile());
			final ObjectOutputStream oout = new ObjectOutputStream(fout);
		) {
			oout.writeObject(state);
		} catch (IOException e) {
			log.error("Unable to persist serialized sync state.", e);
			throw e;
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized Map<String, byte[]> getPersistedSyncState() throws IOException, ClassNotFoundException {
		try (
			final FileInputStream fin = new FileInputStream(getSyncStatePersistenceFile());
			final ObjectInputStream oin = new ObjectInputStream(fin);
		) {
			return (Map<String, byte[]>) oin.readObject();
		} catch (IOException | ClassNotFoundException e) {
			log.error("Unable to read persisted sync state.", e);
			throw e;
		}
	}
}