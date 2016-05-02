package com.kbdunn.nimbus.web.files.action;

import java.util.List;

import com.kbdunn.nimbus.web.component.FileTable;
import com.kbdunn.nimbus.web.files.FileManagerController;
import com.vaadin.event.Action;

public class FileContextMenu implements Action.Handler {
	
	private static final long serialVersionUID = -4251971231093877385L;
	
	private FileManagerController handler;
	
	public FileContextMenu(FileManagerController controller) {
		this.handler = controller;
	}
	
	@Override
	public Action[] getActions(Object target, Object sender) {
		if (!(sender instanceof FileTable)) return null;
		List<AbstractFileAction> actions = handler.getActions();
		return actions.toArray(new AbstractFileAction[actions.size()]);
	}
	
	@Override
	public void handleAction(Action action, Object sender, Object target) {
		if (!(action instanceof AbstractFileAction)) {
			throw new IllegalArgumentException("Action must be an AbstractFileAction");
		}
		if (!(sender instanceof FileTable)) {
			throw new IllegalArgumentException("Sender must be a FileTable");
		}
		
		int selected = handler.getSelectedFiles().size();
		if (selected < 2) {
			if (selected > 0) ((FileTable) sender).unselectAll();
			((FileTable) sender).select(target);
		}
		
		handler.handle((AbstractFileAction) action);
	}
}