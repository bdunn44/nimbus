package com.kbdunn.nimbus.web.settings.profile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public class UserProfileForm extends FormLayout {

	private static final long serialVersionUID = 7955932787157764033L;
	
	private final ProfileController controller;
	private TextField name;
	private PasswordField password;
	
	public UserProfileForm(ProfileController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		setSizeFull();
		setSpacing(true);
		setMargin(new MarginInfo(false, false, false, false));
		addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
		
		Label title = new Label("User Information");
		title.addStyleName(ValoTheme.LABEL_H3);
		addComponent(title);
		
		name = new TextField("Name");
		name.addValidator(new StringLengthValidator("Name must be between 4 and 50 characters long", 4, 50, false));
		
		password = new PasswordField("Password");
		password.addValidator(new StringLengthValidator("Passwords must be at least 6 characters long", 
				6, null, false));

		addComponent(name);
		addComponent(password);
	}
	
	void refresh() {
		name.setValue(controller.getCurrentUser().getName());
		name.focus();
		password.setValue(ProfileController.DUMMY_PW);
	}
	
	String getNameValue() {
		return name.getValue();
	}
	
	String getPasswordValue() {
		return password.getValue();
	}
	
	boolean validate() {
		if (!name.isValid() || !password.isValid())
			return false;
		
		Pattern namePattern = Pattern.compile(ProfileController.UN_PATTERN);
		Matcher nameMatch = namePattern.matcher(name.getValue());
		if (!nameMatch.matches()) {
			Notification.show("Choose a username with only letters and numbers", Notification.Type.WARNING_MESSAGE);
			refresh();
			return false;
		}
		
		return true;
	}
}
