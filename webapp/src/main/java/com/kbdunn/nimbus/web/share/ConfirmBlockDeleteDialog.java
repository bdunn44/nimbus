package com.kbdunn.nimbus.web.share;

import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ConfirmBlockDeleteDialog extends VerticalLayout implements ClickListener {

	private static final long serialVersionUID = 1936204636656235363L;
	private static final String caption = "Delete Share Block";
	
	private PopupWindow popup;
	
	public ConfirmBlockDeleteDialog() {
		popup = new PopupWindow(caption, this);
		buildLayout();
	}
	
	public void showDialog() {
		popup.setSubmitCaption("Delete Block");
		popup.addSubmitListener(this);
		popup.addCancelListener(this);
		popup.open();
	}
	
	public void addSubmitListener(ClickListener listener) {
		popup.addSubmitListener(listener);
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		Label l = new Label("Really delete this block?");
		l.addStyleName(ValoTheme.LABEL_H3);
		addComponent(l);
	}

	@Override
	public void buttonClick(ClickEvent event) {
		popup.close();
	}
}
