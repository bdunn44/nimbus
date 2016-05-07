package com.kbdunn.nimbus.desktop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Properties;

import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.sync.SyncManager;
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
	
	private TrayMenu trayMenu;
	private SyncManager syncManager;
	
	private Application() {
		display = new Display();
		appProperties = new Properties();
		try (final InputStream in = getClass().getResourceAsStream("/nimbus-desktop.properties")) {
			appProperties.load(in);
		} catch (IOException e) {
			log.error("Error reading application properties." , e);
		} 
	}
	
	public void launch() {
		log.info("Application started");
		
		// Create system tray item and menu
		syncManager = new SyncManager();
		trayMenu = new TrayMenu(display);
		try {
			Application.connect();
		} catch (UnknownHostException e) {
			log.error("Unable to connect", e);
		}
		//updateSyncStatus();
		
		// Event Loop
		while (!trayMenu.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		// Cleanup
		trayMenu.dispose();
		display.dispose();
		ApplicationResources.dispose();
		log.info("Application closed");
		System.exit(0);
	}
	
	public static SyncManager getSyncManager() {
		return instance.syncManager;
	}
	
	public static SyncManager.Status getSyncStatus() {
		synchronized(instance) {
			return instance.syncManager.getSyncStatus();
		}
	}
	
	public static void updateSyncStatus() {
		synchronized(instance) {
			instance.trayMenu.setStatus(instance.syncManager.getSyncStatus());
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
		instance.syncManager.pause();
		instance.syncManager.disconnect();
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
	
	public static Display getDisplay() {
		return instance.display;
	}
	
	public static File getInstallationDirectory() {
		return new File(instance.appProperties.getProperty("com.kbdunn.nimbus.desktop.installdir"));
	}
	
	public static File getSyncRootDirectory() {
		return new File(instance.appProperties.getProperty("com.kbdunn.nimbus.desktop.sync.rootdir"));
	}
}
