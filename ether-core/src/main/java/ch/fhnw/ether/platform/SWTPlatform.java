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

import org.eclipse.swt.widgets.Display;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import ch.fhnw.ether.render.gl.GLContextManager;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.math.Vec2;

final class SWTPlatform implements IPlatform {

	private final GLFWErrorCallback errorCallback;

	private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

	private final IImageSupport imageSupport = new SWTImageSupport();

	public SWTPlatform() {
		errorCallback = GLFWErrorCallback.createPrint(System.err);
	}

	@Override
	public void init() {
		// note: it seems to be important that GLFW is initialized before SWT (e.g. segfaults when closing windows on OS X)
		GLFW.glfwSetErrorCallback(errorCallback);
		if (GLFW.glfwInit() != GLFW.GLFW_TRUE)
			throw new IllegalStateException("unable to initialize glfw");
		
		Display.getDefault();
		Display.setAppName("ether");

		GLContextManager.init();
	}

	@Override
	public void run() {
		try {
			while (true) {
				if (!Display.getDefault().readAndDispatch())
					Display.getDefault().sleep();
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
	public void exit() {
		Display.getDefault().dispose();
		
		errorCallback.free();
		GLFW.glfwTerminate();

		System.exit(0);
	}

	@Override
	public void runOnMainThread(Runnable runnable) {
		queue.offer(runnable);
		Display.getDefault().wake();
	}

	@Override
	public IWindow createWindow(Vec2 size, String title, boolean decorated) {
		return new GLFWWindow(size, title, decorated);
	}

	@Override
	public IImageSupport getImageSupport() {
		return imageSupport;
	}
}
