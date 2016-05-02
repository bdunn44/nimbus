package com.kbdunn.nimbus.web.files;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.async.AsyncOperation;
import com.kbdunn.nimbus.common.async.AsyncOperationQueue;
import com.kbdunn.nimbus.common.async.UploadActionProcessor;
import com.kbdunn.nimbus.common.async.VaadinUploadOperation;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesomeLabel;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Html5File;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DragAndDropUploadField extends VerticalLayout  {

	private static final long serialVersionUID = 1L;
	private UploadActionProcessor controller;
	private NDragAndDropWrapper dropWrapper;
	
	public DragAndDropUploadField(UploadActionProcessor controller) {
		this.controller = controller;
		
		buildLayout();
	}
	
	private void buildLayout() {
		Panel dropPanel = new Panel();
		dropPanel.addStyleName("drop-panel");
		CssLayout cl = new CssLayout();
		Label l = new Label("Drop Files From Desktop Here");
		l.addStyleName(ValoTheme.LABEL_LARGE);
		l.addStyleName("drop-panel-caption");
		cl.addComponent(l);
		FontAwesomeLabel fal = FontAwesome.CHECK.getLabel().setSize4x();
		fal.addStyleName("drop-panel-check");
		cl.addComponent(fal);
		dropPanel.setContent(cl);
		dropWrapper = new NDragAndDropWrapper(dropPanel);
		dropWrapper.setSizeUndefined();
		addComponent(dropWrapper);
	}
	
	class NDragAndDropWrapper extends DragAndDropWrapper implements DropHandler {

		private static final long serialVersionUID = 1L;
		
		public NDragAndDropWrapper(Component root) {
			super(root);
			setDropHandler(this);
            setDragStartMode(DragStartMode.HTML5);
		}
		
		@Override
		public void drop(DragAndDropEvent event) {
			if (NimbusUI.getPropertiesService().isDemoMode()) return;
			
			WrapperTransferable trans = (WrapperTransferable) event.getTransferable();
			Html5File[] files = trans.getFiles();
			
			if (files == null) 
				return;
			
			List<AsyncOperation> uploads = new ArrayList<>();
			VaadinUploadOperation op = null;
			for (Html5File hFile: files) {
				// Don't attempt to upload folders
				if (hFile.getFileSize() > 0l) {
					try {
						op = NimbusUI.getAsyncService().uploadFile(hFile.getFileName(), hFile.getFileSize(), controller);
						//UploadOperation op = new UploadOperation(hFile, controller);
						uploads.add((AsyncOperation) op);
						hFile.setStreamVariable(op);
					} catch (Exception e) {
						// Do nothing - must be dealt with by the upload operation
					}
				}
			}
			if (uploads.size() > 0) {
				String desc = uploads.size() == 1 ? 
						"Uploading " + files[0].getFileName() : 
						"Uploading " + uploads.size() + " files";
				AsyncOperationQueue uploadQueue = NimbusUI.getAsyncService().buildAsyncOperationQueue(desc, uploads);
				NimbusUI.getCurrent().getTaskController().addTask(uploadQueue);
			}
		}
		
		@Override
		public AcceptCriterion getAcceptCriterion() {
			return AcceptAll.get();
		}

		/*@Override
		public void operationFinished(AsyncOperation operation) {
			// Don't think this is needed
			//controller.processFinishedMultiUpload();
		}*/
	}
}
