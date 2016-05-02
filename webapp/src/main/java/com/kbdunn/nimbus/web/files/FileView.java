package com.kbdunn.nimbus.web.files;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

public class FileView extends FileManagerLayout implements View {
	
	private static final long serialVersionUID = 1L;
	public static final String NAME = "files";
	
	public FileView(FileManagerController fileManagerController) {
		super(fileManagerController);
	}
	
	private FileManagerController getFileController() {
		return (FileManagerController) super.controller;
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		super.buildLayout();
		getFileController().handleUri(NAME + "/" + event.getParameters());
	}
}
