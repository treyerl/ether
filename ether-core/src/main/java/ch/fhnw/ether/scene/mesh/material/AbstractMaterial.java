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

package ch.fhnw.ether.scene.mesh.material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.ether.scene.mesh.geometry.IGeometry.IGeometryAttribute;
import ch.fhnw.util.UpdateRequest;

public abstract class AbstractMaterial implements IMaterial {
	protected static final IAttribute[] NO_ATTRIBUTES = {};
	protected static final Object[] NO_DATA = {};
	
	private final IAttribute[] providedAttributes;
	private final IGeometryAttribute[] geometryAttributes;

	private final UpdateRequest update = new UpdateRequest();
	
	private String name = "material";
	
	protected AbstractMaterial(IAttribute[] providedAttributes, IGeometryAttribute[] geometryAttributes) {
		List<IAttribute> pa = new ArrayList<>(Arrays.asList(providedAttributes));
		pa.removeIf(Objects::isNull);
		this.providedAttributes = pa.toArray(new IAttribute[0]);
		List<IGeometryAttribute> ga = new ArrayList<>(Arrays.asList(geometryAttributes));
		ga.removeIf(Objects::isNull);
		this.geometryAttributes = ga.toArray(new IGeometryAttribute[0]);
	}

	@Override
	public final String getName() {
		return name;
	}
	
	@Override
	public final void setName(String name) {
		this.name = name;
		updateRequest();
	}
	
	@Override
	public final IAttribute[] getProvidedAttributes() {
		return providedAttributes;
	}

	@Override
	public final IGeometryAttribute[] getGeometryAttributes() {
		return geometryAttributes;
	}
	
	@Override
	public abstract Object[] getData();

	@Override
	public final UpdateRequest getUpdater() {
		return update;
	}
	
	@Override
	public final boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AbstractMaterial))
			return false;
		AbstractMaterial m = (AbstractMaterial) obj;
		return name.equals(m.name) && Arrays.equals(providedAttributes, m.providedAttributes) && Arrays.equals(geometryAttributes, m.geometryAttributes) && Arrays.equals(getData(), m.getData());
	}
	
	@Override
	public final int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	protected static IAttribute[] material(IAttribute... attributes) {
		return attributes;
	}

	protected static IGeometryAttribute[] geometry(IGeometryAttribute... attributes) {
		return attributes;
	}
	
	protected static Object[] data(Object... data) {
		return data;
	}
	
	protected final void updateRequest() {
		update.request();
	}
}
