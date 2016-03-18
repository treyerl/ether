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

package ch.fhnw.ether.media;

import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.util.TextUtilities;

public class Parameter implements IAttribute {
	public enum Type {RANGE, ITEMS}

	private final String   name;
	private final String   description;
	private final float    min;
	private final float    max;
	private final String[] items;
	private       float    val;
	private       int      idx = -1;

	public Parameter(String name, String description, float min, float max, float val) {
		this(name, description, min, max, val, null);
	}

	public Parameter(String name, String description, int val, String ... items) {
		this(name, description, Float.MIN_VALUE, Float.MAX_VALUE, val, items);
	}

	public Parameter(String name, String description, float min, float max, float val, String[] items) {
		if(!name.equals(TextUtilities.cleanForId(name)))
			throw new IllegalArgumentException("Illegal (non-id) characters in name '" + name + "'");
		this.name        = name;
		this.description = description;
		this.min         = min;
		this.max         = max;
		this.val         = val;
		this.items       = items;
	}

	private Parameter(Parameter p) {
		this(p.name, p.description, p.min, p.max, p.val, p.items);
		this.idx = p.idx;
	}


	@Override
	final public String toString() {
		return name;
	}

	final public String getName() {
		return name;
	}

	final public String getDescription() {
		return description;
	}

	final public float getMin() {
		return min;
	}

	final public float getMax() {
		return max;
	}

	final float getVal() {
		return val;
	}

	final void setVal(float val) {
		this.val = val;
	}

	final void setIdx(int idx) {
		if(this.idx == idx) return;
		if(this.idx != -1)
			throw new IllegalArgumentException(name + ": parameter already in use");
		this.idx = idx;
	}

	final int getIdx() {
		return idx;
	}

	protected Parameter copy() {
		return new Parameter(this);
	}

	public Type getType() {
		return items == null ? Type.RANGE : Type.ITEMS; 
	}

	public String[] getItems() {
		return items;
	}

	@Override
	public String id() {
		return name;
	}
}
