package com.kbdunn.nimbus.web.settings.drives;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class DeviceSettingsPopup extends VerticalLayout implements ClickListener {
	
	private static final long serialVersionUID = -4192014354841101597L;
	
	private StorageController controller;
	private StorageDevice device;
	private TextField name;
	private CheckBox autonomous;
	//private Button testScan, testMount;
	private OptionGroup driveUsers;
	private PopupWindow popup;
	
	DeviceSettingsPopup(StorageController controller, StorageDevice device) {
		this.controller = controller;
		this.device = device;
		buildLayout();
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		
		name = new TextField("Device Name");
		name.setSizeFull();
		addComponent(name);
		
		autonomous = new CheckBox("Storage Device Autonomous?");
		autonomous.setDescription("This should be enabled if applications other than Nimbus can modify files here");
		addComponent(autonomous);
		
		driveUsers = new OptionGroup("Assigned Users");
		driveUsers.setDescription("Assign users to this storage device");
		driveUsers.addStyleName(NimbusTheme.OPTIONGROUP_ELLIPSIS_OVERFLOW);
		driveUsers.setMultiSelect(true);
		driveUsers.setNewItemsAllowed(false);
		driveUsers.setSizeFull(); 
		//driveUsers.setWidth("150px");
		addComponent(driveUsers);
	}
	
	private void refresh() {
		name.setValue(device.getName());
		autonomous.setValue(device.isAutonomous());
		
		final StorageService driveService = NimbusUI.getStorageService();
		final UserService userService = NimbusUI.getUserService();
		driveUsers.removeAllItems();
		for (NimbusUser user : userService.getAllUsers()) {
			driveUsers.addItem(user);
			driveUsers.setItemCaption(user, user.getName());
			driveUsers.setItemIcon(user, FontAwesome.USER);
		}
		for (NimbusUser assigned : driveService.getUsersAssignedToStorageDevice(device)) {
			driveUsers.select(assigned);
		}
		if (device instanceof HardDrive) {
			if (!((HardDrive) device).isConnected() || !((HardDrive) device).isMounted()) {
				driveUsers.setEnabled(false);
			}
		}
	}
	
	public void showDialog() {
		refresh();
		popup = new PopupWindow(device.getName() + " Settings", this);
		popup.addSubmitListener(this);
		popup.open();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void buttonClick(ClickEvent event) {
		controller.saveDeviceSettings(device, name.getValue(), 
				autonomous.getValue() != null && autonomous.getValue());
		
		Object value = driveUsers.getValue();
		List<NimbusUser> selected = null;
		if (value instanceof NimbusUser) { // Single selection
			selected = Collections.singletonList((NimbusUser) value); 
		} else if (value instanceof Collection<?>) {
			selected = new ArrayList<>((Collection<NimbusUser>) value);
		} else {
			selected = Collections.emptyList();
		}
		for (NimbusUser user : NimbusUI.getUserService().getAllUsers()) {
			if (selected.contains(user)) {
				controller.assignDeviceToUser(device, user);
			} else {
				controller.revokeDeviceFromUser(device, user);
			}
		}
		
		popup.close();
	}
}