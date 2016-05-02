package com.kbdunn.nimbus.web.files.action;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.event.Action;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

public abstract class AbstractFileAction extends Action {
	
	private static final long serialVersionUID = 8372288128181072918L;
	
	private AbstractActionHandler actionHandler;
	private Button button;
	protected PopupWindow popupWindow;
	
	public AbstractFileAction() {
		super("");
		init();
	}
	
	public AbstractFileAction(final AbstractActionHandler actionHandler) {
		super("");
		this.actionHandler = actionHandler;
		init();
	}
	
	private void init() {
		super.setCaption(getCaption());
		super.setIcon(getIcon());
		
		final AbstractFileAction me = this;
		
		button = new Button();
		button.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				actionHandler.handle(me);
			}
		});
		
		popupWindow = new PopupWindow();
		popupWindow.addSubmitListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				if (NimbusUI.getPropertiesService().isDemoMode()) return;
				doAction();
			}
		});
	}
	
	public abstract FontAwesome getIcon();
	public abstract String getCaption();
	public abstract Category getCategory();
	public abstract AbstractComponentContainer getPopupLayout();
	public abstract void refresh();
	public abstract void doAction();
	public abstract void displayError(AbstractErrorMessage e);
	
	protected AbstractActionHandler getActionHandler() {
		return actionHandler;
	}
	
	public Button getButton() {
		button.setCaption(getCaption());
		button.setDescription(getCaption());
		button.setIcon(getIcon());
		return button;
	}
	
	public PopupWindow getPopupWindow() {
		refresh();
		popupWindow.setCaption(getCaption());
		popupWindow.setPopupLayout(getPopupLayout());
		return popupWindow;
	}
	
	public enum Category {
		VIEW_EDIT("View/Edit"),
		MANAGE("Manage"),
		TRANSFER("Transfer");
		
		private String description;
		
		private Category(String description) {
			this.description = description;
		}
		
		public String getDescription() {
			return description;
		}
	}
}
