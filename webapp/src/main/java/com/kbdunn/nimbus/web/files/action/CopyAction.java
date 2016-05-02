package com.kbdunn.nimbus.web.files.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.component.FolderTree;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class CopyAction extends AbstractFileAction {
	
	private static final long serialVersionUID = 1L;
	
	protected FolderTree targetFolderTree;
	protected VerticalLayout popupLayout;
	protected List<NimbusFile> sources;
	
	public static FontAwesome ICON = FontAwesome.COPY;
	public static String CAPTION = "Copy Files";
	
	public CopyAction(final AbstractActionHandler handler) {
		super(handler);
		
		buildLayout();
	}
	
	public void setCopySources(List<NimbusFile> sources) {
		this.sources = sources;
	}
	
	private void buildLayout() {
		popupLayout = new VerticalLayout();
		popupLayout.setMargin(true);
		popupLayout.setSpacing(true);
		
		Label l = new Label("Select a target folder:");
		l.addStyleName(ValoTheme.LABEL_LARGE);
		popupLayout.addComponent(l);
		
		targetFolderTree = new FolderTree();
		targetFolderTree.addStyleName(ValoTheme.TREETABLE_SMALL);
		popupLayout.addComponent(targetFolderTree);
		popupLayout.setComponentAlignment(targetFolderTree, Alignment.MIDDLE_CENTER);
	}

	// Check that a target folder is selected
	protected boolean canDoAction() {
		if (targetFolderTree.getValue() == null) {
			displayError(new UserError("Select a target folder"));
			return false;
		} else {
			return true;
		}
	}
	
	@Override
	public void doAction() {
		if (getActionHandler().canProcess(this) && canDoAction()) {
			NimbusFile targetFolder = (NimbusFile) targetFolderTree.getValue();
			((CopyActionProcessor) getActionHandler()).processCopy(sources, targetFolder);
		}
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		targetFolderTree.setComponentError(e);
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
	public void refresh() {
		targetFolderTree.setRootContainer(((CopyActionProcessor) getActionHandler()).getRootContainer());
		targetFolderTree.setComponentError(null);
	}
	
	@Override
	public AbstractComponentContainer getPopupLayout() {
		return popupLayout;
	}

	@Override
	public Category getCategory() {
		return Category.MANAGE;
	}
}
