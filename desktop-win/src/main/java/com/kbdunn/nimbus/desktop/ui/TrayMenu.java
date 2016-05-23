package com.kbdunn.nimbus.desktop.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.DesktopSyncManager;
import com.kbdunn.nimbus.desktop.sync.SyncPreferences;
import com.kbdunn.nimbus.desktop.ui.composite.SettingsWindow;

public class TrayMenu {

	private static final Logger log = LoggerFactory.getLogger(TrayMenu.class);
	private static final String PAUSE_SYNC_TEXT = "Pause Sync";
	private static final String RESUME_SYNC_TEXT = "Resume Sync";
	
	private Shell shell;
	private TrayItem trayItem;
	private Menu menu;
	private SettingsWindow settingsWindow;
	
	private MenuItem statusItem;
	private MenuItem syncControlItem;
	private MenuItem openSyncFolderItem;
	private MenuItem openWebAppItem;
	private MenuItem openSettingsItem;
	private MenuItem exitItem;
	
	public TrayMenu(Display display) {
		this.shell = new Shell(display);
		createTrayItem(display);
		createMenu();
	}
	
	private void createTrayItem(Display display) {
		final Tray tray = display.getSystemTray();
		if (tray == null) {
			throw new IllegalStateException("The system tray is not available");
		}
		
		// Create Tray Item
		trayItem = new TrayItem(tray, SWT.NONE);
		trayItem.setToolTipText("Nimbus Sync");
		trayItem.setImage(ApplicationResources.getIcon(display));
		// Display menu on click
		trayItem.addListener(SWT.MenuDetect, new Listener() {
			@Override
			public void handleEvent(Event event) { menu.setVisible(true); }
		});
		// Open settings on double click
		trayItem.addListener(SWT.DefaultSelection, new Listener() {
			@Override
			public void handleEvent(Event event) { onOpenSettingsClick(event); }
		});
	}
	
	private void createMenu() {
		// Create menu
		menu = new Menu(shell, SWT.POP_UP);
		
		// Disabled menu item for displaying sync status
		statusItem = new MenuItem(menu, SWT.NONE);
		statusItem.setEnabled(false);
		
		// Pause or Resume file synchronization
		syncControlItem = new MenuItem(menu, SWT.PUSH);
		syncControlItem.setText(PAUSE_SYNC_TEXT);
		syncControlItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) { onSyncControlClick(event); }
		});
		
		new MenuItem(menu, SWT.SEPARATOR); // Separator
		
		// Open the synchronized folder
		openSyncFolderItem = new MenuItem(menu, SWT.PUSH);
		openSyncFolderItem.setText("Open Nimbus Sync Folder");
		openSyncFolderItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) { onOpenSyncFolderClick(event); }
		});

		// Open the Nimbus webapp
		openWebAppItem = new MenuItem(menu, SWT.PUSH);
		openWebAppItem.setText("Open Nimbus Web App");
		openWebAppItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) { onOpenWebAppClick(event); }
		});

		new MenuItem(menu, SWT.SEPARATOR); // Separator
		
		// Open desktop app preferences window
		openSettingsItem = new MenuItem(menu, SWT.PUSH);
		openSettingsItem.setText("Settings");
		openSettingsItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) { onOpenSettingsClick(event); }
		});

		// Open desktop app preferences window
		exitItem = new MenuItem(menu, SWT.PUSH);
		exitItem.setText("Exit");
		exitItem.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) { onExitClick(event); }
		});
	}

	public void setStatus(DesktopSyncManager.Status status) {
		statusItem.setText(status.toString());
		if (status == DesktopSyncManager.Status.PAUSED || status == DesktopSyncManager.Status.CONNECTED) {
			syncControlItem.setText(RESUME_SYNC_TEXT);
		} else {
			syncControlItem.setText(PAUSE_SYNC_TEXT);
		}
		syncControlItem.setEnabled(status.isConnected());
	}
	
	public DesktopSyncManager.Status getSyncStatus() {
		return DesktopSyncManager.Status.fromString(statusItem.getText());
	}
	
	public boolean isDisposed() {
		return trayItem.isDisposed()
				&& shell.isDisposed()
				&& settingsWindow.isDisposed();
	}
	
	public void dispose() {
		trayItem.dispose();
		shell.dispose();
		settingsWindow.dispose();
	}
	
	public void showNotification(String content) {
		ToolTip tt = new ToolTip(shell, SWT.BALLOON | SWT.ICON_INFORMATION);
		tt.setText("Nimbus Sync");
		tt.setMessage(content);
		trayItem.setToolTip(tt);
		trayItem.setVisible(true);
		tt.setVisible(true);
	}
	
	private void onSyncControlClick(Event event) {
		if (syncControlItem.getText().equals(PAUSE_SYNC_TEXT)) {
			Application.pause();
		} else {
			Application.resume();
		}
	}
	
	private void onOpenSyncFolderClick(Event event) {
		// TODO Auto-generated method stub
		log.debug("Open synchronized folder");
		
	}

	private void onOpenWebAppClick(Event event) {
		String url = SyncPreferences.getEndpoint();
		log.info("Opening web app in browser (" + url + ")");
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI(url));
			} catch (IOException | URISyntaxException e) {
				log.error("Error opening web app in browser", e);
			}
		} else {
			try {
				Runtime.getRuntime().exec("xdg-open " + url);
			} catch (IOException e) {
				log.error("Error opening web app in browser", e);
			}
		}
	}
	
	private void onOpenSettingsClick(Event event) {
		settingsWindow = new SettingsWindow(shell.getDisplay());
		settingsWindow.open();
	}
	
	private void onExitClick(Event event) {
		Application.exit();
	}
}
