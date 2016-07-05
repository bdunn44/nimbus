package com.kbdunn.nimbus.web.dashboard;

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.UsageInformation;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesomeLabel;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DashboardView extends VerticalLayout implements View {
	
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getLogger(DashboardView.class.getName());
	public static final String NAME = "dashboard";
	
	private SimpleDisplay disk, memory, managed, heap;
    
	public DashboardView() {
		addStyleName("dashboard");
		setSizeFull();
		setSpacing(true);
		
		// Panel for System stats
		Panel systemPanel = new Panel("System Details");
		addComponent(systemPanel);
		HorizontalLayout systemRow = new HorizontalLayout();
		systemRow.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		systemRow.setWidth("100%");
		systemPanel.setContent(systemRow);
		disk = new SimpleDisplay("Disk Usage");
		memory = new SimpleDisplay("Memory Usage");
		systemRow.addComponent(disk);
		systemRow.setComponentAlignment(disk, Alignment.MIDDLE_CENTER);
		Image rpilogo = new Image(null, new ThemeResource("images/raspberry_pi_logo_rgb_85x100.png"));
		systemRow.addComponent(rpilogo);
		systemRow.setComponentAlignment(rpilogo, Alignment.MIDDLE_CENTER);
		systemRow.addComponent(memory);
		systemRow.setComponentAlignment(memory, Alignment.MIDDLE_CENTER);
		
		Label legal = new Label("Raspberry Pi is a trademark of the Raspberry Pi Foundation");
		legal.setSizeUndefined();
		legal.addStyleName(ValoTheme.LABEL_LIGHT);
		legal.addStyleName(ValoTheme.LABEL_TINY);
		addComponent(legal);
		setComponentAlignment(legal, Alignment.TOP_CENTER);
		
		// Panel for Nimbus stats
		Panel nimbusPanel = new Panel("Nimbus Details");
		addComponent(nimbusPanel);
		HorizontalLayout nimbusRow = new HorizontalLayout();
		nimbusRow.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		nimbusRow.setWidth("100%");
		nimbusPanel.setContent(nimbusRow);
		managed = new SimpleDisplay("Files Managed");
		heap = new SimpleDisplay("Memory Usage");
		nimbusRow.addComponent(managed);
		nimbusRow.setComponentAlignment(managed, Alignment.MIDDLE_CENTER);
		FontAwesomeLabel nicon = FontAwesome.CLOUD.getLabel().setSize6x();
		nicon.setSizeUndefined();
		nicon.addStyleName(ValoTheme.LABEL_COLORED);
		nimbusRow.addComponent(nicon);
		nimbusRow.setComponentAlignment(nicon, Alignment.MIDDLE_CENTER);
		nimbusRow.addComponent(heap);
		nimbusRow.setComponentAlignment(heap, Alignment.MIDDLE_CENTER);
	}

	@Override
	public void enter(ViewChangeEvent event) {
		updateDisk();
		updateMemory();
		updateManaged();
		updateHeap();
	}
	
	private void updateDisk() {
		final UsageInformation sys = NimbusUI.getStorageService().getSystemDiskUsage();
		if (sys != null) {
			log.debug("Disk used: " + sys.getUsedString());
			log.debug("Disk total: " + sys.getTotalString());
			disk.setValue(sys.getPercentageString());
			disk.setSubValue("[ <span style=\"font-weight:bold;\">" + sys.getUsedString() + "</span> "
					+ "used of "
					+ "<span style=\"font-weight:bold;\">" + sys.getTotalString() + "</span> ]");
		} else {
			disk.setValue("??");
			disk.setSubValue("[ <span style=\"font-weight:bold;\">??</span> "
					+ "used of "
					+ "<span style=\"font-weight:bold;\">??</span> ]");
		}
		
	}
	
	private void updateMemory() {
		final UsageInformation sys = NimbusUI.getStorageService().getSystemMemoryUsage();
		if (sys != null) {
			log.debug("Memory used: " + sys.getUsedString());
			log.debug("Memory total: " + sys.getTotalString());
			memory.setValue(sys.getPercentageString());
			memory.setSubValue("[ <span style=\"font-weight:bold;\">" + sys.getUsedString() + "</span> "
					+ "used of "
					+ "<span style=\"font-weight:bold;\">" + sys.getTotalString() + "</span> ]");
		} else {
			disk.setValue("??");
			disk.setSubValue("[ <span style=\"font-weight:bold;\">??</span> "
					+ "used of "
					+ "<span style=\"font-weight:bold;\">??</span> ]");
		}
	}
	
	private void updateManaged() {
		managed.setValue(NumberFormat.getNumberInstance(Locale.US).format(NimbusUI.getFileService().getTotalFileCount()));
		managed.setSubValue("[ <span style=\"font-weight:bold;\">" 
				+ StringUtil.toHumanSizeString(NimbusUI.getFileService().getTotalFileSize()) 
				+ "</span> total ]");
	}
	
	private void updateHeap() {
		Long total = Runtime.getRuntime().maxMemory();
		// free memory = free + unallocated
		Long free = Runtime.getRuntime().freeMemory() + total - Runtime.getRuntime().totalMemory();
		log.debug("Heap free: " + free);
		log.debug("Heap total: " + total);
		heap.setValue((int)Math.ceil((double)(total-free)/(double)total*100) + "%");
		heap.setSubValue("[ <span style=\"font-weight:bold;\">" + StringUtil.toHumanSizeString(total - free) + "</span> "
				+ "used of "
				+ "<span style=\"font-weight:bold;\">" + StringUtil.toHumanSizeString(total) + "</span> ]");
	}
}