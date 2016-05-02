package com.kbdunn.nimbus.web.landing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.NimbusUI;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.EmailValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class WelcomeView extends Panel implements View, ClickListener {

	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger(WelcomeView.class.getName());
	private static final String UN_PATTERN = "^[a-zA-Z0-9]+$"; // only letters and numbers
	
	public static final String NAME = "welcome";
	
	private VerticalLayout content;
	private TextField name;
	private TextField email;
	private PasswordField password;
	private PasswordField confirmPassword;
	private Button submit;

	public WelcomeView() {
		setSizeFull();
		buildContent();
	}
	
	private void buildContent() {
		content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		Label h1 = new Label("Welcome to Nimbus - Your Personal Cloud!");
		h1.addStyleName(ValoTheme.LABEL_H1);
		h1.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		Label body =  new Label("It looks like this is your first time logging on, so let's get started."
				+ " Enter the information below to create your account:");
		body.addStyleName(ValoTheme.LABEL_LARGE);
		
		name = new TextField("Username");
		name.addValidator(new StringLengthValidator("Name must be 4 thru 50 characters long", 4, 50, false));
		name.setRequired(true);
		name.setRequiredError("Please enter a username");
		name.setValidationVisible(false);
		name.setWidth("210px");
		
		email = new TextField("Email");
		email.addValidator(new StringLengthValidator("Email must be 6 thru 50 characters long", 6, 50, false));
		email.addValidator(new EmailValidator("Enter a valid email"));
		email.setRequired(true);
		email.setRequiredError("Please enter your email address");
		email.setValidationVisible(false);
		email.setWidth("210px");
		
		password = new PasswordField("Password");
		password.addValidator(new StringLengthValidator("Passwords must be at least 6 characters long", 
				6, null, false));
		password.setRequired(true);
		password.setRequiredError("Please enter a password");
		password.setValidationVisible(false);
		password.setWidth("210px");
		
		confirmPassword = new PasswordField("Confirm Password");
		confirmPassword.addValidator(new PasswordValidator(password));
		confirmPassword.setRequired(true);
		confirmPassword.setRequiredError("Please confirm your password");
		confirmPassword.setValidationVisible(false);
		confirmPassword.setWidth("210px");
		
		submit = new Button("Create Account", this);
		submit.setDisableOnClick(true);
		
		content.addComponent(h1);
		content.addComponent(body);
		
		HorizontalLayout form = new HorizontalLayout();
		form.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		form.setSpacing(true);
		form.addComponent(name);
		form.addComponent(email);
		form.addComponent(password);
		form.addComponent(confirmPassword);
		form.addComponent(submit);
		form.setComponentAlignment(submit, Alignment.BOTTOM_CENTER);
		submit.setClickShortcut(KeyCode.ENTER);
		
		content.addComponent(form);
		form.addStyleName("create-account");
		content.setComponentAlignment(form, Alignment.MIDDLE_CENTER);

		setContent(content);
	}
	
	private void showLogin() {
		content.removeAllComponents();
		Label h1 = new Label("Welcome to Nimbus - Your personal cloud!");
		h1.addStyleName(ValoTheme.LABEL_H1);
		h1.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		Label body =  new Label("Thanks! You're ready to go. Use the login link above to start using your cloud!");
		body.addStyleName(ValoTheme.LABEL_LARGE);
		
		content.addComponent(h1);
		content.addComponent(body);
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (!checkValidation(name) || !checkValidation(email) || !checkValidation(password) || !checkValidation(confirmPassword)) {
			submit.setEnabled(true);
			return;
		}
		
		log.info("Validating username and email");
		Pattern namePattern = Pattern.compile(UN_PATTERN);
		Matcher nameMatch = namePattern.matcher(name.getValue());
		if (!nameMatch.matches()) {
			Notification.show("Choose a username with only letters and numbers", Notification.Type.WARNING_MESSAGE);
			submit.setEnabled(true);
			return;
		}
		if (name.getValue().length() > 50) {
			Notification.show("Username must be less than 50 characters", Notification.Type.WARNING_MESSAGE);
			submit.setEnabled(true);
			return;
		}
		if (email.getValue().length() > 50) {
			Notification.show("Email must be less than 50 characters", Notification.Type.WARNING_MESSAGE);
			submit.setEnabled(true);
			return;
		}
		
		NimbusUI.getCurrent().showLoadingOverlay();
		
		// Run creation in a separate thread to show modal loading window
		new Thread(new Runnable() {
			@Override
			public void run() {
				log.info("Creating user");
				final NimbusUser user = new NimbusUser();
				user.setName(name.getValue());
				user.setEmail(email.getValue());
				user.setAdministrator(true);
				user.setOwner(true);
				log.info("Creating user's password hash");
				user.setPasswordDigest(NimbusUI.getUserService().getDigestedPassword(password.getValue()));
				
				UI.getCurrent().access(new Runnable() {
					
					@Override
					public void run() {
						try {
							if (NimbusUI.getUserService().save(user)) {
								showLogin();
							} else {
								Notification.show("There was an error creating your account!", Notification.Type.ERROR_MESSAGE);
							}
						} catch (UsernameConflictException
								| EmailConflictException
								| FileConflictException e) {
							log.error(e, e);
							Notification.show("There was an error creating your account!", Notification.Type.ERROR_MESSAGE);
						}
						NimbusUI.getCurrent().hideLoadingOverlay();
						UI.getCurrent().push();
					}
				});
			}
		}).start();
	}
	
	private boolean checkValidation(AbstractTextField field) {
		if (field.isValid()) return true;
		field.setValidationVisible(true);
		return false;
	}
	
	private class PasswordValidator implements Validator {
		
		private static final long serialVersionUID = -3857351747706323713L;
		private PasswordField comparison;

		PasswordValidator(PasswordField comparison) {
			this.comparison = comparison;
		}
		
		@Override
		public void validate(Object value) throws InvalidValueException {
			if (!((String) value).equals(comparison.getValue()))
				throw new InvalidValueException("Those passwords don't match!");
		}
	}
}
