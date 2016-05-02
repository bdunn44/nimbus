package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponentContainer;

public class ViewImagesAction extends AbstractFileAction {
	
	private static final long serialVersionUID = -6895712135065668286L;
	
	public ViewImagesAction(AbstractActionHandler handler) {
		super(handler);
	}
	
	@Override
	public FontAwesome getIcon() {
		return FontAwesome.PICTURE_O;
	}
	
	@Override
	public String getCaption() {
		return "View Images";
	}
	
	@Override
	public Category getCategory() {
		return Category.VIEW_EDIT;
	}
	
	public void viewImage(NimbusFile image) {
		if (getActionHandler().canProcess(this))
			((ViewImagesActionProcessor) getActionHandler()).handleViewImage(image);
	}
	
	@Override
	public AbstractComponentContainer getPopupLayout() {
		throw new UnsupportedOperationException("This action does not provide a popup layout.");
	}
	
	@Override
	public void refresh() {
		// Do nothing
	}
	
	@Override
	public void doAction() {
		// Do nothing
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		// Do nothing
	}
}
