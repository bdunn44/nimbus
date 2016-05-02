package com.kbdunn.nimbus.web.settings.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.popup.ConfirmDialog;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesomeLabel;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.themes.ValoTheme;

public class NimbusUserLayout extends HorizontalLayout implements ClickListener {

	private static final long serialVersionUID = -1637277727943468322L;
	
	private StorageService driveService;
	private UserSettingsController controller;
	private NimbusUser user;
	private FontAwesomeLabel icon;
	private Label username;
	//private TextField quota;
	//private ComboBox uom;
	private OptionGroup userDrives;
	private CheckBox admin;
	private Button resetPassword, delete;
	
	NimbusUserLayout(UserSettingsController controller, NimbusUser user) {
		this.controller = controller;
		this.user = user;
		driveService = NimbusUI.getStorageService();
		buildLayout();
	}
	
	private void buildLayout() {
		setSpacing(true);
		addStyleName("nimbus-user-layout");
		
		HorizontalLayout leftLayout = new HorizontalLayout();
		leftLayout.addStyleName("left-layout");
		leftLayout.setSpacing(true);
		addComponent(leftLayout);
		setComponentAlignment(leftLayout, Alignment.MIDDLE_LEFT);
		
		icon = FontAwesome.USER.getLabel().stack(FontAwesome.SQUARE_O).setSize2x();
		leftLayout.addComponent(icon);
		leftLayout.setComponentAlignment(icon, Alignment.MIDDLE_CENTER);
		
		String uValue = "<span class='name'>" + user.getName() + "</span>";
		if (user.isOwner() || !user.getName().equals(user.getEmail())) {
			uValue += "<span class='desc'>(" + (user.isOwner() ? "Owner" : user.getEmail()) + ")</span>";
		}
		username = new Label(uValue, ContentMode.HTML);
		username.setWidth("275px");
		username.addStyleName("username-label");
		username.setDescription(user.getEmail());
		leftLayout.addComponent(username);
		leftLayout.setComponentAlignment(username, Alignment.MIDDLE_LEFT);
		
		HorizontalLayout rightLayout = new HorizontalLayout();
		rightLayout.setSpacing(true);
		addComponent(rightLayout);
		setComponentAlignment(rightLayout, Alignment.MIDDLE_RIGHT);
		
		admin = new CheckBox("Admin?", user.isAdministrator());
		admin.setDescription("Administrators can manage hard drives and other users");
		rightLayout.addComponent(admin);
		rightLayout.setComponentAlignment(admin, Alignment.MIDDLE_RIGHT);
		if (user.isOwner() || !controller.getCurrentUser().isOwner()) {
			admin.setEnabled(false);
		} else {
			admin.addValueChangeListener(new ValueChangeListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void valueChange(ValueChangeEvent event) {
					controller.updateAdminStatus(user, (admin.getValue() == null ? false : admin.getValue()));
				}
			});
		}
		
		resetPassword = new Button("Reset Password");
		resetPassword.addStyleName(ValoTheme.BUTTON_SMALL);
		resetPassword.addClickListener(this);
		rightLayout.addComponent(resetPassword);
		rightLayout.setComponentAlignment(resetPassword, Alignment.MIDDLE_RIGHT);
		if (user.isOwner()) resetPassword.setEnabled(false);
		
		userDrives = new OptionGroup();
		userDrives.setDescription("Select the hard drives this user can use");
		userDrives.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
		userDrives.addStyleName(NimbusTheme.OPTIONGROUP_ELLIPSIS_OVERFLOW);
		userDrives.setMultiSelect(true);
		userDrives.setNewItemsAllowed(false);
		//userDrives.setSizeUndefined(); // clear height
		userDrives.setWidth("150px");
		rightLayout.addComponent(userDrives);
		rightLayout.setComponentAlignment(userDrives, Alignment.MIDDLE_CENTER);
		
		for (StorageDevice d : driveService.getAllStorageDevices()) {
			if (driveService.storageDeviceIsAvailable(d)) {
				userDrives.addItem(d);
				userDrives.setItemCaption(d, d.getName());
				userDrives.setItemIcon(d, FontAwesome.HDD_O);
			}
		}
		//}
		
		// Select current user drives
		List<StorageDevice> currentDevices = driveService.getStorageDevicesAssignedToUser(user);
		for (StorageDevice d : currentDevices) {
			userDrives.select(d);
		}
		
		// Add value change listener
		userDrives.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;
			
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(ValueChangeEvent event) {
				Object value = userDrives.getValue();
				if (value instanceof StorageDevice) { // Single selection
					controller.setUserDrives(user, Collections.singletonList((StorageDevice) value)); 
				} else if (value instanceof Collection<?>) {
					controller.setUserDrives(user, new ArrayList<StorageDevice>((Collection<StorageDevice>) value));
				} else {
					controller.setUserDrives(user, Collections.<StorageDevice> emptyList());
				}
			}
		});
		
		delete = new Button(FontAwesome.TRASH_O);
		delete.setDescription("Delete User");
		delete.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		delete.addStyleName(ValoTheme.BUTTON_LARGE);
		rightLayout.addComponent(delete);
		rightLayout.setComponentAlignment(delete, Alignment.MIDDLE_RIGHT);
		if (user.isOwner()) {
			delete.setEnabled(false);
		} else {
			delete.addClickListener(this);
		}
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton().equals(resetPassword)) {
			new ResetPasswordDialog(user, controller.getCurrentUser()).openPopup();
		} else {
			final ConfirmDialog cd = new ConfirmDialog("Delete User", "Are you sure you want to delete this user?");
			cd.addSubmitListener(new ClickListener() {
				private static final long serialVersionUID = 1L;
				
				@Override
				public void buttonClick(ClickEvent event) {
					controller.deleteUser(user);
					cd.close();
				}
			});
			cd.open();
		}
	}
}
