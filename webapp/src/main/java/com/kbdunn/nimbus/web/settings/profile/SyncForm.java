package com.kbdunn.nimbus.web.settings.profile;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.component.FileContainerComboBox;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SyncForm extends VerticalLayout {

	private static final long serialVersionUID = 8893384669269072750L;
	
	private ProfileController controller;
	private FileContainerComboBox rootSelect;
	
	public SyncForm(ProfileController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		setSizeFull();
		setSpacing(true);
		
		Label title = new Label("File Synchronization");
		title.addStyleName(ValoTheme.LABEL_H3);
		addComponent(title);
		
		HorizontalLayout container = new HorizontalLayout();
		container.setSpacing(true);
		addComponent(container);
		
		container.addComponent(new Label("Select your sync root directory:"));
		
		rootSelect = new FileContainerComboBox(false);
		rootSelect.setNullSelectionAllowed(true);
		rootSelect.setInputPrompt("None Selected");
		container.addComponent(rootSelect);
		
		Label info = new Label();
		info.setSizeUndefined();
		info.setIcon(FontAwesome.QUESTION_CIRCLE);
		info.setDescription("You can choose one home directory to sync to your desktop.");
		info.addStyleName(ValoTheme.LABEL_SMALL);
		container.addComponent(info);
	}
	
	StorageDevice getRootSelection() {
		return (StorageDevice) rootSelect.getValue();
	}
	
	void refresh() {
		if (NimbusUI.getStorageService().getStorageDevicesAssignedToUser(controller.getCurrentUser()).isEmpty()) {
			setVisible(false);
		} else {
			rootSelect.refresh();
			final FileContainer root = NimbusUI.getStorageService().getSyncRootStorageDevice(controller.getCurrentUser());
			if (root == null) {
				rootSelect.select(rootSelect.getNullSelectionItemId());
			} else {
				rootSelect.select(root);
			}
		}
	}
}
