package com.kbdunn.nimbus.web.header;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.dashboard.DashboardView;
import com.vaadin.server.Page;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class Authenticator implements ClickListener {

	private static final long serialVersionUID = 1996889169639048885L;
	private static final Logger log = LogManager.getLogger(Authenticator.class.getName());
	
	private LoginDialog loginDialog;
	
	public Authenticator(LoginDialog loginDialog) {
		this.loginDialog = loginDialog;
	}

	// Validate user login
	@Override
	public void buttonClick(ClickEvent event) {
		
		String username = loginDialog.getUsername();
		
		if (NimbusUI.getCurrent().isDevMode()) {
			username = (username == null || username.isEmpty()) ? "Bryson" : username;
		} else {
			if (!loginDialog.isValid()) return;
		}
		
		// Get user, validate password
		NimbusUser user = NimbusUI.getUserService().getUserByNameOrEmail(username); 
		if (user != null) {
			if (NimbusUI.getCurrent().isDevMode()
					|| NimbusUI.getUserService().validatePassword(loginDialog.getPassword(), user.getPasswordDigest())) {
				log.info("User '" + user.getName() + "' was authenticated");
				
				// Set session attribute, switch to authenticated view
				UI.getCurrent().getSession().setAttribute("user", user);
				NimbusUI.getCurrent().setAuthenticated(true);
				
				// If user has a temporary password (it is their first login after being invited), prompt to update it
				if (user.isPasswordTemporary()) {
					NimbusUI.getCurrent().getSettingsController().openProfileSettings();
					new Notification("Welcome to Nimbus! Please update your name and password").show(Page.getCurrent());
				} else {
					UI.getCurrent().getNavigator().navigateTo(DashboardView.NAME);
				}
				
				// Switch header from logged-out to logged-in view
				NimbusUI.getCurrent().refresh();
				loginDialog.close();
				return;
			} else {
				log.warn("Failed login attempt (wrong password) for user '" + user.getName() + "'");
				event.getButton().setComponentError(new UserError("Invalid username or password. Try again."));
				return;
			}
		}
		
		// Authentication failed. Try again
		log.warn("Failed login attempt (bad username) for user '" + loginDialog.getUsername() + "'");
		event.getButton().setComponentError(new UserError("Invalid username or password. Try again."));
	}
}
