package ch.fhnw.ether.midi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.Parameter.Type;
import ch.fhnw.ether.media.Parametrizable;
import ch.fhnw.util.ByteList;
import ch.fhnw.util.TextUtilities;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.math.MathUtilities;
import ch.fhnw.util.math.Vec3;

public class AbletonPush implements IMidiHandler {
	private final MidiDevice[] liveDevs = new MidiDevice[2];
	private final MidiDevice[] userDevs = new MidiDevice[2];

	private static final String LIVE_PORT = "Ableton Push Live Port";
	private static final String USER_PORT = "Ableton Push User Port";

	public AbletonPush(int deviceIdx) throws MidiUnavailableException, IOException, InvalidMidiDataException {
		MidiIO.init();

		int liveCount = 0;
		int userCount = 0;
		for(MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
			if(LIVE_PORT.equals(info.getDescription())) {
				if(liveCount / 2 == deviceIdx)
					liveDevs[liveCount % 2] = MidiSystem.getMidiDevice(info);
				liveCount++;
			}
			else if(USER_PORT.equals(info.getDescription())) {
				if(userCount / 2 == deviceIdx)
					userDevs[userCount % 2] = MidiSystem.getMidiDevice(info);
				userCount++;
			}
		}

		for(MidiDevice dev : liveDevs)
			if(dev == null)
				throw new MidiUnavailableException("No Ableton Push found for index " + deviceIdx);

		for(MidiDevice dev : userDevs)
			if(dev == null)
				throw new MidiUnavailableException("No Ableton Push found for index " + deviceIdx);

		MidiIO.setHandler(userDevs[0], this);

		//init();

		setBrightness(1f);

		for(int l = 0; l < 4; l++)
			clearLine(l);
		for(int y = 0; y < 8; y++)
			for(int x = 0; x < 8; x++)
				setControl(x, y, RGB.BLACK);
	}


	void init() throws IOException, InvalidMidiDataException, MidiUnavailableException {
		try(BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("ableton_push_init.txt")))) {
			for(int lineno = 1;;lineno++) {
				MidiMessage msg;
				String line = in.readLine();
				if(line == null) break;
				String[] tokens = line.split("[ ]+");
				int  idx       = 1;
				@SuppressWarnings("unused")
				long timestamp = Long.parseLong(tokens[idx++]);
				@SuppressWarnings("unused")
				int  port      = Integer.parseInt(tokens[idx++]);
				idx++;
				int cmd       = Integer.parseInt(tokens[idx++]);
				if(cmd == SysexMessage.SYSTEM_EXCLUSIVE) {
					ByteList bl = new ByteList();
					bl.add((byte)cmd);
					for(;idx < tokens.length; idx++) {
						int val = Integer.parseInt(tokens[idx]);
						if(val > 255) throw new IOException("value (" + val + ") out of range at idx " + idx + ", line " + lineno + ":'" + line + "'");
						bl.add((byte)val);
					}
					msg = new SysexMessage(bl.toArray(), bl.size());
				} else {
					int data1 = Integer.parseInt(tokens[idx++]);
					int data2 = Integer.parseInt(tokens[idx++]);
					msg = new ShortMessage(cmd, data1, data2);
				}
				//System.out.println(MidiToString.toString(msg));
				MidiIO.send(userDevs[1], msg);
			}
		}
	}

	static SysexMessage sysex(int ... data) throws InvalidMidiDataException {
		byte[] msg = new byte[data.length];
		for(int i = 0; i < msg.length; i++)
			msg[i] = (byte) data[i];
		return new SysexMessage(msg, msg.length);
	}

	public void setBrightness(float value) throws MidiUnavailableException, InvalidMidiDataException {
		send(sysex(SysexMessage.SYSTEM_EXCLUSIVE,71,127,21,124,0,1,MathUtilities.clamp((int)(value*127), 0, 127),SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE));
	}

	public void clearLine(int line) throws InvalidMidiDataException, MidiUnavailableException {
		MidiIO.send(userDevs[1], sysex(SysexMessage.SYSTEM_EXCLUSIVE,71,127,21,28+line,0,0,SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE));
	}

	public enum TouchStrip {
		Host_Point,
		Host_Bar_Bottom,
		Host_Bar_Center,
		Host_Point_Center,
		Fixed_Point_Center2,
		Autoreturn_Twopoint_Center,
		Bar_bottom,
		Bar_center,
		Point,
	};

	public void setTouchStrip(TouchStrip conf) throws MidiUnavailableException, InvalidMidiDataException {
		send(sysex(SysexMessage.SYSTEM_EXCLUSIVE,71,127,21,99,0,1,conf.ordinal(),SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE));
	}

	public void setLine(int line, String text) throws InvalidMidiDataException, MidiUnavailableException {
		byte[] msg = new byte[77];
		Arrays.fill(msg, (byte)' ');
		int idx = 0;
		msg[idx++] = (byte) SysexMessage.SYSTEM_EXCLUSIVE;
		msg[idx++] = 71;
		msg[idx++] = 127;
		msg[idx++] = 21;
		msg[idx++] = (byte) (24+line);
		msg[idx++] = 0;
		msg[idx++] = 69;
		msg[idx++] = 0;
		for(int i = 0; i < Math.min(68,  text.length()); i++)
			msg[idx++] = (byte) (text.charAt(i) & 0x7F);
		msg[msg.length-1] = (byte) 247;

		send(new SysexMessage(msg, msg.length));
	}

	public void setControl(PControl pad, RGB color) throws InvalidMidiDataException, MidiUnavailableException {
		if(pad.isKey())
			setControl(pad.x, pad.y, color);
		else
			send(new ShortMessage(ShortMessage.CONTROL_CHANGE, pad.ordinal(), Blink.valueOf(color).ordinal()));
	}

	public void setControl(PControl pad, Blink blink) throws InvalidMidiDataException, MidiUnavailableException {
		if(pad.isKey())
			setControl(pad.x, pad.y, blink.color);
		else
			send(new ShortMessage(ShortMessage.CONTROL_CHANGE, pad.ordinal(), blink.ordinal()));
	}

	public void setControl(int x, int y, RGB color) throws InvalidMidiDataException, MidiUnavailableException {
		send(new ShortMessage(ShortMessage.NOTE_ON, y*8+36+x, rgb2idx(color)));
	}

	private void send(MidiMessage msg) throws MidiUnavailableException {
		MidiIO.send(userDevs[1], msg);
	}

	@Override
	public void handle(MidiMessage msg) {
		if(msg instanceof ShortMessage) {
			ShortMessage smsg = (ShortMessage)msg;
			switch(smsg.getCommand()) {
			case ShortMessage.CONTROL_CHANGE:
				int cc = smsg.getData1();
				if(cc2run[cc] != null) cc2run[cc].handle(msg);
				break;
			case ShortMessage.PITCH_BEND:
				if(pitch != null) pitch.handle(smsg);
				break;
			}
		}
		System.out.println(MidiToString.toString(msg));
	}

	private static Vec3 RGB24(int rgb24) {
		return new Vec3(((rgb24 >> 16) &0xFF) / 255f, ((rgb24 >> 8) &0xFF) / 255f, ((rgb24 >> 0) &0xFF) / 255f);
	}

	private static final Vec3[] PALETTE = {
			RGB24(0x000000), RGB24(0x1E1E1E), RGB24(0x7F7F7F), RGB24(0xFFFFFF), RGB24(0xFF4C4C), RGB24(0xFF0000), RGB24(0x590000), RGB24(0x190000),
			RGB24(0xFFBD6C), RGB24(0xFF5400), RGB24(0x591D00), RGB24(0x271B00), RGB24(0xFFFF4C), RGB24(0xFFFF00), RGB24(0x595900), RGB24(0x191900),
			RGB24(0x88FF4C), RGB24(0x54FF00), RGB24(0x1D5900), RGB24(0x142B00), RGB24(0x4CFF4C), RGB24(0x00FF00), RGB24(0x005900), RGB24(0x001900),
			RGB24(0x4CFF5E), RGB24(0x00FF19), RGB24(0x00590D), RGB24(0x001902), RGB24(0x4CFF88), RGB24(0x00FF55), RGB24(0x00591D), RGB24(0x001F12),
			RGB24(0x4CFFB7), RGB24(0x00FF99), RGB24(0x005935), RGB24(0x001912), RGB24(0x4CC3FF), RGB24(0x00A9FF), RGB24(0x004152), RGB24(0x001019),
			RGB24(0x4C88FF), RGB24(0x0055FF), RGB24(0x001D59), RGB24(0x000819), RGB24(0x4C4CFF), RGB24(0x0000FF), RGB24(0x000059), RGB24(0x000019),
			RGB24(0x874CFF), RGB24(0x5400FF), RGB24(0x190064), RGB24(0x0F0030), RGB24(0xFF4CFF), RGB24(0xFF00FF), RGB24(0x590059), RGB24(0x190019),
			RGB24(0xFF4C87), RGB24(0xFF0054), RGB24(0x59001D), RGB24(0x220013), RGB24(0xFF1500), RGB24(0x993500), RGB24(0x795100), RGB24(0x436400),
			RGB24(0x033900), RGB24(0x005735), RGB24(0x00547F), RGB24(0x0000FF), RGB24(0x00454F), RGB24(0x2500CC), RGB24(0x7F7F7F), RGB24(0x202020),
			RGB24(0xFF0000), RGB24(0xBDFF2D), RGB24(0xAFED06), RGB24(0x64FF09), RGB24(0x108B00), RGB24(0x00FF87), RGB24(0x00A9FF), RGB24(0x002AFF),
			RGB24(0x3F00FF), RGB24(0x7A00FF), RGB24(0xB21A7D), RGB24(0x402100), RGB24(0xFF4A00), RGB24(0x88E106), RGB24(0x72FF15), RGB24(0x00FF00),
			RGB24(0x3BFF26), RGB24(0x59FF71), RGB24(0x38FFCC), RGB24(0x5B8AFF), RGB24(0x3151C6), RGB24(0x877FE9), RGB24(0xD31DFF), RGB24(0xFF005D),
			RGB24(0xFF7F00), RGB24(0xB9B000), RGB24(0x90FF00), RGB24(0x835D07), RGB24(0x392B00), RGB24(0x144C10), RGB24(0x0D5038), RGB24(0x15152A),
			RGB24(0x16205A), RGB24(0x693C1C), RGB24(0xA8000A), RGB24(0xDE513D), RGB24(0xD86A1C), RGB24(0xFFE126), RGB24(0x9EE12F), RGB24(0x67B50F),
			RGB24(0x1E1E30), RGB24(0xDCFF6B), RGB24(0x80FFBD), RGB24(0x9A99FF), RGB24(0x8E66FF), RGB24(0x404040), RGB24(0x757575), RGB24(0xE0FFFF),
			RGB24(0xA00000), RGB24(0x350000), RGB24(0x1AD000), RGB24(0x074200), RGB24(0xB9B000), RGB24(0x3F3100), RGB24(0xB35F00), RGB24(0x4B1502),
	};

	private static final Map<RGB, Integer> RGB2IDX = new HashMap<>();

	private static int rgb2idx(RGB rgb) {
		Integer result = RGB2IDX.get(rgb);
		int   minIdx  = 0;
		float minDist = Float.MAX_VALUE; 
		Vec3  col     = rgb.toVec3();
		if(result == null) {
			for(int i = PALETTE.length; --i >= 0;) {
				float d = col.distance(PALETTE[i]); 
				if(d < minDist) {
					minDist = d;
					minIdx  = i;
				}
			}
			result = Integer.valueOf(minIdx);
			RGB2IDX.put(rgb, result);
		}
		return result.intValue();
	}

	public enum Blink {
		Off(RGB.BLACK),
		Dim(RGB.GRAY),
		Dim_Blink(RGB.GRAY),
		Dim_Blink_Fast(RGB.GRAY),
		Lit(RGB.WHITE),
		Lit_Blink(RGB.WHITE),
		Lit_Blink_Fast(RGB.WHITE);

		final RGB color;

		Blink(RGB color) {this.color = color;}

		static Blink valueOf(RGB rgb) {
			float value = rgb.r + rgb.g + rgb.b;
			if(value < 1)      return Off;
			else if(value < 2) return Dim;
			else               return Lit;
		}
	}

	public enum BiColor {
		Off,
		RedDim,
		RedDimBlink,
		RedDimBlinkFast,
		Red,
		RedBlink,
		RedBlinkFast,
		OrangeDim,
		OrangeDimBlink,
		OrangeDimBlinkFast,
		Orange,
		OrangeBlink,
		OrangeBlinkFast,
		YellowDim,
		YellowDimBlink,
		YellowDimBlinkFast,
		Yellow,
		YellowBlink,
		YellowBlinkFast,
		GreenDim,
		GreenDimBlink,
		GreenDimBlinkFast,
		Green,
		GreenBlink,
		GreenBlinkFast,
	}

	public enum PControlType {
		BUTTON,
		KEY,
		KNOB_RELATIVE,
		KNOB_RELATIVE_DISCRETE,
		PITCH_WHEEL,
	}

	public enum PControl {
		P_00,
		P_01,
		P_02,
		TAP,
		P_04,
		P_05,
		P_06,
		P_07,
		P_08,
		METRONOME,
		P_0A,
		P_0B,
		P_0C,
		P_0D,
		TEMPO(PControlType.KNOB_RELATIVE_DISCRETE),
		MONITOR(PControlType.KNOB_RELATIVE),

		P_10,
		P_11,
		P_12,
		P_13,
		ROW0_0,
		ROW0_1,
		ROW0_2,
		ROW0_3,
		ROW0_4,
		ROW0_5,
		ROW0_6,
		ROW0_7,
		P_1C,
		P_1D,
		P_1E,
		P_1F,

		P_20,
		P_21,
		P_22,
		P_23,
		BEAT_4,
		BEAT_4t,
		BEAT_8,
		BEAT_8t,
		BEAT_16,
		BEAT_16t,
		BEAT_32,
		BEAT_32t,
		P_2C,
		P_2D,
		P_2E,
		P_2F,

		P_30,
		P_31,
		P_32,
		P_33,
		P_34,
		P_35,
		P_36,
		P_37,
		P_38,
		P_39,
		P_3A,
		P_3B,
		P_3C,
		P_3D,
		P_3E,
		P_3F,

		P_40,
		P_41,
		P_42,
		P_43,
		P_44,
		P_45,
		P_46,
		KNOB_0(PControlType.KNOB_RELATIVE),
		KNOB_1(PControlType.KNOB_RELATIVE),
		KNOB_2(PControlType.KNOB_RELATIVE),
		KNOB_3(PControlType.KNOB_RELATIVE),
		KNOB_4(PControlType.KNOB_RELATIVE),
		KNOB_5(PControlType.KNOB_RELATIVE),
		KNOB_6(PControlType.KNOB_RELATIVE),
		KNOB_7(PControlType.KNOB_RELATIVE),
		MASTER(PControlType.KNOB_RELATIVE),
		
		P_50,
		P_51,
		P_52,
		P_53,
		P_54,
		PLAY,
		P_56,
		P_57,
		P_58,
		P_59,
		P_5A,
		P_5B,
		P_5C,
		P_5D,
		P_5E,
		P_5F,

		PITCH(PControlType.PITCH_WHEEL)
		;

		final PControlType type;
		final int          x;
		final int          y;
		PControl() {this(PControlType.BUTTON);}
		PControl(PControlType type) {
			this.type     = type;
			this.x        = -1;
			this.y        = -1;
		}
		public boolean isKnob()     {return type == PControlType.KNOB_RELATIVE || type == PControlType.KNOB_RELATIVE_DISCRETE;}
		public boolean isDiscrete() {return type == PControlType.BUTTON || type == PControlType.KNOB_RELATIVE_DISCRETE;}
		public boolean isKey()      {return type == PControlType.KEY;}
	}

	private IMidiHandler[] cc2run   = new IMidiHandler[128];
	private IMidiHandler   pitch;

	public void set(PControl pad, Parametrizable cmd, Parameter p) throws MidiUnavailableException, InvalidMidiDataException {
		set(pad, msg->{
			if(msg instanceof ShortMessage) {
				ShortMessage smsg = (ShortMessage)msg;
				if(smsg.getCommand() == ShortMessage.PITCH_BEND) {
					float val   = (float)MidiIO.toInt14(smsg.getData1(), smsg.getData2()) / (float)MidiIO.MAX_14BIT;
					cmd.setVal(p, ((cmd.getMax(p) - cmd.getMin(p)) * val) + cmd.getMin(p));
				} else {
					switch(pad) {
					case BEAT_4:   cmd.setVal(p, 1); break;
					case BEAT_8:   cmd.setVal(p, 2); break;
					case BEAT_16:  cmd.setVal(p, 4); break;
					case BEAT_32:  cmd.setVal(p, 8); break;
					case BEAT_4t:  cmd.setVal(p, -1); break;
					case BEAT_8t:  cmd.setVal(p, -2); break;
					case BEAT_16t: cmd.setVal(p, -4); break;
					case BEAT_32t: cmd.setVal(p, -8); break;
					
					case ROW0_0:   pattern(cmd, p, smsg.getData2(), 0x80); break;
					case ROW0_1:   pattern(cmd, p, smsg.getData2(), 0x40); break;
					case ROW0_2:   pattern(cmd, p, smsg.getData2(), 0x20); break;
					case ROW0_3:   pattern(cmd, p, smsg.getData2(), 0x10); break;
					case ROW0_4:   pattern(cmd, p, smsg.getData2(), 0x08); break;
					case ROW0_5:   pattern(cmd, p, smsg.getData2(), 0x04); break;
					case ROW0_6:   pattern(cmd, p, smsg.getData2(), 0x02); break;
					case ROW0_7:   pattern(cmd, p, smsg.getData2(), 0x01); break;
					
					default:
						int cc  = smsg.getData1();
						int val = smsg.getData2();
						if(p.getType() == Type.BOOL && val > 64)
							cmd.setVal(p, cmd.getVal(p) == 0 ? cmd.getMax(p) : cmd.getMin(p));
						else if(PControl.values()[cc].isKnob()) {
							if(PControl.values()[cc].isDiscrete()) {
								cmd.setVal(p, Math.round(cmd.getVal(p)));
								cmd.incVal(p, val < 64 ? val : val-128, 1);
							} else
								cmd.incVal(p, val < 64 ? val : val-128);
						}
						break;
					}
				}
			}
		});
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, pad.ordinal(), Blink.Lit.ordinal()));
	}

	private void pattern(Parametrizable cmd, Parameter p, int val, int mask) {
		System.out.println((int) cmd.getVal(p) + "/" + mask + "/" + (((int) cmd.getVal(p)) ^ mask));
		if(val > 64) cmd.setVal(p, ((int) cmd.getVal(p)) ^ mask);
	}


	public void set(PControl pad, IMidiHandler r) throws MidiUnavailableException, InvalidMidiDataException {
		if(pad.type == PControlType.PITCH_WHEEL) {
			pitch = r;
		} else {
			cc2run[pad.ordinal()] = r;
			setControl(pad, r == null ? Blink.Off : Blink.Lit);
		}
	}

	public String sliderText(float v) {
		StringBuilder result = new StringBuilder(TextUtilities.repeat((char)6, 8));
		int i = (int) (v * 15);
		result.setCharAt(i/2, (char)(i % 2 == 0 ? 3 : 4));
		return result.toString();
	}

	public void setTouchStrip(float value) throws MidiUnavailableException, InvalidMidiDataException {
		send(new ShortMessage(ShortMessage.PITCH_BEND, 0, (int) (value * 127)));
	}

	private static final PControl[] BEATS   = {PControl.BEAT_4,  PControl.BEAT_8,  PControl.BEAT_16,  PControl.BEAT_32};
	private static final PControl[] BEATS_T = {PControl.BEAT_4t, PControl.BEAT_8t, PControl.BEAT_16t, PControl.BEAT_32t};

	public void setBeat(float beatValue, boolean t) throws InvalidMidiDataException, MidiUnavailableException {
		PControl[] beats = t ? BEATS_T : BEATS;
		if(beatValue < 1/64f) {
			for(int i = 0; i < 4; i++) 
				setControl(beats[i], Blink.Off);
		} else {
			float beat = 1f/4f;
			for(int i = 0; i < 4; i++, beat /= 2f) {
				if(Math.abs(beat-beatValue) < beat/4f) setControl(beats[i], Blink.Lit);
				else								   setControl(beats[i], Blink.Dim);
			}
		}
	}
	
	public void setRow0(BiColor c0, BiColor c1, BiColor c2, BiColor c3, BiColor c4, BiColor c5, BiColor c6, BiColor c7) throws MidiUnavailableException, InvalidMidiDataException {
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_0.ordinal(), c0.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_1.ordinal(), c1.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_2.ordinal(), c2.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_3.ordinal(), c3.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_4.ordinal(), c4.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_5.ordinal(), c5.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_6.ordinal(), c6.ordinal()));
		send(new ShortMessage(ShortMessage.CONTROL_CHANGE, PControl.ROW0_7.ordinal(), c7.ordinal()));
	}
}
