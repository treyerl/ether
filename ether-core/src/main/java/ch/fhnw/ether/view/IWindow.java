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

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.util.math.Vec2;

public interface IWindow {
	enum PointerMode {
		NORMAL,
		HIDDEN,
		DISABLED
	}
	
	interface IWindowListener {
		void windowCloseRequest(IWindow window);

		void windowRefresh(IWindow window);

		void windowFocusChanged(IWindow window, boolean focused);

		void windowResized(IWindow window, Vec2 windowSize, Vec2 framebufferSize);
	}
	
	interface IKeyListener {
		void keyPressed(IWindow window, int mods, int key, int scancode, boolean repeat);

		void keyReleased(IWindow window, int mods, int key, int scancode);
	}

	interface IPointerListener {
		public void pointerEntered(IWindow window, int mods, Vec2 position);

		public void pointerExited(IWindow window, int mods, Vec2 position);

		public void pointerPressed(IWindow window, int mods, Vec2 position, int button);

		public void pointerReleased(IWindow window, int mods, Vec2 position, int button);

		public void pointerClicked(IWindow window, int mods, Vec2 position, int button);

		public void pointerMoved(IWindow window, int mods, Vec2 position);

		public void pointerDragged(IWindow window, int mods, Vec2 position);

		public void pointerWheelMoved(IWindow window, int mods, Vec2 position, Vec2 scroll);
	}
	
	/**
	 * Destroy this window and free all resources associated with it.
	 */
	void destroy();
	
	/**
	 * Make this window's OpenGL context current or release the current context.
	 */
	void makeCurrent(boolean current);

	/**
	 * Swap buffers.
	 */
	void swapBuffers();	

	/**
	 * Get this window's title.
	 */
	String getTitle();
	
	/**
	 * Set this window's title.
	 */
	void setTitle(String title);

	/**
	 * Returns true if this window is visible.
	 */
	boolean isVisible();
	
	/**
	 * Shows / hides this window.
	 */
	void setVisible(boolean visible);

	/**
	 * Get this window's position (in screen units).
	 */
	Vec2 getPosition();
	
	/**
	 * Set this window's position (in screen units).
	 */
	void setPosition(Vec2 position);

	/**
	 * Get this window's size (in screen units).
	 */
	Vec2 getSize();
	
	/**
	 * Set this window's size (in screen units)
	 */
	void setSize(Vec2 size);

	/**
	 * Get this window's framebuffer size (in pixel units);
	 * @return
	 */
	Vec2 getFramebufferSize();
	
	/**
	 * Enable or disable this window as a fullscreen window.
	 */
	void setFullscreen(boolean enabled);

	/**
	 * Set pointer mode for this window.
	 */
	void setPointerMode(PointerMode mode);

	/**
	 * Warp pointer to x y (in pixel units), in right-handed window coordinates
	 * (origin bottom left).
	 */
	void setPointerPosition(float x, float y);
	
	public void setWindowListener(IWindowListener windowListener);
	
	public void setKeyListener(IKeyListener keyListener);
	
	public void setPointerListener(IPointerListener pointerListener);
	
	static IWindow create(Vec2 size, String title, boolean decorated) {
		return Platform.get().createWindow(size, title, decorated);
	}
}
