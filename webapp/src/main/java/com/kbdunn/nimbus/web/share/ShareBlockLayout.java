package com.kbdunn.nimbus.web.share;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.component.FileSelectTree;
import com.kbdunn.nimbus.web.component.FileSelectTree.FileSelectTreeController;
import com.kbdunn.nimbus.web.component.FileContainerComboBox;
import com.kbdunn.nimbus.web.files.FileManagerUri;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbsoluteLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ShareBlockLayout extends ShareBlockEditor implements FileSelectTreeController, ValueChangeListener {
	
	private static final long serialVersionUID = -4013433234981401572L;
	private static final String EXPANDED_HEIGHT = "450px";
	private static final String EXPANDED_WIDTH = "915px";
	private static final String COLLAPSED_HEIGHT = "110px";
	private static final String COLLAPSED_WIDTH = "325px";
	
	private StorageDevice currentDevice;
	private Map<StorageDevice, List<NimbusFile>> selectedFiles;
	
	protected AbsoluteLayout expandedLayout, collapsedLayout;
	protected Panel treePanel;
	private VerticalLayout blockDetailLayout;
	protected FileContainerComboBox driveSelect;
	protected FileSelectTree fileSelectTree;
	private Label noDrivesLabel;
	protected UserAccessLayout userAccessLayout;
	private Label iconLabel, nameLabel, subInfoLabel, filesLabel, foldersLabel, usersLabel, viewsLabel;
	
	private boolean expandedBuilt;
	
	protected ShareBlockLayout(ShareBlock block, ShareController controller) {
		super(block, controller);
		super.initializeComponents();
		super.setComponentSizeTiny();
		this.controller = controller;
		
		addStyleName("share-block-layout");
		addStyleName(ValoTheme.PANEL_WELL);
		setHeight(COLLAPSED_HEIGHT);
		setWidth(COLLAPSED_WIDTH);
		
		collapsedLayout = new AbsoluteLayout();
		collapsedLayout.addStyleName("collapsed-layout");
		collapsedLayout.setHeight(COLLAPSED_HEIGHT);
		collapsedLayout.setWidth(COLLAPSED_WIDTH);
		addComponent(collapsedLayout);
		buildCollapsedLayout();
		
		expandedLayout = new AbsoluteLayout();
		expandedLayout.addStyleName("expanded-layout");
		expandedLayout.setHeight(EXPANDED_HEIGHT);
		expandedLayout.setWidth(EXPANDED_WIDTH);
		addComponent(expandedLayout);
		expandedBuilt = false;
		refresh();
	}
	
	void expand() {
		if (!expandedBuilt) buildExpandedLayout();
		refresh();
		setHeight(EXPANDED_HEIGHT);
		setWidth(EXPANDED_WIDTH);
		addStyleName("expanded");
		removeStyleName("collapsed");
		userAccessLayout.refresh();
		name.setReadOnly(false);
		message.setReadOnly(false);
	}
	
	void collapse() {
		refresh();
		setHeight(COLLAPSED_HEIGHT);
		setWidth(COLLAPSED_WIDTH);
		addStyleName("collapsed");
		removeStyleName("expanded");
	}
	
	@Override
	protected void refresh() {
		super.refresh();
		if (expandedBuilt) {
			driveSelect.removeValueChangeListener(this);
			userAccessLayout.refresh();
			this.selectedFiles = controller.getBlockFilesByHardDrive(block);
			driveSelect.refresh();
			if (driveSelect.getItemIds().isEmpty()) {
				treePanel.setContent(noDrivesLabel);
			} else {
				treePanel.setContent(fileSelectTree);
				if (currentDevice == null) currentDevice = (StorageDevice) driveSelect.getValue();//driveSelect.select(driveSelect.getHardDrive(0));
				else driveSelect.select(currentDevice);
				fileSelectTree.refresh();
			}
			driveSelect.addValueChangeListener(this);
		}
		
		String subinfo = "";
		if (controller.currentUserIsBlockOwner(block)) {
			if (block.getCreated() != null ) subinfo = "created " + new SimpleDateFormat("M/d/yyyy 'at' h:mm a").format(block.getCreated());
		} else {
			subinfo = "owned by " + controller.getBlockOwner(block).getName();
		}
		
		if (block.getId() != null) {
			nameLabel.setValue("<b>" + block.getName() + "</b>");
			nameLabel.setDescription(block.getName());
			subInfoLabel.setValue("<span style='font-size:small;'>(" + subinfo + ")</span>");
			foldersLabel.setValue(String.format("%,d", controller.getBlockFolderCount(block)));
			filesLabel.setValue(String.format("%,d", controller.getBlockFileCount(block)));
			viewsLabel.setValue(String.format("%,d", block.getVisitCount()));
			usersLabel.setValue(String.format("%,d", controller.getBlockAccessCount(block)));
		}
	}
	
	void buildCollapsedLayout() {
		iconLabel = FontAwesome.CUBE.getLabel().setSize3x();
		iconLabel.setHeight("100%");
		iconLabel.setWidth("48px");
		iconLabel.addStyleName("block-icon");
		collapsedLayout.addComponent(iconLabel, "left:20px; top:0px; bottom:0px;"); 
		
		nameLabel = new Label();
		nameLabel.setContentMode(ContentMode.HTML);
		nameLabel.addStyleName("name-label");
		nameLabel.setWidth("195px");
		collapsedLayout.addComponent(nameLabel, "left:85px; top:10px;");
		
		subInfoLabel = new Label();
		subInfoLabel.setContentMode(ContentMode.HTML);
		collapsedLayout.addComponent(subInfoLabel, "left:85px; top:30px;");
		
		HorizontalLayout dtlLayout = new HorizontalLayout();
		dtlLayout.setSpacing(true);
		collapsedLayout.addComponent(dtlLayout, "left:85px; bottom:10px;");
		
		filesLabel = new Label();
		filesLabel.addStyleName("stats-label");
		filesLabel.addStyleName(ValoTheme.LABEL_TINY);
		filesLabel.setCaption("Files");
		dtlLayout.addComponent(filesLabel);
		dtlLayout.setComponentAlignment(filesLabel, Alignment.MIDDLE_CENTER);
		
		foldersLabel = new Label();
		foldersLabel.addStyleName("stats-label");
		foldersLabel.addStyleName(ValoTheme.LABEL_TINY);
		foldersLabel.setCaption("Folders");
		dtlLayout.addComponent(foldersLabel);
		dtlLayout.setComponentAlignment(foldersLabel, Alignment.MIDDLE_CENTER);
		
		usersLabel = new Label();
		usersLabel.addStyleName("stats-label");
		usersLabel.addStyleName(ValoTheme.LABEL_TINY);
		usersLabel.setCaption("Users");
		dtlLayout.addComponent(usersLabel);
		dtlLayout.setComponentAlignment(usersLabel, Alignment.MIDDLE_CENTER);
		
		viewsLabel = new Label();
		viewsLabel.addStyleName("stats-label");
		viewsLabel.addStyleName(ValoTheme.LABEL_TINY);
		viewsLabel.setCaption("Views");
		dtlLayout.addComponent(viewsLabel);
		dtlLayout.setComponentAlignment(viewsLabel, Alignment.MIDDLE_CENTER);
		
		collapsedLayout.addLayoutClickListener(new LayoutClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void layoutClick(LayoutClickEvent event) {
				viewBlock();
			}
		});
	}
	
	void buildExpandedLayout() {
		driveSelect = new FileContainerComboBox(false); // Don't add other share blocks as selection items
		driveSelect.setWidth("300px");
		driveSelect.addValueChangeListener(this);
		expandedLayout.addComponent(driveSelect, "top:0px; left:0px;");
		
		treePanel = new Panel();
		treePanel.addStyleName(ValoTheme.PANEL_WELL);
		treePanel.setWidth("300px");
		treePanel.setHeight("100%");
		treePanel.addStyleName("tree-panel");
		expandedLayout.addComponent(treePanel, "top:32px; bottom:0px; left:0px;");
		
		fileSelectTree = new FileSelectTree(this);
		fileSelectTree.setSizeFull();
		fileSelectTree.setCaption("Shared Files and Folders");
		treePanel.setContent(fileSelectTree);
		
		noDrivesLabel = new Label("No storage devices are available!");
		noDrivesLabel.addStyleName(ValoTheme.LABEL_H3);
		noDrivesLabel.setSizeUndefined();
		
		blockDetailLayout = new VerticalLayout();
		blockDetailLayout.addStyleName("block-detail-layout");
		blockDetailLayout.setWidth("500px");
		blockDetailLayout.setSpacing(true);
		expandedLayout.addComponent(blockDetailLayout, "left:325px; top:0px; bottom:0px;");
		
		CssLayout nameLayout = new CssLayout();
		blockDetailLayout.addComponent(nameLayout);
		nameLayout.addComponent(name);
		
		CssLayout msgLayout = new CssLayout();
		blockDetailLayout.addComponent(msgLayout);
		msgLayout.addComponent(message);
		
		CssLayout dateLayout = new CssLayout();
		blockDetailLayout.addComponent(dateLayout);
		dateLayout.addComponent(expires);
		
		blockDetailLayout.addComponent(shareExternally);
		blockDetailLayout.setComponentAlignment(shareExternally, Alignment.MIDDLE_LEFT);
		blockDetailLayout.addComponent(externalOptionsLayout);
		
		userAccessLayout = new UserAccessLayout(block);
		blockDetailLayout.addComponent(userAccessLayout);
		
		expandedBuilt = true;
	}

	void viewBlock() {
		UI.getCurrent().getNavigator().navigateTo(new FileManagerUri(block).getUri());
	}
	
	public List<NimbusFile> getSelectedFiles() {
		commitCurrentSelection();
		List<NimbusFile> sbnfs = new ArrayList<NimbusFile>();
		for (List<NimbusFile> hdnfs : selectedFiles.values()) {
			if (hdnfs != null && hdnfs.size() > 0) 
				sbnfs.addAll(hdnfs);
		}
		return sbnfs;
	}
	
	@Override
	public List<NimbusFile> getInitialSelection() {
		if (currentDevice == null) return Collections.emptyList();
		List<NimbusFile> selected = selectedFiles.get(currentDevice);
		if (selected == null) return Collections.emptyList();
		return selected;
	}
	
	@Override
	public FileContainer getCurrentFileContainer() {
		return (FileContainer) currentDevice;
	}
	
	@Override
	public NimbusUser getCurrentUser() {
		return NimbusUI.getCurrentUser();
	}
	
	// Drive select combo box
	@Override
	public void valueChange(ValueChangeEvent event) {
		commitCurrentSelection();
		currentDevice = (StorageDevice) driveSelect.getValue();
		fileSelectTree.refresh();
	}
	
	private void commitCurrentSelection() {
		if (currentDevice != null) {
			selectedFiles.put(currentDevice, fileSelectTree.getSelected());
		}
	}
}
