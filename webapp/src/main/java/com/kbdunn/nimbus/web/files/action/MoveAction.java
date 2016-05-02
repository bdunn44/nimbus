package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;

public class MoveAction extends CopyAction {
	
	private static final long serialVersionUID = -8527936667745414701L;

	public MoveAction(final AbstractActionHandler handler) {
		super(handler);
	}
	
	@Override
	public FontAwesome getIcon() {
		return FontAwesome.SIGN_IN;
	}
	
	@Override
	public String getCaption() {
		return "Move Files";
	}
	
	@Override
	public void doAction() {
		if (getActionHandler().canProcess(this) && super.canDoAction()) {
			NimbusFile targetFolder = (NimbusFile) targetFolderTree.getValue();
			((MoveActionProcessor) getActionHandler()).processMove(sources, targetFolder);
		}
	}
}