package com.kbdunn.nimbus.web.share;

import java.util.Iterator;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ShareListView extends VerticalLayout implements View {
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger(ShareListView.class.getName());
	public static final String NAME = "shares";
	
	private ShareController controller;
	private HorizontalLayout mySharesLayout;
	private HorizontalLayout sharedWithMeLayout;
	
	private boolean layoutBuilt = false;
	
	public ShareListView(ShareController controller) {
		this.controller = controller;
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		if (!layoutBuilt) buildLayout();
		refreshMyShares();
		refreshTheirShares();
	}
	
	private void buildLayout() {
		addStyleName("share-list-view");
		setMargin(true);
		setSpacing(true);
		addStyleName(ValoTheme.LAYOUT_CARD);
		
		HorizontalLayout mySharesHeader = new HorizontalLayout();
		mySharesHeader.addStyleName("section-header");
		addComponent(mySharesHeader);
		Label mySharesLabel = new Label("My Share Blocks");
		mySharesHeader.addComponent(mySharesLabel);
		mySharesLabel.addStyleName(ValoTheme.LABEL_H1);
		mySharesLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		
		Button refreshMine = new Button(FontAwesome.REFRESH);
		mySharesHeader.addComponent(refreshMine);
		mySharesHeader.setComponentAlignment(refreshMine, Alignment.BOTTOM_CENTER);
		refreshMine.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		refreshMine.addStyleName(ValoTheme.BUTTON_SMALL);
		refreshMine.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				refreshMyShares();
			}
		});
		
		Button addBlock = new Button(FontAwesome.PLUS_SQUARE_O);
		mySharesHeader.addComponent(addBlock);
		mySharesHeader.setComponentAlignment(addBlock, Alignment.BOTTOM_CENTER);
		addBlock.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		addBlock.addStyleName(ValoTheme.BUTTON_SMALL);
		addBlock.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				collapseAll();
				addBlock(mySharesLayout, new ShareBlock(NimbusUI.getCurrentUser()), 0).expand();
			}
		});
		
		mySharesLayout = new HorizontalLayout();
		//shareListLayout.setWidth("100%");
		mySharesLayout.setSizeUndefined();
		mySharesLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		mySharesLayout.addStyleName("share-list-layout");
		addComponent(mySharesLayout);
		mySharesLayout.setSpacing(true);
		
		HorizontalLayout sharedWithMeHeader = new HorizontalLayout();
		sharedWithMeHeader.addStyleName("section-header");
		addComponent(sharedWithMeHeader);
		Label sharedWithMeLabel = new Label("Blocks Shared With Me");
		sharedWithMeHeader.addComponent(sharedWithMeLabel);
		sharedWithMeLabel.addStyleName(ValoTheme.LABEL_H1);
		sharedWithMeLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		
		Button refreshTheirs = new Button(FontAwesome.REFRESH);
		sharedWithMeHeader.addComponent(refreshTheirs);
		sharedWithMeHeader.setComponentAlignment(refreshTheirs, Alignment.BOTTOM_CENTER);
		refreshTheirs.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
		refreshTheirs.addStyleName(ValoTheme.BUTTON_SMALL);
		refreshTheirs.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				refreshTheirShares();
			}
		});
		
		sharedWithMeLayout = new HorizontalLayout();
		//shareListLayout.setWidth("100%");
		sharedWithMeLayout.setSizeUndefined();
		sharedWithMeLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		sharedWithMeLayout.addStyleName("share-list-layout");
		addComponent(sharedWithMeLayout);
		sharedWithMeLayout.setSpacing(true);
		
		layoutBuilt = true;
	}
	
	protected void refreshMyShares() {
		mySharesLayout.removeAllComponents();
		for (ShareBlock sb : NimbusUI.getFileShareService().getShareBlocks(NimbusUI.getCurrentUser())) {
			log.trace("Adding owned share " + sb.getName());
			addBlock(mySharesLayout, sb, null).collapse();
		}
	}
	
	protected void refreshTheirShares() {
		sharedWithMeLayout.removeAllComponents();
		for (ShareBlock sb : NimbusUI.getFileShareService().getAccessibleShareBlocks(NimbusUI.getCurrentUser())) {
			log.trace("Adding other user's share " + sb.getName());
			addBlock(sharedWithMeLayout, sb, null).collapse();
		}
	}
	
	private ShareBlockLayout addBlock(HorizontalLayout layout, ShareBlock block, Integer index) {
		ShareBlockLayout sbl = null;
		if (layout == sharedWithMeLayout){
			sbl = new ShareBlockLayout(block, controller);
		} else {
			sbl = new EditableShareBlockLayout(block, controller);
		}
		if (index == null) layout.addComponent(sbl);
		else layout.addComponent(sbl, index);
		return sbl;
	}
	
	private void collapseAll() {
		Iterator<Component> i = mySharesLayout.iterator();
		while (i.hasNext()) {
			Component c = i.next();
			if (c instanceof ShareBlockLayout) {
				((ShareBlockLayout) c).collapse();
			}
		}
	}
}