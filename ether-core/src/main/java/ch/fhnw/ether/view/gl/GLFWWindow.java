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

import ch.fhnw.ether.view.IView.Config;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.math.Vec2;

/**
 * JOGL/NEWT window class.
 *
 * @author radar
 */
final class GLFWWindow implements IWindow {
	private static int numWindows = 0;

	private long window;

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
	public GLFWWindow(int width, int height, Config config) {
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
	public GLFWWindow(int width, int height, String title, Config config) {
	}

	public void dispose() {
	}

	public long getWindow() {
		return 0;
	}

	public void requestFocus() {
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public void setTitle(String title) {
	}

	@Override
	public void setVisible(boolean visible) {
	}

	public Vec2 getPosition() {
		return null;
	}

	@Override
	public void setPosition(int x, int y) {
	}

	@Override
	public void setSize(int width, int height) {
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

	@Override
	public void display() {
	}
}
