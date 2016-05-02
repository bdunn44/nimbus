package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;

public class CreateFileAction extends CreateFolderAction {

	public CreateFileAction(AbstractActionHandler handler) {
		super(handler);
		super.folderName.setCaption("File Name");
	}

	private static final long serialVersionUID = 8344482398887038686L;
	
	@Override
	public FontAwesome getIcon() {
		return FontAwesome.PLUS_SQUARE_O;
	}
	
	@Override
	public String getCaption() {
		return "Create New File";
	}
	
	@Override
	public Category getCategory() {
		return Category.MANAGE;
	}
	
	@Override
	public void doAction() {
		if (getActionHandler().canProcess(this))
			((CreateFileActionProcessor) getActionHandler()).processCreateFile(folderName.getValue());
	}
}