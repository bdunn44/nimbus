package com.kbdunn.nimbus.web.settings.drives;

import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesomeLabel;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class StorageDeviceDisplay extends VerticalLayout implements ClickListener {

	private static final long serialVersionUID = 8486324353307448600L;
	
	private StorageService storageService;
	private StorageDevice device;
	private StorageController controller;
	private DeviceSettingsPopup settingsPopup;
	private FontAwesomeLabel iconLabel;
	private Label name, path, size;
	private Button settings, remove;
	
	public StorageDeviceDisplay(StorageDevice device, StorageController controller) {
		this.device = device;
		this.controller = controller;
		this.storageService = NimbusUI.getStorageService();
		this.settingsPopup = new DeviceSettingsPopup(controller, device);
		buildLayout();
		refresh();
	}
	
	private void buildLayout() {
		addStyleName("drive-display");
		addStyleName(ValoTheme.LAYOUT_CARD);
		setMargin(true);
		setWidth("250px");
		
		iconLabel = FontAwesome.HDD_O.getLabel().setSize4x();
		iconLabel.addStyleName(NimbusTheme.LABEL_CENTER_ALIGN);
		name = new Label();
		name.addStyleName(NimbusTheme.LABEL_ELLIPSIS_OVERFLOW);
		name.addStyleName(NimbusTheme.LABEL_CENTER_ALIGN);
		name.addStyleName(ValoTheme.LABEL_LARGE);
		name.addStyleName(ValoTheme.LABEL_COLORED);
		path = new Label();
		path.addStyleName(NimbusTheme.LABEL_ELLIPSIS_OVERFLOW);
		path.addStyleName(NimbusTheme.LABEL_CENTER_ALIGN);
		path.addStyleName(ValoTheme.LABEL_SMALL);
		size = new Label();
		size.addStyleName(NimbusTheme.LABEL_ELLIPSIS_OVERFLOW);
		size.addStyleName(NimbusTheme.LABEL_CENTER_ALIGN);
		size.addStyleName(ValoTheme.LABEL_SMALL);
		
		addComponent(iconLabel);
		addComponent(name);
		addComponent(path);
		addComponent(size);
		
		// Bottom button bar
		HorizontalLayout botBar = new HorizontalLayout();
		botBar.setSpacing(true);
		botBar.setSizeUndefined();
		addComponent(botBar);
		setComponentAlignment(botBar, Alignment.BOTTOM_CENTER);
		
		settings = new Button("Settings", this);
		settings.setIcon(FontAwesome.COG);
		settings.setDescription("Configure Storage Device");
		settings.addClickListener(this);
		settings.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		settings.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		botBar.addComponent(settings);
		botBar.setComponentAlignment(settings, Alignment.BOTTOM_CENTER);
		
		remove = new Button("Remove", this);
		remove.setIcon(FontAwesome.TIMES);
		remove.setDescription("Delete Storage Device");
		remove.addClickListener(this);
		remove.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		remove.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		botBar.addComponent(remove);
		botBar.setComponentAlignment(remove, Alignment.BOTTOM_CENTER);
	}
	
	public void refresh() {
		iconLabel.clearStack();
		name.setValue(device.getName());
		
		if (device instanceof HardDrive) {
			HardDrive hd = (HardDrive) device;
			if (!hd.isConnected()) {
				iconLabel.setDescription("Disconnected");
				iconLabel.stack(FontAwesome.TIMES).setSize2x().reverseStackSize();
			} else if (!hd.isMounted()) {
				iconLabel.setDescription("Unable to mount");
				iconLabel.stack(FontAwesome.EXCLAMATION).setSize2x().reverseStackSize();
			} else {
				iconLabel.setDescription("Connected");
				remove.setVisible(false); // can't remove a connected hard drive - TODO: possibly add unmount-ability
			}
			
			if (name.getValue() == null) name.setValue("No drive label");
			path.setValue(hd.getDevicePath() + " (" + hd.getType().toUpperCase() + ")");
			path.setDescription("Mounted to '" + hd.getPath() + "'");
			size.setValue(
					StringUtil.toHumanSizeString(hd.getUsed())
					+ " used of " 
					+ StringUtil.toHumanSizeString(hd.getSize())
				);
		} else { // Filesystem Location
			path.setValue(device.getPath());
			path.setDescription(device.getPath());
			size.setValue(
					StringUtil.toHumanSizeString(storageService.getUsedBytes(device)) + " used"
				);
			if (!storageService.storageDeviceIsAvailable(device)) {
				iconLabel.setDescription("Unavailable");
				iconLabel.stack(FontAwesome.TIMES).setSize2x().reverseStackSize();
			}
		}
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton().equals(settings)) {
			settingsPopup.showDialog();
		} else if (event.getButton().equals(remove)) {
			controller.deleteDevice(device);
		}
	}
}