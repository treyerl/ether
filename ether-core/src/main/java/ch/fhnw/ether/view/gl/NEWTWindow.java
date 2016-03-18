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

import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.IOUtil.ClassResources;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLRunnable;

import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IWindow;

/**
 * JOGL/NEWT window class.
 *
 * @author radar
 */
final class NEWTWindow implements IWindow {
	private static GLAutoDrawable sharedDrawable = null;
	private static int numWindows = 0;

	private GLWindow window;

	/**
	 * Creates undecorated frame.
	 *
	 * @param width
	 *            the frame's width
	 * @param height
	 *            the frame's height
	 * @param config
	 *            The configuration.
	 */
	public NEWTWindow(int width, int height, Config config) {
		this(width, height, null, config);
	}

	/**
	 * Creates a decorated or undecorated frame with given dimensions
	 *
	 * @param width
	 *            the frame's width
	 * @param height
	 *            the frame's height
	 * @param title
	 *            the frame's title, nor null for an undecorated frame
	 * @param config
	 *            The configuration.
	 */
	public NEWTWindow(int width, int height, String title, Config config) {
		GLCapabilities capabilities = GLContextManager.getCapabilities(config);
		if (sharedDrawable == null)
			sharedDrawable = GLContextManager.getSharedDrawable(capabilities);

		numWindows++;

		window = GLWindow.create(sharedDrawable.getChosenGLCapabilities());
		window.setSharedAutoDrawable(sharedDrawable);
		window.setSize(width, height);

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyed(WindowEvent e) {
				numWindows--;
				if (numWindows == 0)
					System.exit(0);
			}
		});
		if (title != null)
			window.setTitle(title);
		else
			window.setUndecorated(true);
	}

	public void dispose() {
		window.destroy();
	}

	public GLWindow getWindow() {
		return window;
	}

	public void requestFocus() {
		window.requestFocus();
	}

	@Override
	public String getTitle() {
		return window.getTitle();
	}

	@Override
	public void setTitle(String title) {
		window.setTitle(title);
	}

	@Override
	public void setVisible(boolean visible) {
		window.setVisible(visible);
	}

	public Point getPosition() {
		return window.getLocationOnScreen(null);
	}

	@Override
	public void setPosition(int x, int y) {
		window.setPosition(x, y);
	}

	@Override
	public void setSize(int width, int height) {
		window.setSize(width, height);
	}

	@Override
	public void setFullscreen(boolean enabled) {
		window.setFullscreen(enabled);
	}

	@Override
	public void setPointerVisible(boolean visible) {
		window.setPointerVisible(visible);
	}

	@Override
	public void setPointerConfined(boolean confined) {
		window.confinePointer(confined);
	}

	@Override
	public void setPointerIcon(File pngImage, int hotspotX, int hotspotY) {
		String[] path = { pngImage.getPath() };

		try {
			ClassResources res = new IOUtil.ClassResources(path, null, null);
			Display display = window.getScreen().getDisplay();
			PointerIcon icon = display.createPointerIcon(res, hotspotX, hotspotY);
			window.setPointerIcon(icon);
		} catch (Exception e) {
		} 
	}

	@Override
	public void warpPointer(int x, int y) {
		window.warpPointer(x, window.getSurfaceHeight() - y);
	}
	
	@Override
	public int convertFromPixelToWindowUnits(int value) {
		return window.convertToWindowUnits(new int[] { value })[0];
	}
	
	@Override
	public int convertFromWindowToPixelUnits(int value) {
		return window.convertToPixelUnits(new int[] { value })[0];
	}

	@Override
	public void display(GLRunnable runnable) {
		window.invoke(false, runnable);
	}
}
