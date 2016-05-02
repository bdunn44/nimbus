package com.kbdunn.nimbus.server.async;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.model.Email;

public class EmailOperation extends AsyncServerOperation {
	
	private EmailTransport transport;
	private Email email;
	
	public EmailOperation(AsyncConfiguration config, EmailTransport transport, Email email) {
		super(config);
		super.getConfiguration().setName("Sending email");
		this.transport = transport;
		this.email = email;
	}
	
	public Email getEmail() {
		return email;
	}
	
	@Override
	public void doOperation() throws Exception {
		transport.send(email);
		super.setSucceeded(true);
	}
}