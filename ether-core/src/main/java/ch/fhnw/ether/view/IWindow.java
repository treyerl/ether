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

import java.io.File;

public interface IWindow {
	/**
	 * Returns the title, which is currently displayed for this window.
	 * @return
	 */
	String getTitle();

	/**
	 * Changes the title of this window to the given string.
	 * @param title New window title.
	 */
	void setTitle(String title);
	
	/**
	 * Shows / hides this window.
	 */
	void setVisible(boolean visible);

	/**
	 * Set this window's position (in window units)
	 */
	void setPosition(int x, int y);

	/**
	 * Set this window's size.
	 */
	void setSize(int width, int height);

	/**
	 * Enable or disable this window as a fullscreen window.
	 */
	void setFullscreen(boolean enabled);

	/**
	 * Enable or disable mouse pointer for this window.
	 */
	void setPointerVisible(boolean visible);

	/**
	 * Confine or unconfine pointer for this window.
	 */
	void setPointerConfined(boolean confined);

	/**
	 * Set pointer icon for this window.
	 */
	void setPointerIcon(File pngImage, int hotspotX, int hotspotY);

	/**
	 * Warp pointer to x y (in pixel units), in right-handed window coordinates
	 * (origin bottom left).
	 */
	void warpPointer(int x, int y);

	/**
	 * Convert from pixel to window units.
	 */
	int convertFromPixelToWindowUnits(int value);

	/**
	 * Convert from window to pixel units.
	 */
	int convertFromWindowToPixelUnits(int value);

	/**
	 * Display (i.e. render) this view. Must be run from render thread.
	 */
	void display();
}
