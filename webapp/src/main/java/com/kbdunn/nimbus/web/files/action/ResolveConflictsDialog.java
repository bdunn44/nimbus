package com.kbdunn.nimbus.web.files.action;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.interfaces.ConflictResolver;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.event.LayoutEvents.LayoutClickEvent;
import com.vaadin.event.LayoutEvents.LayoutClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ResolveConflictsDialog extends VerticalLayout implements ConflictResolver {

	private static final long serialVersionUID = -8470166295260644525L;
	
	private List<ResolutionListener> listeners;
	private List<FileConflict> conflicts;
	private FileDisplay sourceDisplay;
	private FileDisplay targetDisplay;
	private CopyDisplay copyDisplay;
	private CheckBox ubiquity;
	private PopupWindow popup;

	public ResolveConflictsDialog(List<FileConflict> conflicts) {		
		this.conflicts = conflicts;
		
		buildLayout();
	}
	
	public void showDialog() {
		String caption = "Resolve " + conflicts.size() + " file conflict";
		caption += conflicts.size() > 1 ? "s" : "";
		
		popup = new PopupWindow(caption, this);
		popup.hideSubmitButton();
		popup.open();

		displayConflict(0);
	}
	
	private void buildLayout() {

		setMargin(true);
		setSpacing(true);
		
		// Popup layout
		Label header = new Label("There is already a file with the same name in this location.");
		header.addStyleName(ValoTheme.LABEL_LARGE);
		addComponent(header);
		Label subHeader = new Label("Choose the file you want to keep, or rename it");
		subHeader.addStyleName(ValoTheme.LABEL_LIGHT);
		addComponent(subHeader);
		
		sourceDisplay = new FileDisplay(FileDisplay.SOURCE);
		sourceDisplay.setSizeUndefined();
		addComponent(sourceDisplay);
		
		targetDisplay = new FileDisplay(FileDisplay.TARGET);
		targetDisplay.setSizeUndefined();
		addComponent(targetDisplay);
		
		copyDisplay = new CopyDisplay();
		copyDisplay.setSizeUndefined();
		addComponent(copyDisplay);
		
		ubiquity = new CheckBox();
		addComponent(ubiquity);
		ubiquity.addStyleName(ValoTheme.CHECKBOX_LARGE);
		setComponentAlignment(ubiquity, Alignment.MIDDLE_LEFT);
		ubiquity.setVisible(false);
	}
	
	private void displayConflict(int index) {
		// Check if all conflicts have been resolved
		if (index == conflicts.size()) {
			fireResolutionEvent();
			return;
		}
		
		sourceDisplay.setCurrentConflict(index);
		targetDisplay.setCurrentConflict(index);
		copyDisplay.setCurrentConflict(index);
		
		int remaining = conflicts.size() - index - 1;
		if (conflicts.size() > 1) {
			String cap = "Do this for the next " + remaining + " conflict";
			cap += remaining > 1 ? "s" : "";
			ubiquity.setCaption(cap);
			if (remaining > 0) {
				ubiquity.setVisible(true);
			} else {
				ubiquity.setVisible(false);
			}
		}
	}
	
	private void resolveAll(int startIndex, FileConflict.Resolution resolution) {
		for (int i = startIndex; i < conflicts.size(); i++) {
			conflicts.get(i).setResolution(resolution);
		}
		fireResolutionEvent();
	}
	
	@Override
	public void addResolutionListener(ResolutionListener listener) {
		if (listeners == null) listeners = new ArrayList<ResolutionListener>();
		listeners.add(listener);
	}

	@Override
	public void removeResolutionListener(ResolutionListener listener) {
		if (listeners == null || !listeners.contains(listener)) return;
		listeners.remove(listener);
	}

	@Override
	public void fireResolutionEvent() {
		for (ResolutionListener listener: listeners) {
			listener.conflictsResolved(conflicts);
		}
		popup.close();
	}
	
	class FileDisplay extends HorizontalLayout implements LayoutClickListener {

		private static final long serialVersionUID = 6828833168358083390L;
		private static final String SOURCE = "source";
		private static final String TARGET = "target";
		
		private int currentConflict;
		private String type;
		private VerticalLayout details;
		private Label icon;
		private Label fileName;
		private Label filePath;
		private Label fileSize;
		
		public FileDisplay(String type) {
			addStyleName("file-conflict-display");
			this.type = type;
			addLayoutClickListener(this);
			
			icon = FontAwesome.FILE_O.getLabel().setSize3x();
			addComponent(icon);
			setComponentAlignment(icon, Alignment.MIDDLE_CENTER);
			
			String optionCaption = type.equals(SOURCE) ? "Replace the file" : "Skip";
			details = new VerticalLayout();
			Label option = new Label(optionCaption);
			option.addStyleName(ValoTheme.LABEL_LARGE);
			details.addComponent(option);
			fileName = new Label();
			fileName.addStyleName(ValoTheme.LABEL_SMALL);
			fileName.addStyleName(ValoTheme.LABEL_LIGHT);
			details.addComponent(fileName);
			filePath = new Label();
			filePath.addStyleName(ValoTheme.LABEL_SMALL);
			filePath.addStyleName(ValoTheme.LABEL_LIGHT);
			details.addComponent(filePath);
			fileSize = new Label();
			fileSize.addStyleName(ValoTheme.LABEL_SMALL);
			fileSize.addStyleName(ValoTheme.LABEL_LIGHT);
			details.addComponent(fileSize);
			addComponent(details);
		}
		
		public void setCurrentConflict(int conflict) {
			this.currentConflict = conflict;
			refresh();
		}
		
		private void refresh() {
			NimbusFile nf = type.equals(SOURCE) ? conflicts.get(currentConflict).getSource()
					: conflicts.get(currentConflict).getTarget();
			Label newIcon = NimbusFileTypeResolver.getIcon(nf).getLabel().setSize3x();
			replaceComponent(icon, newIcon);
			icon = newIcon;
			fileName.setValue(nf.getName());
			filePath.setValue(NimbusUI.getFileService().getParentFile(nf).getPath());
			fileSize.setValue(StringUtil.toHumanSizeString(nf.getSize()));
		}

		@Override
		public void layoutClick(LayoutClickEvent event) {
			FileConflict.Resolution resolution = type.equals(SOURCE) ? FileConflict.Resolution.REPLACE : FileConflict.Resolution.IGNORE;
			if (ubiquity.getValue()) {
				resolveAll(currentConflict, resolution);
				return;
			}
			conflicts.get(currentConflict).setResolution(resolution);
			displayConflict(currentConflict + 1);
		}
	}	

	class CopyDisplay extends VerticalLayout implements LayoutClickListener {

		private static final long serialVersionUID = 8969918112109280217L;
		private int currentConflict;
		private Label newFile;

		public CopyDisplay() {
			addStyleName("file-conflict-display");
			addLayoutClickListener(this);
			Label option = new Label("Rename the file");
			option.addStyleName(ValoTheme.LABEL_LARGE);
			addComponent(option);
			newFile = new Label();
			newFile.addStyleName(ValoTheme.LABEL_SMALL);
			newFile.addStyleName(ValoTheme.LABEL_LIGHT);
			addComponent(newFile);
		}
		
		public void setCurrentConflict(int conflict) {
			this.currentConflict = conflict;
			refresh();
		}
		
		private void refresh() {
			newFile.setValue("The new file will be named '" 
					+ NimbusUI.getFileService().getFileConflictResolution(conflicts.get(currentConflict)).getName()
					+ "'.");
		}

		@Override
		public void layoutClick(LayoutClickEvent event) {
			if (ubiquity.getValue()) {
				resolveAll(currentConflict, FileConflict.Resolution.COPY);
				return;
			}
			conflicts.get(currentConflict).setResolution(FileConflict.Resolution.COPY);
			displayConflict(currentConflict + 1);
		}
	}
}
