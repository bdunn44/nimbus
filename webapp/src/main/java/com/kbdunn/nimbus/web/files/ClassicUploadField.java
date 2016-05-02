package com.kbdunn.nimbus.web.files;

import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.async.VaadinUploadOperation;
import com.kbdunn.nimbus.web.NimbusUI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;

public class ClassicUploadField extends VerticalLayout {

	private static final long serialVersionUID = 1L;
	private UploadActionProcessor controller;
	private VaadinUploadOperation operation;
	private Upload upload;
	
	public ClassicUploadField(UploadActionProcessor controller) {
		this.controller = controller;

		addStyleName("classic-upload-field");
		buildUploadField();
	}
	
	private void buildUploadField() {
		operation = NimbusUI.getAsyncService().uploadFile(controller);
		upload = new Upload(null, operation);
		upload.setButtonCaption("Start Upload");
		
		upload.addStartedListener(operation);
		upload.addProgressListener(operation);
		upload.addFailedListener(operation);
		upload.addSucceededListener(operation);
		
		addComponent(upload);
	}
	
	public void refresh() {
		removeAllComponents();
		buildUploadField();
	}
	
	/*
	@Override
	public void buttonClick(ClickEvent event) {
		upload.interruptUpload();
		refresh();
	}*/
}
