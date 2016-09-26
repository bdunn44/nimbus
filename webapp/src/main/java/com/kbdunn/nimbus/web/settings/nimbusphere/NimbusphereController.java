package com.kbdunn.nimbus.web.settings.nimbusphere;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;
import com.kbdunn.nimbus.common.model.nimbusphere.VerifyResponse;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.settings.SettingsController;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;

public class NimbusphereController implements SettingsTabController {
	
	private static final Logger log = LogManager.getLogger(NimbusphereController.class);

	//private SettingsController controller;
	private NimbusphereSettingsTab tab;
	
	public NimbusphereController(SettingsController controller) {
		//this.controller = controller;
		this.tab = new NimbusphereSettingsTab(this);
	}
	
	@Override
	public SettingsTab getTab() {
		return tab;
	}

	NimbusphereStatus getNimbusphereStatus() {
		return NimbusUI.getPropertiesService().getNimbusphereStatus();
	}
	
	VerifyResponse setConfirmationToken(String token) {
		try {
			return NimbusUI.getNimbusphereService().verify(token);
		} catch (Exception e) {
			log.error("Error processing Nimbusphere confirmation token", e);
			return null;
		}
	}
}
