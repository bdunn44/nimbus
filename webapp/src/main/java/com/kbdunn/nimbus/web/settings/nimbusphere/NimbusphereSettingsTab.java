package com.kbdunn.nimbus.web.settings.nimbusphere;

import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;
import com.kbdunn.nimbus.common.model.nimbusphere.VerifyResponse;
import com.kbdunn.nimbus.web.settings.SettingsTab;
import com.kbdunn.nimbus.web.settings.SettingsTabController;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class NimbusphereSettingsTab extends VerticalLayout implements SettingsTab {

	private static final long serialVersionUID = -884361572606820647L;
	public static final String FRAGMENT = "nimbusphere";

	private NimbusphereController controller;

	private NimbusphereStatus status;
	private TextField token;
	private Button connect, enterNew, ok;
	private Label detail, tokenInfo, error, verifier;
	private HorizontalLayout tokenLayout;
	
	public NimbusphereSettingsTab(NimbusphereController controller) {
		this.controller = controller;
		buildLayout();
	}

	@Override
	public String getName() {
		return "Nimbusphere";
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
		this.clearErrors();
		tokenLayout.setVisible(true);
		token.setReadOnly(false);
		token.setValue("");
		verifier.setVisible(false);
		ok.setVisible(false);
		this.status = controller.getNimbusphereStatus();
		System.out.println("Status is: " + status);
		if (status == null || status.getToken() == null || status.isDeleted()) {
			token.setReadOnly(false);
			tokenInfo.setVisible(true);
			connect.setVisible(true);
			detail.setValue("Nimbusphere provides an easy way to keep track of your personal cloud, "
				+ "and ensure you always have easy access to it using your own subdomain (for example, my-cloud.nimbusphere.org). "
				+ "Visit <a href='https://nimbusphere.org' target='_blank'>nimbusphere.org</a> for information.");
			enterNew.setVisible(false);
			
		} else if (status.getToken() != null) {
			token.setValue(status.getToken());
			token.setReadOnly(true);
			tokenInfo.setVisible(false);
			connect.setVisible(false);
			enterNew.setVisible(true);
			detail.setValue("Your cloud has been registered with Nimbusphere!");
		}
		
		if (status != null && status.isDeleted()) {
			this.detail.setValue("This cloud has been removed from Nimbusphere!<br><br> " + this.detail.getValue());
		}
	}

	@Override
	public SettingsTabController getController() {
		return controller;
	}

	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		setSizeFull();
		
		Label title = new Label("Nimbusphere Configuration");
		title.addStyleName(ValoTheme.LABEL_H2);
		title.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		addComponent(title);
		
		detail = new Label();
		detail.setContentMode(ContentMode.HTML);
		addComponent(detail);
		
		tokenInfo = new Label("During the Nimbusphere cloud registration process you will be asked to copy a confirmation token into Nimbus. "
				+ "Use the text field below for this, and click 'Connect' to register your cloud.");
		addComponent(tokenInfo);
		
		tokenLayout = new HorizontalLayout();
		tokenLayout.setSpacing(true);
		tokenLayout.setMargin(true);
		tokenLayout.setSizeUndefined();
		addComponent(tokenLayout);
		setComponentAlignment(tokenLayout, Alignment.MIDDLE_CENTER);
		
		token = new TextField();
		token.addStyleName(ValoTheme.TEXTFIELD_HUGE);
		token.setMaxLength(48);
		token.setWidth("750px");
		token.setInputPrompt("Paste Registration Token Here");
		tokenLayout.addComponent(token);
		
		connect = new Button("Connect", (event) -> {
			this.clearErrors();
			final String token = this.token.getValue();
			if (token == null || token.isEmpty()) {
				this.token.setComponentError(new UserError("Please enter the registration token"));
				return;
			} 
			final VerifyResponse response = this.controller.setConfirmationToken(this.token.getValue());
			if (response == null || response.getVerifier() == null) {
				this.error.setValue("The submitted token was rejected by Nimbusphere");
				this.error.setVisible(true);
				return;
			}
			tokenLayout.setVisible(false);
			this.verifier.setValue(response.getVerifier());
			this.verifier.setVisible(true);
			this.ok.setVisible(true);
		});
		connect.addStyleName(ValoTheme.BUTTON_HUGE);
		tokenLayout.addComponent(connect);
		
		enterNew = new Button("Enter New Token", (event) -> {
			this.clearErrors();
			this.enterNew.setVisible(false);
			this.connect.setVisible(true);
			this.tokenInfo.setVisible(true);
			this.token.setReadOnly(false);
			this.token.setValue("");
		});
		enterNew.addStyleName(ValoTheme.BUTTON_HUGE);
		tokenLayout.addComponent(enterNew);
		
		error = new Label();
		error.addStyleName(ValoTheme.LABEL_FAILURE);
		addComponent(error);
		
		verifier = new Label();
		verifier.setSizeUndefined();
		verifier.setCaption("Verification Code");
		verifier.addStyleName(ValoTheme.LABEL_H1);
		verifier.addStyleName(ValoTheme.LABEL_BOLD);
		addComponent(verifier);
		setComponentAlignment(verifier, Alignment.MIDDLE_CENTER);
		
		ok = new Button("Ok", (event) -> {
			this.refresh();
		});
		ok.addStyleName(ValoTheme.BUTTON_LARGE);
		ok.addStyleName(ValoTheme.BUTTON_FRIENDLY);
		addComponent(ok);
		setComponentAlignment(ok, Alignment.BOTTOM_RIGHT);
	}
	
	private void clearErrors() {
		this.token.setComponentError(null);
		this.error.setVisible(false);
	}
}
