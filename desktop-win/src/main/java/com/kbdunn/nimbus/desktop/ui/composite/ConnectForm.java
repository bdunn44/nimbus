package com.kbdunn.nimbus.desktop.ui.composite;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.model.SyncCredentials;
import com.kbdunn.nimbus.desktop.sync.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.DesktopSyncManager.Status;
import com.kbdunn.nimbus.desktop.ui.fontawesome.FontAwesome;

public class ConnectForm extends Composite implements SelectionListener, KeyListener {
	
	private static final Logger log = LoggerFactory.getLogger(ConnectForm.class);
	
	private SelectionListener onConnect;
	private Text endpoint;
	private Text username;
	private Text apiToken;
	private Text hmacKey;
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
		
		GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		new Label(this, SWT.RIGHT).setText("Nimbus URL");
		endpoint = new Text(this, SWT.SINGLE | SWT.BORDER);
		endpoint.setLayoutData(textData);
		endpoint.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("Username");
		username = new Text(this, SWT.SINGLE | SWT.BORDER);
		username.setLayoutData(textData);
		username.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("API Token");
		apiToken = new Text(this, SWT.SINGLE | SWT.BORDER);
		apiToken.setLayoutData(textData);
		apiToken.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("API Passcode");
		hmacKey = new Text(this, SWT.SINGLE | SWT.PASSWORD | SWT.BORDER);
		hmacKey.setLayoutData(textData);
		hmacKey.addKeyListener(this);
		new Label(this, SWT.RIGHT).setText("Node Name");
		nodeName = new Text(this, SWT.SINGLE | SWT.BORDER);
		nodeName.setLayoutData(textData);
		nodeName.addKeyListener(this);
		
		connect = new Button(this, SWT.PUSH);
		connect.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1));
		connect.setText("Connect");
		connect.addSelectionListener(this);
		fd.setHeight(12);
		connect.setFont(new Font(getDisplay(), fd));
	}
	
	public void refresh() {
		if (this.isDisposed()) return;
		SyncCredentials creds = SyncPreferences.getCredentials();
		endpoint.setText(SyncPreferences.getEndpoint());
		username.setText(creds.getUsername());
		apiToken.setText(creds.getApiToken());
		hmacKey.setText(creds.getHmacKey());
		nodeName.setText(SyncPreferences.getNodeName());
		/*endpoint.setSize(100, 50);
		username.setSize(100, 50);
		apiToken.setSize(100, 50);
		hmacKey.setSize(100, 50);
		nodeName.setSize(100, 50);*/
		keyReleased(null);
		
		if (Application.getSyncStatus() == Status.CONNECTING) {
			onConnectAttempt();
			// Check again in 1s
			final ConnectForm instance = this;
			Application.asyncExec(() -> {
				try {
					if (instance.isDisposed()) return;
					getDisplay().syncExec(() -> { 
						refresh(); 
					});
				} catch (Exception e) {
					log.debug("Error encountered while waiting for connection attempt completion", e);
				}
			}, 1, TimeUnit.SECONDS);
		} else if (Application.getSyncStatus() == Status.CONNECTION_ERROR) {
			onConnectAttemptComplete(false, null, "Unable to connect");
		}
	}
	
	@Override
	public void widgetSelected(final SelectionEvent arg0) {
		// Save settings
		SyncPreferences.setCredentials(
				new SyncCredentials(username.getText(), apiToken.getText(), hmacKey.getText()));
		SyncPreferences.setEndpoint(endpoint.getText());
		SyncPreferences.setNodeName(nodeName.getText());
		
		// Check that we have everything we need
		// Will always be the case if user initiates this 
		if (requiredFieldsArePopulated()) {
			// Run connection process in new thread to avoid blocking
			Application.asyncExec(new ConnectRunnable(arg0));
		}
	}
	
	private boolean requiredFieldsArePopulated() {
		return !endpoint.getText().isEmpty() && !username.getText().isEmpty() && 
				!apiToken.getText().isEmpty() && !hmacKey.getText().isEmpty() && !nodeName.getText().isEmpty();
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
		connect.setEnabled(requiredFieldsArePopulated());
	}
	
	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// Do nothing
	}

	@Override
	public void keyPressed(KeyEvent arg0) {
		// Do nothing
	}
	
	private void onConnectAttempt() {
		if (this.isDisposed()) return;
		getDisplay().syncExec(() -> {
			// Disable form while connecting
			errorMessage.setVisible(false);
			errorMessage.pack();
			endpoint.setEnabled(false);
			username.setEnabled(false);
			apiToken.setEnabled(false);
			hmacKey.setEnabled(false);
			nodeName.setEnabled(false);
			connect.setEnabled(false);
			connect.setText("Connecting...");
			connect.setFocus();
			connect.pack();
			this.layout();
		});
	}
	
	private void onConnectAttemptComplete(boolean success, SelectionEvent successCallback, String error) {
		if (this.isDisposed()) return;
		getDisplay().syncExec(() -> {
			if (success) {
				// Always trigger callback
				onConnect.widgetSelected(successCallback);
			} 
			if (ConnectForm.this.isDisposed()) return;
			if (!success) {
				errorMessage.setText(FontAwesome.EXCLAMATION_TRIANGLE.hex() + " " + error);
				errorMessage.setVisible(true);
				getShell().layout(new Control[] { errorMessage });
			}
			
			// Enable form after connection attempt
			endpoint.setEnabled(true);
			endpoint.setFocus();
			username.setEnabled(true);
			apiToken.setEnabled(true);
			hmacKey.setEnabled(true);
			nodeName.setEnabled(true);
			connect.setEnabled(true);
			connect.setText("Connect");
			connect.pack();
			layout();
		});
	}
	
	public class ConnectRunnable implements Runnable {
		
		private SelectionEvent event;
		
		public ConnectRunnable(SelectionEvent event) {
			this.event = event;
		}
		
		@Override
		public void run() {
			onConnectAttempt();
			try {
				// Run the connect process in a background thread
				if (Application.connect()) {
					onConnectAttemptComplete(true, event,  null);
				} else {
					throw new Exception("Connection error. Try again.");
				}
			} catch (Exception e) {
				String msg = "";
				if (e instanceof UnknownHostException) {
					msg = "Unable to connect to URL.";
				} else {
					msg = e.getMessage();
				}
				
				onConnectAttemptComplete(false, event, msg);
			}
		}
	}
}
