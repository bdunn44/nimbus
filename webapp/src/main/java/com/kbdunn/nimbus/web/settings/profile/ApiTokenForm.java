package com.kbdunn.nimbus.web.settings.profile;

import com.kbdunn.nimbus.web.popup.ConfirmDialog;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.vaadin.jsclipboard.ClipboardButton;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ApiTokenForm extends VerticalLayout {

	private static final long serialVersionUID = 8215626769479203091L;

	private final ProfileController controller;
	private HorizontalLayout tokenLayout;
	private TextField token;
	private ClipboardButton tokenCopy;
	private Button generateTokens;
	
	public ApiTokenForm(ProfileController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		setSizeFull();
		setSpacing(true);
		
		Label title = new Label("API Token");
		title.addStyleName(ValoTheme.LABEL_H3);
		addComponent(title);
		
		tokenLayout = new HorizontalLayout();
		tokenLayout.setSpacing(true);
		token = new TextField();
		token.setId("token");
		token.setWidth("38em");
		token.addStyleName(NimbusTheme.COPY_BUTTON_INPUT_MERGE);
		tokenCopy = new ClipboardButton("token");
		tokenCopy.addStyleName(NimbusTheme.COPY_BUTTON_INPUT_MERGE);
		tokenCopy.addSuccessListener(() -> {
			token.selectAll();
			token.focus();
			Notification.show("Copied");
		});
		tokenCopy.addErrorListener(() -> Notification.show("Copy Error", Notification.Type.WARNING_MESSAGE) );
		tokenCopy.setDescription("Copy to Clipboard");
		tokenLayout.addComponent(token);
		tokenLayout.addComponent(tokenCopy);
		tokenLayout.setComponentAlignment(tokenCopy, Alignment.BOTTOM_LEFT);
		addComponent(tokenLayout);
		setComponentAlignment(tokenLayout, Alignment.MIDDLE_LEFT);
		
		generateTokens = new Button("Generate New Token");
		generateTokens.addClickListener((e) -> {
			ConfirmDialog dialog = new ConfirmDialog("Generate New API Token", 
					"Are you sure you want to generate a new API token? Your old token will no longer work, "
					+ "and the change is immediately applied.");
			dialog.addSubmitListener((bce) -> {
				controller.generateNewTokens();
				refresh();
				dialog.close();
			});
			dialog.open();
		});
		
		tokenLayout.addComponent(generateTokens);
		tokenLayout.setComponentAlignment(generateTokens, Alignment.MIDDLE_RIGHT);
	}
	
	void refresh() {
		token.setReadOnly(false);
		token.setValue(controller.getCurrentUser().getApiToken());
		token.setReadOnly(true);
	}
}
