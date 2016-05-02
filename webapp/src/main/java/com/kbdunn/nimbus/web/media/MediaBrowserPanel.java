package com.kbdunn.nimbus.web.media;

import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;

public class MediaBrowserPanel extends Panel {
	
	private static final long serialVersionUID = 1L;
	
	private MediaController controller;
	private HorizontalSplitPanel browserContent;
	private MediaGroupTable coarseBrowse;
	private Table fineBrowse;
	
	public MediaBrowserPanel(MediaController controller) {	
		this.controller = controller;
		addStyleName("media-browser-panel");
		
		buildLayout();
	}
	
	private void buildLayout() {
		browserContent = new HorizontalSplitPanel();
		browserContent.setSizeFull();
		browserContent.setSplitPosition(20f);
		setContent(browserContent);	
		
		coarseBrowse = new MediaGroupTable(controller);
		coarseBrowse.setSizeFull();
		browserContent.setFirstComponent(coarseBrowse);
	}
	
	protected void setItemTable(Table table) {
		fineBrowse = table;
		
		fineBrowse.setSizeFull();
		browserContent.setSecondComponent(fineBrowse);
	}
	
	void refreshItemTable() {
		if (!(fineBrowse instanceof Refreshable)) throw new IllegalStateException("Item table must implement Refreshable");
		((Refreshable) fineBrowse).refresh();
	}
	
	void refreshGroupTable() {
		if (!(coarseBrowse instanceof Refreshable)) throw new IllegalStateException("Media Group table must implement Refreshable");
		((Refreshable) coarseBrowse).refresh();
	}
	
	protected void displayGroupSelector(boolean displayed) {
		if (displayed) {
			browserContent.setSecondComponent(fineBrowse);
			setContent(browserContent);
		} else {
			setContent(fineBrowse);
		}
	}
	
	protected MediaGroupTable getGroupSelectTable() {
		return coarseBrowse;
	}
	
	protected Table getItemSelectTable() {
		return fineBrowse;
	}
}
