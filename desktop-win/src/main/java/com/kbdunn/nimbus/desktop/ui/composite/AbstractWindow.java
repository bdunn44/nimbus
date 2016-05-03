package com.kbdunn.nimbus.desktop.ui.composite;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractWindow {

	private Display display;
	private Shell shell;
	private int shellStyle;
	
	public AbstractWindow(Display display, int shellStyle) {
		this.display = display;
		this.shellStyle = shellStyle;
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public Shell getShell() {
		return shell;
	}
	
	public boolean isDisposed() {
		return shell.isDisposed() ;
	}
	
	public void dispose() {
		shell.dispose();
	}
	
	public void open() {
		if (shell.isDisposed()) {
			buildWindow();
		}
		shell.open();
	}
	
	protected void buildWindow() {
		shell = new Shell(display, shellStyle);
	}
}
