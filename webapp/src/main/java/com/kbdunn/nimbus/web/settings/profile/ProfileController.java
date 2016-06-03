package com.kbdunn.nimbus.web.settings.profile;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.model.Token;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.common.security.OAuthAPIService.Type;
import com.kbdunn.nimbus.common.util.EmailUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.settings.SettingsController;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class ProfileController implements SettingsTabController {
	
	private static final Logger log = LogManager.getLogger(ProfileController.class.getName());
	protected static final String UN_PATTERN = "^[a-zA-Z0-9]+$"; // only letters and numbers
	protected static final String DUMMY_PW = "xxxxxxxxx";
	protected static final String GMAIL_SUFFIX = "@gmail.com";
	
	private SettingsController controller;
	private ProfileSettingsTab tab;
	
	public ProfileController(SettingsController controller) {
		this.controller = controller;
		tab = new ProfileSettingsTab(this);
	}
	
	@Override
	public SettingsTab getTab() {
		return tab;
	}
	
	void refreshUser() {
		controller.refreshUser();
	}
	
	NimbusUser getCurrentUser() {
		return controller.getCurrentUser();
	}
	
	SMTPSettings getSavedSmtpSettings() {
		return NimbusUI.getUserService().getSmtpSettings(getCurrentUser());
	}
	
	void saveProfileSettings() {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		getCurrentUser().setEmail(tab.getEmailForm().getEmail());
		getCurrentUser().setName(tab.getProfileForm().getNameValue());
		
		if (NimbusUI.getUserService().hasDuplicateName(getCurrentUser())) {
			Notification.show("A user with that name is already registered!", Notification.Type.WARNING_MESSAGE);
			tab.refresh();
			return;
		}
		if (NimbusUI.getUserService().hasDuplicateEmail(getCurrentUser())) { 
			Notification.show("A user with that email address is already registered!", Notification.Type.WARNING_MESSAGE);
			tab.refresh();
			return;
		}
		
		final String newPassword = tab.getProfileForm().getPasswordValue();
		
		if (!newPassword.isEmpty() && !newPassword.equals(DUMMY_PW)) {
			new ConfirmPasswordDialog(this).showDialog(newPassword);
			return;
		}
		
		finalizeSave();
	}
	
	void passwordConfirmed(String newPw) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		getCurrentUser().setPasswordDigest(NimbusUI.getUserService().getDigestedPassword(newPw));
		finalizeSave();
	}
	
	private void finalizeSave() {
		if (tab.getEmailForm().isSMTPService()) {
			getCurrentUser().setEmailServiceName(null); // Switch to SMTP
		} else {
			try {
				final OAuthAPIService.Type type = tab.getEmailForm().getCurrentOAuthServiceType();
				OAuthAPIService api = NimbusUI.getOAuthService().getOAuthAPIService(
						controller.getCurrentUser(), type
					);
				if (api == null) {
					// Check if the user has pasted a verifier token
					swapVerifierForAccessToken();
					api = NimbusUI.getOAuthService().getOAuthAPIService(
							controller.getCurrentUser(), type
						);
				}
				if (api != null) setUserEmailService(tab.getEmailForm().getCurrentOAuthServiceType());
			} catch (NimbusException e) {
				Notification.show("Error saving email settings!", Notification.Type.ERROR_MESSAGE);
				log.error(e, e);
				tab.refresh();
				return;
			}
		}
		
		// Update Profile
		try {
			SMTPSettings savedSettings = tab.getEmailForm().getCurrentSmtpSettings();
			NimbusUI.getUserService().updateSmtpSettings(savedSettings);
			if (!NimbusUI.getUserService().save(getCurrentUser())) {
				Notification.show("There was an error updating your profile!");
				log.warn("User profile save failed");
				return;
			}
		} catch (UsernameConflictException | EmailConflictException | FileConflictException e) {
			Notification.show("There was an error updating your profile!");
			log.error(e, e);
			if (e instanceof FileConflictException) {
				log.error("First conflict was " + ((FileConflictException) e).getConflicts().get(0).getSource());
			}
			return;
		}
		Notification.show("Your profile has been updated");
		UI.getCurrent().getSession().setAttribute("user", getCurrentUser());
		NimbusUI.getCurrent().refresh(); // Refresh UI to apply name change (updates controller.getUser() menu)
		refreshUser();
		tab.refresh();
	}

	// This currently only works with OAuth2.0, and is specifically needed for GMAIL b/c the verifier token must be manually pasted
	// This should be called to exchange the verifier token with an access token
	void swapVerifierForAccessToken() {
		final OAuthAPIService.Type type = tab.getEmailForm().getCurrentOAuthServiceType();
		final String verifier = tab.getEmailForm().getOAuthToken();
		if (type == null || verifier == null || verifier.isEmpty()) return; // Nothing to do
		Token accessToken = null;
		if (type == OAuthAPIService.Type.GOOGLE) {
			OAuth20Service scribe = GoogleApi20.instance().createService(tab.getEmailForm().getScribeOAuthConfig());
			accessToken = scribe.getAccessToken(verifier);
		}
		if (accessToken == null) {
			Notification.show("Error authenticating with " + type.name() + "!", Notification.Type.ERROR_MESSAGE);
			return;
		}
		addOAuthCredential(accessToken, type);
		tab.getEmailForm().hideOAuthTokenField();
	}
	
	void addOAuthCredential(Token token, OAuthAPIService.Type type) {
		final OAuthCredential cred = new OAuthCredential(controller.getCurrentUser(), token, type);
		cred.setScope(type.getEmailScope());
		NimbusUI.getOAuthService().save(cred);
	}
	
	private void setUserEmailService(OAuthAPIService.Type type) throws NimbusException {
		final OAuthAPIService service = NimbusUI.getOAuthService().getOAuthAPIService(getCurrentUser(), type);
		if (service == null) return; // nothing to do
		final String email = ((EmailTransport) service).getEmailAddress();
		getCurrentUser().setEmail(email);
		getCurrentUser().setEmailServiceName(type);
	}

	public void deleteOAuthCredential(Type oAuthServiceType) {
		NimbusUI.getOAuthService().delete(NimbusUI.getCurrentUser(), oAuthServiceType);
	}
	
	boolean testSMTPSettings(SMTPSettings settings) {
		boolean succeeded = false;
		if (tab.getEmailForm().isSMTPService()) {
			succeeded = EmailUtil.checkSMTPSettings(tab.getEmailForm().getCurrentSmtpSettings());
		} 
		return succeeded;
	}
	
	boolean testOAuthConnection(OAuthAPIService.Type type) {
		OAuthAPIService api = NimbusUI.getOAuthService().getOAuthAPIService(
				controller.getCurrentUser(), type
			);
		if (api == null) {
			// Check if the user has pasted a verifier token
			swapVerifierForAccessToken();
			api = NimbusUI.getOAuthService().getOAuthAPIService(
					controller.getCurrentUser(), type
				);
		}
		boolean succeeded = false;
		try {
			succeeded = api != null && api.getEmailTransport().testConnection();
		} catch (NimbusException ne) {
			log.error(ne, ne);
		}
		if (!succeeded) {
			deleteOAuthCredential(type);
			tab.getEmailForm().refresh();
		}
		return succeeded;
	}
	
	String getOAuthEmailAddress(OAuthAPIService.Type type) {
		final OAuthAPIService api = NimbusUI.getOAuthService().getOAuthAPIService(
				controller.getCurrentUser(), type
			);
		try {
			if (api != null) return api.getEmailTransport().getEmailAddress();
		} catch (NimbusException e) {
			log.error(e, e);
		}
		return null;
	}
	
	void generateNewTokens() {
		//TODO
	}
}
