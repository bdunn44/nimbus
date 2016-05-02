package com.kbdunn.nimbus.web.settings;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SettingsView extends Panel implements View {
	
	private static final long serialVersionUID = 6266615950195300306L;
	public static final String NAME = "settings";
	
	private SettingsController controller;
	private VerticalLayout content;
	private TabSheet tabsheet;
	private boolean layoutBuilt = false;
	
	SettingsView(SettingsController controller) {
		this.controller = controller;
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		if (!layoutBuilt) buildLayout();
		controller.parseFragment(event.getParameters());
	}
	
	/*void refresh() {
		Iterator<Component> i = tabsheet.iterator();
		SettingsTab tab = null;
		while (i.hasNext()) {
			tab = (SettingsTab) i.next();
			tab.refresh();
		}
	}*/
	
	void openFirstTab() {
		tabsheet.setSelectedTab(0);
		((SettingsTab) tabsheet.getSelectedTab()).refresh();
	}
	
	void openTab(SettingsTab tab) {
		if (tabsheet.getSelectedTab().equals(tab)) tab.refresh();
		else tabsheet.setSelectedTab(tab);
	}
	
	private void buildLayout() {
		addStyleName("settings-view");
		
		content = new VerticalLayout();
		content.setSpacing(true);
		content.setMargin(true);
		setContent(content);
		
		Label header = new Label("Settings");
		header.addStyleName(ValoTheme.LABEL_H1);
		header.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		content.addComponent(header);
		
		tabsheet = new TabSheet();
		tabsheet.addStyleName(ValoTheme.TABSHEET_FRAMED);
		tabsheet.setSizeFull();
		content.addComponent(tabsheet);
		
		for (SettingsTab tab : controller.getTabs()) {
			tabsheet.addTab(tab, tab.getName());
		}
		
		tabsheet.addSelectedTabChangeListener(controller);
		
		layoutBuilt = true;
	}
}
