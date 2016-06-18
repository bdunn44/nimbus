package com.kbdunn.nimbus.web.files;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.bean.FileBean;
import com.kbdunn.nimbus.web.component.FileTable;
import com.kbdunn.nimbus.web.files.action.ResolveConflictsDialog;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.interfaces.ConflictResolver.ResolutionListener;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class MoveFilesDropHandler implements DropHandler, ResolutionListener {
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger(MoveFilesDropHandler.class.getName());
	
	private Object[] droppableIds;
	private AbstractSelect dropTarget;
	private List<NimbusFile> sources;
	private NimbusFile targetFolder;
	
	public MoveFilesDropHandler(AbstractSelect dropTarget) {
		this.dropTarget = dropTarget;
	}
	
	public MoveFilesDropHandler(AbstractSelect dropTarget, Object... droppableIds) {
		this.droppableIds = droppableIds;
		this.dropTarget = dropTarget;
	}
	
	public void setDroppableIds(Object... droppableIds) {
		this.droppableIds = droppableIds;
	}
	
	@Override
	public void drop(DragAndDropEvent event) {
		// Move dragged files into target folder
		Transferable trans = event.getTransferable();
		AbstractSelectTargetDetails targetDetails = (AbstractSelectTargetDetails) event.getTargetDetails();
		
		if (!(trans.getSourceComponent() instanceof FileTable))
			return;
		if (targetDetails.getDropLocation() != VerticalDropLocation.MIDDLE)
			return;
		
		final FileTable dropSource = (FileTable) trans.getSourceComponent();
		targetFolder = ((NimbusFile) targetDetails.getItemIdOver());
		
		if (targetFolder == null) 
			return;
		if (!targetFolder.isDirectory())
			return;
		
		sources = new ArrayList<NimbusFile>(dropSource.getSelectedFiles());
		if (sources.isEmpty()) {
			sources.add(((FileBean) trans.getData("itemId")).getNimbusFile());
		}

		for (NimbusFile nf: sources) 
			if (nf.equals(targetFolder)) { return; }
		
		VerticalLayout layout = new VerticalLayout();
		final PopupWindow popup = new PopupWindow("Move files", layout);
		
		layout.addComponent(new Label("Move selected files to '" + targetFolder.getName() + "'?"));
		popup.setSubmitCaption("Move Files");
		popup.addSubmitListener(new ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				// Check for filename conflicts
				List<FileConflict> conflicts = new ArrayList<FileConflict>();
				for (NimbusFile source : sources) {
					conflicts.addAll(NimbusUI.getFileService().checkConflicts(source, targetFolder));
				}
				
				// If there are conflicts, show resolution popup
				if (conflicts.size() > 0) {
					popup.close();
					resolveConflicts(conflicts);
					return;
					
				// Otherwise process the copy/move
				} else {

					boolean success = true;
					
					for (NimbusFile source : sources) {
						try {
							success = NimbusUI.getFileService().moveFileTo(source, targetFolder) != null ? success : false;
						} catch (Exception e) {
							log.error(e, e);
						}
					}
					if (!success) 
						Notification.show("There was an error moving the files", Notification.Type.ERROR_MESSAGE);
				}
				
				popup.close();
				dropSource.refresh();
				if (dropTarget instanceof Refreshable) 
					((Refreshable) dropTarget).refresh();
			}
		});
		
		popup.open();
	}
	
	private void resolveConflicts(List<FileConflict> conflicts) {
		ResolveConflictsDialog resolver = new ResolveConflictsDialog(conflicts);
		resolver.addResolutionListener(this);
		resolver.showDialog();
	}

	@Override
	public void conflictsResolved(List<FileConflict> resolution) {
		boolean success = NimbusUI.getFileService().batchCopy(sources, targetFolder, resolution);
		if (!success) 
			Notification.show("There was an error", Notification.Type.ERROR_MESSAGE);
	}

	@Override
	public AcceptCriterion getAcceptCriterion() {
		if (dropTarget instanceof FileTable)
			return new AbstractSelect.TargetItemIs(dropTarget, droppableIds);
		return AcceptAll.get();
	}
}