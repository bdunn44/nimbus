package com.kbdunn.nimbus.web.files.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DeleteAction extends AbstractFileAction {
	
	private static final long serialVersionUID = 1L;
	public static final FontAwesome ICON = FontAwesome.TRASH;
	public static final String CAPTION = "Delete Files";
	
	private List<NimbusFile> targetFiles;
	private VerticalLayout popupLayout;
	
	public DeleteAction(final AbstractActionHandler handler) {
		super(handler);

		popupLayout = new VerticalLayout();
		popupLayout.addStyleName("delete-action-layout");
		popupLayout.setMargin(true);
		popupLayout.setSpacing(true);
	}
	
	public void setTargetFiles(List<NimbusFile> targetFiles) {
		this.targetFiles = targetFiles;
	}
	
	public String getDescription() {
		if (targetFiles == null || targetFiles.isEmpty()) 
			return null;
		
		// Get stats on selected files for super smart dynamic caption
		boolean containsChildren = false;
		boolean multipleFiles = targetFiles.size() > 1;
		int fileCount = 0;
		int folderCount = 0;
		
		for (NimbusFile nf: targetFiles) {
			if (nf.isDirectory()) folderCount++;
			else fileCount++;
			containsChildren = NimbusUI.getFileService().folderHasContents(nf) ? true : containsChildren;
		}
		
		// Build super smart dynamic caption
		String windowCaption = "Delete ";
		windowCaption += multipleFiles ? "these " : "this ";
		windowCaption += fileCount > 0 ? "file": "";
		windowCaption += fileCount > 1 || (fileCount > 0 && multipleFiles) ? "s" : "";
		windowCaption += fileCount > 0 && folderCount > 0 ? " and " : "";
		windowCaption += folderCount > 0 ? "folder" : "";
		windowCaption += folderCount > 1 || (folderCount > 0 && multipleFiles) ? "s" : "";
		windowCaption += containsChildren ? " (including folder contents)?" : "?";
		return windowCaption;
	}

	@Override
	public FontAwesome getIcon() {
		return ICON;
	}

	@Override
	public String getCaption() {
		return CAPTION;
	}

	@Override
	public AbstractComponentContainer getPopupLayout() {
		return popupLayout;
	}
	
	@Override
	public void refresh() {
		popupLayout.removeAllComponents();

		Label l = new Label(getDescription());
		l.addStyleName(ValoTheme.LABEL_LARGE);
		popupLayout.addComponent(l);
		
		for (NimbusFile f : targetFiles) {
			Label lf = new Label (f.getName());
			lf.addStyleName("label-inline-icon");
			lf.setIcon(NimbusFileTypeResolver.getIcon(f));
			popupLayout.addComponent(lf);
		}
	}
	
	@Override
	public void doAction() {
		if (getActionHandler().canProcess(this))
			((DeleteActionProcessor) getActionHandler()).processDelete(targetFiles);
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		throw new UnsupportedOperationException("This action cannot display error messages");
	}

	@Override
	public Category getCategory() {
		return Category.MANAGE;
	}
}