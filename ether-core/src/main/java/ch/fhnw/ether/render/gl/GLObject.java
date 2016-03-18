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

import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.opengl.GL3;

import ch.fhnw.ether.view.gl.GLContextManager;
import ch.fhnw.ether.view.gl.GLContextManager.IGLContext;
import ch.fhnw.util.AutoDisposer;
import ch.fhnw.util.AutoDisposer.Reference;
import ch.fhnw.util.IDisposable;
import ch.fhnw.util.Log;

public class GLObject {
	
	private static final boolean DBG = false;
	private static final Log LOG = Log.create();

	public enum Type {
		TEXTURE, BUFFER, RENDERBUFFER, FRAMEBUFFER, PROGRAM
	}

	public static class GLObjectRef extends Reference<GLObject> {
		private static final AtomicInteger REF_COUNT = new AtomicInteger();

		private final Type        type;
		private final int[]       id;
		private final IDisposable userData;

		public GLObjectRef(GLObject referent, ReferenceQueue<? super GLObject> q) {
			super(referent, q);
			type     = referent.getType();
			id       = referent.id;
			userData = referent.userData;
			int n = REF_COUNT.incrementAndGet();
			if (DBG)
				System.out.println("create globject " + type + " (" + n + " objects allocated)");
		}

		@Override
		public void dispose() {
			try (IGLContext context = GLContextManager.acquireContext()) {
				if(userData != null)
					userData.dispose();
				else {
					GL3 gl = context.getGL();
					switch (type) {
					case TEXTURE:
						gl.glDeleteTextures(1, id, 0);
						break;
					case BUFFER:
						gl.glDeleteBuffers(1, id, 0);
						break;
					case RENDERBUFFER:
						gl.glDeleteRenderbuffers(1, id, 0);
						break;
					case FRAMEBUFFER:
						gl.glDeleteFramebuffers(1, id, 0);
						break;
					case PROGRAM:
						gl.glDeleteProgram(id[0]);
						break;
					}
				}
				int n = REF_COUNT.decrementAndGet();
				if (DBG)
					System.out.println("destroy globject " + type + " (" + n + " objects allocated)");
			} catch(Throwable t) {
				LOG.severe(t);
			}
		}
	}

	private static final AutoDisposer<GLObject> AUTO_DISPOSER = new AutoDisposer<>(GLObjectRef.class);

	private final Type        type;
	private final int[]       id = new int[1];
	private final IDisposable userData;

	public GLObject(Type type, int glName, IDisposable userData) {
		this.type     = type;
		this.id[0]    = glName;
		this.userData = userData;
		AUTO_DISPOSER.add(this);
	}

	public GLObject(GL3 gl, Type type) {
		this.type     = type;
		this.userData = null;
		switch (type) {
		case TEXTURE:
			gl.glGenTextures(1, id, 0);
			break;
		case BUFFER:
			gl.glGenBuffers(1, id, 0);
			break;
		case RENDERBUFFER:
			gl.glGenRenderbuffers(1, id, 0);
			break;
		case FRAMEBUFFER:
			gl.glGenFramebuffers(1, id, 0);
			break;
		case PROGRAM:
			id[0] = gl.glCreateProgram();
			break;
		}
		AUTO_DISPOSER.add(this);
	}

	public Type getType() {
		return type;
	}

	public int getId() {
		return id[0];
	}

	public IDisposable getUserData() {
		return userData;
	}

	@Override
	public String toString() {
		return type + ":" + getId();
	}
}
