package com.kbdunn.nimbus.web;

import com.vaadin.navigator.ViewChangeListener;

public class NimbusViewChangeListener implements ViewChangeListener {

	private static final long serialVersionUID = 3090646510370831736L;

	@SuppressWarnings("unused")
	private NimbusUI ui;
	
	protected NimbusViewChangeListener(NimbusUI ui) {
		this.ui = ui;
	}
	
	@Override
	public boolean beforeViewChange(ViewChangeEvent event) {
		// Do nothing
		return true;
	}

	@Override
	public void afterViewChange(ViewChangeEvent event) {
		// Do nothing
	}
}
