package com.kbdunn.nimbus.web.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.bean.FileBean;
import com.kbdunn.nimbus.web.bean.FileBeanQuery;
import com.kbdunn.nimbus.web.files.MoveFilesDropHandler;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree;
import com.vaadin.ui.themes.ValoTheme;

public class FileTable extends Table implements Refreshable, ItemClickListener {
	
	private static final long serialVersionUID = 1L;
	
	protected FileContainer currentDirectory;
	private MoveFilesDropHandler dropHandler;
	private boolean dropEnabled = false;
	
	private List<NimbusFile> selected;
	private boolean selectionChanged = true;
	
	public FileTable() {
		//addStyleName(ValoTheme.TABLE_COMPACT);
		addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
		//addStyleName(ValoTheme.TABLE_BORDERLESS);
		buildFileTable();
	}
	
	public FileTable(NimbusFile currentDirectory) {
		this.currentDirectory = currentDirectory;
		buildFileTable();
		refresh();
	}
	
	protected void buildFileTable() {
		setSelectable(true);
		setMultiSelect(true);
		addItemClickListener(this);
		
		setSortEnabled(true);
		setColumnReorderingAllowed(true);
		setColumnCollapsingAllowed(true);
		
		setRowHeaderMode(RowHeaderMode.ICON_ONLY);
		
		setDragMode(TableDragMode.MULTIROW);
	}
	
	public boolean setCurrentContainer(FileContainer newDirectory) {
		if ((currentDirectory == null || !currentDirectory.equals(newDirectory))) {
			currentDirectory = newDirectory;
			refresh();
			return true;
		} 
		return false;
	}
	
	public FileContainer getCurrentDirectory() {
		return currentDirectory;
	}
	
	public void enableDropListener() {
		dropEnabled = true;
		dropHandler = new MoveFilesDropHandler(this);
		setDropHandler(dropHandler);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void refresh() {
		Container fileContainer;
		if (currentDirectory instanceof NimbusFile) {
			BeanQueryFactory<FileBeanQuery> queryFactory = new BeanQueryFactory<FileBeanQuery>(FileBeanQuery.class);
			Map<String, Object> config = new HashMap<String, Object>();
			config.put(FileBeanQuery.ROOT_CONTAINER_KEY, currentDirectory);
			queryFactory.setQueryConfiguration(config);
			fileContainer = new LazyQueryContainer(queryFactory, FileBean.PROPERTY_ITEM_ID, 30, false);
			((LazyQueryContainer) fileContainer).addContainerProperty(FileBean.PROPERTY_ICON, FontAwesome.class, FontAwesome.QUESTION, true, false);
			((LazyQueryContainer) fileContainer).addContainerProperty(FileBean.PROPERTY_NAME, String.class, "", true, false);
			((LazyQueryContainer) fileContainer).addContainerProperty(FileBean.PROPERTY_SIZE, String.class, "", true, false);
			((LazyQueryContainer) fileContainer).addContainerProperty(FileBean.PROPERTY_MODIFIED, String.class, "", true, false);
		} else {
			// ShareBlock - TODO: Lazy loading. Limited benefit...
			fileContainer = new BeanItemContainer<FileBean>(FileBean.class);
			List<NimbusFile> contents = NimbusUI.getFileShareService().getContents(((ShareBlock) currentDirectory));
			if (contents.size() == 1 && contents.get(0).isDirectory()) {
				// Block consists of only one directory. Skip a level and display folder contents
				contents = NimbusUI.getFileService().getContents(contents.get(0));
			}
			Collections.sort(contents);
			for (NimbusFile f: contents) {
				((BeanItemContainer<FileBean>) fileContainer).addBean(new FileBean(f));
			}
		}
		setContainerDataSource(fileContainer);
		
		setVisibleColumns(new Object[] { 
				FileBean.PROPERTY_NAME, FileBean.PROPERTY_SIZE, FileBean.PROPERTY_MODIFIED 
			});
		setColumnHeaders(new String [] { "Name", "Size", "Modified"});
		setColumnAlignments(Align.LEFT, Align.RIGHT, Align.RIGHT);
		setItemIconPropertyId(FileBean.PROPERTY_ICON);
		setColumnExpandRatio(FileBean.PROPERTY_ICON, .04f);
		setColumnExpandRatio(FileBean.PROPERTY_NAME, .61f);
		setColumnExpandRatio(FileBean.PROPERTY_SIZE, .10f);
		setColumnExpandRatio(FileBean.PROPERTY_MODIFIED, .25f);
		
		if (fileContainer.size() < 10)
			setPageLength(0);
		else
			setPageLength(10);
		
		if (dropEnabled) 
			dropHandler.setDroppableIds(getCurrentDirectoryFolderContents().toArray());
	}
	
	private List<NimbusFile> getCurrentDirectoryFolderContents() {
		if (currentDirectory instanceof NimbusFile) return NimbusUI.getFileService().getFolderContents((NimbusFile) currentDirectory);
		else if (currentDirectory instanceof ShareBlock) return NimbusUI.getFileShareService().getFolderContents((ShareBlock) currentDirectory);
		throw new IllegalStateException("Current directory is not an instance of NimbusFile or ShareBlock");
	}
	
	public List<NimbusFile> getSelectedFiles() {
		if (selectionChanged) {
			Collection<?> selectedItems = (Collection<?>) getValue(); 
			selected =  new ArrayList<NimbusFile>();
			// Single select is 0 length collection
			if (selectedItems.size() > 0) {
				for (Object o: selectedItems) {
					selected.add(((FileBean) o).getNimbusFile());
				}
			} else {
				if (getValue() instanceof FileBean)
					selected.add(((FileBean) getValue()).getNimbusFile());
			}
			selectionChanged = false;
		}
		return selected;
	}
	
	@SuppressWarnings("unchecked")
	public Tree getselectedItemsTree() {
		Tree selectedTree = new Tree("");
		selectedTree.addContainerProperty(FileBean.PROPERTY_NAME, String.class, "");
		selectedTree.setItemCaptionPropertyId(FileBean.PROPERTY_NAME);
		Collection<NimbusFile> selectedFiles = getSelectedFiles();
		
		for (NimbusFile nf: selectedFiles) {
			Item newItem = selectedTree.addItem(nf);
			newItem.getItemProperty(FileBean.PROPERTY_NAME).setValue(nf.getName());
			addChildItems(selectedTree, nf);
			selectedTree.setItemIcon(nf, NimbusFileTypeResolver.getIcon(nf));
		}
		return selectedTree;
	}
	
	@SuppressWarnings("unchecked")
	private void addChildItems(Tree tree, NimbusFile parent) {
		final List<NimbusFile> l = NimbusUI.getFileService().getFileContents(parent);
		if (l == null || l.size() == 0) {
			tree.setChildrenAllowed(parent, false);
			return;
		}
		
		tree.setChildrenAllowed(parent, true);
		Collections.sort(l);
		
		for (NimbusFile nf: l) {
			Item item = tree.addItem(nf);
			item.getItemProperty(FileBean.PROPERTY_NAME).setValue(nf.getName());
			tree.setParent(nf, parent);
			tree.setItemIcon(nf, NimbusFileTypeResolver.getIcon(nf));
			addChildItems(tree, nf);
		}
	}

	public void unselectAll() {
		for (Object selected : (Collection<?>) getValue())
			unselect(selected);
		selectionChanged = true;
	}
	
	@Override
	public void select(Object itemId) {
		super.select(itemId);
		selectionChanged = true;
	}
	
	@Override
	public void itemClick(ItemClickEvent event) {
		selectionChanged = true;
	}
}
