package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class RenameAction extends AbstractFileAction {
	
	private static final long serialVersionUID = 1L;
	public static final FontAwesome ICON = FontAwesome.FONT;
	public static final String CAPTION = "Rename File";
	
	private NimbusFile targetFile;
	private VerticalLayout popupLayout;
	private TextField rename;
	
	public RenameAction(final AbstractActionHandler handler) {
		super(handler);
		buildLayout();
	}
	
	private void buildLayout() {
		popupLayout = new VerticalLayout();
		popupLayout.setMargin(true);
		popupLayout.setSpacing(true);
		
		rename = new TextField("File Name");
		popupLayout.addComponent(rename);
	}
	
	public void setTargetFile(NimbusFile targetFile) {
		this.targetFile = targetFile;
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
	public AbstractComponentContainer getPopupLayout() {
		return popupLayout;
	}
	
	@Override
	public void refresh() {
		rename.setValue(targetFile.getName()); 
		rename.setComponentError(null);
	}
	
	@Override
	public void doAction() {
		if (getActionHandler().canProcess(this))
			((RenameActionProcessor) getActionHandler()).processRename(targetFile, rename.getValue());
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		rename.setComponentError(e);
	}
	
	@Override
	public Category getCategory() {
		return Category.MANAGE;
	}
}