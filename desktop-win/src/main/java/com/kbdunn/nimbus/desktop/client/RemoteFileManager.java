package com.kbdunn.nimbus.desktop.client;

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
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.PushTransport;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class RemoteFileManager implements PushEventListener {

	private static final Logger log = LoggerFactory.getLogger(RemoteFileManager.class);
	
	private final NimbusClient client;
	private final List<FileEventListener> remoteEventListeners = new ArrayList<>();
	
	public RemoteFileManager(NimbusClient client) {
		this.client = client;
		client.addPushEventListener(this);
	}
	
	public Callable<Void> createAddProcess(SyncFile syncFile) throws IllegalArgumentException {
		if (syncFile.isDirectory()) throw new IllegalArgumentException("Argument is a directory " + syncFile);
		final File file = DesktopSyncFileUtil.toFile(syncFile);
		log.debug("Creating add process for file {}" + file);
		return new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				try {
					client.getFileSyncManager().upload(syncFile, file);
					log.debug("Add process executed for {}", syncFile);
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
				}
				log.debug("Create folder process executed for {} folders ", syncFolders.size());
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
				log.debug("Delete process executed for {}", syncFile);
			} catch (Exception e) {
				log.error("Error encountered during remote file delete", e);
			}
			return null;
		}};
	}

	public Callable<Void> createUpdateProcess(SyncFile syncFile) throws IllegalArgumentException {
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
	}

	public Callable<Void> createDownloadProcess(SyncFile syncFile) throws IllegalArgumentException {
		if (syncFile.isDirectory()) throw new IllegalArgumentException("Argument is a directory " + syncFile);
		return new Callable<Void>() {

		@Override
		public Void call() throws Exception {
			try {
				File tmpfile = client.getFileSyncManager().download(syncFile);
				log.debug("Download process executed for {}. Temp file is {}", syncFile, tmpfile);
				FileUtils.moveFile(tmpfile, DesktopSyncFileUtil.toFile(syncFile));
			} catch (Exception e) {
				log.error("Error encountered during file download", e);
			}
			return null;
		}};
	}

	public Callable<Void> createMoveProcess(SyncFile source, SyncFile target) throws IllegalArgumentException {
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
	}

	public Callable<List<SyncFile>> createFileListProcess() {
		return new Callable<List<SyncFile>>() {

		@Override
		public List<SyncFile> call() throws Exception {
			try {
				List<SyncFile> list = client.getFileSyncManager().getSyncFileList();
				log.debug("Get file list process executed");
				return list;
			} catch (Exception e) {
				log.error("Error encountered during remote file list operation", e);
				return Collections.emptyList();
			}
		}};
	}

	public void subscribeFileEvents(FileEventListener listener) throws TransportException {
		remoteEventListeners.add(listener);
		log.debug("Listening to pushed file events");
	}
	
	public void unsubscribeAll() {
		remoteEventListeners.clear();
	}

	@Override
	public void onClose(PushTransport transport) {
		log.info("Push connection closed");
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
		} else if (event instanceof FileAddEvent) {
			fireFileUpdateEvent((FileUpdateEvent) event);
		}
	}

	@Override
	public void onError(PushTransport transport, Throwable t) {
		log.error("Push connection encountered an error", t);
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
