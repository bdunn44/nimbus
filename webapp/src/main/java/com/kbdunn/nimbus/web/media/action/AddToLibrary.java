package com.kbdunn.nimbus.web.media.action;

import java.util.Collections;
import java.util.List;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.component.FileContainerComboBox;
import com.kbdunn.nimbus.web.component.FileSelectTree;
import com.kbdunn.nimbus.web.component.FileSelectTree.FileSelectTreeController;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;

public class AddToLibrary extends VerticalLayout implements ClickListener, ValueChangeListener, FileSelectTreeController {
	
	private static final long serialVersionUID = -4373998473752406185L;
	public static final String CAPTION = "Add Files to Library";
	
	private Refreshable callback;
	private FileContainerComboBox driveSelect;
	private FileSelectTree fileSelect;
	private PopupWindow popup;
	
	public AddToLibrary() { 
		buildLayout();
	}
	
	public AddToLibrary(Refreshable callback) {
		this.callback = callback;
		buildLayout();
	}
	
	private void buildLayout() {
		setSpacing(true);
		driveSelect = new FileContainerComboBox(true);
		fileSelect = new FileSelectTree(this);
		
		driveSelect.setSizeFull();
		fileSelect.setSizeFull();
		
		addComponent(driveSelect);
		addComponent(fileSelect);
		
		driveSelect.addValueChangeListener(this);
	}
	
	private void refresh() {
		fileSelect.refresh();
		driveSelect.removeValueChangeListener(this);
		driveSelect.refresh();
		driveSelect.addValueChangeListener(this);
	}
	
	public void showDialog() {
		refresh();
		popup = new PopupWindow(CAPTION, this);
		popup.setSubmitCaption("Add Selection");
		popup.addSubmitListener(this);
		popup.open();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		FileService ns = NimbusUI.getFileService();
		for (NimbusFile nf : fileSelect.getSelected()) {
			nf.setIsLibraryRemoved(false);
			ns.reconcile(nf);
		}
		if (callback != null) callback.refresh();
		popup.close();
	}
	
	@Override
	public List<NimbusFile> getInitialSelection() {
		return Collections.emptyList();
	}
	
	@Override
	public FileContainer getCurrentFileContainer() {
		return driveSelect.getValue();
	}
	
	@Override
	public NimbusUser getCurrentUser() {
		return NimbusUI.getCurrentUser();
	}
	
	@Override
	public void valueChange(ValueChangeEvent event) {
		fileSelect.refresh();
	}
}