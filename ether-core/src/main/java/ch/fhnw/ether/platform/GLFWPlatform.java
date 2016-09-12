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

package ch.fhnw.ether.platform;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import ch.fhnw.ether.render.gl.GLContextManager;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.math.Vec2;

class GLFWPlatform implements IPlatform {

	private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

	private final GLFWErrorCallback errorCallback;
	private final IImageSupport imageSupport;

	private final Thread mainThread;

	public GLFWPlatform() {
		this(new STBImageSupport());
	}

	protected GLFWPlatform(IImageSupport imageSupport) {
		this.errorCallback = GLFWErrorCallback.createPrint(System.err);
		this.imageSupport = imageSupport;
		this.mainThread = Thread.currentThread();
	}

	@Override
	public final void init() {
		GLFW.glfwSetErrorCallback(errorCallback);

		if (!GLFW.glfwInit())
			throw new IllegalStateException("unable to initialize glfw");

		initInternal();

		GLContextManager.init();
	}

	@Override
	public final void run() {
		try {
			while (true) {
				waitForEvents();
				Runnable runnable;
				while ((runnable = queue.poll()) != null) {
					try {
						runnable.run();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		exit();
	}

	@Override
	public final void exit() {
		exitInternal();
		errorCallback.free();
		GLFW.glfwTerminate();
		System.exit(0);
	}

	@Override
	public boolean isMainThread() {
		return Thread.currentThread().equals(mainThread);
	}

	@Override
	public final void runOnMainThread(Runnable runnable) {
		queue.offer(runnable);
		GLFW.glfwPostEmptyEvent();
	}

	@Override
	public IWindow createWindow(Vec2 size, String title, boolean decorated) {
		return new GLFWWindow(size, title, decorated);
	}

	@Override
	public IWindow createWindow(IMonitor monitor, String title) {
		return new GLFWWindow(monitor, title);
	}

	@Override
	public IImageSupport getImageSupport() {
		return imageSupport;
	}

	protected void initInternal() {
	}

	protected void exitInternal() {
	}

	protected void waitForEvents() {
		GLFW.glfwWaitEvents();		
	}

	@Override
	public IMonitor[] getMonitors() {
		PointerBuffer monitors = GLFW.glfwGetMonitors();
		IMonitor[] result = new IMonitor[monitors.limit()];
		for(int i = 0; i < result.length; i++)
			result[i] = new GLFWMonitor(monitors.get(i), i);
		return result;
	}
}
