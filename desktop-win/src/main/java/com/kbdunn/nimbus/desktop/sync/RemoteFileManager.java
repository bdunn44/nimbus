package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.util.ComparatorUtil;
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
				long start = System.nanoTime();
				try {
					if (syncFile.isDirectory()) {
						client.getFileSyncManager().createDirectory(syncFile);
					} else {
						client.getFileSyncManager().upload(syncFile, file);
					}
					SyncStateCache.instance().update(syncFile); // Only update the cache after processing
					log.info("Finished processing file upload in {}ms: {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), syncFile);
				} catch (Exception e) {
					SyncStateCache.instance().addError(syncFile);
					throw new NimbusException("Error encountered during upload of " + syncFile, e);
				} finally {
					Application.updateSyncStatus(); // Update task count
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
				long start = System.nanoTime();
				List<Exception> exceptions = new ArrayList<>();
				try {
					for (SyncFile syncFolder : syncFolders) {
						try {
							client.getFileSyncManager().createDirectory(syncFolder);
							SyncStateCache.instance().update(syncFolder); // Only update the cache after processing
						} catch (Exception e) {
							SyncStateCache.instance().addError(syncFolder);
							// Don't throw it here, finish processing the entire list
							exceptions.add(new NimbusException("Error encountered during remote folder creation of " + syncFolder, e));
						}
					}
					if (exceptions.size() > 0) {
						Exception e = new NimbusException(exceptions.size() + " folder creation error(s) occurred.", exceptions.get(0));
						for (Exception suppressed : exceptions) {
							e.addSuppressed(suppressed);
						}
						throw e;
					}
				} finally {
					Application.updateSyncStatus(); // Update task count - will be a little off (counts once for all folders)
					log.info("Finished processing remote folder creation in {}ms. {} folder(s) succeeded, {} failed", 
							TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), syncFolders.size()-exceptions.size(), exceptions.size());
				}
				
				return null;
			}
		};
	}

	public Callable<Void> createDeleteProcess(SyncFile syncFile) throws IllegalArgumentException {
		return new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				long start = System.nanoTime();
				try {
					client.getFileSyncManager().delete(syncFile);
					SyncStateCache.instance().delete(syncFile); // Only update the cache after processing
					log.info("Finished processing remote delete process in {}ms: {}", 
							TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), syncFile);
				} catch (Exception e) {
					// No error to add, it doesn't exist locally
					throw new NimbusException("Error encountered during remote file delete of " + syncFile, e);
				} finally {
					Application.updateSyncStatus(); // Update task count
				}
				return null;
			}
		};
	}

	public Callable<Void> createDownloadProcess(SyncFile syncFile) throws IllegalArgumentException {
		if (syncFile.isDirectory()) throw new IllegalArgumentException("Argument is a directory " + syncFile);
		return new Callable<Void>() {
		
			@Override
			public Void call() throws Exception {
				long start = System.nanoTime();
				try {
					File tmpfile = client.getFileSyncManager().download(syncFile);
					File tgtfile = DesktopSyncFileUtil.toFile(syncFile);
					if (!tgtfile.exists() || tgtfile.delete()) {
						FileUtils.moveFile(tmpfile, tgtfile);
					}
					log.info("Finished processing file download in {}ms: {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), syncFile);
					SyncStateCache.instance().update(tgtfile);
				} catch (Exception e) {
					SyncStateCache.instance().addError(syncFile);
					throw new NimbusException("Error encountered during file download of " + syncFile, e);
				} finally {
					Application.updateSyncStatus(); // Update task count
				}
				return null;
			}
		};
	}
	
	public Callable<Void> createCopyProcess(FileCopyEvent event) throws IllegalArgumentException {
		return new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				long start = System.nanoTime();
				try {
					client.getFileSyncManager().copy(event);
					SyncStateCache.instance().update(event.getDstFile()); // Only update the cache after processing
					log.info("Finished processing remote file copy in {}ms: {}", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), event);
				} catch (Exception e) {
					SyncStateCache.instance().addError(event.getDstFile());
					throw new NimbusException("Error encountered during remote file copy " + event, e);
				} finally {
					Application.updateSyncStatus(); // Update task count
				}
				return null;
			}
		};
	}

	public Callable<List<SyncFile>> createFileListProcess() {
		return new Callable<List<SyncFile>>() {
			
			@Override
			public List<SyncFile> call() throws Exception {
				long start = System.nanoTime();
				try {
					List<SyncFile> list = client.getFileSyncManager().getSyncFileList();
					// Correct cached errors that have been resolved
					SyncFile cached = null;
					for (SyncFile error : SyncStateCache.instance().getCurrentSyncErrors().values()) {
						SyncFile network = null;
						for (SyncFile netFile : list) {
							if (netFile.getPath().equals(error.getPath())) {
								network = netFile;
								break;
							}
						}
						if (network == null) continue;
						cached = SyncStateCache.instance().getCurrentSyncState().get(network.getPath());
						if (cached == null // File no longer exists
								|| ComparatorUtil.nullSafeStringComparator(network.getMd5(), cached.getMd5()) == 0) { // File is up-to-date
							log.info("Detected sync error that has been resolved: " + error);
							SyncStateCache.instance().removeError(error);
						}
					}
					log.info("Finished processing remote file list process in {}ms. {} file(s) retrieved.",
							TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start), list.size());
					return list;
				} catch (Exception e) {
					//log.error("Error encountered during remote file list operation", e);
					throw new NimbusException("Error encountered during remote file list operation", e);
				} finally {
					Application.updateSyncStatus(); // Update task count
				}
			}
		};
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
		//log.error("Push connection encountered an error", t);
		Application.updateSyncStatus();
		Application.showWarning("Error connecting to Nimbus!");
		Application.openSettingsWindow();
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
