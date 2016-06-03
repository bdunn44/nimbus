package com.kbdunn.nimbus.web.settings.profile;

import com.kbdunn.nimbus.web.popup.ConfirmDialog;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.vaadin.jsclipboard.ClipboardButton;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class ApiTokenForm extends Panel {

	private static final long serialVersionUID = 8215626769479203091L;

	private final ProfileController controller;
	private VerticalLayout apiForm;
	private HorizontalLayout tokenLayout;
	private TextField token;
	private ClipboardButton tokenCopy;
	private Button generateTokens;
	
	public ApiTokenForm(ProfileController controller) {
		this.controller = controller;
		buildLayout();
	}
	
	private void buildLayout() {
		apiForm = new VerticalLayout();
		apiForm.setSizeFull();
		apiForm.setSpacing(true);
		apiForm.setMargin(true);
		setContent(apiForm);
		
		tokenLayout = new HorizontalLayout();
		tokenLayout.setSpacing(true);
		token = new TextField("Token");
		token.setId("token");
		token.setWidth("300px");
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
		apiForm.addComponent(tokenLayout);
		apiForm.setComponentAlignment(tokenLayout, Alignment.MIDDLE_CENTER);
		
		generateTokens = new Button("Generate New Token");
		generateTokens.addClickListener((e) -> {
			ConfirmDialog dialog = new ConfirmDialog("Generate New API Token", 
					"Are you sure you want to generate a new API token? Your old token will no longer work.");
			dialog.addSubmitListener((bce) -> {
				controller.generateNewTokens();
				refresh();
				dialog.close();
			});
			dialog.open();
		});
		
		apiForm.addComponent(generateTokens);
		apiForm.setComponentAlignment(generateTokens, Alignment.MIDDLE_RIGHT);
	}
	
	void refresh() {
		token.setReadOnly(false);
		token.setValue(controller.getCurrentUser().getApiToken());
		token.setReadOnly(true);
	}
}
