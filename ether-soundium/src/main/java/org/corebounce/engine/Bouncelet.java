package org.corebounce.engine;

import java.text.NumberFormat;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;

import org.corebounce.audio.IBeatListener;
import org.corebounce.io.MIDI;
import org.corebounce.io.OSC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.ether.midi.AbletonPush;
import ch.fhnw.ether.midi.AbletonPush.BiLed;
import ch.fhnw.ether.midi.AbletonPush.PControl;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.RGB;

public class Bouncelet implements IDisposable, IBeatListener {
	private static final Log log = Log.create();

	public static final String ACTIVE     = "active";
	public static final String BPM_DOWNER = "BPM_downer";
	public static final String PATTERN    = "pattern";
	private static final String INSPECTOR  = "Inspector";

	private static final Parametrizable NO_PARAMS = new Parametrizable(INSPECTOR);

	private static final PControl[] CTRLS = {
			PControl.KNOB_0,	
			PControl.KNOB_1,	
			PControl.KNOB_2,	
			PControl.KNOB_3,	
			PControl.KNOB_4,	
			PControl.KNOB_5,	
			PControl.KNOB_6,	
			PControl.KNOB_7,	
	};

	private static  Bouncelet       lastOnPush;
	public  final   int             id; 
	private final   String          type; 
	private         String          label; 
	private         RGB             color = RGB.BLACK;
	protected       long            lastUpdate;
	private         Object[]        names;
	private         Object[]        mins;
	private         Object[]        maxs;
	private         Object[]        vals;
	private         Parametrizable  params = new Parametrizable(INSPECTOR, new Parameter(ACTIVE, ACTIVE, 0, 1, 1));
	protected final IBounceletUpdate update;
	protected final OSC              osc;
	private   final AbletonPush      push;
	private         Image            ctrlImage;
	private         float            lastActive;

	public Bouncelet(Engine engine, OSC osc, MIDI midi, int id, String type, String label, float active, RGB color, IBounceletUpdate updateOnUi) {
		this.id         = id;
		this.type       = type;
		this.label      = label;
		setActive(active);
		setColor(color);
		this.lastUpdate = System.currentTimeMillis();
		this.update     = updateOnUi;
		this.osc        = osc;
		this.push       = midi.getPush();
		if(push != null) {
			try {
				push.set(pControl(), msg->{
					if(((ShortMessage)msg).getData2() > 10) {
						Display.getDefault().asyncExec(()->engine.select(this, false));
					}
				});
			} catch(Throwable t) {log.warning(t);}
		}
	}

	public boolean update(String label, float active, RGB color) {
		boolean changed = 
				!(this.label.equals(label)) ||
				!(this.color.equals(color)) ||
				Math.abs(lastActive - active) > 0.05;

				this.label      = label;
				setActive(active);
				setColor(color);
				this.lastUpdate = System.currentTimeMillis();
				this.lastActive = active;
				return changed;
	}

	private void setColor(RGB color) {
		if(!(this.color.equals(color)) && ctrlImage != null) {
			ctrlImage.dispose();
			ctrlImage = null;
		}
		this.color = color;
		if(push != null) {
			try {push.setColor(pControl(), color);} catch(Throwable t) {log.warning(t);}
		}
	}

	public String getType() {
		return type;
	}

	public boolean olderThan(long time) {
		return lastUpdate <= time;
	}

	public String getLabel() {
		return label;
	}

	public RGB getColor() {
		return color;
	}

	private static final PaletteData RGB_PALETTE = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
	public Image getControllerImage(Display display) {
		if(ctrlImage == null) {
			ImageData img = new ImageData(48, 48, 32, RGB_PALETTE);
			for(int y = 0; y < 48; y++)
				for(int x = 0; x < 48; x++) {
					int col = x / 6;
					int row = (y + 2) / 5;
					int id  = (row * 8) + col; 
					if(x % 6 < 5 && (y + 2) % 5 < 4) {
						img.setAlpha(x, y, 255);
						if(id == this.id)
							img.setPixel(x, y, color.toARGB32());
						else
							img.setPixel(x, y, 0x444444);
					} else
						img.setAlpha(x, y, 0);
				}
			ctrlImage = new Image(display, img);
		}
		return ctrlImage;
	}

	public Parametrizable getParameters() {
		return params;
	}

	public int getId() {
		return id;
	}

	public void setNames(Object ... args) {names = args; updateParams();}
	public void setMins(Object ... args)  {mins  = args; updateParams();}
	public void setMaxs(Object ... args)  {maxs  = args; updateParams();}
	public void setVals(Object ... args)  {vals  = args; updateParams();}

	private static final NumberFormat FMT = TextUtilities.decimalFormat(2);
	private void updateParams() {
		if(names != null && mins != null && maxs != null && vals != null) {
			Parameter[] ps = new Parameter[names.length];
			for(int i = 0; i < names.length; i++) {
				String name = names[i].toString();
				if(BPM_DOWNER.equals(name)) {
					int count = (int)(toFloat(maxs[i]) - toFloat(mins[i]));
					float[] dVals    = new float[count];
					String[] dLabels = new String[count];
					int val = (int)toFloat(mins[i]);
					for(int v = 0; v < count; v++, val++) {
						if(val == 0) val++;
						dVals[v]   = val;
						dLabels[v] = Integer.toString(val);
					}
					ps[i] = new Parameter(name, format(name), (int)toFloat(vals[i]), dVals, dLabels);
				} else if(PATTERN.equals(name)) {
					ps[i] = new Parameter(name, format(name), (int)toFloat(vals[i]), Parameter.BITMAP8);
				} else
					ps[i] = new Parameter(name, format(name), toFloat(mins[i]), toFloat(maxs[i]), toFloat(vals[i]));
			}

			ArrayUtilities.reverseArrayRange(ps, 0, ps.length);

			params = createParams(ps);

			names = null;
			mins  = null;
			maxs  = null;
			vals  = null;
			Display.getDefault().asyncExec(()->{
				update.update(this);
				setPush();
			});
		}		
	}

	protected Parametrizable createParams(Parameter[] ps) {
		return new Parametrizable(INSPECTOR, ps) {
			@Override
			public void setVal(Parameter p, float val) {
				lastUpdate = System.currentTimeMillis();
				super.setVal(p, val);
				updateDecklight(p, val);
				updatePush();
			}
		};
	}
	
	protected void updateDecklight(Parameter p, float val) {
		osc.send("/" + Engine.BOUNCELET + "/" + id + "/" + p.getName(), val);
	}

	private void setPush() {
		lastOnPush = this;
		if(push != null) {
			int idx = 0;
			try {
				Parametrizable p      = getParameters();
				StringBuilder labels  = new StringBuilder();
				for(Parameter param : p.getParameters()) {
					String name = param.getName();
					if(ACTIVE.equals(name)) {
						push.set(PControl.PITCH, p, param);
					} else if(BPM_DOWNER.equals(name)) {
						push.set(PControl.BEAT_4,   p, param);
						push.set(PControl.BEAT_4t,  p, param);
						push.set(PControl.BEAT_8,   p, param);
						push.set(PControl.BEAT_8t,  p, param);
						push.set(PControl.BEAT_16,  p, param);
						push.set(PControl.BEAT_16t, p, param);
						push.set(PControl.BEAT_32,  p, param);
						push.set(PControl.BEAT_32t, p, param);
					} else if(PATTERN.equals(name)) {
						push.set(PControl.ROW0_0, p, param);
						push.set(PControl.ROW0_1, p, param);
						push.set(PControl.ROW0_2, p, param);
						push.set(PControl.ROW0_3, p, param);
						push.set(PControl.ROW0_4, p, param);
						push.set(PControl.ROW0_5, p, param);
						push.set(PControl.ROW0_6, p, param);
						push.set(PControl.ROW0_7, p, param);
						updatePattern();
					} else if(idx < CTRLS.length) {
						push.set(CTRLS[idx], p, param);
						labels.append(trunc(param.getDescription(), 8));
						if((idx % 2) == 0) labels.append(' ');
						idx++;
					}
				}
				push.setLine(0, labels.toString());
				push.setLine(3, "[" + center(getType(), 15) + "]" + getLabel());
			} catch(Throwable t) {
				log.warning(t);
			}
			updatePush();
		}
	}

	protected void updatePush() {
		if(push != null) {
			int idx = 0;
			try {
				Parametrizable p = getParameters();
				StringBuilder values  = new StringBuilder();
				StringBuilder sliders = new StringBuilder();
				for(Parameter param : p.getParameters()) {
					float  normalized = (p.getVal(param) - p.getMin(param)) / (p.getMax(param)-p.getMin(param));
					String name       = param.getName();
					if(ACTIVE.equals(name)) {
						push.setTouchStrip(normalized);
					} else if(BPM_DOWNER.equals(name)) {
						float val = 0.25f / p.getVal(param);
						if(val < 0) {
							push.setBeat(-val, true);
							push.setBeat(0,    false);
						} else {
							push.setBeat(0,   true);
							push.setBeat(val, false);
						}
					} else if(PATTERN.equals(name)) {
						updatePattern();
					} else if(idx < CTRLS.length) {
						values.append(trunc(FMT.format(p.getVal(param)), 8));
						sliders.append(push.sliderText(normalized));
						if((idx % 2) == 0) {
							values.append(' ');
							sliders.append(' ');
						}
						idx++;
					}
				}
				push.setLine(1, values.toString());
				push.setLine(2, sliders.toString());
			} catch(Throwable t) {
				log.warning(t);
			}
		}
	}

	private BiLed[] biColors = new BiLed[8];
	int     beatNo;
	@Override
	public void beat(int beatNo) {
		this.beatNo = beatNo;
		updatePattern();
	}

	private void updatePattern() {
		if(push != null) {
			try {
				Parameter p = params.getParameter(PATTERN);
				if(p != null) {
					int pattern = (int)params.getVal(p);
					for(int i = 0; i < biColors.length; i++) {
						if((pattern & (1 << (biColors.length - 1 - i))) != 0) {
							biColors[i] = beatNo % biColors.length == i ? BiLed.GREEN  : BiLed.GREEN_HALF;
						} else
							biColors[i] = beatNo % biColors.length == i ? BiLed.AMBER : BiLed.AMBER_HALF;
					}
				} else 
					Arrays.fill(biColors, BiLed.OFF);
				push.setRow0(biColors[0], biColors[1], biColors[2], biColors[3], biColors[4], biColors[5], biColors[6], biColors[7]);
			} catch(Throwable t) {
				log.warning(t);
			}
		}
	}

	private float toFloat(Object object) {
		return ((Number)object).floatValue();
	}

	private String format(String string) {
		if(string.length() == 0) return string;

		StringBuilder result = new StringBuilder();
		result.append(Character.toUpperCase(string.charAt(0)));
		for(int i = 1; i < string.length(); i++) {
			if(string.charAt(i) == '_') {
				result.append(' ');
				continue;
			}
			result.append(string.charAt(i));
			if(i < string.length()-1)
				if(Character.isUpperCase(string.charAt(i)) != Character.isUpperCase(string.charAt(i+1)))
					result.append(' ');
		}

		return result.toString();
	}

	private static StringBuilder trunc(String str, int len) {
		StringBuilder result = new StringBuilder(TextUtilities.repeat(' ', len));
		for(int i = 0; i  < Math.min(str.length(), len); i++)
			result.setCharAt(i, str.charAt(i));
		return result;
	}

	private static StringBuilder center(String str, int len) {
		StringBuilder result = new StringBuilder(TextUtilities.repeat(' ', len));
		int count = Math.min(str.length(), len);
		int off   = (len - count) / 2;
		for(int i = 0; i  < count; i++)
			result.setCharAt(i+off, str.charAt(i));
		return result;
	}

	@Override
	public String toString() {
		return "["+type+"]" + label + " " + getActive();
	}

	private PControl pControl() {
		switch(id) {
		case 0:  return PControl.ROW1_0;
		case 1:  return PControl.ROW1_1;
		case 2:  return PControl.ROW1_2;
		case 3:  return PControl.ROW1_3;
		case 4:  return PControl.ROW1_4;
		case 5:  return PControl.ROW1_5;
		case 6:  return PControl.ROW1_6;
		case 7:  return PControl.ROW1_7;
		default: return PControl.valueOf(id % 8, 8 - ((id - 8) / 8));
		}
	}

	@Override
	public void dispose() {
		setColor(RGB.BLACK);
		if(push != null) {
			try {push.set(pControl(), null); } catch(Throwable t) {log.warning(t);}
		}
		lastUpdate = 0;
		params     = NO_PARAMS;
		update.clear(this);
		clearPush();
	}

	private void clearPush() {
		if(push != null && lastOnPush == this) {
			lastOnPush = null;
			try {
				clearPush(push);
			} catch(Throwable t) {
				log.warning(t);
			}
		}
	}

	public static void clearPush(AbletonPush push) throws InvalidMidiDataException, MidiUnavailableException {
		for(int i = 0; i < 4; i++) push.clearLine(i);
		push.setTouchStrip(0);
		push.setBeat(0, false);
		push.setBeat(0, true);
		push.setRow0(BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF, BiLed.OFF);
		for(PControl c : CTRLS)
			push.set(c, null);
	}


	private void setActive(float active) {
		params.setVal(ACTIVE, active);
	}

	public float getActive() {
		return params.getVal(ACTIVE);
	}
}
