package com.kbdunn.nimbus.web.popup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class PopupWindow extends Window {

	private static final long serialVersionUID = -877700991890503309L;

	private VerticalLayout layout;
	protected AbstractComponentContainer content;
	private HorizontalLayout buttonLayout;
	private Button submit, cancel;
	private List<Button> customActions;

	public PopupWindow() {
		buildPopup();
	}
	
	public PopupWindow(String caption, AbstractComponentContainer content) {
		this.content = content;
		setCaption(caption);
		buildPopup();
	}
	
	public void setPopupLayout(AbstractComponentContainer popupLayout) {
		layout.replaceComponent(content, popupLayout);
		content = popupLayout;
		refresh();
	}
	
	private void buildPopup() {
		addStyleName("popup-window");
		setModal(true);
		setResizable(true);
		setClosable(true);
		setDraggable(true);
		setSizeUndefined();
		
		layout = new VerticalLayout();
		layout.setMargin(true);
		layout.setSpacing(true);
		
		if (content == null) content = new CssLayout();
		content.addStyleName("popup-content");
		layout.addComponent(content);
		
		buttonLayout = new HorizontalLayout();
		buttonLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		buttonLayout.setSizeUndefined();
		buttonLayout.setSpacing(true);
		layout.addComponent(buttonLayout);
		layout.setComponentAlignment(buttonLayout, Alignment.MIDDLE_RIGHT);
		
		setContent(layout);
		
		cancel = new Button("Cancel");
		cancel.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				close();
			}
		});
		
		submit = new Button("Ok");
		submit.setClickShortcut(KeyCode.ENTER);
	}
	
	private void refresh() {
		Iterator<Component> cit = content.iterator();
		int i = 1;
		while (cit.hasNext()) {
			Component c = cit.next();
			if (c instanceof Focusable) {
				((Focusable) c).setTabIndex(i);

				if (i == 1) ((Focusable) c).focus();
			}
			i++;
		}

		buttonLayout.removeAllComponents();
		buttonLayout.addComponent(cancel);
		if (customActions != null) {
			for (Button b : customActions) {
				buttonLayout.addComponent(b);
				b.setTabIndex(++i);
			}
		}
		buttonLayout.addComponent(submit);
		cancel.setTabIndex(++i);
		submit.setTabIndex(++i);
	}
	
	public void setCancelCaption(String caption) {
		cancel.setCaption(caption);
	}
	
	public void setSubmitCaption(String caption) {
		submit.setCaption(caption);
	}
	
	public void hideSubmitButton() {
		submit.setVisible(false);
	}
	
	public void hideButtons() {
		buttonLayout.setVisible(false);
	}
	
	public void setCustomActions(List<Button> customActions) {
		this.customActions = customActions;
	}
	
	public void addCustomAction(Button action) {
		if (customActions == null) customActions = new ArrayList<Button>();
		customActions.add(action);
	}
	
	public void open() {
		refresh();
		if (!super.isAttached()) {
			UI.getCurrent().addWindow(this);
		}
		center();
	}
	
	public void addSubmitListener(ClickListener listener) {
		submit.addClickListener(listener);
	}
	
	public void addCancelListener(ClickListener listener) {
		cancel.addClickListener(listener);
	}
}