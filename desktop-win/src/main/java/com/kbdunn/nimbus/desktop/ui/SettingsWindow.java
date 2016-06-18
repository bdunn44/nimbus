package com.kbdunn.nimbus.desktop.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.ui.components.AbstractWindow;
import com.kbdunn.nimbus.desktop.ui.components.ConnectForm;
import com.kbdunn.nimbus.desktop.ui.components.StatusPane;
import com.kbdunn.nimbus.desktop.ui.resources.ApplicationResources;

public class SettingsWindow extends AbstractWindow {
	
	public static int CONTENT_WIDTH = 400;
	public static int CONTENT_HEIGHT = 350;
	
	private Composite content;
	private StackLayout stackLayout;
	private ConnectForm connectForm;
	private StatusPane statusPane;
	
	public SettingsWindow(Display display) {
		super(display, SWT.CLOSE | SWT.TITLE | SWT.MIN); // Fixed size, can be minimized
		buildWindow();
		refresh();
	}
	
	@Override
	protected void buildWindow() {
		super.buildWindow();
		getShell().setImage(ApplicationResources.getIcon(getDisplay()));
		getShell().setText("Nimbus Sync");
		getShell().setSize(CONTENT_WIDTH, CONTENT_HEIGHT);
		
		RowLayout container = new RowLayout(SWT.NONE);
		container.center = true;
		container.justify = false;
		container.fill = false;
		container.wrap = false;
		container.type = SWT.VERTICAL;
		container.spacing = 25;
		getShell().setLayout(container);
		
		Canvas canvas = new Canvas(getShell(), SWT.NONE);
		canvas.setLayoutData(new RowData(getContentWidth(), getContentHeight()/4));
		canvas.addPaintListener((e) -> {
			Image logo = ApplicationResources.getLogo(getDisplay());
			e.gc.drawImage(logo, 
					(canvas.getSize().x-logo.getImageData().width)/2,
					(canvas.getSize().y-logo.getImageData().height)/2 + 10); // top-pad 10px
		});
		
		content = new Composite(getShell(), SWT.NONE);
		stackLayout = new StackLayout();
		content.setLayout(stackLayout);
		content.setBounds(0, 0, getContentWidth(), 250);
		
		statusPane = new StatusPane(content, SWT.NONE, new SelectionListener() {

			@Override public void widgetDefaultSelected(SelectionEvent arg0) {  }

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				refresh();
			}
		});
		
		connectForm = new ConnectForm(content, SWT.NONE, new SelectionListener() {

			@Override public void widgetDefaultSelected(SelectionEvent arg0) {  }

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				refresh();
			}
		});
		connectForm.setLayoutData(new RowData(getContentWidth()/4*3, 200));
	}
	
	public void refresh() {
		if (!connectForm.isDisposed()) connectForm.refresh();
		if (!statusPane.isDisposed()) statusPane.refresh();
		if (!this.isDisposed()) stackLayout.topControl = (Application.getSyncStatus().isConnected()) ? statusPane : connectForm;
		if (!content.isDisposed()) content.layout();
	}

	@Override
	protected int getContentWidth() {
		return CONTENT_WIDTH;
	}

	@Override
	protected int getContentHeight() {
		return CONTENT_HEIGHT;
	}
}
