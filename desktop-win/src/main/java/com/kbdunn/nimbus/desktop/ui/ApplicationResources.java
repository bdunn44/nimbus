package com.kbdunn.nimbus.desktop.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ApplicationResources {

	private static ApplicationResources instance;
	private Image icon;
	private Image logo;
	private Color grey;
	
	private ApplicationResources(Display display) { 
		icon = new Image(display, 
				"C:\\Users\\kdunn\\Documents\\STS\\nimbus\\webapp\\src\\main\\webapp\\VAADIN\\themes\\nimbus\\favicon.ico");
		logo = new Image(display, 
				"C:\\Users\\kdunn\\Documents\\Nimbus\\nlogo.png");
		grey = display.getSystemColor(SWT.COLOR_DARK_GRAY);
	}
	
	public static void dispose() {
		if (instance == null) return;
		instance.icon.dispose();
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
	
	public static Image getLogo(Display display) {
		instantiate(display);
		return instance.logo;
	}
	
	public static Color getGreyColor(Display display) {
		instantiate(display);
		return instance.grey;
	}
}
