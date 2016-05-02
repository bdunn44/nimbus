package com.kbdunn.nimbus.web.header;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

public class NimbusLogo extends CssLayout {

	private static final long serialVersionUID = 5006208756001774610L;

	public NimbusLogo() {
		addStyleName("nimbus-logo");
		Label nimbusLogo = new Label("Nimbus");
		nimbusLogo.addStyleName("nimbus-text");
		Label cloud = FontAwesome.CLOUD.getLabel();
		cloud.addStyleName("nimbus-cloud");
		cloud.addStyleName(ValoTheme.LABEL_COLORED);
		addComponent(cloud);
		addComponent(nimbusLogo);
	}
	
	public void setSizeLg() {
		addStyleName("size-lg");
	}
}
