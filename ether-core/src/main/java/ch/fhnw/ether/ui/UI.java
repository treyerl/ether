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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.fhnw.ether.controller.IController;
import ch.fhnw.ether.controller.event.IEvent;
import ch.fhnw.ether.controller.event.IKeyEvent;
import ch.fhnw.ether.controller.event.IPointerEvent;
import ch.fhnw.ether.view.IView;
import ch.fhnw.util.UpdateRequest;

public final class UI {
	private final IController controller;
	private final GraphicsPlane plane = new GraphicsPlane(0, 0, 512, 512);
	private final UpdateRequest updater = new UpdateRequest();

	private final List<IWidget> widgets = new ArrayList<>();
	private String message;

	public UI(IController controller) {
		this.controller = controller;
		enable();
	}

	public void enable() {
		controller.getRenderManager().addMesh(plane.getMesh());
		updateRequest();
	}

	public void disable() {
		controller.getRenderManager().removeMesh(plane.getMesh());
	}

	public boolean update() {
		if (!updater.testAndClear())
			return false;
		
		if (widgets.isEmpty() && message == null)
			return false;

		plane.clear();

		for (IWidget widget : widgets) {
			widget.draw(plane);
		}

		if (message != null)
			plane.drawString(message, 256, plane.getHeight() - 8);
		
		plane.update();

		return true;
	}

	public List<IWidget> getWidgets() {
		return Collections.unmodifiableList(widgets);
	}

	public void addWidget(IWidget widget) {
		widget.setUI(this);
		synchronized (widgets) {
			widgets.add(widget);
		}
		updateRequest();
	}

	public void addWidgets(Collection<? extends IWidget> widgets) {
		widgets.forEach(this::addWidget);
	}

	public void setMessage(String message) {
		synchronized (widgets) {
		 	if (this.message != null && this.message.equals(message))
				return;
			this.message = message;
		}
		updateRequest();
	}

	public int getX() {
		return plane.getX();
	}

	public int getY() {
		return plane.getX();
	}

	public int getWidth() {
		return plane.getWidth();
	}

	public int getHeight() {
		return plane.getHeight();
	}

	public void updateRequest() {
		updater.request();
	}

	// key listener

	public boolean keyPressed(IKeyEvent e) {
		if (isInteractive(e)) {
			for (IWidget widget : getWidgets()) {
				if (widget.keyPressed(e))
					return true;
			}
		}
		return false;
	}

	// pointer listener

	public boolean pointerPressed(IPointerEvent e) {
		if (isInteractive(e)) {
			for (IWidget widget : getWidgets()) {
				if (widget.pointerPressed(e))
					return true;
			}
		}
		return false;
	}

	public boolean pointerReleased(IPointerEvent e) {
		if (isInteractive(e)) {
			for (IWidget widget : getWidgets()) {
				if (widget.pointerReleased(e))
					return true;
			}
		}
		return false;
	}

	// pointer motion listener

	public void pointerMoved(IPointerEvent e) {
		if (isInteractive(e)) {
			for (IWidget widget : getWidgets()) {
				if (widget.hit(e)) {
					String message = widget.getHelp();
					setMessage(message);
					return;
				}
			}
			setMessage(null);
		}
	}

	public boolean pointerDragged(IPointerEvent e) {
		if (isInteractive(e)) {
			for (IWidget widget : getWidgets()) {
				if (widget.pointerDragged(e))
					return true;
			}
		}
		return false;
	}
	
	private static boolean isInteractive(IEvent e) {
		return e.getView().getConfig().getViewType() == IView.ViewType.INTERACTIVE_VIEW;
	}
}
