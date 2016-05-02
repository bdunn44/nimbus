package com.kbdunn.nimbus.web.email;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EmailForm extends VerticalLayout {
	
	private static final long serialVersionUID = -6470596893373652741L;
	private static final String EMAIL_PATTERN = "^([_A-Za-z0-9-\\+]+(?:\\.[_A-Za-z0-9-]+)*)@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
	
	private FormLayout formContent;
	private TextField nimbusAddress, subject;
	private TextArea recipients, message;
	private Button send;
	private boolean urlGuess = false;
	
	public EmailForm(String header, ClickListener submitListener) {
		//setSizeFull();
		setSizeUndefined();
		//setMargin(true);
		
		
		/*Label h2 = new Label(header);
		h2.addStyleName(ValoTheme.LABEL_H2);
		h2.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		addComponent(h2);*/
		
		formContent = new FormLayout();
		formContent.setMargin(true);
		formContent.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
		addComponent(formContent);
		
		buildEmailLayout();
		if (submitListener != null)
			send.addClickListener(submitListener);
		
		addAttachListener(new AttachListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void attach(AttachEvent event) {
				if (urlGuess) {
					nimbusAddress.setValue(NimbusUI.getUrlGuess());
				}
				refresh();
			}
		});
	}
	
	public String getNimbusUrl() {
		return nimbusAddress.getValue();
	}
	
	public String getSubject() {
		return subject.getValue();
	}
	
	public void setSubject(String subject) {
		this.subject.setValue(subject);
	}
	
	public String getMessage() {
		return message.getValue();
	}
	
	public void setMessage(String message) {
		this.message.setValue(message);
	}
	
	public String getRecipientsString() {
		return recipients.getValue();
	}
	
	public void setRecipientsString(String recipients) {
		this.recipients.setValue(recipients);
	}
	
	public void enableUrlGuess() {
		urlGuess = true;
	}
	
	public void addSendListener(ClickListener listener) {
		send.addClickListener(listener);
	}
	
	public void refresh() {
		if (urlGuess) {
			nimbusAddress.setValue(NimbusUI.getUrlGuess());
		}
	}
	
	public List<String> getRecipients() {
		List<String> recipients = new ArrayList<String>();
		for (String entry: this.recipients.getValue().split(", ")) {
			entry = entry.trim();
			if (!entry.isEmpty())
				recipients.add(entry);
		}
		return recipients;
	}
	
	public List<String> getInvalidRecipients() {
		List<String> invalid = new ArrayList<String>();
		for (String entry: recipients.getValue().split(", ")) {
			Matcher validEmail = emailPattern.matcher(entry);
			if (validEmail.matches()) {
				continue;
			} else {
				invalid.add(entry);
			}
		}
		return invalid;
	}
	
	public boolean validateAndNotify() {
		if (nimbusAddress.getValue().isEmpty()) {
			Notification.show("Enter the URL of your Nimbus cloud!", Notification.Type.ERROR_MESSAGE);
			return false;
		}
		if (recipients.getValue().isEmpty()) {
			Notification.show("Enter at least one recipient", Notification.Type.WARNING_MESSAGE);
			return false;
		}
		if (!recipients.isValid()) {
			return false;
		}
		String invalid = "";
		for (String s : getInvalidRecipients())
			invalid += invalid.isEmpty() ? s : ", " + s;
		
		if (!invalid.isEmpty()) {
			String message = StringUtils.countMatches(invalid, ",") > 1 ? 
					"These emails are invalid: " :
					"This email is invalid: ";
			Notification.show(message + "'" + invalid + "'", Notification.Type.WARNING_MESSAGE);
			return false;
		}
		
		return true;
	}
	
	public void setRecipientsEditable(boolean editable) {
		recipients.setEnabled(editable);
		recipients.setReadOnly(editable);
	}
	
	public void setSubjectEditable(boolean editable) {
		subject.setEnabled(editable);
		subject.setReadOnly(editable);
	}
	
	public void setMessageEditable(boolean editable) {
		message.setEnabled(editable);
		message.setReadOnly(editable);
	}
	
	private void buildEmailLayout() {
		// TODO: slick interface with "add" button to add each individual email. Too lazy
		recipients = new TextArea("Recipients");
		recipients.addStyleName(ValoTheme.TEXTAREA_SMALL);
		recipients.setRows(2);
		recipients.setDescription("Email addresses, separated by commas");
		recipients.addValidator(new StringLengthValidator("Enter at least one recipient", 0, null, false));
		formContent.addComponent(recipients);
		
		nimbusAddress = new TextField("Nimbus URL");
		nimbusAddress.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		nimbusAddress.setDescription("The externally-visible URL of your Nimbus cloud");
		formContent.addComponent(nimbusAddress);
		
		subject = new TextField("Subject");
		subject.addStyleName(ValoTheme.TEXTFIELD_SMALL);
		formContent.addComponent(subject);
		
		message = new TextArea("Message");
		message.addStyleName(ValoTheme.TEXTAREA_TINY);
		message.setRows(2);
		message.setWidth("100%");
		formContent.addComponent(message);
		
		send = new Button("Send");
		//submit.setDisableOnClick(true);
		addComponent(send);
		setComponentAlignment(send, Alignment.MIDDLE_RIGHT);
		
		Label helpTitle = new Label(FontAwesome.ASTERISK.getHtml() + " Having trouble authenticating with Gmail or Yahoo?", ContentMode.HTML);
		//helpTitle.setIcon(FontAwesome.ASTERISK);
		//helpTitle.addStyleName("label-inline-icon");
		helpTitle.addStyleName(ValoTheme.LABEL_SMALL);
		helpTitle.addStyleName(ValoTheme.LABEL_BOLD);
		Label helpMessage = new Label("If you have 2-step verification enabled, you may need to set an application password.<br />"
				+ "Click <a href='http://youtu.be/clg1xF1fsLc' target='_blank'>here</a> for help with Yahoo, or "
				+ "<a href='https://support.google.com/accounts/answer/185833?hl=en' target='_blank'>here</a> for help with Gmail. "
				+ "If all else fails, you may need to disable 2-step verification.", ContentMode.HTML);
		helpMessage.addStyleName(ValoTheme.LABEL_SMALL);
		
		addComponent(helpTitle);
		addComponent(helpMessage);
	}
}
