package com.kbdunn.nimbus.web.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.Tree.ItemStyleGenerator;

public class FileSelectTree extends Tree implements ExpandListener, ItemStyleGenerator, ItemClickListener {

	private static final long serialVersionUID = 7253286497427462596L;
	private static final Logger log = LogManager.getLogger(FileSelectTree.class.getName());
	private static final String PROPERTY_NAME = "name";
	
	private final FileService fileService;
	private final FileShareService shareService;
	private final UserService userService;
	private final FileSelectTreeController controller;
	private HierarchicalContainer fileContainer;
	private ArrayList<NimbusFile> currentSelection;
	
	public FileSelectTree(FileSelectTreeController controller) {
		this.controller = controller;
		fileService = NimbusUI.getFileService();
		shareService = NimbusUI.getFileShareService();
		userService = NimbusUI.getUserService();
		build();
	}
	
	private void build() {
		addStyleName("file-select-tree");
		fileContainer = new HierarchicalContainer();
		fileContainer.addContainerProperty(PROPERTY_NAME, String.class, "");
		
		setItemStyleGenerator(this);
		addItemClickListener(this);
		setContainerDataSource(fileContainer);
		setSelectable(false);
		setNullSelectionAllowed(false);
		setItemCaptionPropertyId(PROPERTY_NAME);
		addExpandListener(this);
	}
	
	void select(List<NimbusFile> selected) {
		Collections.sort(selected);
		log.debug("Selecting " + selected.size() + " files");
		for (NimbusFile nf : selected) {
			addParentItems(nf);
			select(nf);
		}
	}
	
	synchronized void select(NimbusFile file) {
		log.debug("Adding " + file + " to selection");
		currentSelection.add(file);
	}
	
	private synchronized void unselect(NimbusFile file) {
		for (NimbusFile nf : currentSelection) {
			if (nf.equals(file) || nf.getId().equals(file.getId())) {
				currentSelection.remove(nf);
				return;
			}
		}
	}
	
	/*private synchronized void unselect(ShareBlockFile file) {
		log.debug("Removing " + file + " from selection");
		currentSelection.remove(file);
	}*/
	
	public List<NimbusFile> getSelected() {
		return currentSelection;
	}
	
	public void refresh() {
		removeAllItems();
		currentSelection = new ArrayList<NimbusFile>();
		addChildItems(controller.getCurrentFileContainer());
		select(controller.getInitialSelection());
		markAsDirty();
		log.debug("REFRESH: " + currentSelection.size() + " files currently selected");
	}
	
	private void addParentItems(NimbusFile child) {
		final List<NimbusFile> rootItems = getCurrentRootItems();
		//NimbusFile userHome = userService.getUserHomeFolder(controller.getCurrentUser(), controller.getCurrentFileContainer());
		
		// Check if it's a root file (has no parent items)
		if (rootItems.contains(child))
			return;
		
		log.debug("Adding parent items for " + child);
		
		NimbusFile rootParent = null;
		for (NimbusFile root : rootItems) {
			if (fileService.fileIsChildOf(child, root)) {
				rootParent = root;
				break;
			}
		}
		// File is on another drive
		if (rootParent == null) {
			log.warn("Unable to select child that is stored on another drive " + child);
			return;
		}
		
		final NimbusFile immediateParent = fileService.getParentFile(child);
		NimbusFile nextParent = rootParent;
		boolean nextParentFound = false;
		List<NimbusFile> added = null;
		while (true) {
			added = addChildItems(nextParent);
			log.debug(added.size() + " child items added for " + nextParent.getName());
			if (nextParent.equals(immediateParent)) break;
			nextParentFound = false;
			for (NimbusFile nf : added) {
				if (nf.isDirectory() && fileService.fileIsChildOf(child, nf)) {
					nextParentFound = true;
					nextParent = nf;
					log.debug("Next parent is " + nf.getName());
					break;
				}
			}
			if (!nextParentFound) {
				log.warn("Something went wrong!! Could not find the next parent for child " + child + ". Parent was " + nextParent);
				break;
			}
		}
	}
	
	private List<NimbusFile> getCurrentRootItems() {
		return getChildItems(controller.getCurrentFileContainer());
	}
	
	private List<NimbusFile> getChildItems(FileContainer container) {
		List<NimbusFile> children = null;
		if (container instanceof StorageDevice) {
			children = fileService.getContents(userService.getUserHomeFolder(controller.getCurrentUser(), (StorageDevice) container));
		} else if (container instanceof ShareBlock) {
			children = shareService.getContents((ShareBlock) container);
		} else {
			children = fileService.getContents((NimbusFile) container);
		}
		
		return children;
	}
	
	private List<NimbusFile> addChildItems(FileContainer parent) {
		log.trace("Adding child items for " + parent);
		
		final List<NimbusFile> children = getChildItems(parent);
		
		if (children.size() == 0) {
			log.debug("No child files found");
			setChildrenAllowed(parent, false);
			return children;
		}
		Collections.sort(children);
		for (NimbusFile child : children)		
			addFile(child, parent);
		return children;
	}
	
	@SuppressWarnings("unchecked")
	private void addFile(NimbusFile nf, Object parent) {
		final Item item = addItem(nf);
		if (item == null) {
			// The node has been expanded before - no need to worry
			log.trace("Attempt to add child " + nf +" failed");
			return;
		}
		log.trace("Child item " + nf + " added");
		item.getItemProperty(PROPERTY_NAME).setValue(nf.getName());
		if (parent != null) setParent(nf, parent);
		setItemIcon(nf, NimbusFileTypeResolver.getIcon(nf));
		
		if (!fileService.folderHasContents(nf)) 
			setChildrenAllowed(nf, false);
	}
	
	@Override
	public void nodeExpand(ExpandEvent event) {
		if (!hasChildren(event.getItemId()))
			addChildItems((NimbusFile) event.getItemId());
	}
	
	@Override
	public String getStyle(Tree source, Object itemId) {
		if (!source.equals(this))
			return null;
		
		NimbusFile item = (NimbusFile) itemId;
		for (NimbusFile nf : currentSelection) {
			if (nf.getId().equals(item.getId())) {
				return "checked";
			} else if (fileService.fileIsChildOf(item, nf)) {
				return "disabled";
			} else if (fileService.fileIsChildOf(nf, item)) {
				return "indeterminate";
			}
		}
		return "unchecked";
	}
	
	@Override
	public void itemClick(ItemClickEvent event) {
		NimbusFile nf = (NimbusFile) event.getItemId();
		
		if (fileSelected(nf)) {
			unselect(nf);
			markAsDirty();
			
		} else if (!parentSelected(nf)) {
			select(nf);
			unselectChildren(nf);
			markAsDirty();
		}
	}
	
	private boolean fileSelected(NimbusFile file) {
		for (NimbusFile nf : currentSelection) {
			if (nf.getId().equals(file.getId())) return true;
		}
		return false;
	}
	
	private boolean parentSelected(NimbusFile child) {
		final List<NimbusFile> rootItems = getCurrentRootItems();
		NimbusFile walker = child;
		while (!rootItems.contains(walker)) {
			if (fileSelected(walker = fileService.getParentFile(walker))) 
				return true;
		}
		return false;
	}
	
	private void unselectChildren(NimbusFile parent) {
		Iterator<NimbusFile> i = currentSelection.iterator();
		while (i.hasNext()) {
			NimbusFile next = i.next();
			if (fileService.fileIsChildOf(next, parent)) {
				log.trace("Removing " + next + " from selection");
				i.remove();
			}
		}
	}
	
	public interface FileSelectTreeController {
		List<NimbusFile> getInitialSelection();
		FileContainer getCurrentFileContainer();
		NimbusUser getCurrentUser();
	}
}
