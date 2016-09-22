package org.corebounce.resman;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.corebounce.resman.Splash.SplashAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.platform.IMonitor;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.platform.Platform.OS;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.Subprogress;
import ch.fhnw.util.TextUtilities;

public class Resman {
	private static final Log log = Log.create();

	static private final Class<?>[] SUBSYSTEMS = {
			Monitor.class,
			Audio.class,
			MetaDB.class,
			OSC.class,
			Resources.class,
	};

	public static final String VERSION = "Soundium 2016";

	private final Monitor           monitors;
	private final Audio             audio;
	private final OSC               osc;
	private final MetaDB            db;
	private final PreviewFactory    pf;
	private final Resources         resources;
	private final Shell             shell;

	public Resman(String ... args) throws InterruptedException, RenderCommandException, IOException, NoSuchAlgorithmException {
		Display display = Display.getDefault();

		Preferences  prefs;
		SplashAction about;
		if(Platform.getOS() == OS.MACOSX) {
			CocoaUIEnhancer enhancer = new CocoaUIEnhancer(VERSION);
			enhancer.hookApplicationMenu(display, about = new Splash.SplashAction(), prefs = new Preferences());
		} else {
			about = new SplashAction();
			prefs = new Preferences();
		}		

		monitors       = new Monitor(args);
		IMonitor  sndmMon  = monitors.getSoundiumMonitor();
		Rectangle sndmMonR = new Rectangle(sndmMon.getX(), sndmMon.getY(), sndmMon.getWidth(), sndmMon.getHeight());
		Splash splash = new Splash(display, sndmMonR, true);
		splash.open();

		int cam = 0;
		for(IMonitor mon : monitors.getEngineMonitors()) {
			log.info("Engine camera " + cam++ + " assigned to monitor " + mon.getIndex() + " '" + mon + "'");
		}

		float step       = 0;
		float audioSteps = 3;
		float numSteps   = 5 + audioSteps;
		splash.setProgress(++step/numSteps);
		audio      = new Audio(new Subprogress(splash, audioSteps / numSteps), args);
		step += audioSteps;
		db         = new MetaDB(args);
		splash.setProgress(++step/numSteps);
		osc        = new OSC(args, audio, monitors);
		splash.setProgress(++step/numSteps);
		pf         = new PreviewFactory(db);
		splash.setProgress(++step/numSteps);
		resources = new Resources(db, pf, args);
		splash.setProgress(++step/numSteps);
		splash.done();
		splash.dispose();

		shell = new Shell(display);
		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		shell.setLayout(layout);

		new BrowserPanel(pf, osc, db).createPartControl(shell);
		new AudioPanel(audio).createPartControl(shell);		

		shell.setText(VERSION);
		shell.setLocation(sndmMonR.x, sndmMonR.y);
		shell.setMaximized(true);

		if(Platform.getOS() != OS.MACOSX) {
			final Menu m = new Menu(shell, SWT.BAR);

			final MenuItem file = new MenuItem(m, SWT.CASCADE);
			file.setText("&File");
			final Menu filemenu = new Menu(shell, SWT.DROP_DOWN);
			file.setMenu(filemenu);
			final MenuItem exitItem = new MenuItem(filemenu, SWT.PUSH);
			exitItem.setAccelerator(SWT.CTRL + 'Q');
			exitItem.setText("&Quit\tCTRL+Q");
			exitItem.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {widgetDefaultSelected(e);}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {System.exit(0);}
			});

			//create a Window menu and add Child item
			final MenuItem window = new MenuItem(m, SWT.CASCADE);
			window.setText("&Window");
			final Menu windowmenu = new Menu(shell, SWT.DROP_DOWN);
			window.setMenu(windowmenu);
			final MenuItem prefItem = new MenuItem(windowmenu, SWT.PUSH);
			prefItem.setText("&Preferences");
			prefItem.addSelectionListener(prefs);

			// create a Help menu and add an about item
			final MenuItem help = new MenuItem(m, SWT.CASCADE);
			help.setText("&Help");
			final Menu helpmenu = new Menu(shell, SWT.DROP_DOWN);
			help.setMenu(helpmenu);
			final MenuItem aboutItem = new MenuItem(helpmenu, SWT.PUSH);
			aboutItem.setText("&About");
			aboutItem.addSelectionListener(about);

			//setBackground(shell, display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			shell.setMenuBar(m);
		}

		shell.setVisible(true);
		shell.addDisposeListener(event->Platform.get().exit());

		prefs.setParent(shell);
		about.setParent(shell);

		display.timerExec(500, ()->osc.start());
	}

	void setBackground(Composite comp, Color color) {
		for(Control c : comp.getChildren())
			if(c instanceof Composite)
				setBackground((Composite)c, color);
			else if(!(c instanceof Table))
				c.setBackground(color);
		comp.setBackground(color);
	}

	private static String getPrefix(Class<?> cls) {
		try {
			return cls.getField("CFG_PREFIX").get(null).toString();
		} catch(Throwable t) {
			return "??";
		}
	}

	private static String[] getOptions(Class<?> cls) {
		try {
			return (String[])cls.getField("CFG_OPTIONS").get(null);
		} catch(Throwable t) {
			return ClassUtilities.EMPTY_StringA;
		}
	}

	static String help(Class<?>[] subsystems) {
		StringBuilder result = new StringBuilder();
		for(Class<?> cls : subsystems) {
			StringBuilder options = new StringBuilder();
			String[]      optionsA =getOptions(cls);
			for(int i = 0; i < optionsA.length; i += 2) {
				if(i > 0) options.append(',');
				options.append(optionsA[i]);
			}
			result.append("--").append(cls.getName()).append(":\n").append(getPrefix(cls)).append(':').append(options).append('\n');
			for(int i = 0; i< optionsA.length; i+= 2) {
				result.append("  ").append(optionsA[i]).append(":\t").append(optionsA[i+1]).append('\n');
			}
		}
		return result.toString();
	}

	public static void main(String[] args) {
		Display.setAppName(VERSION);
		Display.setAppVersion(VERSION);
		
		try {
			if(args.length == 0) {
				System.out.println(help());
				System.exit(0);
			}
			Platform.get().init();
			new Resman(args);
			Platform.get().run();
		} catch(Throwable t) {
			t.printStackTrace();
			System.out.println(TextUtilities.cat(args, ' '));
			System.out.println(help());
		}
	}

	public static String help() {
		return help(SUBSYSTEMS);
	}
}
