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

package ch.fhnw.ether.view.gl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IView.Config;

public class GLContextManager {
	public interface IGLContext extends AutoCloseable {
		GL3 getGL();
	}

	private static final class ExistingContext implements IGLContext {
		@Override
		public GL3 getGL() {
			return GLContext.getCurrentGL().getGL3();
		}

		@Override
		public void close() throws Exception {
		}		
	}

	private static final class TemporaryContext implements IGLContext {
		GLContext context;

		TemporaryContext() {
			GLCapabilitiesImmutable capabilities = getSharedDrawable().getChosenGLCapabilities();
			GLAutoDrawable drawable = GLDrawableFactory.getFactory(capabilities.getGLProfile()).createDummyAutoDrawable(null, true, capabilities, null);
			context = drawable.createContext(getSharedDrawable().getContext());
			drawable.setContext(context, true);
		}

		void makeCurrent() {
			context.makeCurrent();
		}

		void release() {
			context.release();
		}

		@Override
		public GL3 getGL() {
			return GLContext.getCurrentGL().getGL3();
		}
		
		@Override
		public void close() throws Exception {
			releaseContext(this);
		}		
	}

	private static class ContextPool {
		static final int MAX_CONTEXTS = 10;
		final AtomicInteger numContexts = new AtomicInteger();
		final BlockingQueue<TemporaryContext> contexts = new LinkedBlockingQueue<>();
		
		TemporaryContext acquireContext(boolean wait) {
			TemporaryContext context = null;
			context = contexts.poll();
			if (context == null) {
				if (numContexts.incrementAndGet() < MAX_CONTEXTS) {
					context = new TemporaryContext();
				} else if (wait) {
					try {
						context = contexts.take();
					} catch (InterruptedException e) {
					}
				}
			}
			if (context != null)
				context.makeCurrent();
			return context;
		}
		
		void releaseContext(TemporaryContext context) {
			context.release();
			contexts.add(context);			
		}
		
	}

	private static final IGLContext VOID_CONTEXT = new ExistingContext();

	private static ContextPool contexts = new ContextPool();
	
	private static GLAutoDrawable theSharedDrawable;

	public static IGLContext acquireContext() {
		return acquireContext(true);
	}

	public static IGLContext acquireContext(boolean wait) {
		if (GLContext.getCurrent() != null)
			return VOID_CONTEXT;

		return contexts.acquireContext(wait);
	}

	public static void releaseContext(IGLContext context) {
		if (context instanceof TemporaryContext)
			contexts.releaseContext((TemporaryContext)context);
	}

	public static GLAutoDrawable getSharedDrawable() {
		return getSharedDrawable(null);
	}
	
	public synchronized static GLAutoDrawable getSharedDrawable(GLCapabilities capabilities) {
		if (theSharedDrawable == null) {
			if (capabilities == null)
				capabilities = getCapabilities(IView.INTERACTIVE_VIEW);
			theSharedDrawable = GLDrawableFactory.getFactory(capabilities.getGLProfile()).createDummyAutoDrawable(null, true, capabilities, null);
			theSharedDrawable.display();					
		}
		return theSharedDrawable;
	}

	public static GLCapabilities getCapabilities(Config config) {
		// TODO: make this configurable
		GLProfile profile = GLProfile.get(GLProfile.GL3);
		GLCapabilities caps = new GLCapabilities(profile);
		caps.setAlphaBits(8);
		caps.setStencilBits(16);
		if (config.getFSAASamples() > 0) {
			caps.setSampleBuffers(true);
			caps.setNumSamples(config.getFSAASamples());
		} else {
			caps.setSampleBuffers(false);
			caps.setNumSamples(1);
		}
		return caps;
	}
}
