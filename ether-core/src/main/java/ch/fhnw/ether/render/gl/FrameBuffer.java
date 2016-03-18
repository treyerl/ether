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

package ch.fhnw.ether.render.gl;

import com.jogamp.opengl.GL3;

import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.scene.mesh.material.Texture;

public class FrameBuffer {
	private final GLObject fbo;

	public FrameBuffer(GL3 gl) {
		fbo = new GLObject(gl, Type.FRAMEBUFFER);
	}
	
	public GLObject getGLObject() {
		return fbo;
	}
		
	public int checkStatus(GL3 gl) {
		return gl.glCheckFramebufferStatus(GL3.GL_DRAW_FRAMEBUFFER);
	}
	
	public void bind(GL3 gl) {
		gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, fbo.getId());
	}
	
	public static void unbind(GL3 gl) {
		gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, 0);
	}
	
	public void attach(GL3 gl, int attachment, RenderBuffer buffer) {
		gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, attachment, GL3.GL_RENDERBUFFER, buffer.id());
	}

	public void attach(GL3 gl, int attachment, Texture texture) {
		// XXX: is this really necessary? the general contract should be that no texture is bound here
		int[] toRestore = new int[1];

		gl.glGetIntegerv(GL3.GL_TEXTURE_BINDING_2D, toRestore, 0);

		gl.glBindTexture(GL3.GL_TEXTURE_2D, texture.getGlObject().getId());

		gl.glFramebufferTexture2D(GL3.GL_DRAW_FRAMEBUFFER, attachment, GL3.GL_TEXTURE_2D, texture.getGlObject().getId(), 0);

		gl.glBindTexture(GL3.GL_TEXTURE_2D, toRestore[0]);			
	}

	public void detach(GL3 gl, int attachment) {
		gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, attachment, GL3.GL_RENDERBUFFER, 0);
	}
	
	public static String toString(int status) {
		switch(status) {
		case GL3.GL_INVALID_FRAMEBUFFER_OPERATION:                return "GL_INVALID_FRAMEBUFFER_OPERATION";
		case GL3.GL_MAX_RENDERBUFFER_SIZE:                        return "GL_MAX_RENDERBUFFER_SIZE";
		case GL3.GL_FRAMEBUFFER_BINDING:                          return "GL_FRAMEBUFFER_BINDING";
		case GL3.GL_RENDERBUFFER_BINDING:                         return "GL_RENDERBUFFER_BINDING";
		case GL3.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE:           return "GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE";
		case GL3.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME:           return "GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME";
		case GL3.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL:         return "GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL";
		case GL3.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: return "GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE";
		//case GL3.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_3D_ZOFFSET:    return "GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_3D_ZOFFSET";
		case GL3.GL_FRAMEBUFFER_COMPLETE:                         return "GL_FRAMEBUFFER_COMPLETE";
		case GL3.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:            return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
		case GL3.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:    return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
		//case GL3.GL_FRAMEBUFFER_INCOMPLETE_DUPLICATE_ATTACHMENT:  return "GL_FRAMEBUFFER_INCOMPLETE_DUPLICATE_ATTACHMENT";
		case GL3.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:            return "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
		case GL3.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:               return "GL_FRAMEBUFFER_INCOMPLETE_FORMATS";
		case GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:           return "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
		case GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:           return "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
		case GL3.GL_FRAMEBUFFER_UNSUPPORTED:                      return "GL_FRAMEBUFFER_UNSUPPORTED";
		case GL3.GL_MAX_COLOR_ATTACHMENTS:                        return "GL_MAX_COLOR_ATTACHMENTS";
		default:                                                  return "unknown:" + status;
		}
	}
	
	// to add: clear & blit
}
