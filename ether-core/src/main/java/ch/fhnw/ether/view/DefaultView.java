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

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEventScheduler.IAction;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.view.IWindow.IKeyListener;
import ch.fhnw.ether.view.IWindow.IPointerListener;
import ch.fhnw.ether.view.IWindow.IWindowListener;
import ch.fhnw.util.Viewport;
import ch.fhnw.util.math.Vec2;

/**
 * Default view class that implements some basic functionality. Use as base for
 * more complex implementations.
 * 
 * @author radar
 */
public class DefaultView implements IView {

	private final Config viewConfig;

	private final IController controller;

	private volatile GLFWWindow window;

	private volatile Viewport viewport = new Viewport(0, 0, 1, 1);

	private boolean enabled = true;

	public DefaultView(IController controller, int x, int y, int w, int h, Config viewConfig, String title) {
		this.controller = controller;
		this.viewConfig = viewConfig;

		Platform.get().runOnMainThread(() -> {
			window = new GLFWWindow(this, new Vec2(16, 16), title != null ? title : "", viewConfig);
	
			window.setWindowListener(windowListener);
			window.setKeyListener(keyListener);
			window.setPointerListener(pointerListener);
	
			// Note: we open the window initially at a smaller size, and then
			// resize in order to trigger the window listener.
			window.setSize(new Vec2(w, h));
			if (x != -1)
				window.setPosition(new Vec2(x, y));
			window.setVisible(true);
		});
		
		runOnSceneThread(t -> controller.viewCreated(this));
	}

	@Override
	public void dispose() {
		Platform.get().runOnMainThread(() -> {
			window.destroy();
			window = null;
		});
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
		if (controller.isSceneThread())
			action.run(controller.getScheduler().getTime());
		else
			controller.run(action);
	}

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
