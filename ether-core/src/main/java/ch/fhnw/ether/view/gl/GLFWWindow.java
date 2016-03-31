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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;
import org.lwjgl.glfw.GLFWWindowRefreshCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.Closure;

import ch.fhnw.ether.controller.event.IKeyEvent.KeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.controller.event.IPointerEvent.PointerEvent;
import ch.fhnw.ether.platform.Platform;
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
	private static final boolean DBG = false;
	
	private static final AtomicInteger NUM_WINDOWS = new AtomicInteger();
	
	private IView view;
	private int windowWidth;
	private int windowHeight;
	
	private int framebufferWidth;
	private int framebufferHeight;
	
	private long window;
	
	private float pointerX = 0;
	private float pointerY = 0;
	private int pointerButtons = 0;
	private boolean pointerDragged = false;
	private int modifiers = 0;
	private int vao = -1;
	
	private final List<Closure.V> callbacks = new ArrayList<>();
	
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
		if (view != null)
			NUM_WINDOWS.incrementAndGet();
		
		this.view = view;
		this.windowWidth = width;
		this.windowHeight = height;
		
		// make sure this comes before setting up window hints due to side effects!
        GLFWWindow shared = GLContextManager.getSharedContextWindow();

        boolean interactive = config.getViewType() == ViewType.INTERACTIVE_VIEW;
		
        GLFW.glfwDefaultWindowHints();

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);		
        
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, interactive ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, interactive ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, shared != null ? shared.window : MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("failed to create window");
        
        GLFWWindowSizeCallback sizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (DBG)
					System.out.println("window resize: " + view + " " + width + " " + height);

				GLFWWindow.this.windowWidth = width;
				GLFWWindow.this.windowHeight = height;
			}
		};
		GLFW.glfwSetWindowSizeCallback(window, sizeCallback);
		callbacks.add(sizeCallback);
		
		makeCurrent(true);
		GLFW.glfwSwapInterval(1);
		makeCurrent(false);
 	}
	
	GLFWWindow() {
		this(null, 16, 16, "", IView.RENDER_VIEW);
	}
	
	@Override
	public void destroy() {
		callbacks.forEach(c -> c.free());
		callbacks.clear();
		GLFW.glfwDestroyWindow(window);
		window = 0;
		
		// XXX not sure if this is what we want in all cases, but for now ok.
		if (NUM_WINDOWS.decrementAndGet() == 0)
			Platform.get().exit();
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
				if (DBG)
					System.out.println("window close request: " + view);
				
				listener.windowCloseRequest(GLFWWindow.this);
			}
		};
		
		GLFWWindowRefreshCallback refreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				if (DBG)
					System.out.println("window refresh request: " + view);
				
				listener.windowRefresh(GLFWWindow.this);
			}
		};
		
		GLFWWindowFocusCallback focusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, int focused) {
				if (DBG)
					System.out.println("window focus: " + view + " " + focused);

				if (focused > 0)
					listener.windowGainedFocus(GLFWWindow.this);
				else
					listener.windowLostFocus(GLFWWindow.this);
			}
		};
		
		GLFWFramebufferSizeCallback sizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (DBG)
					System.out.println("framebuffer resize: " + view + " " + width + " " + height);
				
				framebufferWidth = width;
				framebufferHeight = height;
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
				if (DBG)
					System.out.println("window key: " + view + " " + key + " " + scancode + " " + action + " " + mods);
				
				modifiers = mods;
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
				if (DBG)
					System.out.println("window pointer entered: " + view + " " + entered);

				IPointerEvent event = new PointerEvent(view, modifiers, 0, 0, pointerX, pointerY, 0, 0);
				if (entered > 0)
					listener.pointerEntered(event);
				else 
					listener.pointerExited(event);
			}
		};

		GLFWCursorPosCallback pointerPositionCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				if (DBG)
					System.out.println("window pointer moved: " + view + " " + xpos + " " + ypos);
				
				float sx = (float)framebufferWidth / (float)windowWidth;
				float sy = (float)framebufferHeight / (float)windowHeight;
				
				pointerX = sx * (float)xpos;
				pointerY = sy * (float)(windowHeight - ypos);				
				
				IPointerEvent event = new PointerEvent(view, modifiers, 0, 0, pointerX, pointerY, 0, 0);
				if (pointerButtons == 0) {
					listener.pointerMoved(event);
				} else {
					pointerDragged = true;
					listener.pointerDragged(event);
				}
			}
		};
		
		GLFWMouseButtonCallback pointerButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				if (DBG)
					System.out.println("window pointer button: " + view + " " + button + " " + action + " " + mods);

				modifiers = mods;
				int mask = 1 << button;
				IPointerEvent event = new PointerEvent(view, mods, button, 1, pointerX, pointerY, 0, 0);
				if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
					pointerButtons |= mask;
					listener.pointerPressed(event);
				} else if (action == GLFW.GLFW_RELEASE) {
					pointerButtons &= ~mask;
					listener.pointerReleased(event);
				}
				if (pointerButtons == 0) {
					if (!pointerDragged)
						listener.pointerClicked(event);
					pointerDragged = false;
				}
			}
		};
		
		GLFWScrollCallback pointerScrollCallback = new GLFWScrollCallback() {			
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				if (DBG)
					System.out.println("window pointer scrolled: " + view + " " + xoffset + " " + yoffset);
	
				listener.pointerWheelMoved(new PointerEvent(view, modifiers, 0, 0, pointerX, pointerY, (float)xoffset, (float)-yoffset));
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
	
	public int getWidth() {
		return windowWidth;
	}
	
	public int getHeight() {
		return windowHeight;
	}
	
	@Override
	public void setFullscreen(boolean enabled) {
	}

	@Override
	public void setPointerMode(PointerMode mode) {
		switch (mode) {
		case NORMAL:
			GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
			break;
		case DISABLED:
			GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
			break;
		case HIDDEN:
			GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
			break;
		}
	}

	@Override
	public void setPointerPosition(float x, float y) {
		GLFW.glfwSetCursorPos(window, x, windowHeight - y);
	}
}
