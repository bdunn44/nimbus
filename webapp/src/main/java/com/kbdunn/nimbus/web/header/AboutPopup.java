package com.kbdunn.nimbus.web.header;

import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class AboutPopup extends VerticalLayout {
	
	private static final long serialVersionUID = 1L;
	
	private PopupWindow popup;
	private NimbusLogo logo;
	private Label version, desc, link, eula;
	
	public AboutPopup() {
		addStyleName("about-popup");
		popup = new PopupWindow("About Nimbus", this);
		popup.hideButtons();
		
		logo = new NimbusLogo();
		logo.setSizeLg();
		addComponent(logo);
		setComponentAlignment(logo, Alignment.MIDDLE_CENTER);
		
		version = new Label(AboutPopup.class.getPackage().getImplementationVersion());
		version.addStyleName("nimbus-version");
		version.setWidthUndefined();
		version.addStyleName(ValoTheme.LABEL_LIGHT);
		addComponent(version);
		setComponentAlignment(version, Alignment.MIDDLE_CENTER);
		
		desc = new Label("<br/>The Nimbus personal cloud was created by Bryson Dunn", ContentMode.HTML);
		desc.addStyleName("nimbus-desc");
		desc.setWidthUndefined();
		addComponent(desc);
		setComponentAlignment(desc, Alignment.MIDDLE_CENTER);
		
		link = new Label("For more information visit us at <a href='http://cloudnimbus.org' target='_blank'>cloudnimbus.org</a>", ContentMode.HTML);
		link.addStyleName("nimbus-desc");
		link.setWidthUndefined();
		addComponent(link);
		setComponentAlignment(link, Alignment.MIDDLE_CENTER);
		
		eula = new Label("By installing and using Nimbus you agree to our <a href='http://cloudnimbus.org/license/EULA.txt' target='_blank'>End User License Agreement</a>", ContentMode.HTML);
		eula.addStyleName("nimbus-desc");
		eula.setWidthUndefined();
		addComponent(eula);
		setComponentAlignment(eula, Alignment.MIDDLE_CENTER);
	}
	
	void open() {
		popup.open();
	}
	
	void close() {
		popup.close();
	}
}
