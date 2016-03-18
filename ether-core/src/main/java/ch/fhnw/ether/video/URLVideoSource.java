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

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.util.TextUtilities;

public class URLVideoSource extends AbstractFrameSource implements IAudioSource, IVideoSource {
	private static final boolean USE_JCODEC = false;

	private  int                         width;
	private  int                         height;
	private  float                       frameRate;
	private  long                        frameCount;
	private  float                       sampleRate;
	private  int                         numChannels;
	private  double                      length;
	private  double                      start;
	protected URL                        url;
	private final FrameAccess            frameAccess;
	private final BlockingQueue<float[]> audioData = new LinkedBlockingQueue<>();
	long                                 samples;
	private final AtomicBoolean          startup   = new AtomicBoolean(true);

	public URLVideoSource(URL url) throws IOException {
		this(url, Integer.MAX_VALUE);
	}

	public URLVideoSource(URL url, int numPlays) throws IOException {
		try {
			this.url    = url;
			frameAccess = isStillImage(url) ? new FrameAccess(this) : USE_JCODEC ? new JCodecAccess(this, numPlays) : new XuggleAccess(this, numPlays);
			init(url, 0, frameAccess.getDuration(), numPlays);
		} catch(Throwable t) {
			throw new IOException(t);
		}
	}

	public URLVideoSource(URL url, double startInSec, double lengthInSec, int numPlays) throws IOException {
		try {
			this.url    = url;
			frameAccess = isStillImage(url) ? new FrameAccess(this) : USE_JCODEC ? new JCodecAccess(this, numPlays) : new XuggleAccess(this, numPlays);
			init(url, startInSec, lengthInSec, numPlays);
		} catch(Throwable t) {
			throw new IOException(t);
		}
	}

	private void init(URL url, double startInSec, double lengthInSec, int numPlays) {
		width       = frameAccess.getWidth();
		height      = frameAccess.getHeight();
		frameRate   = frameAccess.getFrameRate();
		sampleRate  = frameAccess.getSampleRate();
		numChannels = frameAccess.getNumChannels();
		start       = startInSec;
		length      = lengthInSec;
		frameCount  = lengthInSec == frameAccess.getDuration() ? frameAccess.getFrameCount() : (long)(lengthInSec * frameRate);
		if(startInSec != 0) throw new IllegalArgumentException("startTime != 0 not implemented yet");
	}

	public static boolean isStillImage(URL url) {
		return ImageIO.getImageReadersBySuffix(TextUtilities.getFileExtensionWithoutDot(url.getPath())).hasNext();
	}

	@Override
	public String toString() {
		return url.toString();
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public float getFrameRate() {
		return frameRate;
	}

	@Override
	public long getLengthInFrames() {
		return frameCount;
	}

	@Override
	public double getLengthInSeconds() {
		return length;
	}	

	public URL getURL() {
		return url;
	}

	@Override
	public float getSampleRate() {
		return sampleRate;
	}

	@Override
	public int getNumChannels() {
		return numChannels;
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		try {
			if(target instanceof IVideoRenderTarget) {
				if(target.isRealTime()) {
					double targetTime;
					do {
						targetTime = target.getTime();
						if(frameAccess.decodeFrame()) {
							if(frameAccess.getPlayOutTimeInSec() < targetTime && target instanceof AbstractMediaTarget)
								((AbstractMediaTarget<?,?>)target).incFrameCount();
						}
					} while(frameAccess.getPlayOutTimeInSec() < targetTime);
				} else
					frameAccess.decodeFrame();
				VideoFrame frame = new VideoFrame(frameAccess, audioData);
				startup.set(false);
				if(frameAccess.getPlayOutTimeInSec() > start + length)
					frameAccess.rewind();
				if(frameAccess.numPlays <= 0)
					frame.setLast(true);
				((IVideoRenderTarget)target).setFrame(this, frame);
			} else if(target instanceof IAudioRenderTarget && target.isRendering()) {
				float[] frameData = startup.get() ? audioData.poll() : audioData.poll(500, TimeUnit.MILLISECONDS);
				if(frameData == null) {
					((IAudioRenderTarget)target).setFrame(this, createAudioFrame(samples, 64));
					samples += 64;
				} else {
					((IAudioRenderTarget)target).setFrame(this, createAudioFrame(samples, frameData));
					samples += frameData.length;
				}
			}
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}

	public double getPlayoutTimeInSec() {
		return frameAccess.getPlayOutTimeInSec();
	}
}
