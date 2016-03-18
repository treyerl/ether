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

package ch.fhnw.ether.examples.visualizer;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.FFT;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.PitchDetect;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.TextUtilities;

public class AudioVisualizer {
	private static final int N_CUBES = 60;

	public static void main(String[] args) throws RenderCommandException, IOException {
		IAudioSource                      src = new URLAudioSource(new File(args[0]).toURI().toURL());
		//AbstractAudioSource<?>            src   = new SilenceAudioSource(1, 44100, 16);
		//SinGen                            sin   = new SinGen(0);
		DCRemove                          dcrmv = new DCRemove();
		AutoGain                          gain  = new AutoGain();
		FFT                               fft   = new FFT(20, Window.HANN);
		BandsButterworth                  bands = new BandsButterworth(40, 8000, 40, N_CUBES, 1);
		PitchDetect                       pitch = new PitchDetect(fft, 2);
	//	InvFFT                            ifft  = new InvFFT(fft);

		final JavaSoundTarget audioOut = new JavaSoundTarget();

		final Canvas c = new Canvas() {
			private static final long serialVersionUID = 6220722420324801742L;

			@Override
			public void paint(Graphics g) {
				g.clearRect(0, 0, getWidth(), getHeight());
				try{
					int w = getWidth() / N_CUBES;
					for(int i = 0; i < N_CUBES; i++) {
						int h = (int) (bands.power(i) * getHeight());
						g.fillRect(i * w, getHeight() - h, w, h);
					}
					g.drawString(TextUtilities.toString(pitch.pitch()), 0, 20);
					/*
					g.setColor(Color.RED);
					int count = 0;
					for(float f : pitch.pitch(audioOut[0])) {
						if(f < 200) continue;
						int x = (int) ((f * getWidth()) / 10000f);
						g.drawLine(x, 0, x, getHeight());
						if(++count > 2) break;
					}
					 */
				} catch(Throwable t) {
					g.setColor(Color.RED);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
				repaint(20);
			}
		};

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JFrame frame = new JFrame();
				frame.add(c);
				frame.setSize(1000, 200);
				frame.setVisible(true);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});

		/*
		Scene    scene = new Scene();

		scene.add3DObject(new Mesh(Mat4.ID, new CubeGeoemtry(), new ColorMaterial(RGBA.WHITE)));

		for(int i = 0; i < N_CUBES; i++) {
			scene.add3DObject(new Mesh(()->{
				return Mat4.multiply(Mat4.scale(0.8f, 0.8f, spec.get(i)), Mat4.translate(i - (N_CUBES / 2f), 0, 0));
			},
			new CubeGeometry(),
			new ColorMaterial(()->{
				return new RGBA(spec.get(i),spec.get(i),spec.get(i),1);
				};
			}));
		}
		 */

		RenderProgram<IAudioRenderTarget> audio = new RenderProgram<>(src, /*sin,*/ dcrmv, gain, fft, bands, pitch /*, robo, ifft*/);
		//RenderProgram<IVideoRenderTarget> video = new RenderProgram<>();

		//scene.attach(video);

		new ParameterWindow(audio);

		//ViewportTarget  videoOut = new ViewportTarget();

		audioOut.useProgram(audio);
		//videoOut.useProgram(video);

		//videoOut.start();
		audioOut.start();

		// audioOut.stop();
		//videoOut.stop();
	}
}
