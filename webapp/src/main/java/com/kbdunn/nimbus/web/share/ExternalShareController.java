package com.kbdunn.nimbus.web.share;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.files.action.AbstractActionHandler;
import com.kbdunn.nimbus.web.files.action.UploadAction;
import com.vaadin.ui.Notification;

public class ExternalShareController extends AbstractActionHandler implements  UploadActionProcessor { //DownloadActionProcessor,
	
	private static final Logger log = LogManager.getLogger(ExternalShareController.class.getName());
	
	private FileShareService shareService;
	private ExternalShareView view;
	private ShareBlock currentBlock;
	private UploadAction uploadAction;
	//private DownloadAction downloadAction;
	
	public ExternalShareController() {
		this.shareService = NimbusUI.getFileShareService();
		log.debug("Creating ExternalShareView....");
		view = new ExternalShareView(this);
		NimbusUI.getCurrent().addView(ExternalShareView.NAME, view);
		
		uploadAction = new UploadAction(this);
		super.registerAction(uploadAction);
	}
	
	ShareBlock getCurrentBlock() {
		return currentBlock;
	}
	
	UploadAction getUploadAction() {
		return uploadAction;
	}
	
	void parseFragment(String fragment) {
		if (fragment == null || fragment.isEmpty()) {
			currentBlock = null;
			return;
		}
		currentBlock = NimbusUI.getFileShareService().getShareBlockByToken(fragment);
	}
	
	@Override
	public void handleUpload() {
		uploadAction.getPopupWindow().open();
	}
	
	@Override
	public NimbusFile getUploadDirectory() {
		return NimbusUI.getFileShareService().getShareBlockWorkingFolder(currentBlock);
	}
	
	@Override
	public boolean userCanUploadFile(NimbusFile file) {
		if (currentBlock != null && currentBlock.isExternalUploadAllowed() && !NimbusUI.getFileService().fileExistsOnDisk(file)) {
			return true;
		}
		Notification.show("You aren't allowed to overwrite files!");
		return false;
	}
	
	@Override
	public void processFinishedMultiUpload() {
		view.refresh();
	}
	
	@Override
	public void processFinishedUpload(NimbusFile file, boolean succeeded) {
		view.refresh();
	}
	
	List<NimbusFile> getBlockContents() {
		return shareService.getContents(currentBlock);
	}
	
	List<NimbusFile> getFolderContents(NimbusFile folder) {
		return NimbusUI.getFileService().getContents(folder);
	}
	
	boolean fileExistsOnDisk(NimbusFile file) {
		return NimbusUI.getFileService().fileExistsOnDisk(file);
	}
	
	int getBlockFolderCount() {
		return shareService.getFolderCount(currentBlock);
	}

	int getBlockFileCount() {
		return shareService.getFileCount(currentBlock);
	}

	int getBlockAccessCount() {
		return shareService.getShareBlockAccess(currentBlock).size();
	}
	
	void incrementVisitCount() {
		shareService.incrementVisitCount(currentBlock);
	}
	
	//@Override
	//public void handleDownload() {
	//	downloadAction.getPopupWindow().open();
	//}
}