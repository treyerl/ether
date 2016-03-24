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

import org.lwjgl.glfw.GLFW;

public class GLContextManager {
	private static final int NUM_CONTEXTS = 4;

	public interface IGLContext extends AutoCloseable {
	}

	private static final class ExistingContext implements IGLContext {
		@Override
		public void close() throws Exception {
		}		
	}

	private static final class TemporaryContext implements IGLContext {
		GLFWWindow window;

		TemporaryContext() {
			window = new GLFWWindow();
		}

		void acquire() {
			window.makeCurrent(true);
		}

		void release() {
			window.makeCurrent(false);
		}

		@Override
		public void close() throws Exception {
			releaseContext(this);
		}		
	}

	private static class ContextPool {
		final BlockingQueue<TemporaryContext> contexts = new LinkedBlockingQueue<>();
		
		public ContextPool() {
			contexts.add(new TemporaryContext());
		}
		
		TemporaryContext acquireContext(boolean wait) {
			TemporaryContext context = null;
			if (wait) {
				try {
					context = contexts.take();
				} catch (InterruptedException e) {
				}				
			} else {
				context = contexts.poll();
			}
			if (context != null)
				context.acquire();
			return context;
		}
		
		void releaseContext(TemporaryContext context) {
			context.release();
			contexts.add(context);			
		}		
	}
	

	private static final IGLContext VOID_CONTEXT = new ExistingContext();

	private static final ContextPool CONTEXTS;
	
	private static final GLFWWindow SHARED_CONTEXT;

	static {
		CONTEXTS = new ContextPool();
		SHARED_CONTEXT = CONTEXTS.contexts.peek().window;
		for (int i = 0; i < NUM_CONTEXTS - 1; ++i)
			CONTEXTS.contexts.add(new TemporaryContext());
	}

	public static IGLContext acquireContext() {
		return acquireContext(true);
	}

	public static IGLContext acquireContext(boolean wait) {
		if (GLFW.glfwGetCurrentContext() != 0)
			return VOID_CONTEXT;

		return CONTEXTS.acquireContext(wait);
	}

	public static void releaseContext(IGLContext context) {
		if (context instanceof TemporaryContext)
			CONTEXTS.releaseContext((TemporaryContext)context);
	}

	public static GLFWWindow getSharedContextWindow() {
		return SHARED_CONTEXT;
	}
}
