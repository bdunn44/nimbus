package com.kbdunn.nimbus.web.error;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.kbdunn.nimbus.web.NimbusUI;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ErrorView extends Panel implements View {
	
	private static final long serialVersionUID = -2078010383833120119L;
	public static final String NAME = "error";
	
	private Label main;
	private Label sub;
	private Label stack;
	private Throwable currentError;
	private boolean layoutBuilt = false;
	
	@Override
	public void enter(ViewChangeEvent event) {
		if (!layoutBuilt) buildLayout();
		
		// Set error based on fragment
		Error e = null; 
		try {
			e = Error.fromFragment(event.getParameters());
		} catch (IllegalArgumentException x) {
			e = Error.UNKNOWN;
		}
		
		main.setValue(e.getTitle());
		if (e == Error.UNCAUGHT_EXCEPTION) {
			sub.setValue(e.getUserMessage());
			if (currentError != null) {
				final StringWriter sw = new StringWriter();
				currentError.printStackTrace(new PrintWriter(sw));
				stack.setValue(sw.toString());
				stack.setVisible(true);
				currentError = null; // clear
			} else {
				stack.setVisible(false);
			}
		} else {
			sub.setValue(NimbusUI.getCurrentUser() == null || !NimbusUI.getCurrentUser().isAdministrator() 
					? e.getUserMessage() : e.getAdminMessage());
			stack.setVisible(false);
			stack.setValue("");
		}
	}
	
	public void setCurrentError(Throwable error) {
		this.currentError = error;
	}
	
	private void buildLayout() {
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		setContent(content);
		
		main = new Label();
		main.addStyleName(ValoTheme.LABEL_H1);
		main.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		sub = new Label();
		sub.addStyleName(ValoTheme.LABEL_LARGE);
		sub.setContentMode(ContentMode.HTML);
		stack = new Label();
		stack.setContentMode(ContentMode.PREFORMATTED);
		stack.addStyleName(ValoTheme.LABEL_FAILURE);
		stack.addStyleName(ValoTheme.LABEL_TINY);
		stack.setSizeUndefined();
		content.addComponent(main);
		content.addComponent(sub);
		content.addComponent(stack);
		
		layoutBuilt = true;
	}
}