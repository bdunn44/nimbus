package com.kbdunn.nimbus.web.share;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.exception.ShareBlockNameConflictException;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.common.model.ShareBlockRecipient;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.event.ShareBlockModificationEvent;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class ShareController {
	
	private static final Logger log = LogManager.getLogger(ShareController.class.getName());
	
	private FileShareService shareService;
	private ShareBlock currentBlock;
	
	private ShareListView shareListView;
	private SendShareDialog sendShareDialog;
	
	public ShareController() {
		NimbusUI ui = NimbusUI.getCurrent();
		shareService = NimbusUI.getFileShareService();
		
		log.debug("Creating ShareListView...");
		shareListView = new ShareListView(this);
		ui.addView(ShareListView.NAME, shareListView);
		
		sendShareDialog = new SendShareDialog(this);
	}
	
	public void openSendShareDialog(ShareBlock block) {
		sendShareDialog.setCurrentShareBlock(block);
		sendShareDialog.openPopup();
	}
	
	// Used only by the send share view
	boolean parseFragment(String fragment) {
		String token = null;
		if (fragment.contains("/")) {
			token = fragment.substring(0, fragment.indexOf("/"));
		} else {
			token = fragment;
		}
		
		if (!setShareByToken(token)) {
			return false;
		}
		
		return true;
	}
	
	boolean setShareByToken(String token) {
		if (token == null || token.isEmpty()) {
			this.currentBlock = null;
			return false;
		}
		
		ShareBlock newBlock = NimbusUI.getFileShareService().getShareBlockByToken(token);
		if (newBlock == null) {
			UI.getCurrent().getNavigator().navigateTo(Error.SHARE_NOT_FOUND.getPath());
			return false;
		}
		currentBlock = newBlock;
		return true;
	}

	ShareBlock getCurrentBlock() {
		return currentBlock;
	}
	
	/*public ShareBlockAccess getCurrentUserAccess() {
		if (currentBlock != null && getCurrentUser() != null)
			return currentBlock.getAccess(getCurrentUser());
		return null;
	}*/
	
	Map<StorageDevice, List<NimbusFile>> getBlockFilesByHardDrive(ShareBlock shareBlock) {

		log.debug("Compiling contents of Share Block '" + shareBlock.getName() + "' by storage device");
		Map<StorageDevice, List<NimbusFile>> sdFileMap = new HashMap<StorageDevice, List<NimbusFile>>();
		List<NimbusFile> shareFiles = shareBlock.getId() == null ?
				Collections.<NimbusFile> emptyList() :
				shareService.getContents(shareBlock);
		Collections.sort(shareFiles, new Comparator<NimbusFile>() {
			
			@Override
			public int compare(NimbusFile o1, NimbusFile o2) {
				return o1.getStorageDeviceId().compareTo(o2.getStorageDeviceId());
			}
		});
		StorageDevice sd = null;
		List<NimbusFile> hdnfs = null;
		for (NimbusFile nf : shareFiles) {
			if (sd == null || !sd.getId().equals(nf.getStorageDeviceId())) {
				if (sd != null) {
					sdFileMap.put(sd, hdnfs);
				}
				sd = NimbusUI.getStorageService().getStorageDeviceById(nf.getStorageDeviceId());
				hdnfs = new ArrayList<NimbusFile>();
			}
			hdnfs.add(nf);
			log.debug("Shared file: " + nf.getPath());
		}
		sdFileMap.put(sd, hdnfs);
		return sdFileMap;
	}
	
	boolean currentUserIsBlockOwner(ShareBlock shareBlock) {
		return NimbusUI.getCurrentUser().getId().equals(shareBlock.getUserId());
	}
	
	NimbusUser getBlockOwner(ShareBlock shareBlock) {
		return NimbusUI.getUserService().getUserById(shareBlock.getUserId());
	}
	
	boolean newBlockNameIsValid(ShareBlock shareBlock, String newName) {
		return !shareService.shareBlockNameExistsForUser(shareBlock, newName);
	}
	
	void saveBlock(ShareBlockEditor view) {
		view.block.setExpirationDate(view.expires.getValue());
		view.block.setName(view.name.getValue());
		view.block.setMessage(view.message.getValue());
		if (view.shareExternally.getValue() != null) view.block.setExternal(view.shareExternally.getValue());
		if (view.allowUpload.getValue() != null) view.block.setExternalUploadAllowed(view.allowUpload.getValue());
		try {
			shareService.save(view.block);
			fireShareBlockModificationEvent();
		} catch (ShareBlockNameConflictException e) {
			log.error(e, e);
			// TODO: Handle
		}
	}
	
	void deleteBlock(final ShareBlock block) {
		ConfirmBlockDeleteDialog dialog = new ConfirmBlockDeleteDialog();
		dialog.addSubmitListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				if (shareService.delete(block)) {
					shareListView.refreshMyShares();
					fireShareBlockModificationEvent();
				} else {
					Notification.show("There was an error deleting the share block!", Notification.Type.WARNING_MESSAGE);
				}
			}
		});
		dialog.showDialog();
	}
	
	void setSharedFiles(ShareBlock shareBlock, List<NimbusFile> sharedFiles) {
		if (!sharedFiles.isEmpty()) {
			shareService.saveShareBlockNimbusFiles(shareBlock, sharedFiles);
			fireShareBlockModificationEvent();
		}
	}
	
	void setBlockAccess(ShareBlock shareBlock, List<ShareBlockAccess> access) {
		if (!access.isEmpty()) {
			// Need this in case access was added before block was saved
			for (ShareBlockAccess sba : access) sba.setShareBlockId(shareBlock.getId()); 
			shareService.saveShareBlockAccess(shareBlock, access);
			fireShareBlockModificationEvent();
		}
	}
	
	void refreshView() {
		shareListView.refreshMyShares();
	}

	int getBlockFolderCount(ShareBlock shareBlock) {
		return shareService.getFolderCount(shareBlock);
	}

	int getBlockFileCount(ShareBlock shareBlock) {
		return shareService.getFileCount(shareBlock);
	}

	int getBlockAccessCount(ShareBlock shareBlock) {
		return shareService.getShareBlockAccess(shareBlock).size();
	}
	
	void addBlockRecipient(ShareBlock shareBlock, String email) {
		shareService.addRecipient(
				new ShareBlockRecipient(shareBlock.getId(), email));
		fireShareBlockModificationEvent();
	}
	
	private void fireShareBlockModificationEvent() {
		NimbusUI.getCurrentEventRouter().publishShareBlockModificationEvent(new ShareBlockModificationEvent(this));
	}
}