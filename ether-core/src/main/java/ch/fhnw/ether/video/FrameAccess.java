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
import java.util.concurrent.BlockingQueue;

import ch.fhnw.ether.image.awt.AWTImageSupport;
import ch.fhnw.ether.image.awt.Frame;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.scene.mesh.material.Texture;
import ch.fhnw.util.IDisposable;

public class FrameAccess implements IDisposable {
	protected final URLVideoSource src;
	protected int                  numPlays;
	private         Frame          frame;

	FrameAccess(URLVideoSource src) throws IOException {
		this.frame    = AWTImageSupport.readFrame(src.getURL());
		this.src      = src;
		this.numPlays = 0;
	}

	FrameAccess(Frame frame) {
		this.frame    = frame;
		this.src      = null;
		this.numPlays = 0;
	}

	protected FrameAccess(URLVideoSource src, int numPlays) {
		this.frame    = null;
		this.src      = src;
		this.numPlays = numPlays;
	}

	protected Frame getFrame(BlockingQueue<float[]> audioData) {
		return frame;
	}
	
	public Texture getTexture(BlockingQueue<float[]> audioData) {
		return frame.getTexture();
	}

	protected int getWidth() {
		return frame.width;
	}
	protected int getHeight() {
		return frame.height;
	}
	
	protected float getFrameRate() {
		return AbstractFrameSource.FRAMERATE_UNKNOWN;
	}

	protected float getSampleRate() {
		return 0;
	}
	
	protected int getNumChannels() {
		return 0;
	}
	
	protected long getFrameCount() {
		return 1;
	}

	protected double getDuration() {
		return AbstractFrameSource.LENGTH_INFINITE;
	}

	public double getPlayOutTimeInSec() {
		return IScheduler.ASAP;
	}
	
	public URLVideoSource getSource() {
		return src;
	}

	public boolean decodeFrame() {return true;}

	public boolean isKeyframe() {
		return true;
	}
	
	@Override
	public void dispose() {
		frame = null; // help GC
	}
	
	public void rewind() throws IOException {}
}
