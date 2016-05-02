package com.kbdunn.nimbus.web.settings.profile;

import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.PasswordField;

public class ConfirmPasswordDialog extends HorizontalLayout implements ClickListener {

	private static final long serialVersionUID = 4334425981873006178L;
	private static final String caption = "Confirm Password";
	
	private ProfileController controller;
	private String newPw;
	private PasswordField confirmPwField;
	private PopupWindow popup;
	
	public ConfirmPasswordDialog(ProfileController controller) {
		this.controller = controller;
		
		buildLayout();
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		
		confirmPwField = new PasswordField("Re-type Password");
		addComponent(confirmPwField);
	}
	
	public void showDialog(String newPw) {
		this.newPw = newPw;
		popup = new PopupWindow(caption, this);
		popup.setSubmitCaption("Confirm");
		popup.addSubmitListener(this);
		popup.open();
		
		popup.addCancelListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				controller.refreshUser();
				clearData();
			}
		});
		
		popup.addDetachListener(new DetachListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void detach(DetachEvent event) {
				controller.refreshUser();
				clearData();
			}
		});
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (!newPw.equals(confirmPwField.getValue())) {
			confirmPwField.setComponentError(new UserError("The passwords don't match"));
			return;
		}
		
		controller.passwordConfirmed(newPw);
		clearData();
		popup.close();
	}
	
	private void clearData() {
		newPw = null;
		confirmPwField.setValue("");
	}
}