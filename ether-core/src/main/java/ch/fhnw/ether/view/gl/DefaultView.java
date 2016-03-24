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

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler.IAction;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.ether.view.IWindow.IKeyListener;
import ch.fhnw.ether.view.IWindow.IPointerListener;
import ch.fhnw.ether.view.IWindow.IWindowListener;
import ch.fhnw.util.Viewport;

/**
 * Default view class that implements some basic functionality. Use as base for
 * more complex implementations.
 * 
 * @author radar
 */
public class DefaultView implements IView {

	private final Config viewConfig;

	private final IController controller;

	private GLFWWindow window;

	private volatile Viewport viewport = new Viewport(0, 0, 1, 1);

	private boolean enabled = true;

	public DefaultView(IController controller, int x, int y, int w, int h, Config viewConfig, String title) {
		this.controller = controller;
		this.viewConfig = viewConfig;

		window = new GLFWWindow(this, 16, 16, title, viewConfig);

		window.setWindowListener(windowListener);
		window.setKeyListener(keyListener);
		window.setPointerListener(pointerListener);

		// note: the order here is quite important. the view starts sending
		// events after setVisible(), and we're still in the view's constructor.
		// need to see if this doesn't get us into trouble in the long run.
		// XXX NEEDS FIXING ANYWAY (LWJGL)
		controller.run(time -> {
			controller.viewCreated(this);			
		});

		if (x != -1)
			window.setPosition(x, y);
		window.setSize(w, h);
		window.setVisible(true);
	}

	@Override
	public void dispose() {
		window.destroy();
		window = null;
	}

	@Override
	public final boolean isEnabled() {
		return enabled;
	}

	@Override
	public final void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public Config getConfig() {
		return viewConfig;
	}

	@Override
	public final IController getController() {
		return controller;
	}

	@Override
	public final Viewport getViewport() {
		return viewport;
	}

	@Override
	public IWindow getWindow() {
		return window;
	}
	
	@Override
	public String toString() {
		return "[view " + hashCode() + "]";
	}

	private void runOnSceneThread(IAction action) {
		controller.run(action);
	}

	// GLEventListener implementation
/*
		@Override
		public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
			try {
				GL gl = drawable.getGL();
				height = Math.max(1,  height);
				gl.glViewport(0, 0, width, height);
				viewport = new Viewport(0, 0, width, height);
				runOnSceneThread(time -> controller.viewResized(DefaultView.this));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public final void dispose(GLAutoDrawable drawable) {
			runOnSceneThread(time -> {
				controller.viewDisposed(DefaultView.this);
				window = null;
			});
		}
	};
*/
	// window listener

	private IWindowListener windowListener = new IWindowListener() {
		@Override
		public void windowCloseRequest(IWindow window) {
			dispose();
			runOnSceneThread(time -> {
				controller.viewDisposed(DefaultView.this);
			});
		}
		
		@Override
		public void windowRefresh(IWindow window) {
			controller.repaint();
		}
		
		@Override
		public void windowGainedFocus(IWindow w) {
			runOnSceneThread(time -> controller.viewGainedFocus(DefaultView.this));
		}

		@Override
		public void windowLostFocus(IWindow w) {
			runOnSceneThread(time -> controller.viewLostFocus(DefaultView.this));
		}

		@Override
		public void framebufferResized(IWindow window, int w, int h) {
			viewport = new Viewport(0, 0, w, h);
			runOnSceneThread(time -> controller.viewResized(DefaultView.this));
		}
	};

	// key listener
	/*
	private class ViewKeyEvent implements IKeyEvent {
		final int modifiers;
		final short keySym;
		final short keyCode;
		final char keyChar;
		final boolean isAutoRepeat;

		ViewKeyEvent(KeyEvent e) {
			modifiers = e.getModifiers() & IEvent.MODIFIER_MASK;
			keySym = e.getKeySymbol();
			keyCode = e.getKeyCode();
			keyChar = e.getKeyChar();
			isAutoRepeat = e.isAutoRepeat();
		}

		@Override
		public IView getView() {
			return DefaultView.this;
		}

		@Override
		public int getModifiers() {
			return modifiers;
		}

		@Override
		public short getKeySym() {
			return keySym;
		}

		@Override
		public short getKeyCode() {
			return keyCode;
		}

		@Override
		public char getKeyChar() {
			return keyChar;
		}

		@Override
		public boolean isAutoRepeat() {
			return isAutoRepeat;
		}
	}
	*/

	private IKeyListener keyListener = new IKeyListener() {
		@Override
		public void keyPressed(IKeyEvent e) {
			runOnSceneThread(time -> controller.keyPressed(e));
		}

		@Override
		public void keyReleased(IKeyEvent e) {
			runOnSceneThread(time -> controller.keyReleased(e));
		}
	};

	// mouse listener
/*
	private class ViewPointerEvent implements IPointerEvent {
		final int modifiers;
		final int button;
		final int clickCount;
		final int x;
		final int y;
		final float scrollX;
		final float scrollY;

		ViewPointerEvent(MouseEvent e) {
			modifiers = e.getModifiers() & IEvent.MODIFIER_MASK;
			button = e.getButton();
			clickCount = e.getClickCount();
			x = e.getX();
			y = getViewport().h - e.getY();
			if (e.getPointerCount() > 0) {
				scrollX = e.getRotationScale() * e.getRotation()[0];
				scrollY = -e.getRotationScale() * e.getRotation()[1];
			} else {
				scrollX = 0;
				scrollY = 0;
			}
		}

		@Override
		public IView getView() {
			return DefaultView.this;
		}

		@Override
		public int getModifiers() {
			return modifiers;
		}

		@Override
		public int getButton() {
			return button;
		}

		@Override
		public int getClickCount() {
			return clickCount;
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
		}

		@Override
		public float getScrollX() {
			return scrollX;
		}

		@Override
		public float getScrollY() {
			return scrollY;
		}
	}
*/
	private IPointerListener pointerListener = new IPointerListener() {
		@Override
		public void pointerEntered(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerEntered(e));
		}

		@Override
		public void pointerExited(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerExited(e));
		}

		@Override
		public void pointerPressed(IPointerEvent e) {
			window.requestFocus();
			runOnSceneThread(time -> controller.pointerPressed(e));
		}

		@Override
		public void pointerReleased(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerReleased(e));
		}

		@Override
		public void pointerClicked(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerClicked(e));
		}

		@Override
		public void pointerMoved(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerMoved(e));
		}

		@Override
		public void pointerDragged(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerDragged(e));
		}

		@Override
		public void pointerWheelMoved(IPointerEvent e) {
			runOnSceneThread(time -> controller.pointerScrolled(e));
		}
	};
}
