package com.kbdunn.nimbus.web.files;

import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.bean.FileBean;
import com.kbdunn.nimbus.web.component.BreadCrumbs;
import com.kbdunn.nimbus.web.component.FileContainerComboBox;
import com.kbdunn.nimbus.web.component.FileTable;
import com.kbdunn.nimbus.web.component.FolderTree;
import com.kbdunn.nimbus.web.controlbar.FileControlBar;
import com.kbdunn.nimbus.web.files.action.DownloadAction;
import com.kbdunn.nimbus.web.files.action.EditTextFileAction;
import com.kbdunn.nimbus.web.files.action.FileContextMenu;
import com.kbdunn.nimbus.web.files.action.ViewImagesAction;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class FileManagerLayout extends VerticalLayout implements ItemClickListener, ValueChangeListener {

	private static final long serialVersionUID = -5313416268606013295L;
	private static final Logger log = LogManager.getLogger(FileManagerLayout.class.getName());
	
	protected FileManagerController controller;
	protected FileContainerComboBox driveSelect;
	protected FileTable fileTable;
	protected FolderTree folderTree;
	protected BreadCrumbs breadCrumbs;
	protected FileControlBar controlBar;
	protected FileContextMenu contextMenu;
	
	private HorizontalLayout topRow, browsers;
	private CssLayout spacer;
	private Panel folderPanel;
	protected Label containerDescLabel;
	
	boolean layoutBuilt = false;
	
	public FileManagerLayout(FileManagerController controller) {
		this.controller = controller;
	}
	
	public void refresh() {
		fileTable.refresh();
		folderTree.refresh();
		controlBar.refresh();
		refreshDriveSelector();
		
		setFolderBrowserVisible(folderTree.getContainerDataSource().getItemIds().size() > 0);		
		setDriveSelectVisible(driveSelect.getItemIds().size() > 1);
	}
	
	public void setFolderBrowserVisible(boolean visible) {
		if (visible) {
			if (driveSelect.isAttached())
				topRow.setExpandRatio(driveSelect, .25f);
			else if (spacer.isAttached())
				topRow.setExpandRatio(spacer, .25f);
			topRow.setExpandRatio(containerDescLabel, .40f);
			topRow.setExpandRatio(controlBar, .35f);
			
			browsers.setExpandRatio(folderPanel, .25f);
			browsers.setExpandRatio(fileTable, .75f);
			
		} else {
			if (driveSelect.isAttached())
				topRow.setExpandRatio(driveSelect, .0f);
			else if (spacer.isAttached())
				topRow.setExpandRatio(spacer, .0f);
			topRow.setExpandRatio(containerDescLabel, .65f);
			topRow.setExpandRatio(controlBar, .35f);
			
			browsers.setExpandRatio(folderPanel, 0f);
			browsers.setExpandRatio(fileTable, 1f);
			
		}
	}
	
	public void setDriveSelectVisible(boolean visible) {
		if (visible && !driveSelect.isAttached()) {
			topRow.replaceComponent(spacer, driveSelect);
			topRow.setExpandRatio(driveSelect, .25f);
		} else if (!visible && driveSelect.isAttached()) {
			topRow.replaceComponent(driveSelect, spacer);
			topRow.setExpandRatio(spacer, .25f);
		}
	}
	
	public FileTable getFileTable() {
		return fileTable;
	}
	
	public FolderTree getFolderTree() {
		return folderTree;
	}
	
	public BreadCrumbs getBreadCrumbs() {
		return breadCrumbs;
	}
	
	public Label getContainerDescriptionLabel() {
		return containerDescLabel;
	}
	
	public List<NimbusFile> getSelectedFiles() {
		return fileTable.getSelectedFiles();
	}
	
	public void buildLayout() {
		if (layoutBuilt) return;
		
		initialize();
		setSizeFull();
		addStyleName("file-manager");
		
		// Bread Crumbs
		addComponent(breadCrumbs);
		
		topRow = new HorizontalLayout();
		topRow.addStyleName("top-row");
		topRow.setWidth("100%");
		topRow.setSpacing(true);
		addComponent(topRow);
		
		/*spacer = new CssLayout();
		topRow.addComponent(spacer);*/
		driveSelect.setWidth("100%");
		topRow.addComponent(driveSelect);
		topRow.setComponentAlignment(driveSelect, Alignment.MIDDLE_CENTER);
		
		containerDescLabel = new Label();
		containerDescLabel.addStyleName("container-desc");
		containerDescLabel.setContentMode(ContentMode.HTML);
		topRow.addComponent(containerDescLabel);
		topRow.setComponentAlignment(containerDescLabel, Alignment.MIDDLE_LEFT);
		
		topRow.addComponent(controlBar);
		topRow.setComponentAlignment(controlBar, Alignment.MIDDLE_RIGHT);
		
		// Layout for File and Folder Browsers
		browsers = new HorizontalLayout();
		browsers.setSpacing(true);
		browsers.setWidth("100%");
		browsers.addStyleName("browser");
		addComponent(browsers);
		
		// Folders
		folderPanel = new Panel();
		folderPanel.addStyleName(ValoTheme.PANEL_WELL);
		folderPanel.setCaption("Browse Folders");
		folderPanel.setContent(folderTree);
		browsers.addComponent(folderPanel);
		folderTree.setSizeFull();
		
		// Files
		fileTable.setWidth("100%");
		browsers.addComponent(fileTable);
		
		setFolderBrowserVisible(true);
		layoutBuilt = true;
	}
	
	private void initialize() {
		log.trace("Initializing components...");
		driveSelect = new FileContainerComboBox(true);
		driveSelect.addValueChangeListener(this);
		
		fileTable = new FileTable();
		folderTree = new FolderTree();
		breadCrumbs = new BreadCrumbs();
		//breadCrumbs.setViewName(FileView.NAME);
		controlBar = new FileControlBar(controller);
		
		folderTree.addItemClickListener(this);
		folderTree.addStyleName("no-vertical-drag-hints");
		folderTree.addStyleName("no-horizontal-drag-hints");
		
		fileTable.addItemClickListener(this);
		contextMenu = new FileContextMenu(controller);
		fileTable.addActionHandler(contextMenu);
		
		spacer = new CssLayout();
	}
	
	@Override
	public void itemClick(ItemClickEvent event) {
		
		if (event.isDoubleClick()) {
			
			NimbusFile clicked = null;
			if (event.getSource() instanceof FolderTree)
				clicked = (NimbusFile) event.getItemId();
			else
				clicked = ((FileBean) event.getItemId()).getNimbusFile();
			
			// Navigate to directory
			if (clicked.isDirectory()){
				log.debug("Clicked directory " + clicked);
				controller.navigateToDirectory(clicked);
			
			// Edit a text file
			} else if (NimbusFileTypeResolver.isPlainTextFile(clicked)) {
				EditTextFileAction action = controller.getAction(EditTextFileAction.class);
				if (action != null) action.editTextFile(clicked);
				
			// View an image
			} else if (clicked.isImage()) {
				ViewImagesAction action = controller.getAction(ViewImagesAction.class);
				if (action != null) action.viewImage(clicked);
			
			// Download a file
			} else {
				DownloadAction action = controller.getAction(DownloadAction.class);
				if (action != null) controller.handle(action);
			}
		}
	}

	void refreshDriveSelector() {
		if (!layoutBuilt) return;
		driveSelect.removeValueChangeListener(this);
		driveSelect.refresh();
		driveSelect.select(controller.getRootContainer());
		driveSelect.addValueChangeListener(this);
	}
	
	@Override
	public void valueChange(ValueChangeEvent event) {
		FileManagerController.navigateToFileContainer(driveSelect.getValue());
	}
}