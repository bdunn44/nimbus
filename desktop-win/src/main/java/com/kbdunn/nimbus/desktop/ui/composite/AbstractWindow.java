package com.kbdunn.nimbus.desktop.ui.composite;

import org.eclipse.nebula.animation.AnimationRunner;
import org.eclipse.nebula.animation.effects.AlphaEffect;
import org.eclipse.nebula.animation.movement.ExpoOut;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractWindow implements DisposeListener, ShellListener {//MouseListener, MouseMoveListener,  {

	private Display display;
	private Shell shell;
	//private Shell contentShell;
	private int shellStyle;
	private AnimationRunner animationRunner;
	
	/*private boolean mouseDown = false;
	private int xPos = 0;
	private int yPos = 0;*/
	
	public AbstractWindow(Display display, int shellStyle) {
		this.display = display;
		this.shellStyle = shellStyle;
		animationRunner = new AnimationRunner();
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public Shell getShell() {
		//return contentShell;
		return shell;
	}
	
	public boolean isDisposed() {
		return shell.isDisposed();
	}
	
	public void dispose() {
		//contentShell.dispose();
		shell.dispose();
	}
	
	public void open() {
		if (shell.isDisposed()) {
			buildWindow();
		}
		shell.open();
		//contentShell.open();
	}
	
	protected abstract int getContentWidth();
	
	protected abstract int getContentHeight();
	
	protected void buildWindow() {
		shell = new Shell(display, shellStyle);
		shell.setSize(getContentWidth(), getContentHeight());
		//shell.setAlpha(200);
		//shell.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		//shell.setBackgroundImage(ApplicationResources.getWindowBackground(display));
		/*Image bg = ApplicationResources.getWindowBackground(display);
		bgCanvas = new Canvas(shell, SWT.NO_REDRAW_RESIZE);
		bgCanvas.addPaintListener((event) -> {
			event.gc.drawImage(bg, 0, 0);
		});*/
		
		//shell.addMouseListener(this);
		//shell.addMouseMoveListener(this);
		AlphaEffect.fadeOnClose(shell, 600, new ExpoOut(), animationRunner);
		shell.addDisposeListener(this);
		shell.addShellListener(this);
		
		/*contentShell = new Shell(display, shellStyle | SWT.NO_TRIM | SWT.NO_BACKGROUND);
		contentShell.setSize(getContentWidth(), getContentHeight());
		contentShell.setLocation(0, 0);
		contentShell.addMouseListener(this);
		contentShell.addMouseMoveListener(this);
		contentShell.addShellListener(this);*/
		//AlphaEffect.fadeOnClose(contentShell, 600, new ExpoOut(), animationRunner);
	}

	/*@Override
	public void mouseDoubleClick(MouseEvent event) {  }

	@Override
	public void mouseDown(MouseEvent event) {
		mouseDown = true;
		xPos = event.x;
		yPos = event.y;
	}

	@Override
	public void mouseUp(MouseEvent event) {
		mouseDown = false;
	}

	@Override
	public void mouseMove(MouseEvent event) {
		if (mouseDown) {
			shell.setLocation(
					shell.getLocation().x + (event.x - xPos),
					shell.getLocation().y + (event.y - yPos));
			contentShell.setLocation(
					contentShell.getLocation().x + (event.x - xPos),
					contentShell.getLocation().y + (event.y - yPos));
		}
	}*/
	
	@Override
	public void shellActivated(ShellEvent arg0) { 
		int trimx = shell.getSize().x - shell.getClientArea().width;
		int trimy = shell.getSize().y - shell.getClientArea().height;
		shell.setSize(getContentWidth() + trimx, getContentHeight() + trimy);
		//contentShell.setSize(shell.getSize());
		//contentShell.setLocation(shell.getLocation());
		//shell.forceFocus();
	}
	
	@Override
	public void shellClosed(ShellEvent arg0) { 
		dispose();
	}

	@Override
	public void shellDeactivated(ShellEvent arg0) {  }

	@Override
	public void shellDeiconified(ShellEvent arg0) {  }

	@Override
	public void shellIconified(ShellEvent arg0) {  }

	@Override
	public void widgetDisposed(DisposeEvent arg0) {
		dispose();
	}
}
