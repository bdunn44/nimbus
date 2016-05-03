package com.kbdunn.nimbus.desktop.composite;

import java.net.UnknownHostException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.SyncCredentials;
import com.kbdunn.nimbus.desktop.SyncPreferences;
import com.kbdunn.nimbus.desktop.fontawesome.FontAwesome;

public class ConnectForm extends Composite implements SelectionListener, KeyListener {
	
	private SelectionListener onConnect;
	private Text endpoint;
	private Text username;
	private Text password;
	private Text pin;
	private Text nodeName;
	private Label errorMessage;
	private Button connect;
	
	public ConnectForm(Composite parent, int style, SelectionListener onConnect) {
		super(parent, style);
		this.onConnect = onConnect;
		buildForm();
		refresh();
	}
	
	private void buildForm() {
		this.setSize(300, 150);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 10;
		//layout.marginHeight = (SettingsWindow.CLIENT_HEIGHT - 100 - this.getSize().y) / 2;
		layout.marginWidth = (SettingsWindow.CLIENT_WIDTH - this.getSize().x) / 2;
		this.setLayout(layout);
		
		Label header = new Label(this, SWT.CENTER);
		header.setText("Connect to Nimbus");
		FontData fd = header.getFont().getFontData()[0];
		fd.setHeight(16);
		//fd.setStyle(SWT.BOLD);
		header.setFont(new Font(getDisplay(), fd));
		header.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		
		GridData sepdata = new GridData(GridData.FILL_HORIZONTAL);
		sepdata.horizontalSpan = 2;
		sepdata.verticalAlignment = SWT.TOP;
		new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(sepdata);
		
		errorMessage = new Label(this, SWT.CENTER);
		errorMessage.setFont(FontAwesome.font(12));
		errorMessage.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		errorMessage.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		
		new Label(this, SWT.RIGHT).setText("Nimbus URL");
		endpoint = new Text(this, SWT.SINGLE | SWT.BORDER);
		endpoint.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		endpoint.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("Username");
		username = new Text(this, SWT.SINGLE | SWT.BORDER);
		username.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		username.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("Password");
		password = new Text(this, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		password.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		password.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("PIN");
		pin = new Text(this, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		pin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		pin.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("Node Name");
		nodeName = new Text(this, SWT.SINGLE | SWT.BORDER);
		nodeName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		nodeName.addKeyListener(this);
		
		connect = new Button(this, SWT.PUSH);
		connect.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
		connect.setText("Connect");
		connect.addSelectionListener(this);
		fd.setHeight(12);
		connect.setFont(new Font(getDisplay(), fd));
	}
	
	public void refresh() {
		errorMessage.setVisible(false);
		errorMessage.pack();
		SyncCredentials creds = SyncPreferences.getCredentials();
		endpoint.setText(SyncPreferences.getEndpoint());
		username.setText(creds.getUsername());
		password.setText(creds.getPassword());
		pin.setText(creds.getPin());
		nodeName.setText(SyncPreferences.getNodeName());
		keyReleased(null);
	}
	
	@Override
	public void widgetSelected(final SelectionEvent arg0) {
		// Save settings
		SyncPreferences.setCredentials(
				new SyncCredentials(username.getText(), password.getText(), pin.getText()));
		SyncPreferences.setEndpoint(endpoint.getText());
		SyncPreferences.setNodeName(nodeName.getText());

		// Disable form while connecting
		errorMessage.setVisible(false);
		errorMessage.pack();
		endpoint.setEnabled(false);
		username.setEnabled(false);
		password.setEnabled(false);
		pin.setEnabled(false);
		nodeName.setEnabled(false);
		connect.setEnabled(false);
		connect.setText("Connecting...");
		connect.setFocus();
		connect.pack();
		this.layout();
		
		// Run connection process in new thread to avoid blocking
		getDisplay().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				try {
					if (Application.connect()) {
						onConnect.widgetSelected(arg0);
					} else {
						throw new Exception("Connection error. Try again.");
					}
				} catch (Exception e) {
					if (e instanceof UnknownHostException) {
						errorMessage.setText(FontAwesome.EXCLAMATION_TRIANGLE.hex() + " " + "Unable to connect to URL.");
					} else {
						errorMessage.setText(FontAwesome.EXCLAMATION_TRIANGLE.hex() + " " + e.getMessage());
					}
					errorMessage.setVisible(true);
					getShell().layout(new Control[] { errorMessage });
				}
				
				// Enable form after connection attempt
				endpoint.setEnabled(true);
				endpoint.setFocus();
				username.setEnabled(true);
				password.setEnabled(true);
				pin.setEnabled(true);
				nodeName.setEnabled(true);
				connect.setEnabled(true);
				connect.setText("Connect");
				connect.pack();
				layout();
			}
		});
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		connect.setEnabled(
				!endpoint.getText().isEmpty() && !username.getText().isEmpty() && 
				!password.getText().isEmpty() && !pin.getText().isEmpty() && !nodeName.getText().isEmpty()
			);
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// Do nothing
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// Do nothing
	}
}
