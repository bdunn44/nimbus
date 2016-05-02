package com.kbdunn.nimbus.web.settings.users;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class UserSettingsTab extends VerticalLayout implements SettingsTab, ClickListener {
	
	private static final long serialVersionUID = -8028306733429945004L;

	public static final String FRAGMENT = "users";
	
	private UserSettingsController controller;
	private VerticalLayout userContainer;
	private Button refresh, add;
	
	UserSettingsTab(UserSettingsController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	@Override
	public String getName() {
		return "Users";
	}

	@Override
	public String getFragment() {
		return FRAGMENT;
	}

	@Override
	public boolean requiresAdmin() {
		return true;
	}
	
	@Override
	public void refresh() {
		userContainer.removeAllComponents();
		for (NimbusUser u : controller.getUsers()) {
			userContainer.addComponent(new NimbusUserLayout(controller, u));
		}
	}

	@Override
	public SettingsTabController getController() {
		return controller;
	}

	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		addStyleName("user-settings-tab");
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		addComponent(hl);
		
		Label header = new Label("Manage Nimbus Users");
		header.addStyleName(ValoTheme.LABEL_H2);
		header.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		hl.addComponent(header);
		
		refresh = new Button(FontAwesome.REFRESH);
		refresh.setDescription("Refresh");
		refresh.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		refresh.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		refresh.addStyleName(ValoTheme.BUTTON_TINY);
		refresh.addClickListener(this);
		hl.addComponent(refresh);
		hl.setComponentAlignment(refresh, Alignment.BOTTOM_CENTER);
		
		add = new Button(FontAwesome.USER_PLUS);
		add.setDescription("Add a user to your cloud");
		add.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		add.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		add.addStyleName(ValoTheme.BUTTON_TINY);
		add.addClickListener(this);
		hl.addComponent(add);
		hl.setComponentAlignment(add, Alignment.BOTTOM_CENTER);
		
		userContainer = new VerticalLayout();
		userContainer.setSpacing(true);
		addComponent(userContainer);
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (event.getButton().equals(add)) {
			new InviteDialog(controller.getCurrentUser()).openPopup();
		}
		if (event.getButton().equals(refresh)) {
			refresh();
		}
	}
}
