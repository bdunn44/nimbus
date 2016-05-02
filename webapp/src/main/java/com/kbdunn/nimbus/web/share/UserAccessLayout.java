package com.kbdunn.nimbus.web.share;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class UserAccessLayout extends VerticalLayout {

	private static final long serialVersionUID = 3381344287584952459L;
	private static final Logger log = LogManager.getLogger(UserAccessLayout.class.getName());
	
	private boolean ownerHidden = true;
	private boolean editable = true;
	private Label header;
	private ComboBox userDropdown;
	private Button add;
	private ShareBlock block;
	private CssLayout topRow;
	private VerticalLayout accessListLayout;

	public UserAccessLayout() {
		build();
	}
	
	public UserAccessLayout(ShareBlock block) {
		this.block = block;
		build();
		refresh();
	}
	
	public void setShareBlock(ShareBlock block) {
		this.block = block;
		refresh();
	}
	
	public void setHeader(String value) {
		header.setValue(value);
	}
	
	private void build() {
		setSpacing(true);
		addStyleName("user-access-layout");
		
		topRow = new CssLayout();
		addComponent(topRow);
		
		header = new Label("Shared With...");
		header.addStyleName(ValoTheme.LABEL_H3);
		header.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		topRow.addComponent(header);
		
		userDropdown = new ComboBox();
		userDropdown.setNewItemsAllowed(false);
		userDropdown.setNullSelectionAllowed(false);
		userDropdown.setTextInputAllowed(false);
		userDropdown.setInputPrompt("Select a user");
		userDropdown.addStyleName(ValoTheme.COMBOBOX_TINY);
		
		add = new Button("Add");
		add.addStyleName(ValoTheme.BUTTON_TINY);
		topRow.addComponent(add);
		topRow.addComponent(userDropdown);
		add.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				Object o = userDropdown.getValue();
				if (o == null) return;
				NimbusUser u = NimbusUI.getUserService().getUserByNameOrEmail((String) o);
				if (u == null) {
					log.warn("Could not find selected user!");
					refreshUserDropdown();
					return;
				}
				if (userAdded(u)) {
					log.warn("User " + u + " already added to layout");
					refreshUserDropdown();
					return;
				}
				ShareBlockAccess sba = block.getId() == null ? null : NimbusUI.getFileShareService().getUserAccessToShareBlock(u, block);
				if (sba == null) sba = new ShareBlockAccess(block.getId(), u.getId());
				addAccess(sba);
				refreshUserDropdown(); // effectively remove the user from the selection
			}
		});
		
		accessListLayout = new VerticalLayout();
		accessListLayout.addStyleName("user-list-layout");
		accessListLayout.setSpacing(true);
		addComponent(accessListLayout);
	}
	
	protected void refresh() {
		if (block != null) {
			setAccess(block.getId() != null ? 
					NimbusUI.getFileShareService().getShareBlockAccess(block) :
					Collections.<ShareBlockAccess> emptyList());
			refreshUserDropdown();
		}
	}
	
	private void addAccess(ShareBlockAccess sba) {
		UserLayout alo = new UserLayout(sba);
		alo.setEditable(editable);
		if (sba.getUserId().equals(block.getUserId())) alo.setEditable(false); // owner
		accessListLayout.addComponent(alo);
	}
	
	private void setAccess(List<ShareBlockAccess> sba) {
		accessListLayout.removeAllComponents();
		if (!ownerHidden) addAccess(NimbusUI.getFileShareService().getOwnerAccess(block));
		for (ShareBlockAccess a : sba) 
			addAccess(a);
	}
	
	private void refreshUserDropdown() {
		userDropdown.removeAllItems();
		for (NimbusUser nu : NimbusUI.getUserService().getAllUsers()) {
			if (!userAdded(nu) && (!ownerHidden || !nu.getId().equals(block.getUserId())))
				userDropdown.addItem(nu.getName());
		}
		userDropdown.select(userDropdown.getNullSelectionItemId());
	}
	
	public List<ShareBlockAccess> getValue() {
		List<ShareBlockAccess> current = new ArrayList<ShareBlockAccess>();
		if (accessListLayout == null) return current;
		for (int i = 0; i < accessListLayout.getComponentCount(); i++) {
			current.add(((UserLayout) accessListLayout.getComponent(i)).getValue());
		}
		return current;
	}
	
	private boolean userAdded(NimbusUser user) {
		for (ShareBlockAccess sba : getValue()) {
			if (sba.getUserId().equals(user.getId())) return true;
		}
		return false;
	}
	
	public void setEditable(boolean editable) {
		this.editable = editable;
		add.setVisible(editable);
		userDropdown.setVisible(editable);
		Iterator<Component> i = accessListLayout.iterator();
		while (i.hasNext()) {
			((UserLayout) i.next()).setEditable(editable);
		}
	}
	
	public void setHideOwner(boolean ownerHidden) {
		this.ownerHidden = ownerHidden;
		refreshUserDropdown();
		if (ownerHidden) {
			Iterator<Component> i = accessListLayout.iterator();
			while (i.hasNext()) {
				UserLayout u = (UserLayout) i.next();
				if (u.getValue().getUserId().equals(block.getUserId())) { // owner
					i.remove();
					break;
				}
			}
		}
	}
	
	class UserLayout extends HorizontalLayout implements ClickListener {
		
		private static final long serialVersionUID = 1220647403810214210L;
		
		private ShareBlockAccess access;
		private CheckBox read, create, update, delete;
		private Button remove;
		
		public UserLayout(ShareBlockAccess access) {
			this.access = access;
			setHeight("50px");
			setWidth("100%");
			addStyleName("user-layout");
			
			HorizontalLayout leftFloat = new HorizontalLayout();
			addComponent(leftFloat);
			setComponentAlignment(leftFloat, Alignment.MIDDLE_LEFT);
			
			// User Icon/Picture
			Label icon = FontAwesome.USER.getLabel().stack(FontAwesome.SQUARE_O);
			leftFloat.addComponent(icon);
			leftFloat.setComponentAlignment(icon, Alignment.MIDDLE_CENTER);
			
			VerticalLayout rightLayout = new VerticalLayout();
			leftFloat.addComponent(rightLayout);
			leftFloat.setComponentAlignment(rightLayout, Alignment.MIDDLE_LEFT);
			rightLayout.setSizeFull();//setHeight("100%");
			rightLayout.setSpacing(true);
			rightLayout.addStyleName("right-layout");
			
			NimbusUser user = NimbusUI.getUserService().getUserById(access.getUserId());
			Label username = new Label(
					user.getName() + 
					(user.getEmail() != null ?" <span class='user-email'>[" + user.getEmail() + "]</span>" : ""),
					ContentMode.HTML);
			username.addStyleName("user-desc");
			rightLayout.addComponent(username);
			
			HorizontalLayout checkboxes = new HorizontalLayout();
			rightLayout.addComponent(checkboxes);
			checkboxes.setSpacing(true);
			read = new CheckBox("Read");
			create = new CheckBox("Create");
			update = new CheckBox("Update");
			delete = new CheckBox("Delete");
			
			if (access.getShareBlockId() != null  &&
					user.getId().equals(NimbusUI.getFileShareService().getShareBlockById(access.getShareBlockId()).getUserId())) {
				Label ownerLabel = new Label("Owner");
				checkboxes.addComponent(ownerLabel);
			} else {
				read.addStyleName(ValoTheme.CHECKBOX_SMALL);
				read.setDescription("User can view files in this block");
				read.setValue(true);
				read.setReadOnly(true);
				checkboxes.addComponent(read);
				
				create.addStyleName(ValoTheme.CHECKBOX_SMALL);
				create.setDescription("User can upload files and create folders in this block");
				create.setValue(access.canCreate());
				checkboxes.addComponent(create);
				
				update.addStyleName(ValoTheme.CHECKBOX_SMALL);
				update.setDescription("User can rename and overwrite files in this block");
				update.setValue(access.canUpdate());
				checkboxes.addComponent(update);
				
				delete.addStyleName(ValoTheme.CHECKBOX_SMALL);
				delete.setDescription("User can delete files in this block");
				delete.setValue(access.canDelete());
				checkboxes.addComponent(delete);
			}
			
			// Delete button
			remove = new Button(FontAwesome.TRASH_O);
			remove.addStyleName(ValoTheme.BUTTON_BORDERLESS);
			remove.addStyleName(ValoTheme.BUTTON_LARGE);
			remove.addClickListener(this);
			addComponent(remove);
			setComponentAlignment(remove, Alignment.MIDDLE_RIGHT);
		}
		
		public void setEditable(boolean editable) {
			read.setReadOnly(true); // "Read" is always read-only
			create.setReadOnly(!editable);
			update.setReadOnly(!editable);
			delete.setReadOnly(!editable);
			remove.setVisible(editable);
		}
		
		public ShareBlockAccess getValue() {
			access.setCanCreate(create.getValue());
			access.setCanDelete(delete.getValue());
			access.setCanUpdate(update.getValue());
			return access;
		}
		
		@Override
		public void buttonClick(ClickEvent event) {
			accessListLayout.removeComponent(this);
			refreshUserDropdown();
		}
	}
}
