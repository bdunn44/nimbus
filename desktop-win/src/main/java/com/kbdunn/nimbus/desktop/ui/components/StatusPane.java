package com.kbdunn.nimbus.desktop.ui.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.data.SyncPreferences;
import com.kbdunn.nimbus.desktop.ui.SettingsWindow;
import com.kbdunn.nimbus.desktop.ui.resources.ApplicationResources;
import com.kbdunn.nimbus.desktop.ui.resources.FontAwesome;

public class StatusPane extends Composite implements SelectionListener {
	
	private static final String DISCONNECT_ACTION = "Disconnect";
	private static final String LOGIN_ACTION = "Login";
	
	private SelectionListener onLoginClick;
	private Label icon;
	private Label header;
	private Label detail;
	private Button action;
	
	public StatusPane(Composite parent, int style, SelectionListener onLoginClick) {
		super(parent, style);
		this.onLoginClick = onLoginClick;
		buildForm();
		refresh();
	}
	
	private void buildForm() {
		this.setSize(300, 40);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = false;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 10;
		layout.marginHeight = 50;//(SettingsWindow.CLIENT_HEIGHT - 200) / 2;
		layout.marginWidth = (SettingsWindow.CONTENT_WIDTH - this.getSize().x) / 2;
		this.setLayout(layout);
		
		icon = new Label(this, SWT.NONE);
		icon.setFont(FontAwesome.font(26));
		icon.setForeground(ApplicationResources.getGreyColor(getDisplay()));
		icon.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, false, false, 1, 2));
		
		header = new Label(this, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		FontData fd = header.getFont().getFontData()[0];
		fd.setHeight(12);
		fd.setStyle(SWT.BOLD);
		header.setFont(new Font(getDisplay(), fd));
		
		action = new Button(this, SWT.PUSH);
		action.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
		action.addSelectionListener(this);
		fd.setStyle(SWT.NONE);
		action.setFont(new Font(getDisplay(), fd));
		//action.setSize(100, 10);
		
		detail = new Label(this, SWT.NONE);
		detail.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	}
	
	public void refresh() {
		if (Application.getSyncStatus().isConnected()) {
			icon.setText(FontAwesome.LINK.hex());
			header.setText("Connected");
			detail.setText(SyncPreferences.getCredentials().getUsername() + " @ " + SyncPreferences.getUrl());
			action.setText(DISCONNECT_ACTION);
		} else {
			icon.setText(FontAwesome.UNLINK.hex());
			header.setText("Disconnected");
			detail.setText("Click to connect");
			action.setText(LOGIN_ACTION);
		}
		action.pack();
		this.layout();
	}
	
	@Override
	public void widgetSelected(SelectionEvent arg0) {
		// Disable action button while processing
		action.setEnabled(false);
		getDisplay().asyncExec(() -> {
			if (action.getText().equals(DISCONNECT_ACTION)) {
				Application.disconnect();
				refresh();
			} else {
				onLoginClick.widgetSelected(arg0);
			}
			action.setEnabled(true);
		});
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// Do nothing
	}
}
