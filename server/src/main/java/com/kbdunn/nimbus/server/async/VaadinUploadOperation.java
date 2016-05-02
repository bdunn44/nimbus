package com.kbdunn.nimbus.server.async;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gwt.thirdparty.guava.common.io.ByteStreams;
import com.kbdunn.nimbus.common.async.AsyncConfiguration;
import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.NimbusUI;
import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.SucceededEvent;

public class VaadinUploadOperation extends AsyncServerOperation implements com.kbdunn.nimbus.common.async.VaadinUploadOperation {
	
	private static final long serialVersionUID = -1933177730062346106L;
	private static final Logger log = LogManager.getLogger(VaadinUploadOperation.class.getName());
	
	private UploadActionProcessor controller;
	private NimbusFile targetFile;
	
	// Used by both upload methods to generate an output stream
	private OutputStream getFileOutputStream() {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(new File(targetFile.getPath()));
			
		} catch (final FileNotFoundException e) {
			new Notification("There was an error uploading " + targetFile.getName(), 
					Notification.Type.ERROR_MESSAGE).show(Page.getCurrent());
			log.error(e, e);
		}
		return out;
	}
	
	public String getFilename() {
		if (targetFile != null) return targetFile.getName();
		else return null;
	}
	
	//
	/* Start StreamVariable Section */
	/* Used for Drag and Drop uploads */
	//
	
	//private Html5File hFile;
	private long fileSize;
	
	public VaadinUploadOperation(String fileName, long fileSize, UploadActionProcessor controller) {
		super(new AsyncConfiguration("Uploading file '" + fileName + "'"));
		this.controller = controller;
		this.fileSize = fileSize; 
		targetFile = NimbusUI.getFileService().resolveRelativePath(controller.getUploadDirectory(), fileName);
	}
	
	@Override
	public OutputStream getOutputStream() {
		if (NimbusUI.getPropertiesService().isDemoMode() || !controller.userCanUploadFile(targetFile)) 
			return ByteStreams.nullOutputStream();
		
		return getFileOutputStream();
	}
	
	@Override
	public boolean listenProgress() {
		return true;
	}
	
	@Override
	public void streamingStarted(StreamingStartEvent event) {
		/*try {
			waiting = true;
			while (waiting) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			log.error(e, e);
			waiting = false;
		}*/
		//NimbusUI.getCurrent().getTaskController().addTask(this);
	}
	
	@Override
	public void onProgress(StreamingProgressEvent event) {
		float progress = (float)event.getBytesReceived() / (float)fileSize;
		if (progress < 1f) {
			setProgress(progress);
		}
	}
	
	@Override
	public void streamingFinished(StreamingEndEvent event) {
		super.setSucceeded(true);
		setProgress(1f);
		NimbusUI.getFileService().reconcile(targetFile);
		UI.getCurrent().access(new Runnable() {
			
			@Override
			public void run() {
				controller.processFinishedUpload(targetFile, true);
			}
		});
	}
	
	@Override
	public void streamingFailed(StreamingErrorEvent event) {
		super.setSucceeded(false);
		super.setProgress(1f);
	}
	
	@Override
	public boolean isInterrupted() {
		return false;
	}
	
	//
	/* END StreamVariable Section */
	//
	
	
	//
	/* Start Upload Listener Section */
	/* Used for classic uploads */
	//
	
	public VaadinUploadOperation(UploadActionProcessor controller) {
		super(new AsyncConfiguration(""));
		this.controller = controller;
	}
	
	@Override
	public OutputStream receiveUpload(String filename, String mimeType) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return ByteStreams.nullOutputStream();
		if (filename == null || filename.isEmpty()) return ByteStreams.nullOutputStream();
		
		super.getConfiguration().setName("Uploading file '" + filename + "'");
		NimbusUI.getCurrent().getTaskController().addTask(this);
		targetFile = NimbusUI.getFileService().resolveRelativePath(controller.getUploadDirectory(), filename);
		if (!controller.userCanUploadFile(targetFile)) return null;
		return getFileOutputStream();
	}
	
	@Override
	public void uploadStarted(StartedEvent event) {
		event.getComponent().setEnabled(false);
	}
	
	@Override
	public void updateProgress(long readBytes, long contentLength) {
		float progress = (float)readBytes / (float)contentLength;
		if (progress == 1f) {
			super.setSucceeded(true);
		}
		super.setProgress(progress);
	}
	
	@Override
	public void uploadSucceeded(SucceededEvent event) {
		super.setSucceeded(true);
		setProgress(1f);
		NimbusUI.getFileService().reconcile(targetFile);
		UI.getCurrent().access(new Runnable() {

			@Override
			public void run() {
				controller.processFinishedUpload(targetFile, true);
			}
		});
	}
	
	@Override
	public void uploadFailed(FailedEvent event) {
		super.setSucceeded(false);
		setProgress(1f);
		controller.processFinishedUpload(targetFile, false);
	}
	
	//
	/* END Upload Listener Section */
	//
	
	@Override
	public void doOperation() {
		throw new UnsupportedOperationException("Cannot manually start " + VaadinUploadOperation.class.getName());
		/*if (waiting) {
			waiting = false;
			log.debug("Notified");
		}*/
	}
}
