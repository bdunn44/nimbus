package com.kbdunn.nimbus.web.settings.users;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.email.EmailForm;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;

public class ResetPasswordDialog implements ClickListener {

	private static final long serialVersionUID = 1L;

	private EmailForm emailForm;
	private NimbusUser user, initiator;
	private PopupWindow popup;
	
	public ResetPasswordDialog(NimbusUser user, NimbusUser initiator) {
		this.user = user;
		this.initiator = initiator;
		buildLayout();
	}
	
	public void refresh() {
		emailForm.setRecipientsString(user.getEmail());
		emailForm.setSubject("Important! Password Reset");
		emailForm.setMessage("Your password has been reset by " + initiator.getName() + ".");
		emailForm.setRecipientsEditable(false);
		emailForm.setSubjectEditable(false);
		emailForm.setMessageEditable(false);
		
		emailForm.refresh();
	}
	
	private void buildLayout() {
		emailForm = new EmailForm("", this);
		emailForm.enableUrlGuess();
		emailForm.addSendListener(this);
		popup = new PopupWindow("Password Reset", emailForm);
		popup.hideButtons();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) {
			popup.close();
			return;
		}
		
		if (!emailForm.validateAndNotify()) return;
		
		AsyncOperation op;
		try {
			op = NimbusUI.getAsyncService().sendPasswordResetEmail(null, initiator, 
					emailForm.getSubject(), emailForm.getMessage(), emailForm.getNimbusUrl(), user, true);
			NimbusUI.getCurrent().getTaskController().addTask(op);
		} catch (NimbusException e) {
			UI.getCurrent().getNavigator().navigateTo(Error.EMAIL_CONFIGURATION.getPath());
		}
		
		popup.close();
	}
	
	void openPopup() {
		refresh();
		popup.open();
	}
}
