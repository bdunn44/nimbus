package com.kbdunn.nimbus.web.share;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.error.Error;
import com.kbdunn.nimbus.web.files.action.DownloadAction;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.data.Item;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.UserError;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.Table.Align;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.Tree.ExpandListener;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ExternalShareView extends Panel implements View {

	private static final long serialVersionUID = 2201122232325416750L;
	public static final String NAME = "ext/" + ShareView.NAME + "/t";
	private static final int PAGE_LENGTH = 10;
	
	private static final String FILE_PROPERTY = "File";
	private static final String SIZE_PROPERTY = "Size";
	private static final String DL_PROPERTY = "Download";
	private static final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("EEEE, MMMM d YYYY @ h:mm a");

	private ExternalShareController controller;
	private VerticalLayout content;
	private TreeTable fileTreeTable;
	
	public ExternalShareView(ExternalShareController controller) {
		this.controller = controller;
		addStyleName("ext-share-view");
		content = new VerticalLayout();
		content.setMargin(true);
		content.setSpacing(true);
		setContent(content);
	}
	
	@Override
	public void enter(ViewChangeEvent event) {
		content.removeAllComponents();
		
		controller.parseFragment(event.getParameters());
		
		if (controller.getCurrentBlock() == null || !controller.getCurrentBlock().isExternal()) {
			showBadAddress();
			return;
		}
		if (controller.getCurrentBlock().getExpirationDate() != null && controller.getCurrentBlock().getExpirationDate().before(new Date())) {
			showExpired();
			return;
		}
		buildHeader();
		if (controller.getCurrentBlock().getPasswordDigest() == null) {
			displayShareBlock();
		} else {
			showPasswordPrompt();
		}
	}
	
	void refresh() {
		fileTreeTable.removeAllItems();
		List<NimbusFile> sharedFiles = controller.getBlockContents();
		Collections.sort(sharedFiles);
		for (NimbusFile file: sharedFiles) {
			addFile(file, null);
		}
		if (fileTreeTable.size() < PAGE_LENGTH) {
			fileTreeTable.setPageLength(0);
		} else {
			fileTreeTable.setPageLength(PAGE_LENGTH);
		}
	}
	
	private void buildHeader() {
		// Name
		Label name = new Label(controller.getCurrentBlock().getName());
		name.addStyleName(ValoTheme.LABEL_H1);
		name.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		name.setWidthUndefined();		
		content.addComponent(name);
		content.setComponentAlignment(name, Alignment.MIDDLE_CENTER);
		
		// Info
		int foSize = controller.getBlockFolderCount();
		int fiSize = controller.getBlockFileCount();
		String info;
		if (foSize == 0 && fiSize == 0) {
			info = "This share is empty";
		} else {
			info = "This share contains ";
			if (foSize > 0) {
				info += foSize + " folder";
				info += foSize > 1 ? "s" : "";
			}
			if (fiSize > 0) {
				if (foSize > 0) info += " (";
				info += fiSize + " file";
				info += fiSize > 1 ? "s" : "";
				if (foSize > 0) info += ")";
			}
		}
		
		info += controller.getCurrentBlock().getExpirationDate() != null ? 
				" Expires " + new DateTime(controller.getCurrentBlock().getExpirationDate()).toString(dateFormatter) : "";
		Label dtl = new Label(info);
		dtl.addStyleName(ValoTheme.LABEL_LIGHT);
		dtl.setWidthUndefined();
		content.addComponent(dtl);
		content.setComponentAlignment(dtl, Alignment.MIDDLE_CENTER);
		
		// Message
		if (controller.getCurrentBlock().getMessage() != null) {
			TextArea message = new TextArea();
			message.setValue(controller.getCurrentBlock().getMessage());
			message.setReadOnly(true);
			message.addStyleName(ValoTheme.TEXTAREA_BORDERLESS);
			message.addStyleName(ValoTheme.TEXTAREA_ALIGN_CENTER);
			message.setWidth("90%");
			content.addComponent(message);
			content.setComponentAlignment(message, Alignment.MIDDLE_CENTER);
		}
	}
	
	private void showBadAddress() {
		UI.getCurrent().getNavigator().navigateTo(Error.SHARE_NOT_FOUND.getPath());
	}
	
	private void showExpired() {
		Label h2 = new Label("This File Share has Expired!");
		h2.addStyleName(ValoTheme.LABEL_H1);
		h2.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		h2.setWidthUndefined();
		Label dtl = new Label("Don't shoot the messenger...");
		dtl.addStyleName("detail");
		dtl.addStyleName(ValoTheme.LABEL_LIGHT);
		dtl.setWidthUndefined();
		content.addComponent(h2);
		content.setComponentAlignment(h2, Alignment.MIDDLE_CENTER);
		content.addComponent(dtl);
		content.setComponentAlignment(dtl, Alignment.MIDDLE_CENTER);
	}
	
	private void showPasswordPrompt() {
		final HorizontalLayout loginLayout = new HorizontalLayout();
		loginLayout.setMargin(true);
		loginLayout.setSpacing(true);
		final PasswordField pw = new PasswordField("What's the password?");
		Button submit = new Button("Go!", new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				try {
					if (NimbusUI.getUserService().validatePassword(pw.getValue(), controller.getCurrentBlock().getPasswordDigest())) {
						content.removeComponent(loginLayout);
						displayShareBlock();
					} else {
						pw.setComponentError(new UserError("Invalid Password"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		});
		loginLayout.addComponent(pw);
		loginLayout.setComponentAlignment(pw, Alignment.BOTTOM_CENTER);
		loginLayout.addComponent(submit);
		loginLayout.setComponentAlignment(submit, Alignment.BOTTOM_CENTER);
		content.addComponent(loginLayout);
		content.setComponentAlignment(loginLayout, Alignment.TOP_CENTER);
		submit.setClickShortcut(KeyCode.ENTER);
	}
	
	private void displayShareBlock() {
		//ZipDownloadButton downloadAll = new ZipDownloadButton("Download all content as a ZIP file", controller.getCurrentBlock().getNimbusFiles(), "NimbusFiles.zip");
		//downloadAll.addStyleName(ValoTheme.BUTTON_SMALL);
		//content.addComponent(downloadAll);
		
		if (controller.getCurrentBlock().isExternalUploadAllowed()) {
			Button upload = controller.getUploadAction().getButton();
			upload.setCaption("Upload files");
			content.addComponent(upload);
		}
		
		fileTreeTable = new TreeTable();
		fileTreeTable.setWidth("100%");
		fileTreeTable.addContainerProperty(FILE_PROPERTY, HorizontalLayout.class, "");
		fileTreeTable.addContainerProperty(SIZE_PROPERTY, String.class, "");
		fileTreeTable.addContainerProperty(DL_PROPERTY, Button.class, null);
		fileTreeTable.setColumnAlignments(new Align[] { Align.LEFT, Align.CENTER, Align.CENTER });
		fileTreeTable.setColumnWidth(DL_PROPERTY, 150);
		fileTreeTable.setColumnWidth(SIZE_PROPERTY, 75);
		//fileTreeTable.setRowHeaderMode(RowHeaderMode.EXPLICIT);
		
		fileTreeTable.addExpandListener(new ExpandListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void nodeExpand(ExpandEvent event) {
				NimbusFile folder = (NimbusFile) event.getItemId();
				if (!folder.isDirectory()) return;
				List<NimbusFile> contents = controller.getFolderContents(folder);
				Collections.sort(contents);
				for (NimbusFile f : contents)
					addFile(f, folder);
				
				if (fileTreeTable.size() < PAGE_LENGTH) {
					fileTreeTable.setPageLength(0);
				} else {
					fileTreeTable.setPageLength(PAGE_LENGTH);
				}
			}
		});
		
		content.addComponent(fileTreeTable);
		controller.incrementVisitCount();
		refresh();
	}
	
	@SuppressWarnings("unchecked")
	private void addFile(NimbusFile file, NimbusFile parent) {
		Item i = fileTreeTable.addItem(file);
		if (i == null) {
			// Item has already been added (parent expanded) no worries
			return;
		}
		HorizontalLayout nameLayout = new HorizontalLayout();
		nameLayout.setSpacing(true);
		Label icon = NimbusFileTypeResolver.getIcon(file).getLabel();
		nameLayout.addComponent(icon);
		nameLayout.setComponentAlignment(icon, Alignment.MIDDLE_CENTER);
		Label name = new Label(file.getName());
		nameLayout.addComponent(name);
		nameLayout.setComponentAlignment(name, Alignment.MIDDLE_LEFT);
		i.getItemProperty(FILE_PROPERTY).setValue(nameLayout);
		
		if (parent != null) {
			fileTreeTable.setChildrenAllowed(parent, true);
			fileTreeTable.setParent(file, parent);
		}
		
		if (file.isDirectory()) {
			if (controller.getFolderContents(file).isEmpty()) {
				fileTreeTable.setChildrenAllowed(file, false);
			} /*else {
				ZipDownload dl = new ZipDownload("Download Folder", file, "NimbusFiles.zip");
				i.getItemProperty(DL_PROPERTY).setValue(dl);
			}*/
		} else {
			final DownloadAction dl = new DownloadAction(file);
			Button dlb = new Button();
			dlb.addStyleName(ValoTheme.BUTTON_TINY);
			dlb.addClickListener(new ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(ClickEvent event) {
					dl.getPopupWindow().open();
				}
			});
			
			if (controller.fileExistsOnDisk(file)) { 
				dlb.setCaption("Download File");
			} else {
				dlb.setCaption("File Unavailable");
				dlb.setEnabled(false);
			}
			i.getItemProperty(SIZE_PROPERTY).setValue(StringUtil.toHumanSizeString(file.getSize()));
			i.getItemProperty(DL_PROPERTY).setValue(dlb);
			fileTreeTable.setChildrenAllowed(file, false);
		}
	}
}