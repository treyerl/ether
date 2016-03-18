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

package ch.fhnw.ether.examples.raytracing.surface;

import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.Line;

public class Sphere implements IParametricSurface {

	private float r;
	private Vec3 pos = Vec3.ZERO;

	public Sphere(float radius) {
		this.r = radius;
	}

	// From http://www.trenki.net/files/Raytracing1.pdf
	@Override
	public Vec3 intersect(Line ray) {
		Vec3 o = ray.getOrigin();
		Vec3 d = ray.getDirection();
		Vec3 c = pos;

		Vec3 l = c.subtract(o);
		float t;
		float r2 = r * r;
		float s = l.dot(d);
		float l2 = l.dot(l);
		if (s < 0 && l2 > r2)
			return null;
		float m2 = l2 - s * s;
		if (m2 > r2)
			return null;
		float q = (float) Math.sqrt(r2 - m2);
		if (l2 > r2)
			t = s - q;
		else
			t = s + q;
		return o.add(d.scale(t));
	}

	@Override
	public Vec3 getNormalAt(Vec3 position) {
		return position.subtract(pos).normalize();
	}

	public float getR() {
		return r;
	}

	public void setR(float r) {
		this.r = r;
	}

	@Override
	public Vec3 getPosition() {
		return pos;
	}

	@Override
	public void setPosition(Vec3 pos) {
		this.pos = pos;
	}

	@Override
	public String toString() {
		return "sphere(center=" + pos + ",r=" + r + ")";
	}

}
