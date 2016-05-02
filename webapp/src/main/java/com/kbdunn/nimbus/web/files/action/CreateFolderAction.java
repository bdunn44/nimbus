package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

public class CreateFolderAction extends AbstractFileAction {
	
	private static final long serialVersionUID = 1L;
	
	public static final FontAwesome ICON = FontAwesome.FOLDER_O;
	public static final String CAPTION = "Create new folder";
	
	protected TextField folderName;
	private HorizontalLayout popupLayout;
	
	public CreateFolderAction(AbstractActionHandler handler) {
		super(handler);
		buildLayout();
	}
	
	private void buildLayout() {
		popupLayout = new HorizontalLayout();
		popupLayout.setMargin(true);
		popupLayout.setSpacing(true);
		
		folderName = new TextField("Folder Name");
		popupLayout.addComponent(folderName);
	}
	
	@Override
	public FontAwesome getIcon() {
		return ICON;
	}
	
	@Override
	public String getCaption() {
		return CAPTION;
	}
	
	@Override
	public void refresh() {
		folderName.setComponentError(null);
		folderName.setValue("");
	}
	
	@Override
	public void doAction() {
		if (getActionHandler().canProcess(this))
			((CreateFolderActionProcessor) getActionHandler()).processCreateFolder(folderName.getValue());
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		folderName.setComponentError(e);
	}
	
	@Override
	public AbstractComponentContainer getPopupLayout() {
		return popupLayout;
	}

	@Override
	public Category getCategory() {
		return Category.MANAGE;
	}
}
