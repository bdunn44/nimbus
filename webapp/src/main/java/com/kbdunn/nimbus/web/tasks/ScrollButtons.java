package com.kbdunn.nimbus.web.tasks;

import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;

public class ScrollButtons extends CssLayout {

	private static final long serialVersionUID = 2740703108763572907L;

	private Button back, next;
	
	public ScrollButtons(final TaskController controller) {
		addStyleName("task-scroll-buttons");
		
		back = new Button();
		back.setIcon(FontAwesome.CHEVRON_LEFT);
		back.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				controller.scrollBack();
			}
		});

		next = new Button();
		next.setIcon(FontAwesome.CHEVRON_RIGHT);
		next.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				controller.scrollNext();
			}
		});
		
		addComponent(back);
		addComponent(next);
	}
	
	protected void setBackEnabled(boolean enabled) {
		back.setEnabled(enabled);
	}
	
	protected void setNextEnabled(boolean enabled) {
		next.setEnabled(enabled);
	}
}
