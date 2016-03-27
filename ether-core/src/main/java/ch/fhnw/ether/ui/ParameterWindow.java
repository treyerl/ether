/*
 * Copyright (c) 2013 - 2016 Stefan Muller Arisona, Simon Schubiger
 * Copyright (c) 2013 - 2016 FHNW & ETH Zurich
 * All rights reserved.
 *
 * Contributions by: Filip Schramka, Samuel von Stachelski
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *  Neither the name of FHNW / ETH Zurich nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ch.fhnw.ether.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.image.AWTFrameSupport;
import ch.fhnw.ether.image.Frame;
import ch.fhnw.ether.media.AbstractFrame;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.VideoFrame;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.TextUtilities;

public class ParameterWindow {
	public enum Flag {EXIT_ON_CLOSE,CLOSE_ON_STOP}

	static final float   S            = 1000f;
	static final int     NUM_TICKS    = 5;
	static final int     POLL_DELAY   = 40;
	static final boolean SHOW_PREVIEW = false;

	private static NumberFormat FMT = TextUtilities.decimalFormat(2);

	AtomicReference<JFrame> frame = new AtomicReference<>();
	SourceInfoUI            srcInfo;

	static class SourceInfoUI extends JPanel {
		private static final long serialVersionUID = 1948296851485298033L;

		private JLabel           frameRateUI;
		private JLabel           actualRateUI;
		private JLabel           realTimeUI;
		private JLabel           targetTimeUI;
		private JLabel           frameTimeUI;
		private JLabel           relativeFramesUI;
		private JLabel           totalFramesUI;
		private JLabel           lenFramesUI;
		private JLabel           lenSecsUI;
		private JLabel           channelsUI;
		private JLabel           sRateUI;
		private JLabel           widthUI;
		private JLabel           heightUI;
		private JComponent       previewUI;
		private Frame            preview;
		private TitledBorder     titleUI;
		private long             startTime;
		private long             startFrames;
		private RenderProgram<?> program;

		public SourceInfoUI(RenderProgram<?> program) {
			this.program = program;
			AbstractFrameSource src = program.getFrameSource();

			setLayout(new GridBagLayout());
			titleUI = new TitledBorder(new EtchedBorder());
			setBorder(titleUI);

			frameRateUI      = add("Nominal Frame Rate");
			actualRateUI     = add("Actual Frame Rate [FPS]");
			realTimeUI       = add("Real Time [secs]");
			targetTimeUI     = add("Target Time [secs]");
			frameTimeUI      = add("Frame Time [secs]");
			relativeFramesUI = add("Relative Frames");
			totalFramesUI    = add("Total Frames");
			lenFramesUI      = add("Length [frames]");
			lenSecsUI        = add("Length [secs]");
			if(src instanceof IAudioSource) {
				channelsUI = add("Channels");
				sRateUI    = add("Sampling Rate");
			}
			if(src instanceof IVideoSource) {
				widthUI   = add("Width");
				heightUI  = add("Height");
				if(SHOW_PREVIEW) {
					previewUI = new JComponent() {
						private static final long serialVersionUID = -5603882807144516938L;

						@Override
						protected void paintComponent(Graphics g) {
							if(preview != null) {
								int w = SwingUtilities.getRoot(this).getWidth();
								int h = (w * preview.height) / preview.width;
								if(getPreferredSize().width != w) {
									setPreferredSize(new Dimension(w, h));
									pack(SourceInfoUI.this);
								}
								g.drawImage(preview.toBufferedImage(), 0, 0, w, h, AWTFrameSupport.AWT_OBSERVER);
							}
						}
					};
					previewUI.setMinimumSize(new Dimension(16, 16));
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridy     = getComponentCount() / 2;
					gbc.gridwidth = 2;
					gbc.fill      = GridBagConstraints.BOTH;
					gbc.weightx   = 1;
					add(previewUI, gbc);
				}
			}

			update(true);
		}

		@SuppressWarnings("unused")
		void update(boolean programChange) {
			AbstractFrameSource src = program.getFrameSource();
			if(programChange) {
				titleUI.setTitle(src.toString());
				frameRateUI.setText(FMT.format(src.getFrameRate()));
				lenFramesUI.setText(Long.toString(src.getLengthInFrames()));
				lenSecsUI.setText(FMT.format(src.getLengthInSeconds()));
				if(src instanceof IAudioSource) {
					channelsUI.setText(Integer.toString(((IAudioSource)src).getNumChannels()));
					sRateUI.setText(FMT.format(((IAudioSource)src).getSampleRate()));
				}
				if(src instanceof IVideoSource) {
					widthUI.setText(Integer.toString(((IVideoSource)src).getWidth()));
					heightUI.setText(Integer.toString(((IVideoSource)src).getHeight()));
				}
			}
			if(program.getTarget() != null) {
				IRenderTarget<?> target = program.getTarget(); 
				if(target.getTotalElapsedFrames() == 0 || programChange) {
					startFrames = target.getTotalElapsedFrames();
					startTime   = System.nanoTime();
				} else {
					long frames  = target.getTotalElapsedFrames() - startFrames;
					long elapsed = System.nanoTime() - startTime;
					realTimeUI.setText(FMT.format(elapsed / IScheduler.SEC2NS));
					actualRateUI.setText(FMT.format((IScheduler.SEC2NS * frames) / elapsed));
				}
				if(target instanceof IScheduler)
					targetTimeUI.setText(FMT.format(target.getTime()));
				if(target != null) {
					AbstractFrame frame = target.getFrame();
					if(frame != null) {
						frameTimeUI.setText(FMT.format(frame.playOutTime));
						if(SHOW_PREVIEW && frame instanceof VideoFrame) {
							Frame f = ((VideoFrame)frame).getFrame();
							if(f != null) {
								preview = f;
								previewUI.repaint();
							}
						}
					}
				}
				relativeFramesUI.setText(Long.toString(target.getRealtiveElapsedFrames()));
				totalFramesUI.setText(Long.toString(target.getTotalElapsedFrames()));
			}
		}

		private JLabel add(String label) {
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx   = 0;
			gbc.fill    = GridBagConstraints.HORIZONTAL;
			gbc.anchor  = GridBagConstraints.WEST;
			gbc.weightx = 0.7f;
			add(new JLabel(label), gbc);
			JLabel result = new JLabel("?");
			gbc = new GridBagConstraints();
			gbc.gridx   = 1;
			gbc.fill    = GridBagConstraints.HORIZONTAL;
			gbc.anchor  = GridBagConstraints.WEST;
			gbc.weightx = 0.3f;
			add(result, gbc);
			return result;
		}
	}

	static class ParamUI implements ChangeListener, ActionListener, IDisposable {
		JLabel                   label;
		JSlider                  slider;
		JComboBox<String>        combo;
		AbstractRenderCommand<?> cmd;
		Parameter                p;
		float                    def;
		Timer                    t;

		ParamUI(AbstractRenderCommand<?> cmd, Parameter param) {
			Hashtable<Integer, JLabel> labels = new Hashtable<>();
			float d = param.getMax() - param.getMin();
			for(int i = 0; i < NUM_TICKS; i++) {
				float val = param.getMin() + ((i * d) / (NUM_TICKS-1));
				labels.put(Integer.valueOf((int)(val * S)), new JLabel(FMT.format(val))); 
			}
			this.cmd    = cmd;
			this.p      = param;
			this.def    = cmd.getVal(param);
			this.label  = new JLabel(param.getDescription());
			switch(p.getType()) {
			case RANGE:
				try {
					this.slider = new JSlider((int)(param.getMin() * S), (int)(param.getMax() * S), (int)(cmd.getVal(p) * S));
					this.slider.setPaintLabels(true);
					this.slider.setPaintTicks(true);
					this.slider.setLabelTable(labels);
					this.slider.addChangeListener(this);
					t = new Timer(POLL_DELAY, this);
					t.start();
					this.slider.putClientProperty("paramui", this);
				} catch(Throwable t) {
					System.err.println(param);
					t.printStackTrace();
				}
				break;
			case ITEMS:
				this.combo = new JComboBox<>(param.getItems());
				this.combo.addActionListener(this);
				this.combo.setSelectedIndex((int)(cmd.getVal(p)));
				t = new Timer(POLL_DELAY, this);
				t.start();
				break;
			}
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			cmd.setVal(p, slider.getValue() / S);
		}

		public void reset() {
			if(slider != null)
				slider.setValue((int)(def * S));
			if(combo != null)
				combo.setSelectedIndex((int) def);
		}

		public void zero() {
			if(slider != null)
				slider.setValue(0);
			if(combo != null)
				combo.setSelectedIndex(0);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == t) {
				if(this.slider != null) {
					float val =  slider.getValue() / S;
					if(cmd.getVal(p) != val) {
						slider.setValue((int) (cmd.getVal(p) * S));
						cmd.setVal(p, slider.getValue() / S);
					}
				}
				if(this.combo != null) {
					float val =  combo.getSelectedIndex();
					if(cmd.getVal(p) != val) {
						combo.setSelectedIndex((int) cmd.getVal(p));
						cmd.setVal(p, combo.getSelectedIndex());
					}
				}
			} else
				cmd.setVal(p, combo.getSelectedIndex());
		}

		@Override
		public void dispose() {
			t.stop();
		}
	}

	public ParameterWindow(final AbstractRenderCommand<?> src, Flag ... flags) {
		this(null, src, flags);
	}

	private boolean hasFlag(Flag flag, Flag[] flags) {
		for(Flag f : flags)
			if(f == flag)
				return true;
		return false;
	}

	public ParameterWindow(final JComponent addOn, final AbstractRenderCommand<? extends IRenderTarget<?>> src, Flag ... flags) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame f= new JFrame("Parameters");
				if(hasFlag(Flag.EXIT_ON_CLOSE, flags))
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				f.setLayout(new BorderLayout());
				if(addOn != null)
					f.add(addOn, BorderLayout.NORTH);
				f.add(new JScrollPane(createUIRecr(src), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
				f.pack();
				f.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - f.getSize().width, f.getLocation().y);
				f.setVisible(true);
				frame.set(f);
			}

			private void addMenuItem(JPopupMenu menu, JMenuItem item, ActionListener listener) {
				item.addActionListener(listener);
				menu.add(item);
			}

			JComponent createUI(AbstractRenderCommand<?> cmd) {
				if(cmd.getClass().getName().equals(cmd.toString()) && cmd.getParameters().length == 0) {
					JLabel result = new JLabel(cmd.toString());
					result.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
					return result;
				}

				JPopupMenu menu   = new JPopupMenu();
				JPanel     result = new JPanel();
				result.setComponentPopupMenu(menu);
				result.setBorder(new TitledBorder(cmd.toString()));
				Parameter[] params = cmd.getParameters();
				result.setLayout(new GridBagLayout());
				addMenuItem(menu, new JCheckBoxMenuItem("Enabled", cmd.isEnabled()), e->{
					cmd.setEnable(((JCheckBoxMenuItem)e.getSource()).isSelected());
					setEnablded(result, cmd.isEnabled());
				});
				if(params.length > 0) {
					final ParamUI[] uis = new ParamUI[params.length];
					for(int i = 0; i < uis.length; i++) {
						uis[i] = new ParamUI(cmd, params[i]);
						GridBagConstraints gbc = new GridBagConstraints();
						gbc.gridy = i;
						gbc.gridx = 0;
						result.add(uis[i].label, gbc);
						gbc = new GridBagConstraints();
						gbc.gridy = i;
						gbc.gridx = 1;
						if(uis[i].slider != null)
							result.add(uis[i].slider, gbc);
						if(uis[i].combo != null)
							result.add(uis[i].combo, gbc);
					}
					addMenuItem(menu, new JMenuItem("Reset"), e->{for(ParamUI p : uis) p.reset();});
					addMenuItem(menu, new JMenuItem("Zero"), e->{for(ParamUI p : uis) p.zero();});
				} else {
					JTextArea text = new JTextArea(cmd.toString(), 1, 30);
					text.setWrapStyleWord(false);
					text.setEditable(false);
					text.setBackground(result.getBackground());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.gridwidth = 2;
					result.add(text, new GridBagConstraints());
					text.setComponentPopupMenu(menu);
				}
				return result;
			}

			private void setEnablded(JComponent cmp, boolean state) {
				cmp.setEnabled(state);
				for(Component c : cmp.getComponents()) {
					if(c instanceof JComponent)
						setEnablded((JComponent)c, state);
				}
			}

			JPanel createUIRecr(AbstractRenderCommand<?> rcmd) {
				Insets insets = new Insets(0, 0, 0, 0);
				JPanel result = new JPanel();
				result.setLayout(new GridBagLayout());
				JComponent cmp;
				if(rcmd instanceof RenderProgram<?>) {
					srcInfo = new SourceInfoUI((RenderProgram<?>)rcmd);
					cmp = srcInfo;
				} else {
					cmp = createUI(rcmd);
				}
				int w = 1;
				if(rcmd instanceof RenderProgram<?>) {

					final RenderProgram<?> program = (RenderProgram<?>)rcmd;

					final Timer[] t = new Timer[1];
					t[0] = new Timer(40, new ActionListener() {
						AbstractRenderCommand<?>[] lastProgram = program.getProgram();
						@Override
						public void actionPerformed(ActionEvent e) {
							if(!(program.getTarget().isRendering())) {
								t[0].stop();
								dispose(result);
								frame.get().dispose();
								return;
							}
							boolean programChange = false;
							if(lastProgram != program.getProgram()) {
								programChange = true;
								lastProgram = program.getProgram();
								result.removeAll();
								int y = 0;
								for(AbstractRenderCommand<?> cmd : lastProgram)
									result.add(createUIRecr(cmd), new GridBagConstraints(1, y++, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
								pack(result);
							}
							srcInfo.update(programChange);
						}
					});
					t[0].start();
					int y = 0;
					for(AbstractRenderCommand<?> cmd : program.getProgram())
						result.add(createUIRecr(cmd), new GridBagConstraints(1, y++, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
				} else if(rcmd instanceof AbstractFrameSource) {
					result.add(srcInfo, new GridBagConstraints(0, 0, w, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insets, 0, 0));
				} else {
					result.add(cmp, new GridBagConstraints(0, 0, w, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insets, 0, 0));
				}
				return result;
			}
		});
	}

	private static void dispose(Container c) {
		for(Component cmp : c.getComponents()) {
			if(cmp instanceof Container)
				dispose((Container)cmp);
			Object parmui = ((JComponent)cmp).getClientProperty("paramui");
			if(parmui instanceof IDisposable)
				((IDisposable)parmui).dispose();
		}
	}

	private static void pack(JPanel panel) {
		Component c = SwingUtilities.getRoot(panel);
		if(c == null) return;
		if(c instanceof JFrame)
			((JFrame)c).pack();
		else
			c.validate();
	}

	public boolean isVisible() {
		while(frame.get() == null) {
			try {
				Thread.sleep(2);
			} catch (Throwable e) {}
		}
		return frame.get().isVisible();
	}
}