package com.kbdunn.nimbus.web.tasks;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;

public class ToggleButton extends Button implements ClickListener {
	
	private static final long serialVersionUID = -5002710471106164488L;
	
	private TaskController controller;
	
	public ToggleButton(TaskController controller) {
		addStyleName("toggle-button");
		setCaption("Tasks");
		
		this.controller = controller;
		addClickListener(this);
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (controller.isExanded()) 
			controller.collapse();
		else
			controller.expand();
	}
}
