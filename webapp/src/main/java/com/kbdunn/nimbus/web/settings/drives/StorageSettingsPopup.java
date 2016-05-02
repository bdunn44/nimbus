package com.kbdunn.nimbus.web.settings.drives;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.VerticalLayout;

public class StorageSettingsPopup extends VerticalLayout implements ClickListener {
	
	private static final long serialVersionUID = -4192014354841101597L;
	
	private StorageController controller;
	private CheckBox scan;
	//private Button testScan, testMount;
	private PopupWindow popup;
	
	StorageSettingsPopup(StorageController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		scan = new CheckBox("Automatically Scan and Mount USB Hard Drives?");
		addComponent(scan);
	}
	
	private void refresh() {
		scan.setValue(NimbusUI.getPropertiesService().isAutoScan());
	}
	
	public void showDialog() {
		refresh();
		popup = new PopupWindow("Change Drive Management Settings", this);
		popup.addSubmitListener(this);
		popup.open();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		controller.saveStorageSettings(
				scan.getValue() != null && scan.getValue());
		popup.close();
	}
}