package com.kbdunn.nimbus.web.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.kbdunn.nimbus.common.model.FileContainer;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.event.HardDriveModificationEvent;
import com.kbdunn.nimbus.web.event.HardDriveModificationEvent.HardDriveModificationListener;
import com.kbdunn.nimbus.web.event.ShareBlockModificationEvent;
import com.kbdunn.nimbus.web.event.ShareBlockModificationEvent.ShareBlockModificationListener;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.themes.ValoTheme;

public class FileContainerComboBox extends ComboBox {

	private static final long serialVersionUID = -7951660962785100623L;
	
	private boolean showShareBlocks;
	
	public FileContainerComboBox(boolean showShareBlocks) {
		super();
		this.showShareBlocks = showShareBlocks;
		setDescription("Select a storage device or share block");
		addStyleName(ValoTheme.COMBOBOX_SMALL);
		setNewItemsAllowed(false);
		setNullSelectionAllowed(false);
		setTextInputAllowed(false);
		refresh();
		
		// Listen to changes in list items, refresh
		NimbusUI.getCurrentEventRouter().addHardDriveModificationListener(new HardDriveModificationListener() {
			@Override
			public void notifyEvent(HardDriveModificationEvent event) {
				refresh();
			}
		});
		if (showShareBlocks) {
			NimbusUI.getCurrentEventRouter().addShareBlockModificationListener(new ShareBlockModificationListener() {
				@Override
				public void notifyEvent(ShareBlockModificationEvent event) {
					refresh();
				}
			});
		}
	}
	
	public void refresh() {
		Collection<?> listeners = this.getListeners(ValueChangeEvent.class);
		for (Object listener : listeners) 
			this.removeValueChangeListener((ValueChangeListener) listener);
		Object selected = super.getValue();
		removeAllItems();
		StorageService storageService = NimbusUI.getStorageService();
		FileShareService shareService = NimbusUI.getFileShareService();
		
		// Add storage devices
		for (StorageDevice d : storageService.getStorageDevicesAssignedToUser(NimbusUI.getCurrentUser())) {
			if (storageService.storageDeviceIsAvailable(d)) {
				addItem(d);
				setItemCaption(d, d.getName());
				setItemIcon(d, FontAwesome.HDD_O);
			}
		}
		
		if (showShareBlocks) {
			// Add share blocks (owned and accessible)
			List<ShareBlock> shares = shareService.getShareBlocks(NimbusUI.getCurrentUser());
			shares.addAll(shareService.getAccessibleShareBlocks(NimbusUI.getCurrentUser()));
			for (ShareBlock share : shares) {
				addItem(share);
				setItemCaption(share, share.getName());
				setItemIcon(share, FontAwesome.CUBE);
			}
		}
		
		select(selected == null || selected.equals(getNullSelectionItemId()) ? getItemByIndex(0) : selected);
		for (Object listener : listeners) 
			this.addValueChangeListener((ValueChangeListener) listener);
	}
	
	@Override
	public FileContainer getValue() {
		return (FileContainer) super.getValue();
	}
	
	public FileContainer getItemByIndex(int index) {
		if (getItemIds().size() == 0) return null;
		return (FileContainer) getItemIds().toArray()[index];
	}
	
	public List<FileContainer> getAllItems() {
		List<FileContainer> items = new ArrayList<>();
		for (Object item : getItemIds()) {
			items.add((FileContainer) item);
		}
		return items;
	}
}
