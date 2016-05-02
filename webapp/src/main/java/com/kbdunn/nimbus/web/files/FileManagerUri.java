package com.kbdunn.nimbus.web.files;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockFile;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.interfaces.NimbusUri;
import com.kbdunn.nimbus.web.share.ShareView;

public class FileManagerUri implements NimbusUri {
	
	private static final Logger log = LogManager.getLogger(FileManagerUri.class.getName());
	
	public enum Subject {
		FILES, SHARE_BLOCK;
	}
	static final String STORAGE_DEVICE_PREFIX = "sd";
	
	private FileService fileService;
	private FileShareService shareService;
	private String uri;
	private Subject subject;
	private StorageDevice storageDevice;
	private NimbusFile file;
	private ShareBlock shareBlock;
	private boolean isValid = true;
	
	/*public FileManagerUri(NimbusFile file) {
		this.fileService = NimbusUI.getCurrentFileService();
		this.shareService = NimbusUI.getCurrentFileShareService();
		this.subject = Subject.FILES;
		this.file = file;
		this.storageDevice = NimbusUI.getCurrentStorageService().getStorageDeviceById(file.getStorageDeviceId());
		compose();
	}*/
	
	/*public FileManagerUri(ShareBlock shareBlock) {
		this.fileService = NimbusUI.getCurrentFileService();
		this.shareService = NimbusUI.getCurrentFileShareService();
		this.subject = Subject.SHARE_BLOCK;
		this.shareBlock = shareBlock;
		compose();
	}*/
	
	public FileManagerUri(ShareBlockFile file) {
		this.fileService = NimbusUI.getFileService();
		this.shareService = NimbusUI.getFileShareService();
		this.subject = Subject.SHARE_BLOCK;
		this.shareBlock = shareService.getShareBlockById(file.getShareBlockId());
		this.file = fileService.getFileById(file.getNimbusFileId());
		compose();
	}
	
	public FileManagerUri(ShareBlock shareBlock, NimbusFile file) {
		this.fileService = NimbusUI.getFileService();
		this.shareService = NimbusUI.getFileShareService();
		this.subject = Subject.SHARE_BLOCK;
		this.shareBlock = shareBlock;
		this.file = file;
		compose();
	}
	
	public FileManagerUri(FileContainer container) {
		this.fileService = NimbusUI.getFileService();
		this.shareService = NimbusUI.getFileShareService();
		if (container instanceof ShareBlock) {
			this.subject = Subject.SHARE_BLOCK;
			this.shareBlock = (ShareBlock) container;
		} else if (container instanceof StorageDevice) {
			this.subject = Subject.FILES;
			this.file = NimbusUI.getUserService().getUserHomeFolder(NimbusUI.getCurrentUser(), (StorageDevice) container);
			this.storageDevice = (StorageDevice) container;
		} else {
			this.subject = Subject.FILES;
			this.file = (NimbusFile) container;
			this.storageDevice = NimbusUI.getStorageService().getStorageDeviceById(file.getStorageDeviceId());
		}
		compose();
	}
	
	public FileManagerUri(String uri) {
		this.fileService = NimbusUI.getFileService();
		this.shareService = NimbusUI.getFileShareService();
		this.uri = uri;
		parse();
	}
	
	private void compose() {
		if (subject == Subject.SHARE_BLOCK) {
			uri = ShareView.NAME
					+ "/" + shareBlock.getToken()
					+ (file == null ? "" : "/" + shareService.getRelativePath(shareBlock, file));
		} else {
			uri = FileView.NAME
					+ "/" + STORAGE_DEVICE_PREFIX 
					+ "/" + storageDevice.getId() 
					+ "/" + fileService.getRelativePath(NimbusUI.getUserService().getUserRootFolder(NimbusUI.getCurrentUser(), storageDevice), file);
		}
		uri = StringUtil.encodeUriUtf8(uri);
		log.debug("Composed URI is " + uri);
	}
	
	private void parse() {
		String tmpuri = uri.startsWith("/") ? uri.substring(1) : uri;
		tmpuri = StringUtil.decodeUriUtf8(tmpuri);

		this.subject = getUri().startsWith(FileView.NAME) ? Subject.FILES : Subject.SHARE_BLOCK;
		
		if (tmpuri.startsWith(ShareView.NAME)) {
			log.debug("Parsing share block file URI");
			subject = Subject.SHARE_BLOCK;
			tmpuri = tmpuri.substring(ShareView.NAME.length() + 1);//SHARE_BLOCK_FILE_PREFIX.length() + 1);
			String shareToken = tmpuri.contains("/") ? tmpuri.substring(0, tmpuri.indexOf("/")) : tmpuri;
			this.shareBlock = shareService.getShareBlockByToken(shareToken);
			if (shareBlock == null) {
				log.warn("Share Block " + shareToken + " was not found! URI is invalid.");
				isValid = false;
				return;
			}
			tmpuri = tmpuri.substring(shareToken.length());
			if (tmpuri.length() == 0) {
				log.debug("No relative file path found. URI is valid.");
				isValid = true;
				return;
			}
			this.file = shareService.resolveRelativePath(shareBlock, tmpuri.substring(1));
		
		} else if (tmpuri.startsWith(FileView.NAME)) {
			log.debug("Parsing user file URI");
			subject = Subject.FILES;
			if (tmpuri.equals(FileView.NAME)) {
				isValid = true;
				return;
			}
			tmpuri = tmpuri.substring(FileView.NAME.length() + 1);
			if (!tmpuri.startsWith(STORAGE_DEVICE_PREFIX)) {
				log.warn("Storage Device prefix not found! URI is invalid.");
				isValid = false;
				return;
			}
			tmpuri = tmpuri.substring(STORAGE_DEVICE_PREFIX.length() + 1);
			String dId = tmpuri.substring(0, tmpuri.indexOf("/"));
			try {
				this.storageDevice = NimbusUI.getStorageService().getStorageDeviceById(Long.valueOf(dId));
				if (storageDevice == null) throw new IllegalArgumentException();
			} catch (NumberFormatException e) {
				log.warn("Drive ID is invalid");
				this.isValid = false;
				return;
			}
			tmpuri = tmpuri.substring(dId.length() + 1);
			if (tmpuri.length() == 0) {
				log.warn("No relative file path found. URI is invalid.");
				isValid = false;
				return;
			}
			this.file = NimbusUI.getUserService().resolveRelativePath(NimbusUI.getCurrentUser(), this.storageDevice, tmpuri);
			if (this.file == null) {
				log.warn("File was not found! URI is invalid.");
				this.isValid = false;
			}
		} else {
			this.isValid = false;
		}
	}
	
	@Override
	public boolean isValid() {
		return isValid;
	}
	
	@Override
	public String getUri() {
		return uri;
	}
	
	public Subject getSubject() {
		return subject;
	}
	
	public StorageDevice getStorageDevice() {
		return storageDevice;
	}
	
	public NimbusFile getFile() {
		return file;
	}
	
	public void setFile(NimbusFile file) {
		this.file = file;
		compose();
	}
	
	public ShareBlock getShareBlock() {
		return shareBlock;
	}
	
	@Override
	public String toString() {
		return uri;
	}
}
