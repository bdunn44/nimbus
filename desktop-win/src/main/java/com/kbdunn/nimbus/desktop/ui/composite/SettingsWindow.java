package com.kbdunn.nimbus.desktop.ui.composite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.ui.ApplicationResources;

public class SettingsWindow extends AbstractWindow {
	
	public static int CLIENT_WIDTH = 400;
	public static int CLIENT_HEIGHT = 400;
	
	private Composite content;
	private StackLayout stackLayout;
	private ConnectForm connectForm;
	private StatusPane statusPane;
	
	public SettingsWindow(Display display) {
		super(display, SWT.CLOSE | SWT.TITLE | SWT.MIN); // Fixed size, can be minimized
		buildWindow();
		refresh();
	}
	
	@Override
	protected void buildWindow() {
		super.buildWindow();
		getShell().setImage(ApplicationResources.getIcon(getDisplay()));
		getShell().setText("Nimbus Sync");
		getShell().setSize(400, 400);
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				dispose();
			}
		});
		getShell().addShellListener(new ShellListener() {
			@Override
			public void shellActivated(ShellEvent event) {
				int trimx = getShell().getSize().x - getShell().getClientArea().width;
				int trimy = getShell().getSize().y - getShell().getClientArea().height;
				getShell().setSize(CLIENT_WIDTH+trimx, CLIENT_HEIGHT+trimy);
			}
			
			@Override public void shellClosed(ShellEvent event) {  }
			@Override public void shellDeactivated(ShellEvent event) {  }
			@Override public void shellDeiconified(ShellEvent event) {  }
			@Override public void shellIconified(ShellEvent event) {  }
		});
		
		RowLayout container = new RowLayout();
		container.center = true;
		container.fill = true;
		container.wrap = false;
		container.type = SWT.VERTICAL;
		container.spacing = 0;
		getShell().setLayout(container);
		
		Label header = new Label(getShell(), SWT.NONE);
		header.setImage(ApplicationResources.getLogo(getDisplay()));
		header.setBounds(0, 0, 400, 100);
		
		content = new Composite(getShell(), SWT.NONE);
		stackLayout = new StackLayout();
		content.setLayout(stackLayout);
		content.setBounds(0, 0, 400, 300);
		
		statusPane = new StatusPane(content, SWT.NONE, new SelectionListener() {

			@Override public void widgetDefaultSelected(SelectionEvent arg0) {  }

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				refresh();
			}
		});
		
		connectForm = new ConnectForm(content, SWT.NONE, new SelectionListener() {

			@Override public void widgetDefaultSelected(SelectionEvent arg0) {  }

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				refresh();
			}
		});
	}
	
	private void refresh() {
		connectForm.refresh();
		statusPane.refresh();
		stackLayout.topControl = (Application.getSyncStatus().isConnected()) ? statusPane : connectForm;
		content.layout();
	}
}
