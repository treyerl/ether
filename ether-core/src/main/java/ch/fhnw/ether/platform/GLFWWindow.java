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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.lwjgl.BufferUtils;
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

import ch.fhnw.ether.render.gl.GLContextManager;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.math.Vec2;

/**
 * GLFW window class.
 *
 * @author radar
 */
final class GLFWWindow implements IWindow {
	private static final boolean DBG = false;
	
	private static final AtomicInteger NUM_WINDOWS = new AtomicInteger();
	
	private static final IntBuffer INT_BUFFER_0 = BufferUtils.createIntBuffer(1);
	private static final IntBuffer INT_BUFFER_1 = BufferUtils.createIntBuffer(1);
	
	private String title;
	
	private Vec2 windowPosition = Vec2.ZERO;
	private Vec2 windowSize = Vec2.ONE;
	private Vec2 framebufferSize = Vec2.ONE;
	
	private long window;
	
	private Vec2 pointerPosition = Vec2.ZERO;
	private int pointerButtons = 0;
	private boolean pointerDragged = false;
	private int modifiers = 0;
	private int vao = -1;
	
	private final List<Closure.V> callbacks = new ArrayList<>();
	private IWindowListener windowListener;
	private IKeyListener keyListener;
	private IPointerListener pointerListener;
	
	private final Lock contextLock = new ReentrantLock();
	private final IContext context = new IContext() {
		@Override
		public void close() throws Exception {
			releaseContext();
		}
	};
	
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
	public GLFWWindow(Vec2 size, String title, boolean decorated) {
		if (DBG)
			System.out.println("window create: " + size + " " + title);

		if (title != null)
			NUM_WINDOWS.incrementAndGet();
		else
			title = "context";
		
		this.title = title;
		this.windowSize = size;
		
		// make sure this comes before setting up window hints due to side effects!
        GLFWWindow shared = (GLFWWindow)GLContextManager.getSharedContextWindow();

        GLFW.glfwDefaultWindowHints();

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);		
        
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);

        window = GLFW.glfwCreateWindow((int)size.x, (int)size.y, title, MemoryUtil.NULL, shared != null ? shared.window : MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("failed to create window");
        
        setCallbacks();
        
        GLFW.glfwGetFramebufferSize(window, INT_BUFFER_0, INT_BUFFER_1);
        framebufferSize = new Vec2(INT_BUFFER_0.get(0), INT_BUFFER_1.get(0));
		
		try (IContext context = acquireContext()) {
			GLFW.glfwSwapInterval(1);
		} catch (Exception e) {
		}
		
		if (DBG)
			System.out.println("window created.");		
 	}
	
	@Override
	public void destroy() {
		contextLock.lock();
		GLFW.glfwDestroyWindow(window);
		window = 0;
		callbacks.forEach(c -> c.free());
		callbacks.clear();
		contextLock.unlock();
		
		// XXX not sure if this is what we want in all cases, but for now ok.
		if (NUM_WINDOWS.decrementAndGet() == 0)
			Platform.get().exit();
	}
	
	@Override
	public boolean isDestroyed() {
		return window == 0;
	}
	
	@Override
	public IContext acquireContext() {
		contextLock.lock();
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
		return context;
	}
	
	@Override
	public void releaseContext() {
		GLFW.glfwMakeContextCurrent(0);
		contextLock.unlock();
	}
	
	@Override
	public void swapBuffers() {
		if (window == 0)
			return;
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
	
	@Override
	public void setWindowListener(IWindowListener windowListener) {
		this.windowListener = windowListener;
	}
	
	@Override
	public void setKeyListener(IKeyListener keyListener) {
		this.keyListener = keyListener;
	}
	
	@Override
	public void setPointerListener(IPointerListener pointerListener) {
		this.pointerListener = pointerListener;
	}
	
	private void setCallbacks() {
		//---- WINDOW CALLBACKS
		
		GLFWWindowCloseCallback closeCallback = new GLFWWindowCloseCallback() {
			@Override
			public void invoke(long window) {
				if (DBG)
					System.out.println("window close request: " + title);
				
				// still handle close even if there's no listener
				if (windowListener == null) {
					Platform.get().runOnMainThread(() -> {
						destroy();
					});
					return;
				}
				windowListener.windowCloseRequest(GLFWWindow.this);
			}
		};
				
		GLFWWindowRefreshCallback refreshCallback = new GLFWWindowRefreshCallback() {
			@Override
			public void invoke(long window) {
				if (DBG)
					System.out.println("window refresh request: " + title);
				
				if (windowListener == null)
					return;
				windowListener.windowRefresh(GLFWWindow.this);
			}
		};
		
		GLFWWindowFocusCallback focusCallback = new GLFWWindowFocusCallback() {
			@Override
			public void invoke(long window, int focused) {
				if (DBG)
					System.out.println("window focus: " + title + " " + focused);

				if (windowListener == null)
					return;
				windowListener.windowFocusChanged(GLFWWindow.this, focused > 0);
			}
		};
		
		GLFWWindowPosCallback positionCallback = new GLFWWindowPosCallback() {	
			@Override
			public void invoke(long window, int xpos, int ypos) {
				if (DBG)
					System.out.println("window position request: " + title);
				
				windowPosition = new Vec2(xpos, ypos);
			}
		};

		GLFWWindowSizeCallback sizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (DBG)
					System.out.println("window resize: " + title + " " + width + " " + height);

				windowSize = new Vec2(width, height);
				
				if (windowListener == null)
					return;
				windowListener.windowResized(GLFWWindow.this, windowSize, framebufferSize);
			}
		};

		GLFWFramebufferSizeCallback framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (DBG)
					System.out.println("framebuffer resize: " + title + " " + width + " " + height);
				
				framebufferSize = new Vec2(width, height);

				if (windowListener == null)
					return;
				windowListener.windowResized(GLFWWindow.this, windowSize, framebufferSize);
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
					System.out.println("window key: " + title + " " + key + " " + scancode + " " + action + " " + mods);
				
				modifiers = mods;

				if (keyListener == null)
					return;				
				if (action == GLFW.GLFW_PRESS)
					keyListener.keyPressed(GLFWWindow.this, mods, key, scancode, false);
				else if (action == GLFW.GLFW_REPEAT)
					keyListener.keyPressed(GLFWWindow.this, mods, key, scancode, true);
				else if (action == GLFW.GLFW_RELEASE)
					keyListener.keyReleased(GLFWWindow.this, mods, key, scancode);
			}
		};

		GLFW.glfwSetKeyCallback(window, keyCallback);
		callbacks.add(keyCallback);
		

		//---- POINTER CALLBACKS
		
		GLFWCursorEnterCallback pointerEnterCallback = new GLFWCursorEnterCallback() {
			@Override
			public void invoke(long window, int entered) {
				if (DBG)
					System.out.println("window pointer entered: " + title + " " + entered);

				if (pointerListener == null)
					return;				
				if (entered > 0)
					pointerListener.pointerEntered(GLFWWindow.this, modifiers, pointerPosition);
				else 
					pointerListener.pointerExited(GLFWWindow.this, modifiers, pointerPosition);
			}
		};

		GLFWCursorPosCallback pointerPositionCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				if (DBG)
					System.out.println("window pointer moved: " + title + " " + xpos + " " + ypos);
				
				float sx = framebufferSize.x / windowSize.x;
				float sy = framebufferSize.y / windowSize.y;
				pointerPosition = new Vec2(sx * (float)xpos, sy * (float)(windowSize.y - ypos));

				if (pointerListener == null)
					return;				
				if (pointerButtons == 0) {
					pointerListener.pointerMoved(GLFWWindow.this, modifiers, pointerPosition);
				} else {
					pointerDragged = true;
					pointerListener.pointerDragged(GLFWWindow.this, modifiers, pointerPosition);
				}
			}
		};
		
		GLFWMouseButtonCallback pointerButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				if (DBG)
					System.out.println("window pointer button: " + title + " " + button + " " + action + " " + mods);

				modifiers = mods;
				int mask = 1 << button;
				if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
					pointerButtons |= mask;
					if (pointerListener != null)
						pointerListener.pointerPressed(GLFWWindow.this, modifiers, pointerPosition, button);
				} else if (action == GLFW.GLFW_RELEASE) {
					pointerButtons &= ~mask;
					if (pointerListener != null)
						pointerListener.pointerReleased(GLFWWindow.this, modifiers, pointerPosition, button);
				}
				if (pointerButtons == 0) {
					if (!pointerDragged && pointerListener != null)
						pointerListener.pointerClicked(GLFWWindow.this, modifiers, pointerPosition, button);
					pointerDragged = false;
				}
			}
		};
		
		GLFWScrollCallback pointerScrollCallback = new GLFWScrollCallback() {			
			@Override
			public void invoke(long window, double xoffset, double yoffset) {
				if (DBG)
					System.out.println("window pointer scrolled: " + title + " " + xoffset + " " + yoffset);

				if (pointerListener == null)
					return;
				pointerListener.pointerWheelMoved(GLFWWindow.this, modifiers, pointerPosition, new Vec2((float)xoffset, (float)-yoffset));
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
