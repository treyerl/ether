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

package ch.fhnw.ether.video;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import ch.fhnw.ether.video.fx.AbstractVideoFX;

public class AWTFrameTarget extends AbstractVideoTarget implements Runnable {
	private Canvas                         canvas;
	private AtomicReference<BufferedImage> image  = new AtomicReference<>();
	private boolean                        resize = true;

	public AWTFrameTarget() {
		super(Thread.MIN_PRIORITY, AbstractVideoFX.FRAMEFX, true);
		SwingUtilities.invokeLater(this);
	}

	@Override
	public void run() {
		java.awt.Frame frame = new java.awt.Frame();
		canvas = new Canvas() {
			private static final long serialVersionUID = -6659278265970264752L;

			@Override
			public void update(Graphics g) {
				if(image.get() == null) return;

				int w = image.get().getWidth();
				int h = image.get().getHeight();

				if(resize) {
					frame.setSize(w, h);
					resize = false;
				}
				g.drawImage(image.get(), 0, 0, getWidth(), getHeight(), 0, 0, w, h, this);
			}
		};
		frame.add(canvas);
		frame.setSize(64,64);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
	}

	@Override
	public void render() {
		VideoFrame frame = getFrame();
		image.set(frame.getFrame().toBufferedImage());
		sleepUntil(frame.playOutTime);		
		if(canvas == null) return;
		canvas.repaint();
	}	
}
