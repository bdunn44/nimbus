package com.kbdunn.nimbus.desktop;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kbdunn.nimbus.common.util.TrackedScheduledThreadPoolExecutor;
import com.kbdunn.nimbus.desktop.sync.SyncManager;
import com.kbdunn.nimbus.desktop.sync.data.SyncStateCache;
import com.kbdunn.nimbus.desktop.ui.TrayMenu;
import com.kbdunn.nimbus.desktop.ui.resources.ApplicationResources;

public class Application {
	
	private static final Logger log = LoggerFactory.getLogger(Application.class);
	private static Application instance;
	
	private final Display display;
	private final TrackedScheduledThreadPoolExecutor backgroundExecutor;
	
	private TrayMenu trayMenu;
	private SyncManager syncManager;
	
	static Application initialize() {
		return instance = new Application();
	}
	
	public static void main(String[] args) {
		Launcher.main(args);
	}
	
	private Application() {
		display = new Display();
		backgroundExecutor = new TrackedScheduledThreadPoolExecutor(2,
				new ThreadFactoryBuilder().setNameFormat("App Background Thread #%d").build());
	}
	
	public void start() {
		log.info("Starting Nimbus desktop application");
		checkSyncRoot();
		log.info("Sync root directory is {}", ApplicationProperties.instance().getSyncDirectory());
		
		// Create system tray item and menu
		syncManager = new SyncManager();
		trayMenu = new TrayMenu(display);
		
		// Initialize sync cache
		SyncStateCache.initialize();
		
		// Start connection in background thread
		backgroundExecutor.submit(() -> {
			try {
				Application.connect();
			} catch (Exception e) {
				log.error("Error connecting to Nimbus on startup", e);
			}
		});
		
		// Periodically update status
		// This is needed because the "active task" count sometimes lags
		backgroundExecutor.scheduleAtFixedRate(() -> {
			Application.updateSyncStatus();
		}, 5, 10, TimeUnit.SECONDS);
		
		// Event Loop
		while (!trayMenu.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		log.info("Application closed");
		System.exit(0);
	}
	
	static void shutdown() {
		Application.pause();
		access(() -> {
			instance.trayMenu.dispose();
			instance.display.dispose();
			ApplicationResources.dispose();
		});
		instance.backgroundExecutor.shutdownNow();
	}
	
	public void checkSyncRoot() {
		File dir = ApplicationProperties.instance().getSyncDirectory();
		if (dir.isFile()) {
			log.warn("Sync directory is a regular file! Renaming file to " + dir.getAbsolutePath() + "-OLD");
			dir.renameTo(new File(dir.getAbsolutePath() + "-OLD"));
			dir = ApplicationProperties.instance().getSyncDirectory();
		} 
		if (!dir.exists()) {
			log.warn("Sync directory does not exist. Creating folder " + dir.getAbsolutePath());
			if (!dir.mkdir()) {
				log.error("Unable to create folder " + dir.getAbsolutePath(), new IOException());
			}
		}
	}
	
	public static SyncManager getSyncManager() {
		return instance.syncManager;
	}
	
	public static SyncManager.Status getSyncStatus() {
		synchronized(instance) {
			return instance.syncManager.getSyncStatus();
		}
	}
	
	public synchronized static void updateSyncStatus() {
		if (!instance.trayMenu.isDisposed()) {
			access(() -> {
				instance.trayMenu.setStatus(
						instance.syncManager.getSyncStatus(),
						instance.syncManager.getSyncTaskCount(),
						SyncStateCache.instance().getCurrentSyncErrors().size());
			});
		}
	}
	
	public static void showNotification(String notification) {
		access(() -> {
			instance.trayMenu.showNotification(notification);
		});
	}
	
	public static void showWarning(String warning) {
		access(() -> {
			instance.trayMenu.showWarning(warning);
		});
	}
	
	public static void openSettingsWindow() {
		if (!instance.trayMenu.isDisposed()) {
			access(() -> {
				instance.trayMenu.openSettingsWindow();
			});
		}
	}
	
	public static boolean connect() throws UnknownHostException {
		if (instance.syncManager.connect()) {
			resume();
			return true;
		}
		return false;
	}
	
	public static void disconnect() {
		if (instance.syncManager.isSyncActive()) {
			instance.syncManager.pause();
		}
		instance.syncManager.disconnect();
		instance.trayMenu.showNotification("Disconnected from Nimbus");
	}
	
	public static void pause() {
		synchronized(instance) {
			instance.syncManager.pause();
			// Tray menu will be disposed if called in shutdown hook
			if (!instance.trayMenu.isDisposed()) {
				access(() -> {
					instance.trayMenu.showNotification("File synchronization paused");
				});
			}
		}
	}
	
	public static void resume() {
		synchronized(instance) {
			instance.syncManager.resume();
			access(() -> {
				instance.trayMenu.showNotification("File synchronization resumed");
			});
		}
	}
	
	public static void triggerBatchSync() {
		synchronized(instance) {
			// Pause and resume, don't show notifications
			instance.syncManager.pause();
			instance.syncManager.resume();
		}
	}
	
	public static void exit() {
		if (instance == null) return;
		instance.trayMenu.dispose();
	}
	
	public static <T> Future<T> asyncExec(Callable<T> task) {
		return instance.backgroundExecutor.submit(task);
	}
	
	public static Future<?> asyncExec(Runnable command) {
		return instance.backgroundExecutor.submit(command);
	}
	
	public static Future<?> asyncExec(Runnable command, long delay, TimeUnit unit) {
		return instance.backgroundExecutor.schedule(command, delay, unit);
	}
	
	public static void access(Runnable command) {
		instance.display.asyncExec(command);
	}
	
	public static Display getDisplay() {
		return instance.display;
	}
}
