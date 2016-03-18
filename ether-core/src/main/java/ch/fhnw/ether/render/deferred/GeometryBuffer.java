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

package ch.fhnw.ether.render.deferred;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import ch.fhnw.ether.render.IRenderer.IRenderTargetState;
import ch.fhnw.ether.render.gl.FrameBuffer;
import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.scene.mesh.material.Texture;
import ch.fhnw.util.Viewport;

public final class GeometryBuffer {
	
	private FrameBuffer frameBuffer;
	private Texture positionTexture;
	private Texture normalTexture;
	private Texture colorTexture;
	private Texture depthTexture;
	private int width;
	private int height;

	public GeometryBuffer() {

	}
	
	public void update(GL3 gl, IRenderTargetState state) {
		if (frameBuffer == null)
			frameBuffer = new FrameBuffer(gl);
		
		Viewport viewport = state.getViewCameraState().getViewport();
		if (viewport.w != width || viewport.h != height) {
			width = viewport.w;
			height = viewport.h;
			
			frameBuffer.bind(gl);
			
			colorTexture = new Texture(new GLObject(gl, Type.TEXTURE), width, height);
			gl.glBindTexture(GL3.GL_TEXTURE_2D, colorTexture.getGlObject().getId());
			gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RGBA8, width, height, 0, GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, null);
			gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
			gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
			frameBuffer.attach(gl, GL3.GL_COLOR_ATTACHMENT0, colorTexture);
			
			depthTexture = new Texture(new GLObject(gl, Type.TEXTURE), width, height);
			gl.glBindTexture(GL3.GL_TEXTURE_2D, depthTexture.getGlObject().getId());
			gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_DEPTH_COMPONENT, width, height, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_UNSIGNED_INT, null);
			gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
			gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
			frameBuffer.attach(gl, GL3.GL_DEPTH_ATTACHMENT, depthTexture);
			
			gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
			
			gl.glDrawBuffers(2, new int[] { GL3.GL_COLOR_ATTACHMENT0 }, 0);

			int status = frameBuffer.checkStatus(gl);
			if(status != GL3.GL_FRAMEBUFFER_COMPLETE) {
				System.out.println("Status: " + FrameBuffer.toString(status));
			}
			
			FrameBuffer.unbind(gl);
		}
	}

	public void enable(GL3 gl) {
		frameBuffer.bind(gl);
	}
	
	public void disable(GL3 gl) {
		FrameBuffer.unbind(gl);
	}
	
	public void blit(GL3 gl) {
		int object = frameBuffer.getGLObject().getId();
        gl.glBindFramebuffer(GL3.GL_READ_FRAMEBUFFER, object);
        gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT, GL3.GL_NEAREST);
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	}
}
