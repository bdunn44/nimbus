package com.kbdunn.nimbus.server.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.AsyncOperationQueue;
import com.kbdunn.nimbus.common.async.EmailTransport;
import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.exception.NimbusException;
import com.kbdunn.nimbus.common.exception.NullEmailTransportException;
import com.kbdunn.nimbus.common.model.Email;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.AsyncService;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.async.AsyncServerOperation;
import com.kbdunn.nimbus.server.async.AsyncServerOperationQueue;
import com.kbdunn.nimbus.server.async.CopyOperation;
import com.kbdunn.nimbus.server.async.EmailOperation;
import com.kbdunn.nimbus.server.async.MoveOperation;
import com.kbdunn.nimbus.server.async.ReconcileOperation;
import com.kbdunn.nimbus.server.async.UpdateMediaLibraryOperation;
import com.kbdunn.nimbus.server.async.VaadinUploadOperation;

public class LocalAsyncService implements AsyncService {
	
	private static final Logger log = LogManager.getLogger(LocalAsyncService.class);
	private LocalUserService userService;
	
	public LocalAsyncService() {  }
	
	public void initialize(NimbusContext container) {
		userService = container.getUserService();
	}
	
	@Override
	public void startAsyncOperation(AsyncOperation operation) {
		if (!(operation instanceof AsyncServerOperation))
			throw new IllegalArgumentException(operation + " is not an instance of " + AsyncServerOperation.class.getName());
		new Thread((AsyncServerOperation) operation).start();
	}

	@Override
	public AsyncOperationQueue buildAsyncOperationQueue(String name, List<AsyncOperation> operations) {
		List<AsyncServerOperation> ops = new ArrayList<>();
		for (AsyncOperation op : operations) {
			if (!(op instanceof AsyncServerOperation))
				throw new IllegalArgumentException(op + " is not an instance of " + AsyncServerOperation.class.getName());
			ops.add((AsyncServerOperation) op);
		}
		return new AsyncServerOperationQueue(name, ops);
	}
	
	@Override
	public AsyncOperation copyFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, boolean startOperation) {
		AsyncServerOperation op = new CopyOperation(config, source, targetFolder);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation copyFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder,
			List<FileConflict> resolutions, boolean startOperation) {
		AsyncServerOperation op = new CopyOperation(config, source, targetFolder, resolutions);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation copyFiles(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, boolean startOperation) {
		AsyncServerOperation op = new CopyOperation(config, sources, targetFolder);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation copyFiles(AsyncConfiguration config, List<NimbusFile> sources, NimbusFile targetFolder, 
			List<FileConflict> resolutions, boolean startOperation) {
		AsyncServerOperation op = new CopyOperation(config, sources, targetFolder, resolutions);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation moveFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, boolean startOperation) {
		AsyncServerOperation op = new MoveOperation(config, source, targetFolder);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation moveFile(AsyncConfiguration config, NimbusFile source, NimbusFile targetFolder, 
			List<FileConflict> resolutions, boolean startOperation) {
		AsyncServerOperation op = new MoveOperation(config, source, targetFolder, resolutions);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation moveFiles(AsyncConfiguration config, List<NimbusFile> sources, 
			NimbusFile targetFolder, boolean startOperation) {
		AsyncServerOperation op = new MoveOperation(config, sources, targetFolder);
		if (startOperation) startAsyncOperation(op);
		return op;
	}
	
	@Override
	public AsyncOperation moveFiles(AsyncConfiguration config, List<NimbusFile> sources, 
			NimbusFile targetFolder, List<FileConflict> resolutions, boolean startOperation) {
		AsyncServerOperation op = new MoveOperation(config, sources, targetFolder, resolutions);
		if (startOperation) startAsyncOperation(op);
		return op;
	}

	@Override
	public AsyncOperation reconcileStorageDevice(AsyncConfiguration config, StorageDevice device, boolean startOperation) {
		AsyncServerOperation op = new ReconcileOperation(config, device);
		if (startOperation) startAsyncOperation(op);
		return op;
	}

	@Override
	public void pauseStorageDeviceReconciliations() {
		ReconcileOperation.setReconciliationsPaused(true);
	}

	@Override
	public void resumeStorageDeviceReconciliations() {
		ReconcileOperation.setReconciliationsPaused(false);
	}
	
	@Override
	public AsyncOperation updateMediaLibrary(AsyncConfiguration config, NimbusUser user, boolean startOperation) {
		AsyncServerOperation op = new UpdateMediaLibraryOperation(config, user);
		if (startOperation) startAsyncOperation(op);
		return op;
	}

	@Override
	public VaadinUploadOperation uploadFile(String fileName, long fileSize, UploadActionProcessor controller) {
		return new VaadinUploadOperation(fileName, fileSize, controller);
	}

	@Override
	public VaadinUploadOperation uploadFile(UploadActionProcessor controller) {
		return new VaadinUploadOperation(controller);
	}

	@Override
	public AsyncOperation sendEmail(AsyncConfiguration config, Email email, boolean startOperation) throws NimbusException {
		if (email.getFrom() == null) throw new IllegalArgumentException("From NimbusUser cannot be null");
		if (email.getRecipients() == null || email.getRecipients().isEmpty()) throw new IllegalArgumentException("Recipients cannot be null or empty");
		final EmailTransport transport = userService.getEmailTransport(email.getFrom());
		if (transport == null) throw new NullEmailTransportException(email.getFrom());
		AsyncServerOperation op = new EmailOperation(config, transport, email);
		if (startOperation) startAsyncOperation(op);
		return op;
	}

	@Override
	public AsyncOperation sendInvitationEmail(AsyncConfiguration config, NimbusUser from, String subject, String message, String nimbusUrl, 
			String recipientEmail, boolean startOperation) throws NimbusException {
		if (from == null) throw new IllegalArgumentException("From NimbusUser cannot be null");
		if (recipientEmail == null || recipientEmail.isEmpty()) throw new IllegalArgumentException("Recipient email cannot be null or empty");
		
		// Create user
		final NimbusUser recipient = new NimbusUser();
		recipient.setName(recipientEmail);
		recipient.setEmail(recipientEmail);
		String pw = userService.generateTemporaryPassword();
		recipient.setPasswordDigest(userService.getDigestedPassword(pw));
		recipient.setPasswordTemporary(true);
		userService.save(recipient);
		
		// Compose email
		final Email email = new Email(from, recipient);
		email.setSubject(subject);
		final String body = "<html><body style=\"font-family: Arial;\">" 
				+ message
				+ "<br />"
				+ "<p>---------------------------------------------------------"
				+ "<br/><bold>Nimbus</bold> login information:<br/>"
				+ "---------------------------------------------------------</p>"
				+ ""
				+ "<p>Username: " + recipient.getEmail()
				+ "<br />"
				+ "Password: " + pw + "</p>"
				+ ""
				+ "<p>Login to my personal cloud at: <a href=\"" + nimbusUrl + "\">" + nimbusUrl + "</a>"
				+ "</body></html>";
		email.setBody(body);
		pw = null;

		final EmailTransport transport = userService.getEmailTransport(from);
		if (transport == null) throw new NullEmailTransportException(from);
		AsyncServerOperation op = new EmailOperation(config, transport, email);
		op.addFinishedListener(o  -> {
			if (o.succeeded()) {
				log.info("Sent invitation to " + recipient.getEmail());
			} else {
				log.warn("Failed sending invitation to " + recipient.getEmail());
				userService.delete(recipient);
			}
		});
		if (startOperation) startAsyncOperation(op);
		return op;
	}

	@Override
	public AsyncOperation sendPasswordResetEmail(AsyncConfiguration config, NimbusUser from, String subject, String message, String nimbusUrl, 
			NimbusUser recipient, boolean startOperation) throws NimbusException {
		if (from == null) throw new IllegalArgumentException("From NimbusUser cannot be null");
		if (recipient == null) throw new IllegalArgumentException("Recipient cannot be null");
		
		// Create user
		final String oldPw = recipient.getPasswordDigest(); // Revert if pw reset fails
		String pw = userService.generateTemporaryPassword();
		recipient.setPasswordDigest(userService.getDigestedPassword(pw));
		recipient.setPasswordTemporary(true);
		userService.save(recipient);
		
		// Compose email
		final Email email = new Email(from, recipient);
		email.setSubject(subject);
		final String body = "<html><body style=\"font-family: Arial;\">" 
				+ message
				+ "<br />"
				+ "<p>---------------------------------------------------------"
				+ "<br/><bold>Nimbus</bold> login information:<br/>"
				+ "---------------------------------------------------------</p>"
				+ ""
				+ "<p>Username: " + recipient.getName() + " (or your email address)"
				+ "<br />"
				+ "Password: " + pw + "</p>"
				+ ""
				+ "<p>Login to Nimbus at: <a href=\"" + nimbusUrl + "\">" + nimbusUrl + "</a>"
				+ "</body></html>";
		email.setBody(body);
		pw = null;

		final EmailTransport transport = userService.getEmailTransport(from);
		if (transport == null) throw new NullEmailTransportException(from);
		AsyncServerOperation op = new EmailOperation(config, transport, email);
		op.addFinishedListener(o  -> {
			if (o.succeeded()) {
				log.info("Password reset email sent to " + recipient.getEmail());
				
			} else {
				log.error("Failed to send password reset email to " + recipient.getEmail());
				recipient.setPasswordDigest(oldPw);
				try {
					userService.save(recipient);
				} catch (Exception ee) {
					// At least we tried
				}
			}
		});
		if (startOperation) startAsyncOperation(op);
		return op;
	}
}
