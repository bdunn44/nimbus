package com.kbdunn.nimbus.web.dashboard;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SimpleDisplay extends VerticalLayout {

	private static final long serialVersionUID = 3194285405860753550L;
	private Label value, subValue, label;
	
	public SimpleDisplay(String caption) {
		setMargin(true);
		setSizeUndefined();
		
		// Value label
		value = new Label("");
		value.addStyleName(ValoTheme.LABEL_H1);
		value.addStyleName(ValoTheme.LABEL_BOLD);
		value.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		addComponent(value);
		setComponentAlignment(value, Alignment.MIDDLE_CENTER);
		
		// Sub-Value label
		// Hidden by default
		subValue = new Label("", ContentMode.HTML);
		subValue.addStyleName(ValoTheme.LABEL_LIGHT);
		subValue.addStyleName(ValoTheme.LABEL_SMALL);
		addComponent(subValue);
		setComponentAlignment(subValue, Alignment.MIDDLE_CENTER);
		subValue.setVisible(false);
		
		// Caption Label
		label = new Label(caption);
		label.addStyleName(ValoTheme.LABEL_H3);
		label.addStyleName(ValoTheme.LABEL_COLORED);
		addComponent(label);
		setComponentAlignment(label, Alignment.MIDDLE_CENTER);
	}
	
	public void setValue(String value) {
		this.value.setValue(value);
	}
	
	public void setSubValue(String subValue) {
		this.subValue.setValue(subValue);
		this.subValue.setVisible(subValue != null && !subValue.isEmpty());
	}
}
