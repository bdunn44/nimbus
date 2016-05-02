package com.kbdunn.nimbus.web.settings.users;

import org.apache.commons.lang.StringUtils;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.exception.EmailConflictException;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.exception.UsernameConflictException;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.email.EmailForm;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class InviteDialog implements ClickListener {

	private static final long serialVersionUID = 1L;

	private EmailForm emailForm;
	private NimbusUser user;
	private PopupWindow popup;
	
	public InviteDialog(NimbusUser user) {
		this.user = user;
	}
	
	public void refresh() {
		emailForm.setSubject("Join my personal cloud!");
		emailForm.setMessage(user.getName() + " invites you to use their personal cloud!");
		emailForm.refresh();
	}
	
	private void buildLayout() {
		emailForm = new EmailForm("", this);
		emailForm.enableUrlGuess();
		emailForm.addSendListener(this);
		popup = new PopupWindow("Send Invites to Your Cloud", emailForm);
		popup.hideButtons();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) {
			popup.close();
			return;
		}
		
		if (!emailForm.validateAndNotify()) return;
		
		String duplicates = "";
		
		for (String entry: emailForm.getRecipients()) {
			try {
				AsyncOperation op = NimbusUI.getAsyncService().sendInvitationEmail(null, NimbusUI.getCurrentUser(),
						emailForm.getSubject(), emailForm.getMessage(), emailForm.getNimbusUrl(), entry, true);
				NimbusUI.getCurrent().getTaskController().addTask(op);
			} catch (EmailConflictException|UsernameConflictException e) {
				duplicates += duplicates.isEmpty() ? entry : ", " + entry;
			} catch (NimbusException e) {
				UI.getCurrent().getNavigator().navigateTo(Error.EMAIL_CONFIGURATION.getPath());
			}
		}
		
		emailForm.setRecipientsString(duplicates);
		
		if (!duplicates.isEmpty()) {
			String message = StringUtils.countMatches(duplicates, ",") > 1 ? 
					"The remaining email addresses are" :
					"The remaining email address is";
			Notification.show(message + " already registered with this cloud", Notification.Type.WARNING_MESSAGE);
		} else {
			popup.close();
		}
	}
	
	void openPopup() {
		buildLayout();
		refresh();
		popup.open();
	}
}
