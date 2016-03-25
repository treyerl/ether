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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.Closure;

import ch.fhnw.ether.controller.event.IKeyEvent.KeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent.PointerEvent;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.ether.view.IWindow;

/**
 * GLFW window class.
 *
 * @author radar
 */
final class GLFWWindow implements IWindow {
	private IView view;
	private long window;
	private int vao = -1;
	
	private final List<Closure.Void> callbacks = new ArrayList<>();

	// TODO: move this to "platform class"
	private static GLFWErrorCallback errorCallback;

	static {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFW.glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
 
        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (GLFW.glfwInit() != GLFW.GLFW_TRUE)
            throw new IllegalStateException("unable to initialize glfw");
	}
	
	/**
	 * Creates window.
	 *
	 * @param view
	 *            the associated view
	 * @param width
	 *            the frame's width
	 * @param height
	 *            the frame's height
	 * @param title
	 *            the frame's title, nor null for an undecorated frame
	 * @param config
	 *            The configuration.
	 */
	public GLFWWindow(IView view, int width, int height, String title, Config config) {
		this.view = view;
		
		// make sure this comes before setting up window hints due to side effects!
        GLFWWindow shared = GLContextManager.getSharedContextWindow();

        boolean interactive = config.getViewType() == ViewType.INTERACTIVE_VIEW;
		
        GLFW.glfwDefaultWindowHints();

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);		

        //GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
        
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, interactive ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, interactive ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, shared != null ? shared.window : MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("failed to create window");
 	}
	
	GLFWWindow() {
		this(null, 16, 16, "", IView.RENDER_VIEW);
	}
	
	@Override
	public void destroy() {
		callbacks.forEach(c -> c.release());
		GLFW.glfwDestroyWindow(window);
	}
	
	@Override
	public void makeCurrent(boolean current) {
		if (current) {
			GLFW.glfwMakeContextCurrent(window);
			try {
				GL.getCapabilities();
			} catch (Exception e) {
				GL.createCapabilities(true);			
			}
			// we're not using VAOs but still need to create one
			if (vao == -1)
				vao = GL30.glGenVertexArrays();
			GL30.glBindVertexArray(vao);			
		} else {
			GLFW.glfwMakeContextCurrent(0);
		}
	}
	
	@Override
	public void swapBuffers() {
		GLFW.glfwSwapBuffers(window);
	}

	public void setWindowListener(IWindowListener listener) {
		GLFWWindowCloseCallback closeCallback = new GLFWWindowCloseCallback() {
			@Override
			public void invoke(long window) {
				listener.windowCloseRequest(GLFWWindow.this);
			}
		};
		
		GLFWWindowRefreshCallback refreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				listener.windowRefresh(GLFWWindow.this);
			}
		};
		
		GLFWWindowFocusCallback focusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, int focused) {
				if (focused > 0)
					listener.windowGainedFocus(GLFWWindow.this);
				else
					listener.windowLostFocus(GLFWWindow.this);
			}
		};
		
		GLFWFramebufferSizeCallback sizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				listener.framebufferResized(GLFWWindow.this, width, height);
			}
		};

		GLFW.glfwSetWindowCloseCallback(window, closeCallback);
		GLFW.glfwSetWindowRefreshCallback(window, refreshCallback);
		GLFW.glfwSetWindowFocusCallback(window, focusCallback);
		GLFW.glfwSetFramebufferSizeCallback(window, sizeCallback);
		callbacks.add(closeCallback);
		callbacks.add(refreshCallback);
		callbacks.add(focusCallback);
		callbacks.add(sizeCallback);
	}
	
	public void setKeyListener(IKeyListener listener) {
		GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action == GLFW.GLFW_PRESS)
					listener.keyPressed(new KeyEvent(view, mods, key, scancode, false));
				else if (action == GLFW.GLFW_REPEAT)
					listener.keyPressed(new KeyEvent(view, mods, key, scancode, true));
				else if (action == GLFW.GLFW_RELEASE)
					listener.keyReleased(new KeyEvent(view, mods, key, scancode, false));
			}
		};

		GLFW.glfwSetKeyCallback(window, keyCallback);
		callbacks.add(keyCallback);
	}
	
	public void setPointerListener(IPointerListener listener) {
		GLFWCursorEnterCallback pointerEnterCallback = new GLFWCursorEnterCallback() {
			@Override
			public void invoke(long window, int entered) {
				if (entered > 0)
					listener.pointerEntered(new PointerEvent(view, 0, 0, 0, 0, 0, 0, 0));
				else 
					listener.pointerExited(new PointerEvent(view, 0, 0, 0, 0, 0, 0, 0));
			}
		};

		GLFWCursorPosCallback pointerPositionCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				listener.pointerMoved(new PointerEvent(view, 0, 0, 0, (int)xpos, (int)ypos, 0, 0));
			}
		};
		
		GLFWMouseButtonCallback pointerButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				if (action == GLFW.GLFW_PRESS)
					listener.pointerPressed(new PointerEvent(view, mods, button, 1, 0, 0, 0, 0));
				else if (action == GLFW.GLFW_REPEAT)
					listener.pointerPressed(new PointerEvent(view, mods, button, 2, 0, 0, 0, 0));
				else if (action == GLFW.GLFW_RELEASE)
					listener.pointerReleased(new PointerEvent(view, mods, button, 0, 0, 0, 0, 0));
			}
		};
		
		GLFWScrollCallback pointerScrollCallback = new GLFWScrollCallback() {			
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				listener.pointerWheelMoved(new PointerEvent(view, 0, 0, 0, 0, 0, (int)xoffset, (int)yoffset));
			}
		};
		
		GLFW.glfwSetCursorEnterCallback(window, pointerEnterCallback);
		GLFW.glfwSetCursorPosCallback(window, pointerPositionCallback);
		GLFW.glfwSetMouseButtonCallback(window, pointerButtonCallback);
		GLFW.glfwSetScrollCallback(window, pointerScrollCallback);
		callbacks.add(pointerEnterCallback);
		callbacks.add(pointerPositionCallback);
		callbacks.add(pointerButtonCallback);
		callbacks.add(pointerScrollCallback);
	}

	public void requestFocus() {
		// TODO: can this be done with GLFW???
	}

	@Override
	public void setTitle(String title) {
		GLFW.glfwSetWindowTitle(window, title);
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			GLFW.glfwShowWindow(window);
		} else {
			GLFW.glfwHideWindow(window);
		}
	}

	@Override
	public void setPosition(int x, int y) {
		GLFW.glfwSetWindowPos(window, x, y);
	}

	@Override
	public void setSize(int width, int height) {
		GLFW.glfwSetWindowSize(window, width, height);
	}

	@Override
	public void setFullscreen(boolean enabled) {
	}

	@Override
	public void setPointerVisible(boolean visible) {
	}

	@Override
	public void setPointerConfined(boolean confined) {
	}

	@Override
	public void setPointerIcon(File pngImage, int hotspotX, int hotspotY) {
	}

	@Override
	public void warpPointer(int x, int y) {
	}
	
	@Override
	public int convertFromPixelToWindowUnits(int value) {
		return 0;
	}
	
	@Override
	public int convertFromWindowToPixelUnits(int value) {
		return 0;
	}
}
