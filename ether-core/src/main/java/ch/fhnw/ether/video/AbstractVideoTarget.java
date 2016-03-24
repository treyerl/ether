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

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.media.AbstractMediaTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.IScheduler;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.scene.mesh.material.Texture;
import ch.fhnw.ether.video.fx.AbstractVideoFX;
import ch.fhnw.ether.video.fx.IVideoFrameFX;
import ch.fhnw.ether.video.fx.IVideoGLFX;

public abstract class AbstractVideoTarget extends AbstractMediaTarget<VideoFrame, IVideoRenderTarget> implements IVideoRenderTarget, IScheduler {
	private final Class<?> preferredType;

	protected AbstractVideoTarget(int threadPriority, Class<?> preferredType, boolean realTime) {
		super(threadPriority, realTime);
		this.preferredType = preferredType;
	}

	@Override
	public void useProgram(RenderProgram<IVideoRenderTarget> program) throws RenderCommandException {
		AbstractRenderCommand<?>[] cmds = program.getProgram();
		int numGlFx    = 0;
		int numFrameFx = 0;
		for(int i = 1; i < cmds.length; i++) {
			if(!(cmds[i] instanceof AbstractVideoFX))
				throw new ClassCastException("Command '" + cmds[i] + "' not sublcass of " + AbstractVideoFX.class.getName());
			if(cmds[i] instanceof IVideoFrameFX)
				numFrameFx++;
			if(cmds[i] instanceof IVideoGLFX)
				numGlFx++;
		}
		if(numGlFx == cmds.length - 1 && preferredType == AbstractVideoFX.GLFX) {}
		else if(numFrameFx == cmds.length - 1 && preferredType == AbstractVideoFX.FRAMEFX) {}
		else if(numGlFx == 0 && numFrameFx == 0) {}
		else
			throw new IllegalArgumentException("All commands must implement either " + IVideoGLFX.class.getName() + " or " + IVideoFrameFX.class.getName());
		super.useProgram(program);
	}

	@Override
	public IVideoSource getVideoSource() {
		return (IVideoSource)program.getFrameSource();
	}

	public Texture getSrcTexture(AbstractVideoFX fx) {
		AbstractRenderCommand<?>[] cmds = program.getProgram();
		for(int i = cmds.length; --i >= 1;)
			if(cmds[i] == fx)
				for(int j = i; --j >= 0;)
					if(cmds[j].isEnabled())
						return j == 0 ? getFrame().getTexture() : getDstTexture((AbstractVideoFX)cmds[j]);
		return null;
	}

	public Texture getDstTexture(AbstractVideoFX fx) {
		AbstractRenderCommand<?>[] cmds = program.getProgram();
		IVideoSource               src  = (IVideoSource) cmds[0];
		if(cmds[cmds.length - 1] == fx || fx.getDstTexture() == null)
			return createTexture(src);
		return fx.getDstTexture();
	}

	private Texture createTexture(IVideoSource src) {
		Texture result;
		result = new Texture(new GLObject(Type.TEXTURE), src.getWidth(), src.getHeight());
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, result.getGlObject().getId());
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, src.getWidth(), src.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer)null);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		return result;
	}

	public Class<?> runAs() {
		return preferredType;
	}
}
