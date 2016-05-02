package com.kbdunn.nimbus.web.settings.users;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.settings.SettingsController;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;

public class UserSettingsController implements SettingsTabController {

	private static final Logger log = LogManager.getLogger(UserSettingsController.class.getName());
	
	private UserService userService;
	private StorageService driveService;
	private SettingsController controller;
	private UserSettingsTab tab;
	
	public UserSettingsController(SettingsController controller) {
		this.controller = controller;
		userService = NimbusUI.getUserService();
		driveService = NimbusUI.getStorageService();
		tab = new UserSettingsTab(this);
	}
	
	List<NimbusUser> getUsers() {
		List<NimbusUser> users = userService.getAllUsers();
		Collections.sort(users);
		return users;
	}
	
	@Override
	public SettingsTab getTab() {
		return tab;
	}
	
	public NimbusUser getCurrentUser() {
		return controller.getCurrentUser();
	}
	
	void updateAdminStatus(NimbusUser u, boolean isAdmin) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		log.info("Setting user " + u.getName() + "'s administrator status to " + isAdmin);
		u.setAdministrator(isAdmin);
		try {
			userService.save(u);
		} catch (UsernameConflictException | EmailConflictException
				| FileConflictException e) {
			log.error(e, e);
		}
	}
	
	void setUserDrives(NimbusUser user, List<StorageDevice> drives) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		log.info("Assigning " + drives.size() + " drive(s) to " + user.getName());
		if (driveService.setAssignedUserStorageDevices(user, drives)) {
			controller.fireHardDriveModificationEvent(this, user);
		}
	}
	
	/*void addUserDrive(NimbusUser user, HardDrive drive) {
		if (NimbusUI.getCurrentPropertiesService().isDemoMode()) return;
		log.info("Assigning drive " + drive.getPath() + " to user " + user.getName());
		driveService.assignDriveToUser(drive, user);
	}*/
	
	void deleteUser(NimbusUser user) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		log.info("Deleting user " + user);
		userService.delete(user);
		getTab().refresh();
	}
}
