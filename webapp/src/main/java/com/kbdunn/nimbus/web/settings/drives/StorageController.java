package com.kbdunn.nimbus.web.settings.drives;

import com.kbdunn.nimbus.common.model.FilesystemLocation;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.popup.ConfirmDialog;
import com.kbdunn.nimbus.web.settings.SettingsController;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;

public class StorageController implements SettingsTabController {
	
	private StorageService storageService;
	private SettingsController controller;
	private StorageSettingsTab tab;
	
	public StorageController(SettingsController controller) {
		this.controller = controller;
		this.storageService = NimbusUI.getStorageService();
		tab = new StorageSettingsTab(this);
	}
	
	@Override
	public SettingsTab getTab() {
		return tab;
	}
	
	void refreshView() {
		tab.refresh();
	}
	
	/*protected void activateUserDevice(final StorageDevice d) {
		if (NimbusUI.getCurrentPropertiesService().isDemoMode()) return;
		storageService.activateUserDrive(d, getCurrentUser());
		controller.fireHardDriveModificationEvent(this, getCurrentUser());
		tab.refresh();
	}*/
	
	/*NimbusProperties getNimbusProperties() {
		props = new NimbusProperties();
		return props;
	}*/
	
	void saveStorageSettings(boolean autoScan) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		NimbusUI.getPropertiesService().setAutoScan(autoScan);
	}
	
	NimbusUser getCurrentUser() {
		return controller.getCurrentUser();
	}
	
	void saveDeviceSettings(StorageDevice device, String name, boolean autonomous) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		device.setName(name);
		device.setAutonomous(autonomous);
		storageService.save(device);
		refreshView();
	}
	
	void deleteDevice(final StorageDevice device) {
		final ConfirmDialog cd = new ConfirmDialog("Delete Storage Device", 
				"Are you sure you want to delete '" + device.getName() + "'? "
						+ "The physical files will remain, but shared files, playlists, etc. will be deleted.");
		cd.addSubmitListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				if (NimbusUI.getPropertiesService().isDemoMode()) {
					cd.close();
					return;
				}
				storageService.delete(device);
				controller.fireHardDriveModificationEvent(this, getCurrentUser());
				refreshView();
				cd.close();
			}
		});
		cd.open();
	}
	
	boolean isValidStoragePath(String path) {
		if (path == null || path.isEmpty()) return false;
		return storageService.isValidNewFilesystemLocationPath(path);
	}
	
	void createFilesystemLocation(String name, String path, boolean isAutonomous) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		FilesystemLocation fsl = new FilesystemLocation();
		fsl.setName(name);
		fsl.setPath(path);
		fsl.setAutonomous(isAutonomous);
		if (!storageService.save(fsl)) {
			Notification.show("There was an error saving the new storage device!", Notification.Type.ERROR_MESSAGE);
		}
		controller.fireHardDriveModificationEvent(this, getCurrentUser());
		refreshView();
	}
	
	void assignDeviceToUser(StorageDevice device, NimbusUser user) {
		storageService.assignDriveToUser(device, user);
	}
	
	void revokeDeviceFromUser(StorageDevice device, NimbusUser user) {
		storageService.revokeDriveFromUser(device, user);
	}
}