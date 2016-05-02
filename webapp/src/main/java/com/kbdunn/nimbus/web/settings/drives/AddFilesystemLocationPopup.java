package com.kbdunn.nimbus.web.settings.drives;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class AddFilesystemLocationPopup extends VerticalLayout implements ClickListener {
	
	private static final long serialVersionUID = -4192014354841101597L;
	
	private StorageController controller;
	private TextField name, path;
	private CheckBox autonomous;
	private PopupWindow popup;
	
	AddFilesystemLocationPopup(StorageController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		
		name = new TextField("Device Name");
		name.setSizeFull();
		name.setRequired(true);
		name.addValidator(new StringLengthValidator("Name must be 1 thru 25 characters", 1, 25, false));
		addComponent(name);
		
		path = new TextField("Filesystem Path");
		path.setRequired(true);
		path.setDescription("This is the location on the filesystem where user directories and files are stored.");
		path.setSizeFull();
		addComponent(path);
		
		autonomous = new CheckBox("Storage Device Autonomous?");
		autonomous.setDescription("This should be enabled if applications other than Nimbus can modify files here");
		addComponent(autonomous);
	}
	
	private void refresh() {
		path.setValue("");
		path.setComponentError(null);
		name.setValue("");
		name.setValidationVisible(false);
		autonomous.setValue(true);
	}
	
	public void showDialog() {
		refresh();
		popup = new PopupWindow("Add Filesystem Storage", this);
		popup.addSubmitListener(this);
		popup.open();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) {
			popup.close();
			return;
		}
		if (!controller.isValidStoragePath(path.getValue())) {
			path.setComponentError(new UserError("Path doesn't exist or is already in use"));
			return;
		}
		if (!name.isValid()) {
			name.setValidationVisible(true);
			return;
		}
		controller.createFilesystemLocation(name.getValue(), path.getValue(), autonomous.getValue());
		popup.close();
	}
}