package com.kbdunn.nimbus.common.model;

import java.util.Collections;
import java.util.List;

public class Email {
	
	private NimbusUser from;
	private List<String> recipients;
	private String subject;
	private String body;
	
	public Email(NimbusUser from, NimbusUser recipient) {
		this(from, recipient.getEmail());
	}
	
	public Email(NimbusUser from, String recipient) {
		this(from, Collections.singletonList(recipient));
	}
	
	public Email(NimbusUser from, List<String> recipients) {
		this.from = from;
		this.recipients = recipients;
	}
	
	public NimbusUser getFrom() {
		return from;
	}
	
	public void setFrom(NimbusUser from) {
		this.from = from;
	}
	
	public List<String> getRecipients() {
		return recipients;
	}
	
	public void setRecipients(List<String> recipients) {
		this.recipients = recipients;
	}
	
	public void addRecipient(String recipient) {
		recipients.add(recipient);
	}
	
	public String getSubject() {
		return subject;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getBody() {
		return body;
	}
	
	public void setBody(String body) {
		this.body = body;
	}
}
