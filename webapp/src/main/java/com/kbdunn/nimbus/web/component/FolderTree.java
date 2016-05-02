package com.kbdunn.nimbus.web.component;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.files.MoveFilesDropHandler;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandListener;

public class FolderTree extends Tree implements Refreshable, ExpandListener {
		
	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger(FolderTree.class.getName());
	protected static final String PROPERTY_NAME = "name";
	
	private HierarchicalContainer folderContainer; 
	//private List<NFile> rootDirectories;
	private FileContainer rootContainer;
	private MoveFilesDropHandler dropHandler;
	
	public FolderTree() {
		buildFolderTree();
	}
	
	public FolderTree(FileContainer rootContainer) {
		this.rootContainer = rootContainer;
		buildFolderTree();
		refresh();
	}
	
	public void setRootContainer(FileContainer rootContainer) {
		this.rootContainer = rootContainer;
		refresh();
	}
	
	public FileContainer getRootContainer() {
		return rootContainer;
	}
	
	protected void buildFolderTree() {
		folderContainer = new HierarchicalContainer();
		folderContainer.addContainerProperty(PROPERTY_NAME, String.class, "");
		//folderContainer.addContainerProperty(FileBean.PROPERTY_FILE, File.class, new File(""));
		
		setContainerDataSource(folderContainer);
		setSelectable(true);
		setMultiSelect(false);
		setNullSelectionAllowed(false);
		addStyleName("folder-tree");
		
		setItemCaptionPropertyId(PROPERTY_NAME);
		
		addExpandListener(this);
		
		dropHandler = new MoveFilesDropHandler(this);
		setDropHandler(dropHandler);
	}
	
	@Override
	// TODO: Should probably add/remove individual items instead of whacking the whole thing
	public void refresh() {
		// Keep track of currently expanded items
		/*List<Object> expanded = new ArrayList<Object>();
		for (Object o: folderContainer.getItemIds()) 
			if (isExpanded(o))
				expanded.add(o);*/
		
		// Rebuild tree based on filesystem
		removeAllItems();
		//rootDirectories = rootDirectory.getChildFolders();
		addChildItems(rootContainer);
		/*Collections.sort(rootDirectories);
		for (NFile nf: rootDirectories) {
			FileBean newFile = new FileBean(nf);
			Item newItem = folderContainer.addItem(newFile);
			newItem.getItemProperty(FileBean.PROPERTY_NAME).setValue(nf.getName());
			//newItem.getItemProperty(FileBean.PROPERTY_FILE).setValue(nf);
			addChildItems(newFile);
			//expandItem(rootDirectories[i]);
			setItemIcon(newFile, NimbusFileTypeResolver.getIcon(newFile));
			//setItemIcon(rootDirectories[i], new ThemeResource(NimbusFileTypeResolver.DEFAULT_FOLDER_ICON));
		}*/
		//folderContainer.sort(new Object[] { FileBean.PROPERTY_UPPER_NAME}, new boolean[] {true});
		
		// Re-expand previously expanded items
		/*for (Object o: expanded) 
			expandItem(o);*/
	}
	
	protected void addChildItems(Object parent) {
		if (!(parent instanceof FileContainer)) return;
		FileContainer c = (FileContainer) parent;
		log.trace("Adding child items for " + parent);
		
		List<NimbusFile> l = null;
		if (c instanceof NimbusFile) l = NimbusUI.getFileService().getFolderContents((NimbusFile) c);
		else if (c instanceof ShareBlock) l = NimbusUI.getFileShareService().getFolderContents((ShareBlock) c);
		if (l.size() == 0) {
			log.trace("No child items found");
			setChildrenAllowed(parent, false);
			return;
		}
		Collections.sort(l);
		for (NimbusFile child : l)		
			addFolder(child, parent);
	}
	
	protected void addFolder(NimbusFile folder) {
		addFolder(folder, null);
	}
	
	@SuppressWarnings("unchecked")
	private void addFolder(NimbusFile folder, Object parent) {
		final Item item = addItem(folder);
		if (item == null) {
			// The node has been expanded before - no need to worry
			log.trace("Attempt to add child " + folder +" failed");
			return;
		}
		log.trace("Child item " + folder + " added");
		item.getItemProperty(PROPERTY_NAME).setValue(folder.getName());
		if (parent != null) setParent(folder, parent);
		setItemIcon(folder, NimbusFileTypeResolver.getIcon(folder));
		
		if (!NimbusUI.getFileService().folderContentsContainsFolder(folder)) 
			setChildrenAllowed(folder, false);
	}
	
	@Override
	public void nodeExpand(ExpandEvent event) {
		if (!hasChildren(event.getItemId())) {
			addChildItems(event.getItemId());
		}
	}
}