package com.kbdunn.nimbus.web.settings.profile;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.MarginInfo;
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
	private UserProfileForm profileForm;
	private EmailSettingsForm emailForm;
	private ApiTokenForm apiForm;
	private SyncForm syncForm;
	
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
		apiForm.refresh();
		syncForm.refresh();
	}

	@Override
	public SettingsTabController getController() {
		return controller;
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		setSizeFull();
		
		Label title = new Label("Edit Your Profile");
		title.addStyleName(ValoTheme.LABEL_H2);
		title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		addComponent(title);
		
		profileForm = new UserProfileForm(controller);
		addComponent(profileForm);
		
		emailForm = new EmailSettingsForm(controller);
		addComponent(emailForm);
		
		apiForm = new ApiTokenForm(controller);
		addComponent(apiForm);
		
		syncForm = new SyncForm(controller);
		addComponent(syncForm);
		
		HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setMargin(new MarginInfo(true, false, false, false));
		buttonLayout.setSpacing(true);
		Button submit = new Button("Save", this);
		submit.addStyleName(ValoTheme.BUTTON_FRIENDLY);
		submit.setClickShortcut(KeyCode.ENTER);
		buttonLayout.addComponent(submit);
		Button cancel = new Button("Cancel", e -> {
			controller.refreshUser();
			refresh();
		});
		cancel.addStyleName(ValoTheme.BUTTON_QUIET);
		buttonLayout.addComponent(cancel);
		
		addComponent(buttonLayout);
		setComponentAlignment(buttonLayout, Alignment.MIDDLE_RIGHT);
	}
	
	UserProfileForm getProfileForm() {
		return profileForm;
	}
	
	EmailSettingsForm getEmailForm() {
		return emailForm;
	}
	
	SyncForm getSyncForm() {
		return syncForm;
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		if (!profileForm.validate()) return;
		controller.saveProfileSettings();
	}
}
