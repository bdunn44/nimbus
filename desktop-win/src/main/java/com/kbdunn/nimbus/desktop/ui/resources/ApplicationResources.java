package com.kbdunn.nimbus.desktop.ui.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.ApplicationProperties;

public class ApplicationResources {

	private static final Logger log = LoggerFactory.getLogger(ApplicationResources.class);
	
	private static ApplicationResources instance;
	private Image icon, iconSm, logo;
	private Color grey;
	
	private ApplicationResources(Display display) { 
		try {
			icon = new Image(display, new FileInputStream(new File(ApplicationProperties.instance().getInstallDirectory(), "images/cloudsync-lg.png")));
			iconSm = new Image(display, new FileInputStream(new File(ApplicationProperties.instance().getInstallDirectory(), "images/cloudsync-sm.png")));
			logo = new Image(display, new FileInputStream(new File(ApplicationProperties.instance().getInstallDirectory(), "images/logo.png")));
		} catch (FileNotFoundException e) {
			log.error("Error loading application resources", e);
		}
		grey = display.getSystemColor(SWT.COLOR_DARK_GRAY);
	}
	
	public static void dispose() {
		if (instance == null) return;
		instance.icon.dispose();
		instance.iconSm.dispose();
		instance.logo.dispose();
		instance.grey.dispose();
	}
	
	private static void instantiate(Display display) {
		if (instance == null) instance = new ApplicationResources(display);
	}
	
	public static Image getIcon(Display display) {
		instantiate(display);
		return instance.icon;
	}
	
	public static Image getSmallIcon(Display display) {
		instantiate(display);
		return instance.iconSm;
	}
	
	public static Image getLogo(Display display) {
		instantiate(display);
		return instance.logo;
	}
	
	public static Color getGreyColor(Display display) {
		instantiate(display);
		return instance.grey;
	}
}
