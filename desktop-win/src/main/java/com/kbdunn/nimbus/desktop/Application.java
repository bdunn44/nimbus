package com.kbdunn.nimbus.desktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.sync.DesktopSyncManager;
import com.kbdunn.nimbus.desktop.sync.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.SyncStateCache;
import com.kbdunn.nimbus.desktop.ui.ApplicationResources;
import com.kbdunn.nimbus.desktop.ui.TrayMenu;

public class Application {
	
	private static final Logger log = LoggerFactory.getLogger(Application.class);
	private static Application instance;
	
	public static void main(String[] args) {
		try {
			if (instance != null)
				throw new IllegalStateException("Application instance exists");
			instance = new Application();
			instance.launch();
		} catch (Exception e) {
			log.error("Uncaught error!", e);
			System.exit(1);
		}
	}
	
	public static void exit() {
		if (instance == null) return;
		Application.pause();
		instance.trayMenu.dispose();
	}

	private final Display display;
	private final Properties appProperties;
	private final ScheduledExecutorService backgroundExecutor;
	
	private TrayMenu trayMenu;
	private DesktopSyncManager syncManager;
	
	private Application() {
		display = new Display();
		appProperties = new Properties();
		backgroundExecutor = Executors.newSingleThreadScheduledExecutor();
		try (final InputStream in = getClass().getResourceAsStream("/nimbus-desktop.properties")) {
			appProperties.load(in);
		} catch (IOException e) {
			log.error("Error reading application properties." , e);
		}
	}
	
	public void launch() {
		log.info("Starting Nimbus desktop application");
		checkSyncRoot();
		log.info("Sync root directory is {}", SyncPreferences.getSyncDirectory());
		
		// Create system tray item and menu
		syncManager = new DesktopSyncManager();
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
		
		// Event Loop
		while (!trayMenu.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		// Cleanup
		trayMenu.dispose();
		display.dispose();
		backgroundExecutor.shutdownNow();
		ApplicationResources.dispose();
		log.info("Application closed");
		System.exit(0);
	}
	
	public void checkSyncRoot() {
		File dir = SyncPreferences.getSyncDirectory();
		if (dir.isFile()) {
			log.warn("Sync directory is a regular file! Renaming file to " + dir.getAbsolutePath() + "-OLD");
			dir.renameTo(new File(dir.getAbsolutePath() + "-OLD"));
			dir = SyncPreferences.getSyncDirectory();
		} 
		if (!dir.exists()) {
			log.warn("Sync directory does not exist. Creating folder " + dir.getAbsolutePath());
			if (!dir.mkdir()) {
				log.error("Unable to create folder " + dir.getAbsolutePath(), new IOException());
			}
		}
	}
	
	public static DesktopSyncManager getSyncManager() {
		return instance.syncManager;
	}
	
	public static DesktopSyncManager.Status getSyncStatus() {
		synchronized(instance) {
			return instance.syncManager.getSyncStatus();
		}
	}
	
	public static void updateSyncStatus() {
		synchronized(instance) {
			Application.getDisplay().syncExec(() -> {
				instance.trayMenu.setStatus(instance.syncManager.getSyncStatus());
			});
		}
	}
	
	public static boolean connect() throws UnknownHostException {
		if (instance.syncManager.connect()) {
			//resume();
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
			instance.trayMenu.showNotification("File synchronization paused");
		}
	}
	
	public static void resume() {
		synchronized(instance) {
			instance.syncManager.resume();
			instance.trayMenu.showNotification("File synchronization resumed");
		}
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
	
	public static Display getDisplay() {
		return instance.display;
	}
	
	public static File getInstallationDirectory() {
		return new File(instance.appProperties.getProperty("com.kbdunn.nimbus.desktop.installdir"));
	}
}
