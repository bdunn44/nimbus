package com.kbdunn.nimbus.web.settings.profile;

import org.vaadin.addon.oauthpopup.OAuthListener;
import org.vaadin.addon.oauthpopup.OAuthPopupButton;
import org.vaadin.addon.oauthpopup.buttons.GoogleButton;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.Token;
import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.email.EmailService;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EmailSettingsForm extends VerticalLayout {

	private static final long serialVersionUID = 7955932787157764033L;
	
	private static final String CUSTOM_SMTP_ITEM = "Other (SMTP)";
	
	private final ProfileController controller;
	private SMTPSettings savedSettings;
	
	private FormLayout smtpFormLayout;
	private HorizontalLayout buttonLayout;
	private ComboBox mailServices;
	private TextField email, smtpServer, smtpPort, sslPort, oAuthToken;
	private CheckBox sslEnabled;
	private PasswordField password;
	private OAuthPopupButton oAuthAuthenticateButton;
	private Button test;
	private Label testResult;
	
	public EmailSettingsForm(ProfileController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		setSizeFull();
		setSpacing(true);
		
		smtpFormLayout = new FormLayout();
		smtpFormLayout.setMargin(new MarginInfo(false, false, false, false));
		smtpFormLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
		addComponent(smtpFormLayout);

		Label title = new Label("Email Settings");
		title.addStyleName(ValoTheme.LABEL_H3);
		smtpFormLayout.addComponent(title);
		
		buttonLayout = new HorizontalLayout();
		buttonLayout.setSpacing(true);
		addComponent(buttonLayout);
		
		buildSmtpLayout();
	}
	
	void refresh() {
		savedSettings = controller.getSavedSmtpSettings();
		testResult.setCaption(null);
		testResult.setIcon(null);
		email.setValue(controller.getCurrentUser().getEmail());
		oAuthToken.setValue("");
		oAuthToken.setVisible(false);
		final EmailService oauth = EmailService.valueOf(controller.getCurrentUser().getEmailServiceName());
		final EmailService smtp = EmailService.valueOf(savedSettings);
		if (oauth != null || smtp != null) {
			mailServices.select(oauth == null ? smtp : oauth);
		} else if (savedSettings != null && !savedSettings.noAttributesSet()) {
			setSavedSmtpValues();
			mailServices.select(CUSTOM_SMTP_ITEM);
		} 
		if (!isSMTPService()) refreshOAuthButton();
	}
	
	String getEmail() {
		return email.getValue();
	}
	
	String getSmtpServer() {
		return smtpServer.getValue();
	}
	
	String getSmtpPort() {
		return smtpPort.getValue();
	}
	
	boolean isSslEnabled() {
		return sslEnabled.getValue();
	}
	
	String getSslPort() {
		return sslPort.getValue();
	}
	
	String getSmtpPassword() {
		return password.getValue();
	}
	
	SMTPSettings getCurrentSmtpSettings() {
		if (isSMTPService()) {
			return new SMTPSettings(NimbusUI.getCurrentUser().getId(), 
					getSmtpServer(), 
					getSmtpPort(), 
					getSslPort(), 
					getEmail(), 
					getSmtpPassword(), 
					isSslEnabled()
				);
		} else {
			return new SMTPSettings(NimbusUI.getCurrentUser().getId()); // Blank
		}
	}
	
	OAuthAPIService.Type getCurrentOAuthServiceType() {
		if (!isSMTPService()) {
			return ((EmailService) mailServices.getValue()).getOAuthServiceType();
		}
		return null;
	}
	
	String getOAuthToken() {
		return oAuthToken.getValue();
	}
	
	ServiceBuilder getScribeServiceBuilder() {
		return oAuthAuthenticateButton.getOAuthPopupConfig().createScribeServiceBuilder();
	}
	
	boolean isSMTPService() {
		final Object selected = mailServices.getValue();
		return selected == null 
				|| selected instanceof String
				|| ((EmailService) selected).getType() == EmailService.Type.SMTP;
	}
	
	boolean validate() {
		if (!email.isValid())
			return false;
		
		if (isSMTPService()) {
			return smtpServer.isValid()
					&& smtpPort.isValid()
					&& password.isValid()
					&& (sslEnabled.getValue() == false || sslPort.isValid());
		} else {
			return true;
		}
	}
	
	private void showCustomSmtp() {
		email.setEnabled(true);
		smtpServer.setVisible(true);
		smtpPort.setVisible(true);
		sslEnabled.setVisible(true);
		sslPort.setVisible(true);
		password.setVisible(true);
		oAuthAuthenticateButton.setVisible(false);
		oAuthToken.setVisible(false);
	}
	
	private void showSmtp(EmailService selected) {
		email.setEnabled(true);
		smtpServer.setValue(selected.getServer());
		smtpPort.setValue(selected.getPort());
		sslEnabled.setValue(selected.sslEnabled());
		sslPort.setValue(selected.getSslPort());
		
		smtpServer.setVisible(false);
		smtpPort.setVisible(false);
		sslEnabled.setVisible(false);
		sslPort.setVisible(false);
		password.setVisible(true);
		oAuthAuthenticateButton.setVisible(false);
		oAuthToken.setVisible(false);
		
		if (selected == EmailService.valueOf(savedSettings)) {
			password.setValue(savedSettings.getPassword());
		}
	}
	
	private void showOAuth(EmailService service) {
		email.setEnabled(false);
		smtpServer.setVisible(false);
		smtpPort.setVisible(false);
		sslEnabled.setVisible(false);
		sslPort.setVisible(false);
		password.setVisible(false);
		oAuthAuthenticateButton.setVisible(true);
		oAuthToken.setVisible(false); // Will display once button is clicked
	}
	
	private void refreshOAuthButton() {
		final EmailService service = EmailService.valueOf(getCurrentOAuthServiceType());
		if (service == null) return;
		OAuthPopupButton b = null;
		if (service == EmailService.GMAIL) {
			b = new GoogleButton(OAuthAPIService.Type.GOOGLE.getClientKey(), 
					OAuthAPIService.Type.GOOGLE.getClientSecret(), OAuthAPIService.Type.GOOGLE.getEmailScope());
			/*if (NimbusUI.isLocationAccessible()) {
				// Automatically set token and close window
				// this wasn't working - redirect_uri_mismatch error
				b.addOAuthListener(new NimbusOAuthListener(OAuthAPIService.Type.GOOGLE));
			} else {*/
				// Have to copy/paste token for GMAIL b/c non-public redirect URIs are not allowed
				// https://groups.google.com/forum/#!topic/oauth2-dev/egJ5ai0QI70
				b.getOAuthPopupConfig().setCallbackUrl("urn:ietf:wg:oauth:2.0:oob");
			//}
			oAuthToken.setValue("");
			b.addClickListener(e -> {
				oAuthToken.setVisible(true);
			});
		}
		if (b == null) throw new IllegalStateException("OAuth authentication button for " + service.getName() + " is not configured");
		b.setPopupWindowFeatures("resizable,width=500,height=520");
		b.setCaption("Authenticate with " + service.getName());
		buttonLayout.replaceComponent(oAuthAuthenticateButton, b);
		this.oAuthAuthenticateButton = b;
		
		if (NimbusUI.getOAuthService().getOAuthCredential(controller.getCurrentUser(), service.getOAuthServiceType()) != null) {
			b.setEnabled(false);
			b.setCaption("Authenticated with " + service.getName());
		}
	}
	
	private void setSavedSmtpValues() {
		smtpServer.setValue(savedSettings.getSmtpServer());
		smtpPort.setValue(savedSettings.getSmtpPort());
		sslEnabled.setValue(savedSettings.isSslEnabled());
		sslPort.setValue(savedSettings.getSslPort());
		password.setValue(savedSettings.getPassword());
	}
	
	private void setTestSuccessful(boolean success) {
		if (success) {
			testResult.setCaption("Success");
			testResult.setIcon(FontAwesome.CHECK);
			testResult.addStyleName(NimbusTheme.LABEL_SUCCESS_COLOR);
			testResult.removeStyleName(NimbusTheme.LABEL_ERROR_COLOR);
		} else {
			testResult.setCaption("Failure");
			testResult.setIcon(FontAwesome.WARNING);
			testResult.removeStyleName(NimbusTheme.LABEL_SUCCESS_COLOR);
			testResult.addStyleName(NimbusTheme.LABEL_ERROR_COLOR);
		}
	}
	
	private void buildSmtpLayout() {
		email = new TextField("Email");
		smtpServer = new TextField("SMTP Server");
		smtpPort = new TextField("SMTP Port");
		sslEnabled = new CheckBox("SSL Enabled?");
		sslPort = new TextField("SSL Port");
		password = new PasswordField("Password");
		oAuthAuthenticateButton = new GoogleButton(OAuthAPIService.Type.GOOGLE.getClientKey(), 
					OAuthAPIService.Type.GOOGLE.getClientSecret(), OAuthAPIService.Type.GOOGLE.getEmailScope()); // Dummy
		oAuthToken = new TextField("Paste Token Here");
		
		email.addValidator(new StringLengthValidator("Email must be 6 thru 50 characters long", 6, 50, false));
		email.addValidator(new EmailValidator("Enter a valid email"));
		smtpServer.addValidator(new StringLengthValidator("Provide the SMTP server", 1, 75, false));
		smtpPort.addValidator(new RegexpValidator("^[0-9]+$", "Provide a valid SMTP port"));
		sslPort.addValidator(new RegexpValidator("^[0-9]+$", "Provide a valid SSL port"));
		password.addValidator(new StringLengthValidator("Provide your password", 1, null, false));
		oAuthToken.setRequired(true);
		
		email.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		smtpServer.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		smtpPort.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		sslPort.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		password.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		sslEnabled.addStyleName(ValoTheme.CHECKBOX_SMALL);
		oAuthToken.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		
		// Populate combobox with list of mail providers
		mailServices = new ComboBox("Provider");
		mailServices.setTextInputAllowed(false);
		mailServices.setNullSelectionAllowed(false);

		// Add pre-configured SMTP services
		for (EmailService s : EmailService.values()) {
			mailServices.addItem(s);
			mailServices.setItemCaption(s, s.getName());
		}
		
		// Add an option to manually enter SMTP settings
		mailServices.addItem(CUSTOM_SMTP_ITEM);
		
		mailServices.addValueChangeListener(e -> {
			testResult.setCaption(null);
			testResult.setIcon(null);
			if (mailServices.getValue() == null) {
				return;
			} else if (mailServices.getValue().equals(CUSTOM_SMTP_ITEM)) {
				showCustomSmtp();
				return;
			}
			final EmailService service = (EmailService) mailServices.getValue();
			if (service.getType() == EmailService.Type.OAUTH2) {
				showOAuth(service);
				refreshOAuthButton();
			} else {
				showSmtp(service);
			}
		});
		
		smtpFormLayout.addComponent(email);
		smtpFormLayout.addComponent(mailServices);
		smtpFormLayout.addComponent(smtpServer);
		smtpFormLayout.addComponent(smtpPort);
		smtpFormLayout.addComponent(sslEnabled);
		smtpFormLayout.addComponent(sslPort);
		smtpFormLayout.addComponent(password);
		smtpFormLayout.addComponent(oAuthToken);
		
		test = new Button("Test");
		test.setDisableOnClick(true);
		test.addClickListener(e -> {
			if (NimbusUI.getPropertiesService().isDemoMode()) return;
			if (!validate()) {
				test.setEnabled(true);
				return;
			}
			boolean succeeded = false;
			if (isSMTPService()) {
				succeeded = controller.testSMTPSettings(getCurrentSmtpSettings());
			} else {
				final EmailService service = (EmailService) mailServices.getValue();
				succeeded = controller.testOAuthConnection(service.getOAuthServiceType());
			}
			test.setEnabled(true);
			setTestSuccessful(succeeded);
		});
		testResult = new Label();
		testResult.addStyleName(NimbusTheme.LABEL_CAPTION_ONLY);
		testResult.addStyleName(ValoTheme.LABEL_LARGE);
		buttonLayout.addComponent(oAuthAuthenticateButton);
		buttonLayout.addComponent(test);
		buttonLayout.addComponent(testResult);
		buttonLayout.setComponentAlignment(testResult, Alignment.MIDDLE_RIGHT);
		
		mailServices.select(EmailService.GMAIL);
	}

	public void hideOAuthTokenField() {
		oAuthToken.setValue("");
		oAuthToken.setVisible(false);
	}
	
	@SuppressWarnings("unused")
	private class NimbusOAuthListener implements OAuthListener {
		
		private OAuthAPIService.Type type;
		
		private NimbusOAuthListener(OAuthAPIService.Type type) {
			this.type = type;
		}
		
		@Override
		public void authSuccessful(Token token, boolean isOAuth20) {
			UI.getCurrent().access(() -> {
				controller.addOAuthCredential(token, type);
				refreshOAuthButton();
				email.setValue(controller.getOAuthEmailAddress(type));
				UI.getCurrent().push();
			});
		}
		
		@Override
		public void authDenied(String reason) {
			Notification.show("Authorization failed!", Notification.Type.ERROR_MESSAGE);
		}
	}
}
