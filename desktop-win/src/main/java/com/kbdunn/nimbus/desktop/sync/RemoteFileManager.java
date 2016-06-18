package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.NimbusClient;
import com.kbdunn.nimbus.api.client.listeners.FileEventListener;
import com.kbdunn.nimbus.api.client.listeners.PushEventListener;
import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.FileDeleteEvent;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.FileUpdateEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.client.model.SyncRootChangeEvent;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.PushTransport;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.data.SyncStateCache;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class RemoteFileManager implements PushEventListener {

	private static final Logger log = LoggerFactory.getLogger(RemoteFileManager.class);
	
	private final NimbusClient client;
	private final List<FileEventListener> remoteEventListeners = new ArrayList<>();
	
	public RemoteFileManager(NimbusClient client) {
		this.client = client;
		client.addPushEventListener(this);
	}
	
	public Callable<Void> createUploadProcess(SyncFile syncFile) throws IllegalArgumentException {
		final File file = DesktopSyncFileUtil.toFile(syncFile);
		return new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				try {
					if (syncFile.isDirectory()) {
						client.getFileSyncManager().createDirectory(syncFile);
					} else {
						client.getFileSyncManager().upload(syncFile, file);
					}
					SyncStateCache.instance().update(syncFile); // Only update the cache after processing
					Application.updateSyncStatus(); // Update task count
					log.trace("Remote add process finished {}", syncFile);
				} catch (Exception e) {
					log.error("Error encountered during file upload (add)", e);
					// TODO Error icon
				}
				return null;
			}
		};
	}

	public Callable<Void> createFolderAddProcess(List<SyncFile> syncFolders) throws IllegalArgumentException {
		for (SyncFile syncFolder : syncFolders) {
			if (!syncFolder.isDirectory()) throw new IllegalArgumentException("Sync file is not a directory: " + syncFolder);
		}
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				for (SyncFile syncFolder : syncFolders) {
					client.getFileSyncManager().createDirectory(syncFolder);
					SyncStateCache.instance().update(syncFolder); // Only update the cache after processing
				}
				Application.updateSyncStatus(); // Update task count - will be a little off (counts once for all folders)
				log.trace("Remote create folder process finished for {} folders ", syncFolders.size());
			} catch (Exception e) {
				log.error("Error encountered during remote folder creation", e);
				// TODO Error icon
			}
			return null;
		}};
	}

	public Callable<Void> createDeleteProcess(SyncFile syncFile) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				client.getFileSyncManager().delete(syncFile);
				SyncStateCache.instance().update(syncFile); // Only update the cache after processing
				Application.updateSyncStatus(); // Update task count
				log.trace("Remote delete process finished {}", syncFile);
			} catch (Exception e) {
				log.error("Error encountered during remote file delete", e);
			}
			return null;
		}};
	}

	/*public Callable<Void> createUpdateProcess(SyncFile syncFile) throws IllegalArgumentException {
		if (syncFile.isDirectory()) throw new IllegalArgumentException("Argument is a directory " + syncFile);
		final File file = DesktopSyncFileUtil.toFile(syncFile);
		log.debug("Creating update process for file {}" + file);
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				client.getFileSyncManager().upload(syncFile, file);
				log.debug("Update process executed for {}", syncFile);
			} catch (Exception e) {
				log.error("Error encountered during file upload (update)", e);
				// TODO Error icon
			}
			return null;
		}};
	}*/

	public Callable<Void> createDownloadProcess(SyncFile syncFile) throws IllegalArgumentException {
		if (syncFile.isDirectory()) throw new IllegalArgumentException("Argument is a directory " + syncFile);
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				File tmpfile = client.getFileSyncManager().download(syncFile);
				File tgtfile = DesktopSyncFileUtil.toFile(syncFile);
				log.trace("Download process finished for {}. Temp file is {}", syncFile, tmpfile);
				if (!tgtfile.exists() || tgtfile.delete()) {
					FileUtils.moveFile(tmpfile, tgtfile);
				}
				SyncStateCache.instance().update(tgtfile);
				Application.updateSyncStatus(); // Update task count
			} catch (Exception e) {
				log.error("Error encountered during file download", e);
			}
			return null;
		}};
	}

	/*public Callable<Void> createMoveProcess(SyncFile source, SyncFile target) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				client.getFileSyncManager().move(source, target);
				log.debug("Move process executed from {} to {}", source, target);
			} catch (Exception e) {
				log.error("Error encountered during remote file move", e);
				// TODO Error icon
			}
			return null;
		}};
	}*/
	
	public Callable<Void> createCopyProcess(FileCopyEvent event) throws IllegalArgumentException {
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				client.getFileSyncManager().copy(event);
				SyncStateCache.instance().update(event.getDstFile()); // Only update the cache after processing
				Application.updateSyncStatus(); // Update task count
				log.trace("Remote copy process finished {}", event);
			} catch (Exception e) {
				log.error("Error encountered during remote file copy", e);
				// TODO Error icon
			}
			return null;
		}};
	}

	public Callable<List<SyncFile>> createFileListProcess() {
		return new Callable<List<SyncFile>>() {

		@Override
		public List<SyncFile> call() throws Exception {
			try {
				List<SyncFile> list = client.getFileSyncManager().getSyncFileList();
				log.trace("Remote get file list process finished");
				Application.updateSyncStatus(); // Update task count
				return list;
			} catch (Exception e) {
				log.error("Error encountered during remote file list operation", e);
				return Collections.emptyList();
			}
		}};
	}

	public void subscribeFileEvents(FileEventListener listener) throws TransportException {
		remoteEventListeners.add(listener);
		log.debug("{} listener(s) watching pushed file events", remoteEventListeners.size());
	}
	
	public void unsubscribeAll() {
		remoteEventListeners.clear();
	}

	@Override
	public void onClose(PushTransport transport) {
		log.info("Push connection closed");
		Application.updateSyncStatus();
	}
	
	@Override
	public void onConnect(PushTransport transport) {
		log.info("Push connection opened");
	}

	@Override
	public void onFileEvent(PushTransport transport, FileEvent event) {
		if (event instanceof FileAddEvent) {
			fireFileAddEvent((FileAddEvent) event);
		} else if (event instanceof FileCopyEvent) {
			fireFileCopyEvent((FileCopyEvent) event);
		} else if (event instanceof FileDeleteEvent) {
			fireFileDeleteEvent((FileDeleteEvent) event);
		} else if (event instanceof FileMoveEvent) {
			fireFileMoveEvent((FileMoveEvent) event);
		} else if (event instanceof FileUpdateEvent) {
			fireFileUpdateEvent((FileUpdateEvent) event);
		}
	}

	@Override
	public void onSyncRootChangeEvent(PushTransport transport, SyncRootChangeEvent event) {
		// Pause and resume to force a batch sync cycle
		log.info("Sync Root Change Event recieved");
		Application.triggerBatchSync();
	}

	@Override
	public void onError(PushTransport transport, Throwable t) {
		log.error("Push connection encountered an error", t);
		Application.updateSyncStatus();
	}
	
	private void fireFileAddEvent(FileAddEvent event) {
		for (FileEventListener listener : remoteEventListeners) {
			listener.onFileAdd(event);
		}
	}
	
	private void fireFileCopyEvent(FileCopyEvent event) {
		for (FileEventListener listener : remoteEventListeners) {
			listener.onFileCopy(event);
		}
	}
	
	private void fireFileDeleteEvent(FileDeleteEvent event) {
		for (FileEventListener listener : remoteEventListeners) {
			listener.onFileDelete(event);
		}
	}
	
	private void fireFileMoveEvent(FileMoveEvent event) {
		for (FileEventListener listener : remoteEventListeners) {
			listener.onFileMove(event);
		}
	}
	
	private void fireFileUpdateEvent(FileUpdateEvent event) {
		for (FileEventListener listener : remoteEventListeners) {
			listener.onFileUpdate(event);
		}
	}
}
