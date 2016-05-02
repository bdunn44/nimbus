package com.kbdunn.nimbus.web.share;

import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;

public class SetPasswordDialog extends VerticalLayout implements ClickListener {

	private static final long serialVersionUID = 1936204636656235363L;
	private static final String caption = "Set File Share Password";
	
	private ShareBlockEditor editor;
	private PasswordField enterPassword;
	private PasswordField confirmPassword;
	private PopupWindow popup;
	
	public SetPasswordDialog(ShareBlockEditor editor) {
		this.editor = editor;
		
		buildLayout();
	}
	
	public void showDialog() {
		clearData();
		popup = new PopupWindow(caption, this);
		popup.setSubmitCaption("Set Password");
		popup.addSubmitListener(this);
		popup.open();
		
		popup.addCancelListener(new ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				editor.notifySetPasswordCancelled();
				clearData();
				popup.close();
			}
		});
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		enterPassword = new PasswordField("Enter Password");
		confirmPassword = new PasswordField("Confirm Password");
		addComponent(enterPassword);
		addComponent(confirmPassword);
	}

	@Override
	public void buttonClick(ClickEvent event) {
		if (!enterPassword.getValue().equals(confirmPassword.getValue())) {
			confirmPassword.setComponentError(new UserError("The passwords don't match"));
			return;
		}
		
		editor.notifyPasswordSet(confirmPassword.getValue());
		clearData();
		popup.close();
	}
	
	private void clearData() {
		enterPassword.setValue("");
		confirmPassword.setValue("");
	}
}
