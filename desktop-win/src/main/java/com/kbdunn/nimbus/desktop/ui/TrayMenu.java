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
import com.kbdunn.nimbus.desktop.ApplicationProperties;
import com.kbdunn.nimbus.desktop.sync.SyncManager;
import com.kbdunn.nimbus.desktop.sync.data.SyncPreferences;
import com.kbdunn.nimbus.desktop.ui.resources.ApplicationResources;

public class TrayMenu {

	private static final Logger log = LoggerFactory.getLogger(TrayMenu.class);
	private static final String PAUSE_SYNC_TEXT = "Pause";
	private static final String RESUME_SYNC_TEXT = "Resume";
	
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
	
	private boolean windowOpen = false;
	
	public TrayMenu(Display display) {
		this.shell = new Shell(display);
		shell.setImages(ApplicationResources.getIcons(display));
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
		trayItem.setImage(ApplicationResources.getIcons(display)[1]);
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

	public void setStatus(SyncManager.Status status, long taskCount, int errorCount) {
		if (statusItem.isDisposed() || syncControlItem.isDisposed()) return;
		if (status == SyncManager.Status.SYNCING) {
			statusItem.setText(status.toString()
					.replace("{}", (taskCount + " sync task" + (taskCount > 1 ? "s" : "")))
				);
		} else if (status == SyncManager.Status.SYNC_ERROR) {
			if (errorCount > 0) {
				statusItem.setText(status.toString()
						.replace("{}", (errorCount + " file" + (errorCount > 1 ? "s" : "")))
					);
			} else {
				statusItem.setText("Synchronization error");
			}
		} else {
			statusItem.setText(status.toString());
		}
		if (status == SyncManager.Status.PAUSED || status == SyncManager.Status.CONNECTED) {
			syncControlItem.setText(RESUME_SYNC_TEXT);
		} else {
			syncControlItem.setText(PAUSE_SYNC_TEXT);
		}
		syncControlItem.setEnabled(status.isConnected());
		if (settingsWindow != null) settingsWindow.refresh();
	}
	
	public boolean isDisposed() {
		return (trayItem == null ||trayItem.isDisposed())
				&& (shell == null || shell.isDisposed())
				&& (settingsWindow == null || settingsWindow.isDisposed());
	}
	
	public void dispose() {
		if (trayItem!= null) trayItem.dispose();
		if (shell != null) shell.dispose();
		if (settingsWindow != null) settingsWindow.dispose();
	}
	
	public void showNotification(String content) {
		showToolTip(content, SWT.ICON_INFORMATION);
	}
	
	public void showWarning(String content) {
		showToolTip(content, SWT.ICON_WARNING);
	}
	
	private void showToolTip(String content, int iconStyle) {
		ToolTip tt = new ToolTip(shell, SWT.BALLOON | iconStyle);
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
		try {
			Desktop.getDesktop().open(ApplicationProperties.instance().getSyncDirectory());
		} catch (IOException e) {
			log.error("Failed to open sync folder {}", ApplicationProperties.instance().getSyncDirectory(), e);
		}
	}
	
	private void onOpenWebAppClick(Event event) {
		String url = SyncPreferences.getUrl();
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		log.info("Opening web app in browser (" + url + ")");
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI(url));
			} catch (IOException | URISyntaxException e) {
				log.error("Error opening web app in browser", e);
			}
		} else {

			String os = System.getProperty("os.name").toLowerCase();
			String cmd = null;
			if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
				cmd = "xdg-open " + url;
			} else if (os.indexOf("mac" ) >= 0) {
				cmd = "open " + url;
			} else if (os.indexOf("win" ) >= 0) {
				cmd = "rundll32 url.dll,FileProtocolHandler " + url;
			}
			if (cmd != null) {
				try {
					Runtime.getRuntime().exec(cmd);
				} catch (IOException e) {
					log.error("Failed to execute command '{}'", cmd, e);
				}
			}
		}
	}
	
	private void onOpenSettingsClick(Event event) {
		openSettingsWindow();
	}
	
	public void openSettingsWindow() {
		if (!windowOpen) {
			settingsWindow = new SettingsWindow(shell.getDisplay());
			settingsWindow.getShell().addDisposeListener((e) -> {
				windowOpen = false;
			});
			settingsWindow.open();
			windowOpen = true;
		}
		settingsWindow.getShell().forceFocus();
	}
	
	private void onExitClick(Event event) {
		Application.exit();
	}
}
