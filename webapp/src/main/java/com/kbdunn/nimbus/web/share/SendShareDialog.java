package com.kbdunn.nimbus.web.share;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.FinishedListener;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.model.Email;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.email.EmailForm;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.popup.ConfirmDialog;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;

public class SendShareDialog implements ClickListener {
	
	private static final long serialVersionUID = 2739942524475265257L;
	
	private ShareController controller;
	private ShareBlock currentBlock;
	private EmailForm emailForm;
	private PopupWindow popup;
	private boolean layoutBuilt = false;
	
	public SendShareDialog(final ShareController controller) { 
		this.controller = controller;
	}
	
	private void buildLayout() {
		if (layoutBuilt) return;
		
		emailForm = new EmailForm("", this);
		emailForm.setSizeFull();
		emailForm.enableUrlGuess();
		emailForm.addSendListener(this);
		
		popup = new PopupWindow("Share Your Files", emailForm);
		popup.hideButtons();
		
		layoutBuilt = true;
	}
	
	public void setCurrentShareBlock(ShareBlock current) {
		this.currentBlock = current;
	}
	
	private void refresh() {
		emailForm.setSubject("Shared Files");
		emailForm.setMessage("Hey, I've shared some files with you on my personal cloud. Click the link below to take a look!");
		emailForm.refresh();
	}
	
	void openPopup() {
		buildLayout();
		refresh();
		popup.open();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (!emailForm.validateAndNotify()) return;
		
		if (NimbusUI.getPropertiesService().isDemoMode()) {
			popup.close();
		} else if (currentBlock.getPasswordDigest() != null) {
			PopupWindow confirm = new ConfirmDialog("Send Email?", 
					"This file share is password protected - don't forget to share the password!",
					"Are you ready to send this email?");
			confirm.addSubmitListener(e -> sendEmails());
			confirm.open();
		} else {
			sendEmails();
		}
	}
	
	private void sendEmails() {
		String body = "<html><body style=\"font-family: Arial;\">" 
				+ emailForm.getMessage()
				+ "<br />"
				+ "<p>---------------------------------------------------------"
				+ "<br/><bold>Nimbus</bold> file share information:<br/>"
				+ "---------------------------------------------------------</p>"
				+ ""
				+ "<p>Link: " + emailForm.getNimbusUrl() + "#!" + ExternalShareView.NAME +"/" + currentBlock.getToken()
				+ "<br />"
				+ ""
				+ "<p>Login to my personal cloud at: <a href=\"" + emailForm.getNimbusUrl() + "\">" + emailForm.getNimbusUrl() + "</a>"
				+ "</body></html>";
		
		for (String recipient: emailForm.getRecipients()) 
			sendEmail(recipient, body);
		
		popup.close();
	}
	
	private void sendEmail(final String recipient, final String body) {
		final Email email = new Email(NimbusUI.getCurrentUser(), recipient);
		email.setSubject(emailForm.getSubject());
		email.setBody(body);
		AsyncOperation op;
		try {
			op = NimbusUI.getAsyncService().sendEmail(null, email, false);
			op.addFinishedListener(new FinishedListener() {
				@Override
				public void operationFinished(AsyncOperation operation) {
					if (operation.succeeded()) {
						controller.addBlockRecipient(currentBlock, recipient);
					}
				}
			});
			NimbusUI.getCurrent().getTaskController().addAndStartTask(op);
		} catch (NimbusException e) {
			UI.getCurrent().getNavigator().navigateTo(Error.EMAIL_CONFIGURATION.getPath());
		}
	}
}