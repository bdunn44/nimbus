package com.kbdunn.nimbus.web.files;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.FinishedListener;
import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.util.FileUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.files.FileManagerUri.Subject;
import com.kbdunn.nimbus.web.files.action.AbstractActionHandler;
import com.kbdunn.nimbus.web.files.action.AbstractFileAction;
import com.kbdunn.nimbus.web.files.action.CopyAction;
import com.kbdunn.nimbus.web.files.action.CopyActionProcessor;
import com.kbdunn.nimbus.web.files.action.CreateFileAction;
import com.kbdunn.nimbus.web.files.action.CreateFileActionProcessor;
import com.kbdunn.nimbus.web.files.action.CreateFolderAction;
import com.kbdunn.nimbus.web.files.action.CreateFolderActionProcessor;
import com.kbdunn.nimbus.web.files.action.DeleteAction;
import com.kbdunn.nimbus.web.files.action.DeleteActionProcessor;
import com.kbdunn.nimbus.web.files.action.DownloadAction;
import com.kbdunn.nimbus.web.files.action.DownloadActionProcessor;
import com.kbdunn.nimbus.web.files.action.EditTextFileAction;
import com.kbdunn.nimbus.web.files.action.EditTextFileActionProcessor;
import com.kbdunn.nimbus.web.files.action.MoveAction;
import com.kbdunn.nimbus.web.files.action.MoveActionProcessor;
import com.kbdunn.nimbus.web.files.action.RenameAction;
import com.kbdunn.nimbus.web.files.action.RenameActionProcessor;
import com.kbdunn.nimbus.web.files.action.ResolveConflictsDialog;
import com.kbdunn.nimbus.web.files.action.UploadAction;
import com.kbdunn.nimbus.web.files.editor.TextEditorView;
import com.kbdunn.nimbus.web.interfaces.ConflictResolver.ResolutionListener;
import com.kbdunn.nimbus.web.share.ShareView;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.server.UserError;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class FileManagerController extends AbstractActionHandler implements ResolutionListener, CopyActionProcessor, MoveActionProcessor,
	CreateFolderActionProcessor, DeleteActionProcessor, DownloadActionProcessor, EditTextFileActionProcessor, RenameActionProcessor, UploadActionProcessor, 
	CreateFileActionProcessor { //ViewImagesActionProcessor
	
	private static final Logger log = LogManager.getLogger(FileManagerController.class.getName());

	private FileManagerUri uri;
	private FileView fileView;
	private ShareView shareView;
	
	protected EditTextFileAction editAction;
	protected CreateFileAction createFileAction;
	//protected ViewImagesAction viewImagesAction;
	protected CreateFolderAction createFolderAction;
	protected RenameAction renameAction;
	protected CopyAction copyAction;
	protected MoveAction moveAction;
	protected DeleteAction deleteAction;
	protected DownloadAction downloadAction;
	protected UploadAction uploadAction;
	private List<AbstractFileAction> actions;
	
	//protected ImageViewerWindow imageViewer;
	
	// TODO: Really sloppy. Refactor
	protected List<NimbusFile> tempConflictSources;
	protected NimbusFile tempConflictTargetFolder;
	protected boolean conflictsBeingResolved = false;
	protected boolean deleteAfterCopy;
	
	public FileManagerController() {		
		editAction = new EditTextFileAction(this);
		createFileAction = new CreateFileAction(this);
		//viewImagesAction = new ViewImagesAction(this); // TODO
		createFolderAction = new CreateFolderAction(this);
		renameAction = new RenameAction(this);
		copyAction = new CopyAction(this);
		moveAction = new MoveAction(this);
		deleteAction = new DeleteAction(this);
		downloadAction = new DownloadAction(this);
		uploadAction = new UploadAction(this);
		
		//imageViewer = new ImageViewerWindow();
		
		// These register actions for the File state
		super.registerAction(editAction);
		super.registerAction(createFileAction);
		//super.registerAction(viewImagesAction);
		super.registerAction(createFolderAction);
		super.registerAction(renameAction);
		super.registerAction(copyAction);
		super.registerAction(moveAction);
		super.registerAction(deleteAction);
		super.registerAction(uploadAction);
		super.registerAction(downloadAction);
		
		log.debug("Creating FileManagerView...");
		fileView = new FileView(this);
		NimbusUI.getCurrent().addView(FileView.NAME, fileView);
		
		log.debug("Creating ShareView...");
		shareView = new ShareView(this);
		NimbusUI.getCurrent().addView(ShareView.NAME, shareView);
	}
	
	public FileManagerUri getUri() {
		return uri;
	}
	
	// This refreshes actions for the share state
	private void refreshActions() {
		actions = new LinkedList<AbstractFileAction>();
		
		if (uri.getSubject() == Subject.SHARE_BLOCK && uri.getShareBlock() != null) {
			
			if (currentUserIsBlockOwner()) {
				log.debug("Current user is ADMIN!");
				actions.add(editAction);
				actions.add(createFileAction);
				//actions.add(viewImagesAction);
				actions.add(createFolderAction);
				actions.add(renameAction);
				actions.add(copyAction);
				actions.add(moveAction);
				actions.add(deleteAction);
				actions.add(uploadAction);
				actions.add(downloadAction);
				return;
			}
			
			ShareBlockAccess sba = NimbusUI.getFileShareService().getUserAccessToShareBlock(NimbusUI.getCurrentUser(), uri.getShareBlock());
			if (sba == null) return;
			
			// Create
			if (sba.canCreate()) {
				actions.add(createFolderAction);
				actions.add(createFileAction);
				actions.add(copyAction);
				actions.add(uploadAction);
			}
			
			// Read
			actions.add(editAction);
			//actions.add(viewImagesAction);
			actions.add(downloadAction);
			
			// Update
			if (sba.canUpdate()) {
				actions.add(renameAction);
				actions.add(moveAction);
			}
			
			// Delete
			if (sba.canDelete()) {
				actions.add(deleteAction);
			}
		}
	}
	
	@Override
	public List<AbstractFileAction> getActions() {
		if (uri != null && uri.getSubject() == Subject.FILES) {
			return super.getActions();
		}
		if (actions == null) actions = new ArrayList<AbstractFileAction>();
		return actions;
	}
	
	@Override
	public void handle(AbstractFileAction action) {
		if ((uri.getSubject() == Subject.SHARE_BLOCK && actions.contains(action)) || uri.getSubject() == Subject.FILES) 
			super.handle(action);
	}
	
	public List<StorageDevice> getUserStorageDevices() {
		return NimbusUI.getStorageService().getStorageDevicesAssignedToUser(NimbusUI.getCurrentUser());
	}
	
	FileView getFileManagerView() {
		return fileView;
	}
	
	public FileContainer getCurrentFileContainer() {
		if (uri.getFile() != null) {
			// Either is FILES subject or SHARE_BLOCK with relative path
			return uri.getFile().isDirectory() ? 
					uri.getFile() :
					NimbusUI.getFileService().getParentFile(uri.getFile());
		} else {
			return uri.getShareBlock();
		}
	}
	
	/*public FileContainer getRootContainer() {
		if (uri.getSubject() == Subject.FILES)
			return fileView.folderTree.getRootContainer();
		else
			return uri.getShareBlock();
	}*/
	
	// Either a storage device or share block, never a folder
	@Override
	public FileContainer getRootContainer() {
		return uri.getSubject() == Subject.FILES ? uri.getStorageDevice() : uri.getShareBlock();
	}
	
	// Navigate to the home page of a new hard drive
	/*boolean changeCurrentHardDrive(StorageDevice device) {
		if (this.currentStorageDevice != null && this.currentStorageDevice.equals(device)) return false;
		log.debug("Changing hard drive to " + device);
		if (uri.getSubject() == Subject.FILES) {
			changeDirectory(NimbusUI.getCurrentUserService().getUserHomeFolder(currentUser, device));
		}
		return true;
	}*/
	
	// Navigate to a new directory - must be on same StorageDevice or ShareBlock as current
	public void navigateToDirectory(NimbusFile folder) {
		log.debug("Changing directory to " + folder.getName());
		FileContainer root = getRootContainer();
		FileManagerUri target = null;
		if (root instanceof ShareBlock) {
			target = new FileManagerUri((ShareBlock) root, folder);
		} else {
			target = new FileManagerUri(folder);
		}
		UI.getCurrent().getNavigator().navigateTo(target.getUri());
	}
	
	// Navigate to a new StorageDevice or ShareBlock
	public static void navigateToFileContainer(FileContainer container) {
		log.debug("Navigating to FileContainer " + container);
		UI.getCurrent().getNavigator().navigateTo(new FileManagerUri(container).getUri());
	}
	
	public boolean currentUserIsBlockOwner() {
		return uri.getShareBlock() != null && uri.getShareBlock().getUserId().equals(NimbusUI.getCurrentUser().getId());
	}
	
	public void handleUri(String uriString) {
		log.debug("Handling URI fragment " + uriString);
		
		FileManagerUri oldUri = uri;
		uri = new FileManagerUri(uriString);
		
		if (uri.getSubject() == Subject.FILES) {
			List<StorageDevice> userDevices = NimbusUI.getStorageService().getStorageDevicesAssignedToUser(NimbusUI.getCurrentUser());
			if (userDevices.isEmpty()) {
				UI.getCurrent().getNavigator().navigateTo(Error.NO_DRIVES_ASSIGNED.getPath());
				return;
				
			} else if (uriString.isEmpty() || !uri.isValid() || !userDevices.contains(uri.getStorageDevice())) {
				log.warn("Fragment is invalid: " + uriString);
				navigateToDirectory(NimbusUI.getUserService().getUserHomeFolder(NimbusUI.getCurrentUser(), userDevices.get(0)));//managerView.driveSelect.getStorageDevice(0)));
				return;
				
			} else {
				if (oldUri == null || oldUri.getStorageDevice() == null || !oldUri.getStorageDevice().equals(uri.getStorageDevice())) {
					// Storage device change
					//fileView.folderTree.setRootContainer(NimbusUI.getCurrentUserService().getUserHomeFolder(NimbusUI.getCurrentUser(), uri.getStorageDevice()));
					//fileView.breadCrumbs.setHomeCrumb(FileManagerUri.STORAGE_DEVICE_PREFIX + "/" + uri.getStorageDevice().getId() + "/Home"); // TODO: refactor
				} 
			}
			
		} else if (uri.getSubject() == Subject.SHARE_BLOCK) {
			if (!uri.isValid()) {
				log.warn("Fragment is invalid: " + uriString);
				if (uri.getShareBlock() == null) {
					UI.getCurrent().getNavigator().navigateTo(Error.SHARE_NOT_FOUND.getPath());
					return;
				} else {
					UI.getCurrent().getNavigator().navigateTo(Error.INVALID_FILE.getPath());
					return;
				}
			} 
			
			NimbusUI.getFileShareService().incrementVisitCount(uri.getShareBlock());
		}
		
		refreshView();
	}
	
	public void refreshView() {
		refreshActions();
		
		if (uri.getSubject() == Subject.FILES) {
			fileView.folderTree.setRootContainer(uri.getStorageDevice());
			fileView.breadCrumbs.setHomeCrumb(new FileManagerUri(uri.getStorageDevice()).getUri());
			fileView.fileTable.setCurrentContainer(getCurrentFileContainer());
			fileView.breadCrumbs.setCurrentPath(uri.getUri());
			fileView.containerDescLabel.setValue(getContainerShortDescription());
			fileView.containerDescLabel.setDescription(getCurrentFileContainer().getName());
			fileView.controlBar.refresh();
			fileView.refresh();
		} else if (uri.getSubject() == Subject.SHARE_BLOCK) {
			shareView.getInfoPanel().setShareBlock(uri.getShareBlock());
			shareView.getFileManagerLayout().getFolderTree().setRootContainer(uri.getShareBlock());
			shareView.getFileManagerLayout().getBreadCrumbs().setHomeCrumb(new FileManagerUri(uri.getShareBlock()).getUri());
			shareView.getFileManagerLayout().getFileTable().setCurrentContainer(getCurrentFileContainer());
			shareView.getFileManagerLayout().getBreadCrumbs().setCurrentPath(uri.getUri());
			shareView.getFileManagerLayout().getContainerDescriptionLabel().setValue(getContainerShortDescription());
			//shareView.getFileManagerLayout().setDriveSelectVisible(false);
			shareView.refresh();
		}
	}
	
	public List<NimbusFile> getSelectedFiles() {
		if (uri.getSubject() == Subject.FILES)
			return fileView.getSelectedFiles();
		else
			return shareView.getFileManagerLayout().getSelectedFiles();
	}
	
	boolean filesAreSelected() {
		return getSelectedFiles().size() > 0;
	}
	
	boolean requireSelectedFiles() {
		if (!filesAreSelected()) {
			Notification.show("No files selected!");
			return false;
		} else {
			return true;
		}
	}
	
	boolean requireCopyTarget() {
		FileContainer root = uri.getSubject() == Subject.FILES ? 
				fileView.folderTree.getRootContainer() :
				shareView.getFileManagerLayout().folderTree.getRootContainer();
		int targets = 0;
		if (root instanceof NimbusFile)
			targets = NimbusUI.getFileService().getFolderContents((NimbusFile) root).size();
		else if (root instanceof ShareBlock)
			targets = NimbusUI.getFileShareService().getFolderCount((ShareBlock) root);
		if (targets == 0) {
			Notification.show("There aren't any target folders!");
			return false;
		} else {
			return true;
		}
	}
	
	int getCurrentContainerRecursiveContentCount() {
		return getCurrentFileContainer() instanceof NimbusFile ? 
				NimbusUI.getFileService().getRecursiveContentCount((NimbusFile) getCurrentFileContainer()) :
				NimbusUI.getFileShareService().getRecursiveContentCount((ShareBlock) getCurrentFileContainer());
	}
	
	long getCurrentContainerContentSize() {
		return getCurrentFileContainer() instanceof NimbusFile ? 
				NimbusUI.getFileService().getRecursiveContentSize((NimbusFile) getCurrentFileContainer()) :
				NimbusUI.getFileShareService().getRecursiveContentSize((ShareBlock) getCurrentFileContainer());
	}

	public String getContainerShortDescription() {
		int files = getCurrentContainerRecursiveContentCount();
		String size = StringUtil.toHumanSizeString(getCurrentContainerContentSize());
		String s = "<span class='name'>" + getCurrentFileContainer().getName() + "</span>";
		s += "<span class='desc'>";
			s += files + " file" + (files != 1 ? "s" : "");
			s += ", " + size;
		s += "</span>";
		return s;
	}
	
	@Override
	public void handleOpenTextEditor() {
		if (filesAreSelected())
			handleOpenTextEditor(getSelectedFiles().get(0));
	}
	
	boolean canOpenTextFile(NimbusFile file) {
		if (NimbusFileTypeResolver.isPlainTextFile(file)) {
			return true;
		} else {
			log.warn("Refused attempt to edit a non-text file: " + file);
			Notification.show("That's not a text file!");
			return false;
		}
	}
	
	@Override
	public void handleOpenTextEditor(NimbusFile file) {
		if (!canOpenTextFile(file)) return;
		
		if (uri.getSubject() == Subject.FILES) {
			UI.getCurrent().getNavigator().navigateTo(TextEditorView.NAME + "/" + new FileManagerUri(file).getUri());
		} else {
			UI.getCurrent().getNavigator().navigateTo(TextEditorView.NAME + "/" + new FileManagerUri(uri.getShareBlock(), file).getUri());
		}
	}
	
	/*@Override
	public void handleViewImages() {
		if (filesAreSelected()) {
			for (NimbusFile file : getSelectedFiles()) {
				if (file.isImage()) {
					handleViewImage(file);
					return;
				}
			}
		}
		int images = getCurrentFileContainer() instanceof NimbusFile ? 
				NimbusUI.getCurrentFileService().getImageContents((NimbusFile) getCurrentFileContainer()).size() :
				NimbusUI.getCurrentFileShareService().getImageContents((ShareBlock) getCurrentFileContainer()).size();
		if (images > 0) 
			imageViewer.showWindow(getCurrentFileContainer());
		 else 
			Notification.show("There are no supported image files in this folder!");
	}*/
	
	/*@Override
	public void handleViewImage(NimbusFile image) {
		//imageViewer.showWindow(image);
	}*/

	@Override
	public void handleCreateFile() {
		createFileAction.getPopupWindow().open();
	}
	
	boolean validateFileName(String folderName) {
		if (folderName.isEmpty()) {
			createFileAction.displayError(new UserError("File name cannot be empty"));
			return false;
		}
		if (FileUtil.filenameContainsInvalidCharacters(folderName)) {
			createFileAction.displayError(new UserError("File name cannot contain these characters: " + FileUtil.INVALID_FILENAME_CHARACTERS));
			return false;
		}
		return true;
	}
	
	@Override
	public void processCreateFile(String fileName) {
		if (!validateFileName(fileName)) return;
		
		NimbusFile newFile = null;
		
		if (uri.getSubject() == Subject.SHARE_BLOCK && getCurrentFileContainer().equals(uri.getShareBlock())) {
			newFile = NimbusUI.getFileService().resolveRelativePath(
					NimbusUI.getFileShareService().getShareBlockWorkingFolder(uri.getShareBlock()), fileName);
		} else {
			newFile = NimbusUI.getFileService().resolveRelativePath((NimbusFile) getCurrentFileContainer(), fileName);
		}
		
		// Check if file already exists
		if (NimbusUI.getFileService().fileExistsOnDisk(newFile)) {
			createFileAction.displayError(new UserError("That file already exists!"));
			return;
		}
		
		// Create file
		NimbusUI.getFileService().touchFile(newFile);
		createFileAction.getPopupWindow().close();
		refreshView();
	}
	
	@Override
	public void handleCreateFolder() {
		createFolderAction.getPopupWindow().open();
	}
	
	private boolean validateFolderName(String folderName) {
		if (folderName.isEmpty()) {
			createFolderAction.displayError(new UserError("Folder name cannot be empty"));
			return false;
		}
		if (FileUtil.filenameContainsInvalidCharacters(folderName)) {
			createFolderAction.displayError(new UserError("Folder name cannot contain these characters: " + FileUtil.INVALID_FILENAME_CHARACTERS));
			return false;
		}
		return true;
	}
	
	@Override
	public void processCreateFolder(String folderName) {
		if (!validateFolderName(folderName)) return;
		
		// Create folder, check for errors
		NimbusFile newFolder = null;
		
		if (uri.getSubject() == Subject.SHARE_BLOCK && getCurrentFileContainer().equals(uri.getShareBlock())) {
			newFolder = NimbusUI.getFileService().resolveRelativePath(
					NimbusUI.getFileShareService().getShareBlockWorkingFolder(uri.getShareBlock()), folderName);
		} else {
			newFolder = NimbusUI.getFileService().resolveRelativePath((NimbusFile) getCurrentFileContainer(), folderName);
		}
		
		// Check if folder already exists
		if (NimbusUI.getFileService().fileExistsOnDisk(newFolder)) {
			createFolderAction.displayError(new UserError("That folder already exists!"));
			return;
		}
		
		// Create folder
		if (NimbusUI.getFileService().createDirectory(newFolder)) Notification.show("\"" + folderName + "\" created!");
		else Notification.show("There was an error creating the folder", Notification.Type.ERROR_MESSAGE);
		createFolderAction.getPopupWindow().close();
		refreshView();
	}
	
	@Override
	public void handleRename() {
		if (requireSelectedFiles()) {
			renameAction.setTargetFile(getSelectedFiles().get(0));
			renameAction.getPopupWindow().open();
		}
	}
	
	@Override
	public void processRename(NimbusFile targetFile, String newName) {
		if (newName.equals(targetFile.getName())){
			renameAction.getPopupWindow().close();
			return;
		}
		if (FileUtil.filenameContainsInvalidCharacters(newName)) {
			renameAction.displayError(new UserError("File names cannot contain these characters: " + FileUtil.INVALID_FILENAME_CHARACTERS));
			return;
		}
		// Rename file, check for errors
		try {
			if (NimbusUI.getFileService().renameFile(targetFile, newName) == null) {
				Notification.show("There was an error renaming the file", Notification.Type.ERROR_MESSAGE);
			}
		} catch (FileConflictException e) {
			renameAction.displayError(new UserError("A file with that name already exists"));
		}
		renameAction.getPopupWindow().close();
		refreshView();
	}
	
	@Override
	public void handleCopy() {
		if (requireSelectedFiles() && requireCopyTarget()) {
			copyAction.setCopySources(getSelectedFiles());
			copyAction.getPopupWindow().open();
		}
	}
	
	@Override
	public void handleMove() {
		if (requireSelectedFiles() && requireCopyTarget()) {
			moveAction.setCopySources(getSelectedFiles());
			moveAction.getPopupWindow().open();
		}
	}
	
	boolean validateCopyLocation(List<NimbusFile> sources, NimbusFile targetFolder) {
		for (NimbusFile source : sources) {
			if (!NimbusUI.getFileService().fileMoveDestinationIsValid(source, targetFolder)) {
				copyAction.displayError(new UserError("Cannot copy '" + source.getName() + "' into itself!"));
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void processCopy(List<NimbusFile> sources, NimbusFile targetFolder) {
		processCopyOrMove(sources, targetFolder, false);
	}
	
	@Override
	public void processMove(List<NimbusFile> sources, NimbusFile targetFolder) {
		processCopyOrMove(sources, targetFolder, true);
	}
	
	public void processCopyOrMove(final List<NimbusFile> sources, final NimbusFile targetFolder, boolean move) {
		if (!validateCopyLocation(sources, targetFolder)) return;
		
		List<FileConflict> conflicts = getConflicts(sources, targetFolder);
		// If there are conflicts, show resolution popup
		if (conflicts.size() > 0) {
			resolveConflicts(conflicts, sources, targetFolder, move);
		} else if (move) {
			AsyncOperation op = NimbusUI.getAsyncService().moveFiles(null, sources, targetFolder, false);
			op.addFinishedListener(new FinishedListener() {
				
				@Override
				public void operationFinished(AsyncOperation operation) {
					if (uri.getSubject() == Subject.SHARE_BLOCK) {
						// If the move target is a shared folder, remove the individual source files from the share
						for (NimbusFile nf : NimbusUI.getFileShareService().getContents(uri.getShareBlock())) {
							if (targetFolder.equals(nf) || NimbusUI.getFileService().fileIsChildOf(targetFolder, nf)) {
								for (NimbusFile sf : sources) {
									NimbusUI.getFileShareService().removeFileFromShareBlock(uri.getShareBlock(), sf);
								}
								break;
							}
						}
					}
					UI.getCurrent().access(new Runnable() {
						@Override
						public void run() {
							refreshView();
							UI.getCurrent().push();
						}
					});
				}
			});
			NimbusUI.getCurrent().getTaskController().addAndStartTask(op);
			moveAction.getPopupWindow().close();
		} else {
			AsyncOperation op = NimbusUI.getAsyncService().copyFiles(null, sources, targetFolder, false);
			op.addFinishedListener(new FinishedListener() {
				
				@Override
				public void operationFinished(AsyncOperation operation) {
					UI.getCurrent().access(new Runnable() {
						@Override
						public void run() {
							refreshView();
							UI.getCurrent().push();
						}
					});
				}
			});
			NimbusUI.getCurrent().getTaskController().addAndStartTask(op);
			copyAction.getPopupWindow().close();
		}
		
		refreshView();
	}
	
	@Override
	public void handleDelete() {
		if (requireSelectedFiles()) {
			deleteAction.setTargetFiles(getSelectedFiles());
			deleteAction.getPopupWindow().open();
		}
	}
	
	@Override
	public void processDelete(List<NimbusFile> targetFiles) {
		FileService fileService = NimbusUI.getFileService();
		
		// Delete, check for errors
		String failed = "";
		for (NimbusFile nf: targetFiles) {
			if (!fileService.delete(nf))
				failed += failed.isEmpty() ? "'" + nf.getName() + "'" : ", '" + nf.getName() + "'";
		}
		if (!failed.isEmpty())
			Notification.show("Error deleting files: " + failed, Notification.Type.ERROR_MESSAGE);
		
		/*if (uri.getSubject() == Subject.FILES || currentUserIsBlockOwner()) {
			// Delete, check for errors
			String failed = "";
			for (NimbusFile nf: targetFiles) {
				if (!fileService.delete(nf))
					failed += failed.isEmpty() ? "'" + nf.getName() + "'" : ", '" + nf.getName() + "'";
			}
			if (!failed.isEmpty())
				Notification.show("Error deleting files: " + failed, Notification.Type.ERROR_MESSAGE);
		}
		if (uri.getSubject() == Subject.SHARE_BLOCK) {
			// Update share block files
			List<ShareBlockFile> sbfs = NimbusUI.getCurrentFileShareService().getShareBlockFiles(uri.getShareBlock());
			Iterator<ShareBlockFile> i = sbfs.iterator();
			while (i.hasNext()) {
				ShareBlockFile sbf = i.next();
				for (NimbusFile nf : targetFiles) {
					if (sbf.getNimbusFileId().equals(nf.getId())) {
						i.remove();
					}
				}
			}
			NimbusUI.getCurrentFileShareService().saveShareBlockFiles(uri.getShareBlock(), sbfs);
			
			// Move 'deleted' files to shared temp recycle bin
			if (!currentUserIsBlockOwner()) {
				String del = NimbusUI.getCurrentFileShareService().getShareBlockRecycleBin(uri.getShareBlock()).getPath();
				for (NimbusFile tgt : targetFiles) {
					try {
						NimbusFile delfile = fileService.getFileByPath(del + "/" + tgt.getName());
						if (fileService.fileExistsOnDisk(delfile)) fileService.delete(delfile);
						fileService.moveFile(tgt, delfile.getPath());
					} catch (FileConflictException e) {
						// Unreachable
					}
				}
			}
		}*/
		deleteAction.getPopupWindow().close();
		refreshView();
	}
	
	@Override
	public void handleDownload() {
		if (!requireSelectedFiles()) return;
		
		NimbusFile targetFile = getSelectedFiles().get(0);
		
		if (targetFile.isDirectory()) {
			Notification.show("Can't download a folder!");
			return;
		}
		downloadAction.setTargetFile(targetFile);
		downloadAction.getPopupWindow().open();
	}
	
	@Override
	public boolean userCanUploadFile(NimbusFile file) {
		if (uri.getSubject() == Subject.SHARE_BLOCK 
				&& NimbusUI.getFileService().fileExistsOnDisk(file) 
				&& !NimbusUI.getFileShareService().getUserAccessToShareBlock(NimbusUI.getCurrentUser(), uri.getShareBlock()).canUpdate()) {
			Notification.show("You aren't allowed to overwrite files!");
			return false;
		}
		return true;
	}
	
	@Override
	public NimbusFile getUploadDirectory() {
		if (getCurrentFileContainer() instanceof NimbusFile) return (NimbusFile) getCurrentFileContainer();
		else return NimbusUI.getFileShareService().getShareBlockWorkingFolder(uri.getShareBlock());
	}
	
	@Override
	public void handleUpload() {
		uploadAction.getPopupWindow().open();
	}
	
	@Override
	public void processFinishedMultiUpload() {
		refreshView();
	}
	
	@Override
	public void processFinishedUpload(NimbusFile uploadedFile, boolean succeeded) {
		if (!succeeded && uploadedFile != null && NimbusUI.getFileService().fileExistsOnDisk(uploadedFile)) 
			NimbusUI.getFileService().delete(uploadedFile);
		refreshView();
	}
	
	public boolean conflictsBeingResolved() {
		return conflictsBeingResolved;
	}
	
	List<FileConflict> getConflicts(List<NimbusFile> sources, NimbusFile targetFolder) {
		// Check for filename conflicts
		List<FileConflict> conflicts = new ArrayList<FileConflict>();
		for (NimbusFile source : sources) {
			conflicts.addAll(NimbusUI.getFileService().checkConflicts(source, targetFolder));
		}
		
		return conflicts;
	}
	
	void resolveConflicts(List<FileConflict> conflicts, List<NimbusFile> sources, NimbusFile targetFolder, boolean deleteAfterCopy) {
		conflictsBeingResolved = true;
		this.deleteAfterCopy = deleteAfterCopy;
		tempConflictTargetFolder = targetFolder;
		tempConflictSources = sources;
		ResolveConflictsDialog resolver = new ResolveConflictsDialog(conflicts);
		resolver.addResolutionListener(this);
		resolver.showDialog();
	}
	
	@Override
	public void conflictsResolved(List<FileConflict> resolutions) {
		AsyncOperation op = null;
		if (deleteAfterCopy) {
			moveAction.getPopupWindow().close();
			op = NimbusUI.getAsyncService().moveFiles(null, tempConflictSources, tempConflictTargetFolder, resolutions, false);
		} else {
			copyAction.getPopupWindow().close();
			op = NimbusUI.getAsyncService().copyFiles(null, tempConflictSources, tempConflictTargetFolder, resolutions, false);
		}
		op.addFinishedListener(new FinishedListener() {
			@Override
			public void operationFinished(AsyncOperation operation) {
				UI.getCurrent().access(new Runnable() {
					@Override
					public void run() {
						refreshView();
						UI.getCurrent().push();
					}
				});
			}
		});
		NimbusUI.getCurrent().getTaskController().addAndStartTask(op);
		conflictsBeingResolved = false;
		refreshView();
	}
}
