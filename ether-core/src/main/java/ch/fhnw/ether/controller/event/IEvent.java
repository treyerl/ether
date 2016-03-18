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

package ch.fhnw.ether.controller.event;

import ch.fhnw.ether.view.IView;

public interface IEvent {
	int SHIFT_MASK = 1 << 0;
	int CONTROL_MASK = 1 << 1;
	int META_MASK = 1 << 2;
	int ALT_MASK = 1 << 3;
	int ALT_GRAPH_MASK = 1 << 4;
	
	int MODIFIER_MASK = SHIFT_MASK | CONTROL_MASK | META_MASK | ALT_MASK | ALT_GRAPH_MASK;

	IView getView();

	int getModifiers();
	
	default boolean isModifierDown() {
		return getModifiers() != 0;
	}
	
	default boolean isShiftDown() {
		return (getModifiers() & SHIFT_MASK) != 0;
	}

	default boolean isControlDown() {
		return (getModifiers() & CONTROL_MASK) != 0;
	}

	default boolean isMetaDown() {
		return (getModifiers() & META_MASK) != 0;
	}

	default boolean isAltDown() {
		return (getModifiers() & ALT_MASK) != 0;
	}

	default boolean isAltGraphDown() {
		return (getModifiers() & ALT_GRAPH_MASK) != 0;
	}
}
