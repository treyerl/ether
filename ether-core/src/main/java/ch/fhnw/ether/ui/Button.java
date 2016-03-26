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

package ch.fhnw.ether.ui;

import java.awt.Color;

import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.view.IView;

public class Button extends AbstractWidget {
	public interface IButtonAction extends IWidgetAction<Button> {
		@Override
		void execute(Button button, IView view);
	}

	private static final int BUTTON_WIDTH = 48;
	private static final int BUTTON_HEIGHT = 48;

	private static final int BUTTON_GAP = 8;

	public enum State {
		DEFAULT(0.6f, 0, 0, 0.75f), PRESSED(1, 0.2f, 0.2f, 0.75f), DISABLED(0.5f, 0.5f, 0.5f, 0.75f);

		State(float r, float g, float b, float a) {
			this.color = new Color(r, g, b, a);
		}

		public Color getColor() {
			return color;
		}

		private final Color color;
	}

	private int key;
	private State state = State.DEFAULT;

	public Button(int x, int y, String label, String help, int key) {
		this(x, y, label, help, key, null);
	}

	public Button(int x, int y, String label, String help, int key, IButtonAction action) {
		super(x, y, label, help, action);
		this.key = key;
	}

	public Button(int x, int y, String label, String help, int key, State state, IButtonAction action) {
		this(x, y, label, help, key, action);
		setState(state);
	}

	public Button(int x, int y, String label, String help, int key, boolean pressed, IButtonAction action) {
		this(x, y, label, help, key, action);
		setState(pressed);
	}

	public int getKey() {
		return key;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
		updateRequest();
	}

	public void setState(boolean pressed) {
		setState(pressed ? State.PRESSED : State.DEFAULT);
		updateRequest();
	}

	@Override
	public boolean hit(IPointerEvent e) {
		float x = e.getX();
		float y = e.getY();
		UI ui = getUI();
		float bx = ui.getX() + getX() * (BUTTON_GAP + BUTTON_WIDTH);
		float by = ui.getY() + getY() * (BUTTON_GAP + BUTTON_HEIGHT);
		return x >= bx && x <= bx + BUTTON_WIDTH && y >= by && y <= by + BUTTON_HEIGHT;
	}

	@Override
	public void draw(GraphicsPlane surface) {
		int bw = Button.BUTTON_WIDTH;
		int bh = Button.BUTTON_HEIGHT;
		int bg = Button.BUTTON_GAP;
		int bx = getX() * (bg + bw);
		int by = getY() * (bg + bh);
		surface.fillRect(getState().getColor(), bx + 4, surface.getHeight() - by - bh - 4, bw, bh);
		String label = getLabel();
		if (label != null)
			surface.drawString(TEXT_COLOR, label, bx + 6, surface.getHeight() - by - 8);

	}

	@Override
	public void fire(IView view) {
		if (state == State.DISABLED)
			return;

		if (getAction() == null)
			throw new UnsupportedOperationException("button '" + getLabel() + "' has no action defined");
		((IButtonAction) getAction()).execute(this, view);
	}

	@Override
	public boolean keyPressed(IKeyEvent e) {
		if (getKey() == e.getKey()) {
			fire(e.getView());
			return true;
		}
		return false;
	}

	@Override
	public boolean pointerPressed(IPointerEvent e) {
		if (hit(e)) {
			fire(e.getView());
			return true;
		}
		return false;
	}
}
