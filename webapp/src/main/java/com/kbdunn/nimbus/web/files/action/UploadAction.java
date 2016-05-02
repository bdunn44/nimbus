package com.kbdunn.nimbus.web.files.action;

import java.util.Iterator;

import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.files.ClassicUploadField;
import com.kbdunn.nimbus.web.files.DragAndDropUploadField;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.themes.ValoTheme;

public class UploadAction extends AbstractFileAction {
	
	private static final long serialVersionUID = 1L;
	
	private VerticalLayout popupLayout;
	private TabSheet content;
	private DragAndDropUploadField ddupload;
	private VerticalLayout classicLayout;
	private Button addUpload;
	
	public static final FontAwesome ICON = FontAwesome.CLOUD_UPLOAD;
	public static final String CAPTION = "Upload Files";
	
	public UploadAction(final AbstractActionHandler handler) {
		super(handler);

		if (!getActionHandler().canProcess(this)) {
			throw new IllegalArgumentException("The action handler cannot process upload operations.");
		}
		buildLayout();
		
		super.popupWindow.addCloseListener(new CloseListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void windowClose(CloseEvent e) {
				content.removeComponent(ddupload);
				NimbusUI.getCurrent().addHiddenComponent(ddupload);
				Iterator<Component> cit = classicLayout.iterator();
				while (cit.hasNext()) {
					Component next = cit.next();
					if (next instanceof ClassicUploadField) {
						cit.remove();
						NimbusUI.getCurrent().addHiddenComponent((AbstractComponent) next);
					}
				}
			}
		});
	}
	
	private void buildLayout() {
		popupLayout = new VerticalLayout();
		popupLayout.addStyleName("upload-dialog");
		popupLayout.setMargin(true);
		popupLayout.setSpacing(true);
		
		content = new TabSheet();
		content.setSizeFull();
		popupLayout.addComponent(content);
		popupLayout.setComponentAlignment(content, Alignment.TOP_CENTER);
		
		// Drag n Drop upload
		ddupload = new DragAndDropUploadField((UploadActionProcessor) getActionHandler());
		content.addTab(ddupload, "Drag and Drop Upload");
		
		// Classic upload
		classicLayout = new VerticalLayout();
		content.addTab(classicLayout, "Classic Upload");
		
		// Button to add an additional upload field
		addUpload = new Button("Upload Another File");
		addUpload.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				// Remove, add, add to keep button on bottom
				classicLayout.removeComponent(addUpload);
				ClassicUploadField classicupload = new ClassicUploadField((UploadActionProcessor) getActionHandler());
				classicLayout.addComponent(classicupload);
				classicLayout.addComponent(addUpload);
			}
		});
		addUpload.addStyleName("add-upload");
		
		// Overwrite warning
		Label warn = new Label("WARNING!! Existing files are automatically overwritten!");
		warn.setIcon(FontAwesome.ASTERISK);
		warn.addStyleName("upload-warning");
		warn.addStyleName("label-inline-icon");
		warn.addStyleName(ValoTheme.LABEL_SMALL);
		warn.addStyleName(ValoTheme.LABEL_LIGHT);
		popupLayout.addComponent(warn);
		popupLayout.setComponentAlignment(warn, Alignment.BOTTOM_CENTER);
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
		content.removeAllComponents();
		
		// Drag n Drop upload
		ddupload = new DragAndDropUploadField((UploadActionProcessor) getActionHandler());
		content.addTab(ddupload, "Drag and Drop Upload");
		
		// Classic upload
		classicLayout = new VerticalLayout();
		content.addTab(classicLayout, "Classic Upload");
		classicLayout.removeAllComponents();
		ClassicUploadField classicupload = new ClassicUploadField((UploadActionProcessor) getActionHandler());
		classicLayout.addComponent(classicupload);
		classicLayout.addComponent(addUpload);
	}
	
	@Override
	public void doAction() {
		super.popupWindow.close();
	}
	
	@Override
	public void displayError(AbstractErrorMessage e) {
		throw new UnsupportedOperationException("This action cannot display errors to users.");
	}

	@Override
	public Category getCategory() {
		return Category.TRANSFER;
	}
}