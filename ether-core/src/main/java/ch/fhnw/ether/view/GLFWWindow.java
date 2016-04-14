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

package ch.fhnw.ether.view;

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
import org.lwjgl.glfw.GLFWWindowPosCallback;
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
import ch.fhnw.ether.render.gl.GLContextManager;
import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IView.ViewType;
import ch.fhnw.util.math.Vec2;

/**
 * GLFW window class.
 *
 * @author radar
 */
public final class GLFWWindow implements IWindow {
	private static final boolean DBG = false;
	
	private static final AtomicInteger NUM_WINDOWS = new AtomicInteger();
	
	private IView view;
	
	private Vec2 windowPosition = Vec2.ZERO;
	private Vec2 windowSize = Vec2.ONE;
	private Vec2 framebufferSize = Vec2.ONE;
	
	private String title;
	
	private long window;
	
	private float pointerX = 0;
	private float pointerY = 0;
	private int pointerButtons = 0;
	private boolean pointerDragged = false;
	private int modifiers = 0;
	private int vao = -1;
	
	private final List<Closure.V> callbacks = new ArrayList<>();
	private IWindowListener windowListener = VOID_WINDOW_LISTENER;
	private IKeyListener keyListener = VOID_KEY_LISTENER;
	private IPointerListener pointerListener = VOID_POINTER_LISTENER;
	
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
	public GLFWWindow(IView view, Vec2 size, String title, Config config) {
		if (DBG)
			System.out.println("window create: " + size + " " + title);

		if (view != null)
			NUM_WINDOWS.incrementAndGet();
		
		this.view = view;
		this.windowSize = size;
		this.title = title;
		
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

        window = GLFW.glfwCreateWindow((int)size.x, (int)size.y, title, MemoryUtil.NULL, shared != null ? shared.window : MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("failed to create window");
        
        setCallbacks();
		
		makeCurrent(true);
		GLFW.glfwSwapInterval(1);
		makeCurrent(false);
		
		if (DBG)
			System.out.println("window created.");
		
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

	@Override
	public String getTitle() {
		return title;
	}
	
	@Override
	public void setTitle(String title) {
		this.title = title;
		GLFW.glfwSetWindowTitle(window, title);
	}

	@Override
	public boolean isVisible() {
		return GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_VISIBLE) > 0 ? true : false;
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
	public Vec2 getPosition() {
		return windowPosition;
	}

	@Override
	public void setPosition(Vec2 position) {
		windowPosition = position;
		GLFW.glfwSetWindowPos(window, (int)position.x, (int)position.y);
	}
	
	@Override
	public Vec2 getSize() {
		return windowSize;
	}

	@Override
	public void setSize(Vec2 size) {
		windowSize = size;
		GLFW.glfwSetWindowSize(window, (int)size.x, (int)size.y);
	}
	
	@Override
	public Vec2 getFramebufferSize() {
		return framebufferSize;
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
		GLFW.glfwSetCursorPos(window, x, y);
	}
	
	public void setWindowListener(IWindowListener windowListener) {
		this.windowListener = windowListener;
	}
	
	public void setKeyListener(IKeyListener keyListener) {
		this.keyListener = keyListener;
	}
	
	public void setPointerListener(IPointerListener pointerListener) {
		this.pointerListener = pointerListener;
	}
	
	private void setCallbacks() {
		//---- WINDOW CALLBACKS
		
		GLFWWindowCloseCallback closeCallback = new GLFWWindowCloseCallback() {
			@Override
			public void invoke(long window) {
				if (DBG)
					System.out.println("window close request: " + view);
				
				windowListener.windowCloseRequest(GLFWWindow.this);
			}
		};
				
		GLFWWindowRefreshCallback refreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				if (DBG)
					System.out.println("window refresh request: " + view);
				
				windowListener.windowRefresh(GLFWWindow.this);
			}
		};
		
		GLFWWindowFocusCallback focusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, int focused) {
				if (DBG)
					System.out.println("window focus: " + view + " " + focused);

				if (focused > 0)
					windowListener.windowGainedFocus(GLFWWindow.this);
				else
					windowListener.windowLostFocus(GLFWWindow.this);
			}
		};
		
		GLFWWindowPosCallback positionCallback = new GLFWWindowPosCallback() {	
			@Override
			public void invoke(long window, int xpos, int ypos) {
				if (DBG)
					System.out.println("window position request: " + view);
				
				windowPosition = new Vec2(xpos, ypos);
			}
		};

		GLFWWindowSizeCallback sizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (DBG)
					System.out.println("window resize: " + view + " " + width + " " + height);

				windowSize = new Vec2(width, height);
			}
		};

		GLFWFramebufferSizeCallback framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (DBG)
					System.out.println("framebuffer resize: " + view + " " + width + " " + height);
				
				framebufferSize = new Vec2(width, height);
				windowListener.framebufferResized(GLFWWindow.this, width, height);
			}
		};

		GLFW.glfwSetWindowCloseCallback(window, closeCallback);
		GLFW.glfwSetWindowRefreshCallback(window, refreshCallback);
		GLFW.glfwSetWindowFocusCallback(window, focusCallback);
		GLFW.glfwSetWindowPosCallback(window, positionCallback);
		GLFW.glfwSetWindowSizeCallback(window, sizeCallback);
		GLFW.glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);

		callbacks.add(closeCallback);
		callbacks.add(refreshCallback);
		callbacks.add(focusCallback);
		callbacks.add(positionCallback);
		callbacks.add(sizeCallback);
		callbacks.add(framebufferSizeCallback);

		
		//---- KEY CALLBACKS
		
		GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (DBG)
					System.out.println("window key: " + view + " " + key + " " + scancode + " " + action + " " + mods);
				
				modifiers = mods;
				if (action == GLFW.GLFW_PRESS)
					keyListener.keyPressed(new KeyEvent(view, mods, key, scancode, false));
				else if (action == GLFW.GLFW_REPEAT)
					keyListener.keyPressed(new KeyEvent(view, mods, key, scancode, true));
				else if (action == GLFW.GLFW_RELEASE)
					keyListener.keyReleased(new KeyEvent(view, mods, key, scancode, false));
			}
		};

		GLFW.glfwSetKeyCallback(window, keyCallback);
		callbacks.add(keyCallback);
		

		//---- POINTER CALLBACKS
		
		GLFWCursorEnterCallback pointerEnterCallback = new GLFWCursorEnterCallback() {
			@Override
			public void invoke(long window, int entered) {
				if (DBG)
					System.out.println("window pointer entered: " + view + " " + entered);

				IPointerEvent event = new PointerEvent(view, modifiers, 0, 0, pointerX, pointerY, 0, 0);
				if (entered > 0)
					pointerListener.pointerEntered(event);
				else 
					pointerListener.pointerExited(event);
			}
		};

		GLFWCursorPosCallback pointerPositionCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				if (DBG)
					System.out.println("window pointer moved: " + view + " " + xpos + " " + ypos);
				
				float sx = framebufferSize.x / windowSize.x;
				float sy = framebufferSize.y / windowSize.y;
				
				pointerX = sx * (float)xpos;
				pointerY = sy * (float)(windowSize.y - ypos);				
				
				IPointerEvent event = new PointerEvent(view, modifiers, 0, 0, pointerX, pointerY, 0, 0);
				if (pointerButtons == 0) {
					pointerListener.pointerMoved(event);
				} else {
					pointerDragged = true;
					pointerListener.pointerDragged(event);
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
					pointerListener.pointerPressed(event);
				} else if (action == GLFW.GLFW_RELEASE) {
					pointerButtons &= ~mask;
					pointerListener.pointerReleased(event);
				}
				if (pointerButtons == 0) {
					if (!pointerDragged)
						pointerListener.pointerClicked(event);
					pointerDragged = false;
				}
			}
		};
		
		GLFWScrollCallback pointerScrollCallback = new GLFWScrollCallback() {			
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				if (DBG)
					System.out.println("window pointer scrolled: " + view + " " + xoffset + " " + yoffset);
	
				pointerListener.pointerWheelMoved(new PointerEvent(view, modifiers, 0, 0, pointerX, pointerY, (float)xoffset, (float)-yoffset));
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
}
