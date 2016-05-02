package com.kbdunn.nimbus.web.settings.drives;

import java.util.Collections;
import java.util.List;

import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class StorageSettingsTab extends VerticalLayout implements SettingsTab, ClickListener {

	private static final long serialVersionUID = 3443624016756437757L;
	public static final String FRAGMENT = "drives";
	
	private StorageController controller;
	private HorizontalLayout drives;
	private Button refresh, settings, add;
	private StorageSettingsPopup settingsPopup;
	private AddFilesystemLocationPopup addPopup;
	
	StorageSettingsTab(StorageController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	@Override
	public String getName() {
		return "Storage Devices";
	}
	
	@Override
	public String getFragment() {
		return FRAGMENT;
	}

	@Override
	public boolean requiresAdmin() {
		return true;
	}
	
	@Override
	public void refresh() {
		NimbusUI.getStorageService().scanAndMountUSBHardDrives();
		drives.removeAllComponents();
		List<StorageDevice> dList = null;
		dList = NimbusUI.getStorageService().getAllStorageDevices();
		
		Collections.sort(dList);
		for (StorageDevice d : dList) {
			drives.addComponent(new StorageDeviceDisplay(d, controller));
		}
	}
	
	@Override
	public SettingsTabController getController() {
		return controller;
	}
	
	private void buildLayout() {
		addStyleName("drive-manager");
		
		setMargin(true);
		setSpacing(true);
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		addComponent(hl);
		
		Label header = new Label("Manage Nimbus Storage");
		header.addStyleName(ValoTheme.LABEL_H2);
		header.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		hl.addComponent(header);
		
		refresh = new Button(FontAwesome.REFRESH);
		refresh.setDescription("Refresh");
		refresh.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		refresh.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		refresh.addStyleName(ValoTheme.BUTTON_TINY);
		refresh.addClickListener(this);
		hl.addComponent(refresh);
		hl.setComponentAlignment(refresh, Alignment.BOTTOM_CENTER);
		
		settings = new Button(FontAwesome.COG);
		settings.setDescription("Settings");
		settings.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		settings.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		settings.addStyleName(ValoTheme.BUTTON_TINY);
		settings.addClickListener(this);
		hl.addComponent(settings);
		hl.setComponentAlignment(settings, Alignment.BOTTOM_CENTER);
		
		add = new Button(FontAwesome.PLUS_SQUARE_O);
		add.setDescription("Add a Storage Device");
		add.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		add.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		add.addStyleName(ValoTheme.BUTTON_TINY);
		add.addClickListener(this);
		hl.addComponent(add);
		hl.setComponentAlignment(add, Alignment.BOTTOM_CENTER);
		
		drives = new HorizontalLayout();
		drives.setSpacing(true);
		drives.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		addComponent(drives);
		
		settingsPopup = new StorageSettingsPopup(controller);
		addPopup = new AddFilesystemLocationPopup(controller);
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		Button clicked = event.getButton();
		if (refresh.equals(clicked)) {
			refresh();
		} else if (settings.equals(clicked)) {
			settingsPopup.showDialog();
		} else if (add.equals(clicked)) {
			addPopup.showDialog();
		}
	}
}