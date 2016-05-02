package com.kbdunn.nimbus.web.share;

import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditableShareBlockLayout extends ShareBlockLayout {

	private static final long serialVersionUID = 2792879077339210738L;

	private VerticalLayout btnLayout;
	private Button save, cancel, delete;
	
	protected EditableShareBlockLayout(ShareBlock block, ShareController controller) {
		super(block, controller);
	}
	
	@Override
	protected void buildCollapsedLayout() {
		super.buildCollapsedLayout();

		Button edit = new Button(FontAwesome.EDIT);
		edit.setDescription("Open");
		edit.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				expand();
			}
		});
		edit.addStyleName(ValoTheme.BUTTON_LARGE);
		edit.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		edit.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		collapsedLayout.addComponent(edit, "right:5px; top:5px;");
	}
	
	@Override
	protected void buildExpandedLayout() {
		super.buildExpandedLayout();
		
		// Button layout on the right
		btnLayout = new VerticalLayout();
		btnLayout.setSizeUndefined();
		btnLayout.addStyleName("button-layout");
		expandedLayout.addComponent(btnLayout, "top:-8px; left:845px;");

		cancel = new Button(FontAwesome.TIMES_CIRCLE_O);
		cancel.setDescription("Cancel");
		cancel.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				refresh();
				collapse();
				controller.refreshView();
			}
		});
		cancel.addStyleName(ValoTheme.BUTTON_LARGE);
		cancel.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		cancel.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		btnLayout.addComponent(cancel);
		
		final ShareBlockEditor me = this;
		save = new Button(FontAwesome.SAVE);
		save.setDescription("Save");
		save.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				if (NimbusUI.getPropertiesService().isDemoMode()) return;
				if (!name.isValid()) {
					name.setComponentError(new UserError(""));
					return;
				}
				if (!controller.newBlockNameIsValid(block, name.getValue())) {
					name.setComponentError(new UserError("A Share Block with that name already exists!"));
					return;
				}
				editName.click();
				editMessage.click();
				controller.saveBlock(me);
				controller.setSharedFiles(block, getSelectedFiles());
				controller.setBlockAccess(block, userAccessLayout.getValue());
				
				refresh();
				collapse();
			}
		});
		save.addStyleName(ValoTheme.BUTTON_LARGE);
		save.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		save.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		btnLayout.addComponent(save);
		
		delete = new Button(FontAwesome.TRASH_O);
		delete.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				if (NimbusUI.getPropertiesService().isDemoMode()) return;
				controller.deleteBlock(block);
			}
		});
		delete.addStyleName("delete-button");
		btnLayout.addComponent(delete);
		delete.setDescription("Delete");
		delete.addStyleName(ValoTheme.BUTTON_LARGE);
		delete.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
		delete.addStyleName(ValoTheme.BUTTON_BORDERLESS);
	}
}
