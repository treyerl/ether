package org.corebounce.resman;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.corebounce.resman.Splash.SplashAction;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.platform.IMonitor;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.platform.Platform.OS;
import ch.fhnw.util.ClassUtilities;
import ch.fhnw.util.Log;
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
		
		float step     = 0;
		float numSteps = 6;
		splash.progress(++step/numSteps);
		audio      = new Audio(args);
		splash.progress(++step/numSteps);
		db         = new MetaDB(args);
		splash.progress(++step/numSteps);
		osc        = new OSC(args, audio, monitors);
		splash.progress(++step/numSteps);
		pf         = new PreviewFactory(db);
		splash.progress(++step/numSteps);
		resources = new Resources(db, pf, args);
		splash.progress(++step/numSteps);
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
		shell.setVisible(true);
		shell.addDisposeListener(event->Platform.get().exit());
		
		prefs.setParent(shell);
		about.setParent(shell);
		
		display.timerExec(500, ()->osc.start());
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
