package com.kbdunn.nimbus.web.header;

import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.server.UserError;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;

public class LoginDialog extends FormLayout {
	
	private static final long serialVersionUID = 5921992799241014192L;
	
	private TextField username;
	private PasswordField password;
	private PopupWindow popup;
	private static final String caption = "Login";
	
	public LoginDialog() {
		buildLayout();
	}
	
	public void showDialog() {
		popup = new PopupWindow(caption, this);
		popup.setSubmitCaption("Login");
		popup.addSubmitListener(new Authenticator(this));
		popup.open();
	}
	
	public void close() {
		popup.close();
	}
	
	private void buildLayout() {
		setSizeUndefined();
		//addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
		
		// Create fields and login button
		username = new TextField("Username/Email");
		password = new PasswordField("Password");
		
		addComponent(username);
		addComponent(password);
	}
	
	public boolean isValid() {
		// Clear any prior errors
		username.setComponentError(null);
		password.setComponentError(null);
		
		// Check for empty user
		if (username.getValue().isEmpty()) {
			username.setComponentError(new UserError("Please enter your username"));
			return false;
		}
		
		// Check for blank password
		if (password.getValue().isEmpty()) {
			password.setComponentError(new UserError("Please enter your password"));
			return false;
		}
		return true;
	}
	
	protected String getUsername() {
		return username.getValue();
	}
	
	protected String getPassword() {
		String pw = password.getValue();
		password.setValue("");
		return pw;
	}
}
