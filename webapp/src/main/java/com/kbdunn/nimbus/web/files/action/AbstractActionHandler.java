package com.kbdunn.nimbus.web.files.action;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.vaadin.ui.UI;

public abstract class AbstractActionHandler {

	private List<AbstractFileAction> actions = new LinkedList<AbstractFileAction>();
	
	public boolean registerAction(AbstractFileAction action) {
		return actions.add(action);
	}
	
	public boolean unregisterAction(AbstractFileAction action) {
		return actions.remove(action);
	}
	
	public List<AbstractFileAction> getActions() {
		return actions == null ? Collections.emptyList() : actions;
	}
	
	public <T extends AbstractFileAction> T getAction(Class<T> actionType) {
		for (AbstractFileAction action : getActions()) {
			if (actionType.isInstance(action))
				return actionType.cast(action);
		}
		return null;
	}
	
	public boolean canProcess(AbstractFileAction action) {
		if (action instanceof EditTextFileAction && this instanceof EditTextFileActionProcessor) {
			return true;
			
		} else if (action instanceof ViewImagesAction && this instanceof ViewImagesActionProcessor) {
			return true;

		} else if (action instanceof CreateFileAction && this instanceof CreateFileActionProcessor) {
			return true;
			
		} else if (action instanceof CreateFolderAction && this instanceof CreateFolderActionProcessor) {
			return true;
		
		} else if (action instanceof RenameAction && this instanceof RenameActionProcessor) {
			return true;
			
		} else if (action instanceof MoveAction && this instanceof MoveActionProcessor) {
			return true;	
		
		} else if (action instanceof CopyAction && this instanceof CopyActionProcessor) {
			return true;
			
		} else if (action instanceof DeleteAction && this instanceof DeleteActionProcessor) {
			return true;
			
		} else if (action instanceof DownloadAction && this instanceof DownloadActionProcessor) {
			return true;
			
		} else if (action instanceof UploadAction && this instanceof UploadActionProcessor) {
			return true;
		}
		return false;
	}
	
	public void handle(AbstractFileAction action) {
		if (!getActions().contains(action)) {
			UI.getCurrent().getNavigator().navigateTo(com.kbdunn.nimbus.web.error.Error.ACCESS_DENIED.getPath());
			return;
		}
		
		if (action instanceof EditTextFileAction) {
			if (this instanceof EditTextFileActionProcessor) {
				((EditTextFileActionProcessor) this).handleOpenTextEditor();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof ViewImagesAction) {
			if (this instanceof ViewImagesActionProcessor) {
				((ViewImagesActionProcessor) this).handleViewImages();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof CreateFileAction) {
			if (this instanceof CreateFileActionProcessor) {
				((CreateFileActionProcessor) this).handleCreateFile();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof CreateFolderAction) {
			if (this instanceof CreateFolderActionProcessor) {
				((CreateFolderActionProcessor) this).handleCreateFolder();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
		
		} else if (action instanceof RenameAction) {
			if (this instanceof RenameActionProcessor) {
				((RenameActionProcessor) this).handleRename();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof MoveAction) {
			if (this instanceof MoveActionProcessor) {
				((MoveActionProcessor) this).handleMove();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof CopyAction) {
			if (this instanceof CopyActionProcessor) {
				((CopyActionProcessor) this).handleCopy();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
		
		} else if (action instanceof DeleteAction) {
			if (this instanceof DeleteActionProcessor) {
				((DeleteActionProcessor) this).handleDelete();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof DownloadAction) {
			if (this instanceof DownloadActionProcessor) {
				((DownloadActionProcessor) this).handleDownload();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
			
		} else if (action instanceof UploadAction) {
			if (this instanceof UploadActionProcessor) {
				((UploadActionProcessor) this).handleUpload();
			} else {
				throw new IllegalStateException("This action handler does not support the requested action.");
			}
		}
	}
}