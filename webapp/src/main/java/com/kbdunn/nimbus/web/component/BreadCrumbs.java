package com.kbdunn.nimbus.web.component;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesomeLabel;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

public class BreadCrumbs extends Panel {
	
	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger(BreadCrumbs.class.getName());
	private static final Label overflowLabel = new Label("...");
	
	private String currentPath, homeCrumb;
	private boolean autoHide = true;
	private boolean hidden = false;
	private boolean lastCrumbHidden = true;
	private boolean showEndSeparator = false;
	private HorizontalLayout content;
	private FontAwesomeLabel homeLabel;
	
	public BreadCrumbs() {
		buildLayout();
	}
	
	public BreadCrumbs(String currentPath) {
		this.currentPath = currentPath;
		buildLayout();
		buildBreadCrumbs();
	}
	
	private void buildLayout() {
		setWidth("100%");
		addStyleName("breadcrumbs");
		addStyleName(ValoTheme.PANEL_BORDERLESS);
		
		content = new HorizontalLayout();
		setContent(content);
		
		homeLabel = FontAwesome.HOME.getLabel();
		homeLabel.applyChanges(); // Need to apply changes because the label is not added directly - getValue() is called
	}
	
	public void setCurrentPath(String path) {
		currentPath = path;
		log.debug("Current directory is " + currentPath);
		buildBreadCrumbs();
	}
	
	public void setHomeCrumb(String homeCrumb) {
		this.homeCrumb = homeCrumb;
	}
	
	public void setAutoHide(boolean autoHide) {
		this.autoHide = autoHide;
	}
	
	public void setLastCrumbHidden(boolean lastCrumbHidden) {
		this.lastCrumbHidden = lastCrumbHidden;
	}
	
	public void setShowEndSeparator(boolean showEndSeparator) {
		this.showEndSeparator = showEndSeparator;
	}
	
	private void buildBreadCrumbs() {
		if (currentPath == null) return;
		
		String breadCrumbPath = currentPath;
		if (homeCrumb != null && !homeCrumb.isEmpty()) {
			breadCrumbPath = breadCrumbPath.replace(homeCrumb, "");
		}
		breadCrumbPath = breadCrumbPath.startsWith("/") ? breadCrumbPath.substring(1) : breadCrumbPath;
		
		log.debug("Bread crumb path is " + breadCrumbPath);
		String[] crumbs = null;
		if (breadCrumbPath.isEmpty()) {
			crumbs = new String[0];
		} else if (breadCrumbPath.contains("/")) {
			crumbs = breadCrumbPath.split(Pattern.quote("/"));
		} else {
			crumbs = new String[]{breadCrumbPath};
		}
		
		int crumbCount = crumbs.length + (homeCrumb != null ? 1 : 0); // How many crumbs?
		int minCrumbCount = lastCrumbHidden ? 2 : 1; // Minimum crumb count to show()
		if (crumbCount < minCrumbCount) {
			if (autoHide) hide();
			return;
		}
		
		show();
		content.removeAllComponents();
		if (lastCrumbHidden && crumbs.length > 0) crumbs = Arrays.copyOf(crumbs, crumbs.length - 1);
		String uriBuilder = "";
		
		if (homeCrumb != null) {
			uriBuilder += homeCrumb;
			addCrumb(getCrumbLabel(homeCrumb, uriBuilder));
		}
		
		if (crumbs.length > 5) {
			addCrumb(overflowLabel);
			
			// Add a maximum of 4 crumbs
			for (int i = 0; i < crumbs.length; i++) {
				uriBuilder += "/" + crumbs[i];
				
				if (i > crumbs.length - 5) addCrumb(getCrumbLabel(crumbs[i], uriBuilder));
			}
			
		} else {
			for (String crumb: crumbs) {
				uriBuilder += "/" + crumb;//addCrumbToHref(crumb, uriBuilder);
				addCrumb(getCrumbLabel(crumb, uriBuilder));			
			}
		}
		
		if (showEndSeparator) addSeparator();
	}
	
	private Label getCrumbLabel(String crumb, String href) {
		// Build the label with the encoded HREF and un-encoded name as a link
		Label l = new Label("<a href=\"#!" /*+ viewName*/ + href + "\">" 
					+ (homeCrumb != null && crumb.equals(homeCrumb) ? homeLabel.getValue() : StringUtil.decodeUtf8(crumb)) 
					+ "</a>", ContentMode.HTML);
		return l;
	}
	
	private void addCrumb(Label label) {
		// Add an icon separator
		if (content.getComponentCount() > 0) {
			addSeparator();
		}
		
		content.addComponent(label);
		content.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
	}
	
	private void addSeparator() {
		Label s = FontAwesome.ANGLE_RIGHT.getLabel().setSizeLg();
		content.addComponent(s);
		content.setComponentAlignment(s, Alignment.MIDDLE_CENTER);
	}
	
	private void hide() {
		if (!hidden) {
			addStyleName("breadcrumbs-hidden");
			hidden = true;
		}
	}
	
	private void show() {
		if (hidden) {
			removeStyleName("breadcrumbs-hidden");
			hidden = false;
		}
	}
}
