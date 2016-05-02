package com.kbdunn.nimbus.web.share;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

class EditListener implements ClickListener {
	
	private static final long serialVersionUID = 1L;
	private ShareBlockEditor view;
	private Button editButton;
	private AbstractTextField field;
	private boolean saveOnCommit = false;
	
	EditListener(Button editButton, AbstractTextField field, ShareBlockEditor view) {
		this.editButton = editButton;
		this.field = field;
		this.view = view;
	}
	
	EditListener(Button editButton, AbstractTextField field, ShareBlockEditor view, boolean saveOnCommit) {
		this.editButton = editButton;
		this.field = field;
		this.view = view;
		this.saveOnCommit = saveOnCommit;
	}
	
	public void setSaveOnCommit(boolean saveOnCommit) {
		this.saveOnCommit = saveOnCommit;
	}
	
	public boolean isEditing() {
		return !field.isReadOnly();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (field.isReadOnly()) {
			field.setReadOnly(false);
			editButton.setIcon(FontAwesome.CHECK);
		} else {
			field.setReadOnly(true);
			editButton.setIcon(FontAwesome.EDIT);
			if (saveOnCommit) view.getController().saveBlock(view);
		}
	}
}