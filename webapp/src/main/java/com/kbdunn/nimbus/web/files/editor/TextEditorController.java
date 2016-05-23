package com.kbdunn.nimbus.web.files.editor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.error.ErrorView;
import com.kbdunn.nimbus.web.files.FileManagerUri;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class TextEditorController {

	private static final Logger log = LogManager.getLogger(TextEditorController.class.getName());
	private TextEditorView view;
	private ShareBlock shareBlock;
	private NimbusFile file;
	
	public TextEditorController() {
		log.debug("Creating TextEditorView....");
		view = new TextEditorView(this);
		NimbusUI.getCurrent().addView(TextEditorView.NAME, view);
	}
	
	void handleUri(String uri) {
		if (uri.startsWith(TextEditorView.NAME))
			uri = uri.replaceFirst(TextEditorView.NAME, "");
		log.debug("handleUriFragment: Fragment is '" + uri + "'");
		file = null;
		FileManagerUri furi = new FileManagerUri(uri);
		if (furi.isValid()) {
			file = furi.getFile();
			shareBlock = furi.getShareBlock();
		}
		openFile();
	}
	
	private void openFile() {
		// Invalid or non-existant file
		if (file == null || !NimbusFileTypeResolver.isPlainTextFile(file)) {
			log.warn("Blocked attempt to access an invalid or non-existant file");
			UI.getCurrent().getNavigator().navigateTo(ErrorView.NAME + "/" + Error.INVALID_FILE.getPath());
			return;
		}
		// User has read permission?
		if (currentUserCanReadFile(file)) {
			view.setAceMode(NimbusFileTypeResolver.getAceMode(file));
			view.setFileName(file.getName());
			view.setEditorContent(getFileContents());
			view.setSaveEnabled(currentUserCanEditFile(file));
		} else {
			log.warn("Blocked attempt to read a file without permission");
			NimbusUI.getCurrent().getNavigator().navigateTo(ErrorView.NAME + "/" + Error.ACCESS_DENIED);
		}
	}
	
	void refreshView() {
		openFile();
	}
	
	void saveFile() {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		
		// Check permissions
		if (!currentUserCanEditFile(file)) {
			log.warn("Blocked attempt to edit a file without permission");
			Notification.show("You aren't allowed to edit this file!");
			refreshView();
			return;
		}
		// Perform the save
		log.debug("Saving file " + file.getName());
		String filename = view.getFileName();
		try {
			// Rename file if necessary
			if (!filename.equals(file.getName())) {
				log.debug("Renaming the file to " + filename);
				try {
					if (NimbusUI.getFileService().renameFile(file, filename) == null) {
						// Error, revert to old name
						Notification.show("There was an error renaming the file!");
						log.error("There was an error renaming the file!");
						view.setFileName(file.getName());
					}
				} catch (FileConflictException e) {
					Notification.show("A file with that name already exists!");
					view.setFileName(file.getName());
					return;
				}
			}
			
			// Overwrite contents
			FileWriter writer = new FileWriter(new File(file.getPath()), false);
			writer.write(view.getEditorContent());
			writer.close();
			Notification.show("File Saved!");
			NimbusUI.getFileService().save(file); // Update MD5
			log.debug("File saved");
		} catch (IOException e) {
			Notification.show("Error saving file!", Notification.Type.ERROR_MESSAGE);
			log.error("Error saving text file", e);
		}
	}
	
	private String getFileContents() {
		StringBuilder content = new StringBuilder();
		
		try {
			RandomAccessFile rf = new RandomAccessFile(new File(file.getPath()), "r");
			
	        FileChannel inChannel = rf.getChannel();
	        ByteBuffer buffer = ByteBuffer.allocate(1024);
	        
			while (inChannel.read(buffer) > 0) {
			    buffer.flip();
			    for (int i = 0; i < buffer.limit(); i++)
				    content.append((char) buffer.get());
			    buffer.clear();
			}
			
        	inChannel.close();
			rf.close();
		} catch (IOException e) {
			log.error(e, e);
		}
		return content.toString();
	}
	
	private boolean currentUserCanReadFile(NimbusFile file) {
		if (shareBlock != null) {
			return NimbusUI.getFileShareService().getUserAccessToShareBlock(NimbusUI.getCurrentUser(), shareBlock) != null;
		} 
		return true;
	}
	
	private boolean currentUserCanEditFile(NimbusFile file) {
		if (shareBlock != null) {
			ShareBlockAccess access = NimbusUI.getFileShareService().getUserAccessToShareBlock(NimbusUI.getCurrentUser(), shareBlock);
			if (access == null || !access.canUpdate())
				return false;
		} 
		return true;
	}
}
