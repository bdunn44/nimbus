package com.kbdunn.nimbus.web.share;

import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

public abstract class ShareBlockEditor extends CssLayout {
	
	private static final long serialVersionUID = -20571879495589529L;

	protected ShareController controller;
	protected ShareBlock block;
	protected TextField name;
	protected TextArea message;
	protected CheckBox shareExternally, allowUpload;
	protected HorizontalLayout externalOptionsLayout, popupLayout;
	protected Label link;
	protected DateField expires;
	protected CheckBox pwProtected;
	protected ValueChangeListener pwProtectedListener, extShareListener;
	protected Button changePassword;
	protected Button shareLink, editName, editMessage;
	protected SetPasswordDialog setPwDialog;
	protected PopupView viewLinkPopup;
	
	protected ShareBlockEditor() {  }
	
	protected ShareBlockEditor(ShareBlock block, ShareController controller) {
		this.controller = controller;
		this.block = block;
	}
	
	protected ShareController getController() {
		return controller;
	}
	
	protected void refresh() {
		name.setReadOnly(false);
		message.setReadOnly(false);
		if (block.getName() != null) name.setValue(block.getName());
		if (block.getMessage() != null) message.setValue(block.getMessage());
		expires.setValue(block.getExpirationDate());
		//shareExternally.removeValueChangeListener(extShareListener); 
		shareExternally.setValue(block.isExternal()); // Do fire event while setting value
		//shareExternally.addValueChangeListener(extShareListener);
		pwProtected.removeValueChangeListener(pwProtectedListener); // Don't fire event while setting value
		pwProtected.setValue(block.getPasswordDigest() != null);
		changePassword.setEnabled(pwProtected.getValue());
		pwProtected.addValueChangeListener(pwProtectedListener);
		allowUpload.setValue(block.isExternalUploadAllowed());
		viewLinkPopup.setVisible(block.getId() != null);
		String url = NimbusUI.getUrlGuess() + "#!" + ExternalShareView.NAME +"/" + block.getToken();
		link.setValue("<a href='"  + url + "'>" + url + "</a>");
	}
	
	protected void initializeComponents() {
		addStyleName("share-block-editor");
		
		name = new TextField("Name");
		name.setColumns(30);
		name.setRequired(true);
		name.setInputPrompt("Give the share block a nickname");
		name.setReadOnly(true);
		name.setMaxLength(50);
		
		editName = new Button(FontAwesome.EDIT);
		editName.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		editName.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		editName.addClickListener(new EditListener(editName, name, this));
		
		message = new TextArea("Message");
		message.setInputPrompt("A message to display to viewers");
		message.setColumns(30);
		message.setRows(2);
		message.setReadOnly(true);
		message.setMaxLength(500);
		
		editMessage = new Button(FontAwesome.EDIT);
		editMessage.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		editMessage.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		editMessage.addClickListener(new EditListener(editMessage, message, this));
		
		expires = new DateField("Expires");
		
		shareExternally = new CheckBox("Share Externally?");
		shareExternally.setDescription("Create a link for people outside of your cloud to use");
		extShareListener = new ValueChangeListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				if (shareExternally.getValue()) 
					externalOptionsLayout.setVisible(true);
				else 
					externalOptionsLayout.setVisible(false);
			}
		};
		shareExternally.addValueChangeListener(extShareListener);
		
		externalOptionsLayout = new HorizontalLayout();
		externalOptionsLayout.addStyleName("ext-options-layout");
		externalOptionsLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		externalOptionsLayout.setSpacing(true);
		externalOptionsLayout.setVisible(false);
		
		pwProtected = new CheckBox("Password Enabled");
		externalOptionsLayout.addComponent(pwProtected);
		externalOptionsLayout.setComponentAlignment(pwProtected, Alignment.MIDDLE_CENTER);
		pwProtected.setDescription("Require external users to enter a password to access files");
		setPwDialog = new SetPasswordDialog(this);
		changePassword = new Button("Change Password", e -> {
			if (NimbusUI.getPropertiesService().isDemoMode()) return;
			setPwDialog.showDialog();
		});
		externalOptionsLayout.addComponent(changePassword);
		externalOptionsLayout.setComponentAlignment(changePassword, Alignment.MIDDLE_CENTER);
		
		pwProtectedListener = new ValueChangeListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				if (NimbusUI.getPropertiesService().isDemoMode()) return;
				
				if (pwProtected.getValue()) {
					changePassword.setEnabled(true);
					allowUpload.setEnabled(true);
					setPwDialog.showDialog();
				} else {
					changePassword.setEnabled(false);
					allowUpload.setEnabled(false);
					allowUpload.setValue(false);
					block.setPasswordDigest(null);
				}
			}
		};
		pwProtected.addValueChangeListener(pwProtectedListener);
		
		allowUpload = new CheckBox("Allow upload?");
		externalOptionsLayout.addComponent(allowUpload);
		externalOptionsLayout.setComponentAlignment(allowUpload, Alignment.MIDDLE_CENTER);
		allowUpload.setEnabled(false);
		allowUpload.setDescription("Allow external users to upload files into shared folders");
		
		popupLayout = new HorizontalLayout();
		popupLayout.setSpacing(true);
		popupLayout.setMargin(true);
		link = new Label();
		link.setContentMode(ContentMode.HTML);
		shareLink = new Button("Share Link");
		shareLink.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				controller.openSendShareDialog(block);
			}
		});
		
		popupLayout.addComponent(link);
		popupLayout.addComponent(shareLink);
		
		viewLinkPopup = new PopupView(new PopupView.Content() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public String getMinimizedValueAsHTML() {
				return "View Link";
			}
			
			@Override
			public Component getPopupComponent() {
				return popupLayout;
			}
		});
		viewLinkPopup.setHideOnMouseOut(false);
		externalOptionsLayout.addComponent(viewLinkPopup);
		externalOptionsLayout.setComponentAlignment(viewLinkPopup, Alignment.MIDDLE_CENTER);
	}
	
	protected void notifySetPasswordCancelled() {
		pwProtected.setValue(false);
		pwProtected.setValue(false);
	}
	
	protected void notifyPasswordSet(String password) {
		block.setPasswordDigest(NimbusUI.getUserService().getDigestedPassword(password));
	}
	
	protected void setComponentSizeTiny() {
		name.addStyleName(ValoTheme.TEXTFIELD_TINY);
		message.addStyleName(ValoTheme.TEXTAREA_TINY);
		shareExternally.addStyleName(ValoTheme.CHECKBOX_SMALL);
		allowUpload.addStyleName(ValoTheme.CHECKBOX_SMALL);
		link.addStyleName(ValoTheme.LABEL_TINY);
		expires.addStyleName(ValoTheme.DATEFIELD_TINY);
		name.addStyleName(ValoTheme.TEXTFIELD_TINY);
		pwProtected.addStyleName(ValoTheme.CHECKBOX_SMALL);
		changePassword.addStyleName(ValoTheme.BUTTON_TINY);
		shareLink.addStyleName(ValoTheme.BUTTON_TINY);
		editName.addStyleName(ValoTheme.BUTTON_TINY);
		editMessage.addStyleName(ValoTheme.BUTTON_TINY);
	}
}