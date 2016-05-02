package com.kbdunn.nimbus.web.controlbar;

import java.util.LinkedList;
import java.util.List;

import com.kbdunn.nimbus.web.files.FileManagerController;
import com.kbdunn.nimbus.web.files.action.AbstractFileAction;
import com.kbdunn.nimbus.web.files.action.AbstractFileAction.Category;
import com.vaadin.ui.Button;

public class FileControlBar extends ControlBar {

	private static final long serialVersionUID = 1L;
	protected FileManagerController handler;
	
	public FileControlBar(FileManagerController handler) {
		this.handler = handler;
		addStyleName("file-controls");
		refresh();
	}
	
	public void refresh() {
		super.removeAllComponents();
		
		List<Button> viewEditButtons = new LinkedList<Button>();
		List<Button> manageButtons = new LinkedList<Button>();
		List<Button> transferButtons = new LinkedList<Button>();
		for (AbstractFileAction action : handler.getActions()) {
			if (Category.VIEW_EDIT == action.getCategory()) {
				viewEditButtons.add(action.getButton());
			} else if (Category.MANAGE == action.getCategory()) {
				manageButtons.add(action.getButton());
			} else if (Category.TRANSFER == action.getCategory()) {
				transferButtons.add(action.getButton());
			}
		}
		
		addControlGroup(Category.TRANSFER.getDescription(), transferButtons.toArray(new Button[transferButtons.size()]));
		addControlGroup(Category.MANAGE.getDescription(), manageButtons.toArray(new Button[manageButtons.size()]));
		addControlGroup(Category.VIEW_EDIT.getDescription(), viewEditButtons.toArray(new Button[viewEditButtons.size()]));
	}
}
