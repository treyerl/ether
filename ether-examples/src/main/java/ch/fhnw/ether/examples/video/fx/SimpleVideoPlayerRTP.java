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

package ch.fhnw.ether.examples.video.fx;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComboBox;

import ch.fhnw.ether.image.RGB8Frame;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.media.Sync;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.ether.video.ArrayVideoSource;
import ch.fhnw.ether.video.CameraInfo;
import ch.fhnw.ether.video.CameraSource;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.URLVideoSource;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.util.CollectionUtilities;
import ch.fhnw.util.net.rtp.RTPFrameTarget;

public class SimpleVideoPlayerRTP {
	public static void main(String[] args) throws Exception {
		AbstractFrameSource source;
		try {
			try {
				source = new URLVideoSource(new URL(args[0]));
			} catch(MalformedURLException e) {
				source = new URLVideoSource(new File(args[0]).toURI().toURL());
			}
		} catch(Throwable t) {
			source =  CameraSource.create(CameraInfo.getInfos()[0]);
		}
		IVideoSource   mask     = null; 
		RTPFrameTarget videoOut = new RTPFrameTarget(9999);
		try {mask = new ArrayVideoSource(new URLVideoSource(new File(args[1]).toURI().toURL(), 1), Sync.ASYNC);} catch(Throwable t) {}

		List<AbstractVideoFX> fxs = CollectionUtilities.asList(
				new RGBGain(),
				new AnalogTVFX(),
				new BandPass(),
				new Convolution(),
				new FadeToColor(),
				new FakeThermoCam(),
				new MotionBlur(),
				new Posterize());

		AtomicInteger current = new AtomicInteger(0);

		if(mask != null) {
			RGB8Frame maskOut  = new RGB8Frame(((IVideoSource)source).getWidth(), ((IVideoSource)source).getHeight());
			maskOut.setTimebase(videoOut);
			maskOut.useProgram(new RenderProgram<>(mask));
			fxs.add(new ChromaKey(maskOut));
			maskOut.start();
		}

		final RenderProgram<IVideoRenderTarget> video = new RenderProgram<>((IVideoSource)source, fxs.get(current.get()));

		final JComboBox<AbstractVideoFX> fxsUI = new JComboBox<>();
		for(AbstractVideoFX fx : fxs)
			fxsUI.addItem(fx);
		fxsUI.addActionListener(e->{
			int newIdx = fxsUI.getSelectedIndex();
			video.replace(fxs.get(current.get()), fxs.get(newIdx));
			current.set(newIdx);
		});
		new ParameterWindow(fxsUI, video);

		videoOut.useProgram(video);
		videoOut.start();

		videoOut.sleepUntil(IScheduler.NOT_RENDERING);
	}

}
