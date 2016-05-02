package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponentContainer;

public class EditTextFileAction extends AbstractFileAction {
	
	private static final long serialVersionUID = 2670187354914802333L;
	
	public EditTextFileAction(AbstractActionHandler handler) {
		super(handler);
	}
	
	@Override
	public FontAwesome getIcon() {
		return FontAwesome.EDIT;
	}
	
	@Override
	public String getCaption() {
		return "Edit Text File";
	}
	
	@Override
	public Category getCategory() {
		return Category.VIEW_EDIT;
	}
	
	public void editTextFile(NimbusFile file) {
		if (getActionHandler().canProcess(this))
			((EditTextFileActionProcessor) getActionHandler()).handleOpenTextEditor(file);
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
