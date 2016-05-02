package com.kbdunn.nimbus.web.landing;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class LandingView extends Panel implements View {

	private static final long serialVersionUID = 1L;
	public static final String NAME = "";

	public LandingView() {
		setSizeFull();
		VerticalLayout content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		Label h1 = new Label("Welcome to your personal cloud!");
		h1.addStyleName(ValoTheme.LABEL_H1);
		h1.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		Label body =  new Label("Login using the link above.");
		body.addStyleName(ValoTheme.LABEL_LARGE);
		
		content.addComponent(h1);
		content.addComponent(body);
		
		setContent(content);
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		
	}
}
