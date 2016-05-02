package com.kbdunn.nimbus.web.controlbar;


import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

public class ControlBar extends HorizontalLayout {

	private static final long serialVersionUID = -8296469404297605409L;

	private boolean captionsHidden = true;
	
	public ControlBar() {
		addStyleName("controlbar");
		//addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);
		setSpacing(true);
	}
	
	protected void setCaptionsHidden(boolean hidden) {
		this.captionsHidden = hidden;
	}
	
	protected void hideEmptyControlGroups() {
		for (int i = 0; i < getComponentCount(); i++) {
			Panel p = (Panel) getComponent(i);
			Component c = p.getContent();
			if (!(c instanceof HorizontalLayout))
				throw new IllegalStateException("Panel content isn't a HorizontalLayout");
			if (((HorizontalLayout) c).getComponentCount() == 0) {
				p.setVisible(false);
			} else {
				p.setVisible(true);
			}
		}
	}
	
	public Panel addControlGroup(String caption, Button... buttons) {
		if (buttons.length == 0) return null;
		return addButtons(addPanel(caption), buttons);
	}
	
	public Panel setControlGroupButtons(Panel controlGroup, Button... buttons) {
		Component c = controlGroup.getContent();
		if (!(c instanceof HorizontalLayout))
			throw new IllegalArgumentException("First argument must be a Panel containing a HorizontalLayout");
		
		((HorizontalLayout) c).removeAllComponents();
		return addButtons(controlGroup, buttons);
	}
	
	private Panel addPanel(String caption) {
		Panel p = new Panel(caption);
		p.addStyleName("control-group-panel");
		p.setContent(new HorizontalLayout());
		addComponent(p);
		return p;
	}
	
	private Panel addButtons(Panel p, Button... buttons) {
		HorizontalLayout content = (HorizontalLayout) p.getContent();
		for (Button b : buttons) {
			b.addStyleName("control-button");
			b.addStyleName(ValoTheme.BUTTON_BORDERLESS);
			if (captionsHidden) b.addStyleName(ValoTheme.BUTTON_ICON_ONLY);
			content.addComponent(b);
			content.setComponentAlignment(b, Alignment.MIDDLE_CENTER);
		}
		return p;
	}
}
