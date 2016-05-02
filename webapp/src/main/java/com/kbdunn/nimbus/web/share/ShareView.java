package com.kbdunn.nimbus.web.share;

import com.kbdunn.nimbus.web.files.FileManagerController;
import com.kbdunn.nimbus.web.files.FileManagerLayout;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.VerticalLayout;

public class ShareView extends VerticalLayout implements View {

	private static final long serialVersionUID = -5366542944784268226L;
	public static final String NAME = ShareListView.NAME + "/view";
	
	private FileManagerController controller;
	protected FileManagerLayout managerLayout;
	protected ShareBlockInfoPanel infoPanel;
	
	private boolean layoutBuilt = false;
	
	public ShareView(FileManagerController fileManagerController) {
		this.controller = fileManagerController;
		addStyleName("share-view");
		setSpacing(true);
		buildLayout();
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		controller.handleUri(NAME + "/" + event.getParameters());
	}
	
	public void refresh() {
		managerLayout.refresh();
		infoPanel.refresh();
	}
	
	public FileManagerLayout getFileManagerLayout() {
		return managerLayout;
	}
	
	public ShareBlockInfoPanel getInfoPanel() {
		return infoPanel;
	}
	
	private void buildLayout() {
		if (layoutBuilt) return;
		
		// Share Block info
		infoPanel = new ShareBlockInfoPanel();
		infoPanel.setWidth("100%");
		addComponent(infoPanel);
		
		// File management
		managerLayout = new FileManagerLayout(controller);
		managerLayout.buildLayout();
		addComponent(managerLayout);
		
		layoutBuilt = true;
	}
}