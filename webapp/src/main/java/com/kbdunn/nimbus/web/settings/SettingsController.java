package com.kbdunn.nimbus.web.settings;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.event.HardDriveModificationEvent;
import com.kbdunn.nimbus.web.settings.drives.StorageController;
import com.kbdunn.nimbus.web.settings.drives.StorageSettingsTab;
import com.kbdunn.nimbus.web.settings.profile.ProfileController;
import com.kbdunn.nimbus.web.settings.profile.ProfileSettingsTab;
import com.kbdunn.nimbus.web.settings.users.UserSettingsController;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.UI;

public class SettingsController implements SelectedTabChangeListener {

	private static final long serialVersionUID = -8494059837034944762L;
	private static final Logger log = LogManager.getLogger(SettingsController.class.getName());

	private SettingsView view;
	private NimbusUser user;
	private List<SettingsTab> tabs;
	//private Map<String, SettingsTabController> controllers;
	
	public SettingsController() {
		refreshUser();
		tabs = new LinkedList<SettingsTab>();
		//controllers = new HashMap<String, SettingsTabController>();
		
		// Settings tabs for all users
		tabs.add(new ProfileController(this).getTab());
		//ProfileController pc = new ProfileController(this);
		//controllers.put(pc.getTab().getFragment(), pc);
		
		if (user.isAdministrator()) {
			//DriveController dc = new DriveController(this);
			//controllers.put(dc.getTab().getFragment(), dc);
			tabs.add(new UserSettingsController(this).getTab());
			tabs.add(new StorageController(this).getTab());
		}
		
		view = new SettingsView(this);
		UI.getCurrent().getNavigator().addView(SettingsView.NAME, view);
	}
	
	void parseFragment(String fragment) {
		if (fragment == null || fragment.isEmpty()) {
			view.openFirstTab();
			return;
		}
		log.debug("Parsing fragment " + fragment);
		fragment = fragment.startsWith("/") ? fragment.substring(1) : fragment;
		fragment = fragment.endsWith("/") ? fragment.substring(0, fragment.length()-1) : fragment;
		//SettingsTabController c = controllers.get(fragment);
		SettingsTab tab = null;
		for (SettingsTab t : tabs) {
			if (t.getFragment().equals(fragment)) {
				tab = t;
				break;
			}
		}
		if (tab == null) {
			// Not valid
			UI.getCurrent().getNavigator().navigateTo(SettingsView.NAME);
			return;
		}
		view.openTab(tab);
	}
	
	public void openSettingsHome() {
		UI.getCurrent().getNavigator().navigateTo(SettingsView.NAME);
	}
	
	public void openProfileSettings() {
		UI.getCurrent().getNavigator().navigateTo(SettingsView.NAME + "/" + ProfileSettingsTab.FRAGMENT);
	}
	
	public void openDriveSettings() {
		UI.getCurrent().getNavigator().navigateTo(SettingsView.NAME + "/" + StorageSettingsTab.FRAGMENT);
	}
	
	List<SettingsTab> getTabs() {
		/*List<SettingsTab> tabs = new LinkedList<SettingsTab>();
		for (SettingsTabController c : controllers.values())
			tabs.add(c.getTab());*/
		return tabs;
	}
	
	public NimbusUser getCurrentUser() {
		return user;
	}
	
	public void refreshUser() {
		user = NimbusUI.getCurrentUser();
	}

	@Override
	public void selectedTabChange(SelectedTabChangeEvent event) {
		if (!(event.getTabSheet().getSelectedTab() instanceof SettingsTab))
			throw new IllegalStateException("A tab was selected that is not an instance of SettingsTab.class: " + event.getTabSheet().getSelectedTab());
		SettingsTab selected = (SettingsTab) event.getTabSheet().getSelectedTab();
		for (SettingsTab tab : getTabs()) {
			if (tab.equals(selected)) tab.refresh();
		}
	}
	
	public void fireHardDriveModificationEvent(Object source, NimbusUser user) { 
		NimbusUI.getCurrentEventRouter().publishHardDriveModificationEvent(new HardDriveModificationEvent(source, user));
	}
}
