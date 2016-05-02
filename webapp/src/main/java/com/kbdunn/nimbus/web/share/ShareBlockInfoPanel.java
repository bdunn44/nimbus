package com.kbdunn.nimbus.web.share;

import java.text.SimpleDateFormat;

import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ShareBlockInfoPanel extends Panel {

	private static final long serialVersionUID = 8116322003945944235L;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/YYYY 'at' h:mm a");
	
	private ShareBlock block;
	private HorizontalLayout content;
	private UserAccessLayout access;
	private Label icon, name;
	private TextArea message;
	
	public ShareBlockInfoPanel() { 
		buildLayout();
	}
	
	public ShareBlockInfoPanel(ShareBlock block) {
		this.block = block;
		buildLayout();
		refresh();
	}
	
	public void setShareBlock(ShareBlock block) {
		this.block = block;
		refresh();
	}
	
	public void refresh() {
		if (block == null) return;
		
		name.setValue(block.getName() + " <span style='font-size:small;'>(created " + DATE_FORMAT.format(block.getCreated()) + ")</span>");
		if (block.getMessage() != null) {
			message.setReadOnly(false);
			message.setValue(block.getMessage());
			message.setReadOnly(true);
		}
		
		access.setShareBlock(block);
	}
	
	private void buildLayout() {
		setHeight("160px");
		addStyleName(ValoTheme.PANEL_WELL);
		addStyleName("share-block-info-panel");
		
		content = new HorizontalLayout();
		content.setSizeFull();
		content.setMargin(true);
		content.setSpacing(true);
		setContent(content);
		
		icon = FontAwesome.CUBE.getLabel().setSize5x();
		icon.addStyleName("block-icon");
		content.addComponent(icon);
		content.setComponentAlignment(icon, Alignment.MIDDLE_CENTER);
		content.setExpandRatio(icon, .1f);
		
		VerticalLayout titleLayout = new VerticalLayout();
		titleLayout.setSizeFull();
		//titleLayout.setSpacing(true);
		titleLayout.addStyleName("main-detail-layout");
		content.addComponent(titleLayout);
		content.setComponentAlignment(titleLayout, Alignment.TOP_LEFT);
		content.setExpandRatio(titleLayout, .6f);
		
		name = new Label();
		name.setContentMode(ContentMode.HTML);
		name.addStyleName(ValoTheme.LABEL_H2);
		name.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		titleLayout.addComponent(name);
		titleLayout.setExpandRatio(name, .25f);
		
		message = new TextArea();
		message.addStyleName(ValoTheme.TEXTAREA_BORDERLESS);
		message.setSizeFull();
		message.setRows(4);
		message.setReadOnly(true);
		titleLayout.addComponent(message);
		titleLayout.setExpandRatio(message, .75f);
		
		access = new UserAccessLayout();
		access.setEditable(false);
		access.setHideOwner(false);
		access.setHeader("Users");
		content.addComponent(access);
		content.setExpandRatio(access, .4f);
		
		// TODO: Message Board
	}
}
