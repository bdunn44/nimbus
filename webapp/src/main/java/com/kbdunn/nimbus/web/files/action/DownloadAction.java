package com.kbdunn.nimbus.web.files.action;

import java.io.File;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class DownloadAction extends AbstractFileAction {

	private static final long serialVersionUID = 1L;
	public static final FontAwesome ICON = FontAwesome.CLOUD_DOWNLOAD;
	public static final String CAPTION = "Download File";
	
	private NimbusFile targetFile;
	private VerticalLayout popupLayout;
	private Label info;
	private Button dl;
	private FileDownloader fileDownloader;
	
	public DownloadAction(AbstractActionHandler controller) {
		super(controller);
		buildLayout();
	}
	
	public DownloadAction(NimbusFile file) {
		buildLayout();
		setTargetFile(file);
	}
	
	private void buildLayout() {
		popupLayout = new VerticalLayout();
		popupLayout.setMargin(true);
		popupLayout.setSpacing(true);
		
		info = new Label();
		popupLayout.addComponent(info);
		popupLayout.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
		dl = new Button("Download");
		dl.setSizeFull();
		popupLayout.addComponent(dl);
		popupLayout.setComponentAlignment(dl, Alignment.MIDDLE_CENTER);
		super.popupWindow.hideButtons();
	}
	
	public void setTargetFile(NimbusFile targetFile) {
		this.targetFile = targetFile;
		if (fileDownloader == null) {
			fileDownloader = new FileDownloader(new FileResource(new File(targetFile.getPath())));
			fileDownloader.extend(dl);
		} else {
			fileDownloader.setFileDownloadResource(new FileResource(new File(targetFile.getPath())));
		}
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
		info.setValue("Download '" + targetFile.getName() + "'");
		fileDownloader.setFileDownloadResource(new FileResource(new File(targetFile.getPath())));
	}

	@Override
	public void doAction() {
		// Just close the popup
		super.popupWindow.close();
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		throw new UnsupportedOperationException("This action cannot display errors to users");
	}
	
	@Override
	public Category getCategory() {
		return Category.TRANSFER;
	}
}
