package com.kbdunn.nimbus.web.popup;

import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

public class ConfirmDialog extends PopupWindow {

	private static final long serialVersionUID = 3614221277541731522L;

	public ConfirmDialog(String caption, String message) {
		super(caption, new CssLayout());
		super.content.addComponent(new Label(message));
		super.setSubmitCaption("Yes");
		super.setCancelCaption("No");
	}
	
	public ConfirmDialog(String caption, String firstMessage, String secondMessage) {
		this(caption, firstMessage);
		super.content.addComponent(new Label(secondMessage));
	}
}
