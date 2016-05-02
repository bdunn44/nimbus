package com.kbdunn.nimbus.web.media.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.kbdunn.nimbus.web.util.NimbusFileTypeResolver;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class RemoveFromLibrary extends VerticalLayout implements ClickListener {
	
	private static final long serialVersionUID = -7967867707863101285L;
	public static final String CAPTION = "Remove Files from Library";
	
	private MediaController controller;
	private PopupWindow popup;
	private Label info;
	private VerticalLayout list;
	private Button hardDelete;
	private List<NimbusFile> targetFiles;
	
	public RemoveFromLibrary(final MediaController controller) {
		this.controller = controller;
		
		info = new Label();
		list = new VerticalLayout();
		
		info.setSizeUndefined();
		list.setSizeFull();
		list.setMargin(true);
		list.setSpacing(true);
		
		addComponent(info);
		addComponent(list);

		hardDelete = new Button("Permanently Delete");
		hardDelete.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				for (NimbusFile nf : targetFiles)
					NimbusUI.getFileService().delete(nf);
				controller.refresh();
				popup.close();
			}
		});
	}
	
	private void refresh() {
		targetFiles = controller.getSelectedMediaFiles();
		boolean plural = targetFiles.size() != 1;
		info.setValue(
				"Really remove " 
				+ (plural ? "these" : "this") 
				+ " song" + (plural ? "s" : "")
				+ " from your library?");
		list.removeAllComponents();
		Label nfl;
		for (NimbusFile nf : targetFiles) {
			nfl = new Label(nf.getName());
			nfl.setIcon(NimbusFileTypeResolver.getIcon(nf));
			nfl.addStyleName("label-inline-icon");
			list.addComponent(nfl);
		}
	}
	
	public void showDialog() {
		refresh();
		popup = new PopupWindow(CAPTION, this);
		popup.setSubmitCaption("Remove from Library");
		popup.addSubmitListener(this);
		popup.addCustomAction(hardDelete);
		popup.open();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		FileService fs = NimbusUI.getFileService();
		for (NimbusFile nf : targetFiles) {
			nf.setIsLibraryRemoved(true);
			fs.save(nf);
		}
		controller.refresh();
		popup.close();
	}
}