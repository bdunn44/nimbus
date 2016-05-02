package com.kbdunn.nimbus.web;

import com.kbdunn.nimbus.web.header.HeaderPanel;
import com.kbdunn.nimbus.web.tasks.Taskbar;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.VerticalLayout;

public class NimbusLayout extends VerticalLayout {

	private static final long serialVersionUID = 5870578510779988178L;
	private HeaderPanel header;
	private ComponentContainer pageContent;
	private Taskbar taskbar;
	private CssLayout loadingOverlay, hiddenLayout;
	
	public NimbusLayout() {
		setWidth("100%");
		pageContent = new VerticalLayout(); // Container for View content
		header = new HeaderPanel(); // Header for UI
		loadingOverlay = new CssLayout(); // Loading overlay
		loadingOverlay.addStyleName("v-app-loading");
		loadingOverlay.addStyleName("hidden");
		hiddenLayout = new CssLayout();
		hiddenLayout.addStyleName("hidden-layout");
	}
	
	void setTaskbar(Taskbar taskbar) {
		this.taskbar = taskbar;
	}
	
	ComponentContainer getContentContainer() {
		return pageContent;
	}
	
	HeaderPanel getHeaderPanel() {
		return header;
	}
	
	void showLoadingOverlay() {
		loadingOverlay.removeStyleName("hidden");
	}
	
	void hideLoadingOverlay() {
		loadingOverlay.addStyleName("hidden");
	}
	
	void addHiddenComponent(AbstractComponent component) {
		hiddenLayout.addComponent(component);
		component.setParent(hiddenLayout);
		component.attach();
	}
	
	void removeAllHiddenComponents() {
		hiddenLayout.removeAllComponents();
	}
	
	void removeHiddenComponent(AbstractComponent component) {
		hiddenLayout.removeComponent(component);
	}
	
	protected void refresh() {
		removeAllComponents();
		header.refresh();
		
		addComponent(loadingOverlay);
		
		// Add taskbar - CSS styles used to absolutely position
		if (taskbar != null) addComponent(taskbar);
		
		// Add Header Content
		addComponent(header);
		
		// Create content component container
		addComponent(pageContent);
		setComponentAlignment(pageContent, Alignment.TOP_CENTER);
		pageContent.addStyleName("nimbus-content");
		pageContent.setWidth("90%");
		pageContent.setHeight("100%");
		
		addComponent(hiddenLayout);
	}
}
