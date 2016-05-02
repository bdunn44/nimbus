package com.kbdunn.nimbus.web.settings;

import com.vaadin.ui.Component;

public interface SettingsTab extends Component {
	String getName();
	String getFragment();
	boolean requiresAdmin();
	void refresh();
	SettingsTabController getController();
}
