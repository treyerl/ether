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

import java.util.concurrent.BlockingQueue;

import com.jogamp.common.net.Uri;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.State;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;

import ch.fhnw.ether.image.Frame;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.scene.mesh.material.Texture;
import ch.fhnw.ether.view.gl.GLContextManager;
import ch.fhnw.ether.view.gl.GLContextManager.IGLContext;
import ch.fhnw.util.Log;

public final class JOGLAccess extends FrameAccess {
	private static final Log LOG = Log.create();

	private final GLMediaPlayer player;

	public JOGLAccess(URLVideoSource src, int numPlays) {
		super(src, numPlays);
		player = GLMediaPlayerFactory.createDefault();
		try(IGLContext ctx = GLContextManager.acquireContext()) {
			player.initStream(Uri.valueOf(src.getURL()), 0, 0, 1);
			while(player.getState() != State.Initialized)
				Thread.sleep(10);
			player.initGL(ctx.getGL());
		} catch(Throwable t) {
			LOG.severe(t);
		}
	}

	@Override
	public void dispose() {
		try(IGLContext ctx = GLContextManager.acquireContext()) {
			player.destroy(ctx.getGL());
		} catch(Throwable t) {
			LOG.severe(t);
		}
	}

	@Override
	public double getDuration() {
		return player.getDuration() / IScheduler.SEC2MS;
	}

	@Override
	public float getFrameRate() {
		return player.getFramerate();
	}

	@Override
	public long getFrameCount() {
		return player.getVideoFrames();
	}

	@Override
	public int getWidth() {
		return player.getWidth();
	}

	@Override
	public int getHeight() {
		return player.getHeight();
	}

	@Override
	public String toString() {
		return src.getURL() + " (d=" + getDuration() + " fr=" + getFrameRate() + " fc=" + getFrameCount() + " w=" + getWidth() + " h=" + getHeight() + ")";
	}

	@Override
	public void rewind() {
		numPlays--;
		player.seek(0);
	}

	@Override
	protected Frame getFrame(BlockingQueue<float[]> audioData) {
		try(IGLContext ctx = GLContextManager.acquireContext()) {
			return Frame.create(player.getNextTexture(ctx.getGL()).getTexture());
		} catch(Throwable t) {
			LOG.severe(t);
			return null;
		}
	}

	@Override
	public Texture getTexture(BlockingQueue<float[]> audioData) {
		try(IGLContext ctx = GLContextManager.acquireContext()) {
			return new Texture(player.getNextTexture(ctx.getGL()).getTexture());
		} catch(Throwable t) {
			LOG.severe(t);
			return null;
		}
	}

	@Override
	protected int getNumChannels() {
		return 2;
	}

	@Override
	protected float getSampleRate() {
		return 48000;
	}	
}
