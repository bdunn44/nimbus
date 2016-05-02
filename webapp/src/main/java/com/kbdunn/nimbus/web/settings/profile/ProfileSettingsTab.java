package com.kbdunn.nimbus.web.settings.profile;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ProfileSettingsTab extends VerticalLayout implements SettingsTab, ClickListener {

	private static final long serialVersionUID = 1L;
	public static final String FRAGMENT = "profile";
	
	private ProfileController controller;
	private HorizontalLayout panelLayout;
	private UserProfileForm profileForm;
	private EmailSettingsForm emailForm;
	
	public ProfileSettingsTab(ProfileController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	@Override
	public String getName() {
		return "Profile";
	}
	
	@Override
	public String getFragment() {
		return FRAGMENT;
	}
	
	@Override
	public boolean requiresAdmin() {
		return false;
	}
	
	@Override
	public void refresh() {
		controller.refreshUser();
		profileForm.refresh();
		emailForm.refresh();
	}

	@Override
	public SettingsTabController getController() {
		return controller;
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		setSizeUndefined();
		
		Label title = new Label("Edit Your Profile");
		title.addStyleName(ValoTheme.LABEL_H2);
		title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		addComponent(title);
		
		panelLayout = new HorizontalLayout();
		panelLayout.setSpacing(true);
		panelLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		addComponent(panelLayout);
		
		profileForm = new UserProfileForm(controller);
		profileForm.setWidth("400px");
		profileForm.setCaption("Profile");
		panelLayout.addComponent(profileForm);
		panelLayout.setComponentAlignment(profileForm, Alignment.TOP_LEFT);
		
		emailForm = new EmailSettingsForm(controller);
		emailForm.setWidth("400px");
		emailForm.setCaption("Email Account");
		panelLayout.addComponent(emailForm);
		panelLayout.setComponentAlignment(emailForm, Alignment.TOP_LEFT);
		
		HorizontalLayout buttonLayout = new HorizontalLayout();
		//buttonLayout.setMargin(true);
		buttonLayout.setSpacing(true);
		Button submit = new Button("Save", this);
		submit.setClickShortcut(KeyCode.ENTER);
		buttonLayout.addComponent(submit);
		buttonLayout.addComponent(new Button("Cancel", e -> {
				controller.refreshUser();
				refresh();
			}));
		
		addComponent(buttonLayout);
		setComponentAlignment(buttonLayout, Alignment.MIDDLE_RIGHT);
	}
	
	UserProfileForm getProfileForm() {
		return profileForm;
	}
	
	EmailSettingsForm getEmailForm() {
		return emailForm;
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		if (!profileForm.validate()) return;
		controller.saveProfileSettings();
	}
}
