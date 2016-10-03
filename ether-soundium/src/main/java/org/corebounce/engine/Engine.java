package org.corebounce.engine;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.corebounce.audio.Audio;
import org.corebounce.decklight.Bouncelet;
import org.corebounce.io.MIDI;
import org.corebounce.io.OSC;
import org.corebounce.resman.MetaDB;
import org.corebounce.soundium.TabPanel;
import org.corebounce.ui.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.ether.midi.AbletonPush;
import ch.fhnw.ether.midi.AbletonPush.Blink;
import ch.fhnw.ether.midi.AbletonPush.PControl;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.net.osc.IOSCHandler;

public class Engine extends TabPanel implements IOSCHandler, IDisposable {
	private static final Log log = Log.create();

	public  static final String BOUNCELET     = "bouncelet";
	public  static final String OSC_ENGINE    = "/engine/";
	private static final long   POLL_INTERVAL = 250;

	private static final Parameter MASTER = new Parameter("master", "Master", 0, 1, 1);
	private static final Parameter PAUSED = new Parameter("pause",  "Paused", false);

	private final List<Bouncelet> s_bouncelets     = new ArrayList<>();
	private final Map<Bouncelet,  TableItem> b2i = new IdentityHashMap<>();
	private Table                 table;
	private Inspector             inspector;
	private Timer                 pollTimer = new Timer();
	private final MetaDB          db;
	private final Audio           audio;
	private final OSC             osc;
	private final MIDI            midi;
	private final Parametrizable  engine;

	public Engine(MetaDB db, Audio audio, OSC osc, MIDI midi) {
		super("Engine");
		this.db     = db;
		this.audio  = audio;
		this.osc    = osc;
		this.midi   = midi;

		AbletonPush push = midi.getPush();
		this.engine = new Parametrizable("Engine", MASTER, PAUSED) {
			@Override
			public void setVal(Parameter p, float val) {
				super.setVal(p, val);
				osc.send(OSC_ENGINE + p.getName(), val);
				try {
					if(p.equals(PAUSED))
						audio.setPaused(val > 0.5f);

					if(push != null)
						push.setControl(PControl.PLAY, audio.isPaused() ? Blink.Lit_Blink : Blink.Lit);
				} catch(Throwable t) {log.warning(t);}
			}
		};
		try { 
			if(push != null) {
				push.set(PControl.PLAY,   engine, PAUSED);
				push.set(PControl.MASTER, engine, MASTER);
			}
		} catch(Throwable t) {log.warning(t);}

		osc.addHandler("/" + BOUNCELET, this);
		pollTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				cleanup();
				osc.send("/"+BOUNCELET+"s");
			}
		}, 1000, POLL_INTERVAL);
		Platform.get().addShutdownDispose(this);
	}

	@Override
	public void dispose() {
		pollTimer.cancel();
	}

	private static final String[] COLS   = {"Control", "Type", "Name", "Active"};
	private static final int[]    WIDTHS = {56,        56,     250,    120}; 
	private static final Comparator<?>[] CMPS = {
			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return Integer.compare(b1.id, b2.id);};},
			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return -Integer.compare(b1.id, b2.id);};},

			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return b1.getType().compareTo(b2.getType());};},
			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return -b1.getType().compareTo(b2.getType());};},

			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return b1.getLabel().compareTo(b2.getLabel());};},
			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return -b1.getLabel().compareTo(b2.getLabel());};},

			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return Float.compare(b1.getActive(), b2.getActive());};},
			new Comparator<Bouncelet>() {@Override public int compare(Bouncelet b1, Bouncelet b2) {return -Float.compare(b1.getActive(), b2.getActive());};},
	};
	@SuppressWarnings("unchecked")
	@Override
	protected void fillContent(Composite panel) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
		Display display = Display.getDefault();

		SashForm sash = new SashForm(panel, SWT.HORIZONTAL | SWT.SMOOTH);
		sash.setLayoutData(GridDataFactory.fill(true, true));

		if(table != null) table.dispose();
		table = new Table(sash, SWT.V_SCROLL | SWT.MULTI);
		table.setLayoutData(GridDataFactory.fill(true, true));
		table.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		table.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {widgetDefaultSelected(e);}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if(table.getSelectionIndex() < 0) return;
				Bouncelet b = (Bouncelet) table.getItem(table.getSelectionIndex()).getData(BOUNCELET);
				String prefix = "/"+BOUNCELET+"/" + b.getId() + "/"; 
				osc.send(prefix + "names");
				osc.send(prefix + "mins");
				osc.send(prefix + "maxs");
				osc.send(prefix + "vals");
			}
		});
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		for(int i = 0; i < COLS.length; i++) {
			TableColumn col = new TableColumn(table, SWT.NONE);
			col.setText(COLS[i]);
			col.setWidth(WIDTHS[i]);
			final int idx = i;
			col.addListener(SWT.Selection, e->{
				TableColumn column = (TableColumn) e.widget;
				if(table.getSortColumn() != column) {
					table.setSortColumn(column);
					if(table.getSortDirection() != SWT.UP && table.getSortDirection() != SWT.DOWN)
						table.setSortDirection(SWT.UP);
				} else {
					table.setSortDirection(table.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP);
				}
				cmp = (Comparator<Bouncelet>) CMPS[idx*2+(table.getSortDirection() == SWT.UP ? 1 : 0)];
				sort();
			});
		}

		if(inspector != null) inspector.dispose();
		Composite inspectorPanel = new Composite(sash, SWT.NONE);
		inspectorPanel.setLayoutData(GridDataFactory.fill(true, true));
		inspectorPanel.setLayout(new GridLayout(2,false));

		inspector = new Inspector(audio, inspectorPanel);
		Composite engineUI = (Composite)ParameterWindow.createUI(inspectorPanel, engine, true);
		new Label(engineUI, SWT.NONE).setText("Message");
		Text msgUI = new Text(engineUI, SWT.NONE);
		msgUI.setLayoutData(GridDataFactory.fill(true, false));
		msgUI.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {widgetDefaultSelected(e);}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				osc.send(OSC_ENGINE + "msg", msgUI.getText());
				msgUI.selectAll();
			}
		});
		
		sash.setWeights(new int[] {70,30});
	}

	public void update(int id, String type, String label, float active, RGB color) {
		synchronized (s_bouncelets) {
			while(id >= s_bouncelets.size()) s_bouncelets.add(null);
			Bouncelet b = s_bouncelets.get(id);
			boolean doSort = false;
			if(b == null) {
				b = new Bouncelet(osc, midi, id, type, label, active, color, inspector);
				doSort = true;
			}
			s_bouncelets.set(id, b);
			if(b.getType().equals(type))
				doSort |= b.update(label, active, color);
			if(doSort)
				sort();
		}
	}

	private void cleanup() {
		synchronized (s_bouncelets) {
			long remove = System.currentTimeMillis() - 3 * POLL_INTERVAL;
			for(int i = s_bouncelets.size(); --i >= 0;) {
				Bouncelet b = s_bouncelets.get(i);
				if(b == null) continue;
				if(b.olderThan(remove)) {
					s_bouncelets.set(i, null);
					TableItem item = b2i.get(b);
					if(item != null) {
						Display.getDefault().asyncExec(()->{
							item.dispose();	
							b.dispose();
						});
					}
					b2i.remove(b);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	Comparator<Bouncelet> cmp = (Comparator<Bouncelet>) CMPS[0];

	private void sort() {
		List<Bouncelet> bs = new ArrayList<>(s_bouncelets.size());
		synchronized (s_bouncelets) {
			for(Bouncelet b : s_bouncelets)
				if(b != null)
					bs.add(b);
		}
		bs.sort(cmp);
		Bouncelet selection = table.getSelectionIndex() < 0 ? null : (Bouncelet) table.getItem(table.getSelectionIndex()).getData(BOUNCELET);
		table.removeAll();
		Display display = table.getDisplay();
		int newSelectionIndex = 0;
		int i = 0;
		for(Bouncelet b : bs) {
			if(b == selection) newSelectionIndex = i;
			TableItem item = new TableItem(table, SWT.NONE);
			item.setData(BOUNCELET, b);
			b2i.put(b, item);
			updateItem(display, b);
			i++;
		}
		if(selection == null)	table.deselectAll();
		else					table.select(newSelectionIndex);
		table.redraw();
	}

	private static final NumberFormat FMT = TextUtilities.decimalFormat(2);
	private void updateItem(Display display, Bouncelet b) {
		TableItem item = b2i.get(b);
		if(!(item.isDisposed())) {
			item.setImage(0, b.getControllerImage(display));
			item.setImage(1, db.icon48x48(display, b.getType(), "Bouncelet"));
			item.setText(2, b.getLabel());
			item.setForeground(2, display.getSystemColor(SWT.COLOR_WHITE));
			item.setText(3, FMT.format(b.getActive()));
			item.setForeground(3, display.getSystemColor(SWT.COLOR_WHITE));
		}
	}

	@Override
	public Object[] handle(String[] address, int addrIdx, StringBuilder typeString, long timestamp, Object ... args) {
		synchronized (s_bouncelets) {
			Bouncelet b = null;
			switch(address[address.length-1]) {
			case "names":
				b = s_bouncelets.get(Integer.parseInt(address[address.length-2]));
				b.setNames(args);
				break;
			case "mins": 
				b = s_bouncelets.get(Integer.parseInt(address[address.length-2]));
				b.setMins(args);
				break;
			case "maxs": 
				b = s_bouncelets.get(Integer.parseInt(address[address.length-2]));
				b.setMaxs(args);
				break;
			case "vals": 
				b = s_bouncelets.get(Integer.parseInt(address[address.length-2]));
				b.setVals(args);
				break;
			default:
				Display.getDefault().asyncExec(()->update(
						Integer.parseInt(address[address.length-1]), 
						args[0].toString(), 
						args[1].toString(), 
						((Number)args[2]).floatValue(), 
						new RGB(((Number)args[3]).floatValue(),((Number)args[4]).floatValue(),((Number)args[5]).floatValue())));
				break;
			}
			return null;
		}
	}


	public void setPaused(boolean state) {
		engine.setVal(PAUSED, state ? 1 : 0);
	}
}
