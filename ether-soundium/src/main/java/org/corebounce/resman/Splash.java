package org.corebounce.resman;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import ch.fhnw.util.IDisposable;

public class Splash implements IDisposable {
	final Image       image;
	final ProgressBar bar;
	final Shell       splash;

	public Splash(Display display, Rectangle displayRect, boolean showProgress)  {
		image  = new Image(display, getClass().getResourceAsStream("/splash.png"));
		splash = new Shell(SWT.ON_TOP);
		bar    = new ProgressBar(splash, SWT.NONE);
		bar.setMaximum(100);
		
		Label label = new Label(splash, SWT.NONE);
		label.setImage(image);

		FormLayout layout = new FormLayout();
		splash.setLayout(layout);

		FormData labelData = new FormData();
		labelData.right = new FormAttachment(100, 0);
		labelData.bottom = new FormAttachment(100, 0);
		label.setLayoutData(labelData);

		FormData progressData = new FormData();
		progressData.left = new FormAttachment(0, -5);
		progressData.right = new FormAttachment(100, 0);
		progressData.bottom = new FormAttachment(100, 0);
		bar.setLayoutData(progressData);
		bar.setVisible(showProgress);
		splash.pack();

		Rectangle splashRect = splash.getBounds();

		bar.setBounds(2, splashRect.height-14, splashRect.width-6, 10);

		int x = displayRect.x + (displayRect.width - splashRect.width) / 2;
		int y = displayRect.y + (displayRect.height - splashRect.height) / 2;
		splash.setLocation(x, y);
		
		splash.open();		
		progress(0);
	}

	public void open() {
	}
	
	public void progress(float f) {
		bar.setSelection((int) (f * 100));
		long timeout = System.currentTimeMillis() + 100;
		while(System.currentTimeMillis() < timeout)
			Display.getDefault().readAndDispatch();		
	}

	@Override
	public void dispose() {
		splash.dispose();
		image.dispose();
	}

	public static class SplashAction implements IAction, Listener, SelectionListener {
		private Splash splash;
		private Shell  parent;

		@Override
		public void run() {
			Display display = Display.getDefault();
			splash = new Splash(display, (parent == null ? display.getPrimaryMonitor() : parent.getMonitor()).getBounds(), false);
			display.addFilter(SWT.MouseDown, this);
			display.addFilter(SWT.KeyDown, this);
		}
		
		@Override
		public void handleEvent(Event event) {
			Display display = event.display;
			display.removeFilter(SWT.MouseDown, this);
			display.removeFilter(SWT.KeyDown, this);
			splash.dispose();
		}

		public void setParent(Shell parent) {
			this.parent = parent;
		}
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			run();
		}
	}
}