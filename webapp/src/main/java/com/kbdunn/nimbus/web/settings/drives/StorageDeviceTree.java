package com.kbdunn.nimbus.web.settings.drives;

import java.util.List;

import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.bean.FileBean;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

public class StorageDeviceTree extends Tree {

	private static final long serialVersionUID = 1683175004779526333L;
	private HierarchicalContainer container;
	private static final String PROPERTY_NAME = "Name";
	private static final String PROPERTY_DETAILS = "Details";
	
	public StorageDeviceTree() {
		container = new HierarchicalContainer();
		container.addContainerProperty(PROPERTY_NAME, String.class, "");
		container.addContainerProperty(PROPERTY_DETAILS, VerticalLayout.class, null);
		
		setContainerDataSource(container);
		addStyleName("drive-tree");
		
		setItemCaptionPropertyId(FileBean.PROPERTY_NAME);
	}
	
	@SuppressWarnings("unchecked")
	public void setContents(List<HardDrive> contents) {
		removeAllItems();
		for (HardDrive drive : contents) {
			Item i = container.addItem(drive);
			String name = drive.getLabel();
			name = name == null ? drive.getPath() : name;
			i.getItemProperty(PROPERTY_NAME).setValue(name);
			setDriveDetails(drive);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void setDriveDetails(HardDrive drive) {
		setChildrenAllowed(drive, true);
		VerticalLayout driveDetails = new VerticalLayout();
		Label size = new Label(StringUtil.toHumanSizeString(drive.getSize()));
		Label type = new Label(drive.getType());
		Label path = new Label(drive.getPath());
		Label uuid = new Label(drive.getUuid());
		
		driveDetails.addComponent(size);
		driveDetails.addComponent(type);
		driveDetails.addComponent(path);
		driveDetails.addComponent(uuid);
		
		if (drive.getId() != null) { // drive has been added
			Label users = new Label(NimbusUI.getStorageService().getUsersAssignedToStorageDevice(drive).size() + " users assigned");
			//Label files = new Label("Stores " + drive.fileCount() + " Nimbus files");
			
			driveDetails.addComponent(users);
			//driveDetails.addComponent(files);
		}
		Item details = addItem(driveDetails);
		details.getItemProperty(PROPERTY_DETAILS).setValue(driveDetails);
	}
	
	protected void unselectAll() {
		for (Object o : this.getItemIds()) {
			unselect(o);
		}
	}
}