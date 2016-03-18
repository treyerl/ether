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

package ch.fhnw.ether.render.forward;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import ch.fhnw.ether.render.Renderable;
import ch.fhnw.ether.render.ShaderBuilder;
import ch.fhnw.ether.render.shader.builtin.ShadowVolumeShader;
import ch.fhnw.ether.render.shader.builtin.TrivialDeviceSpaceShader;
import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.ether.scene.mesh.DefaultMesh;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Flag;
import ch.fhnw.ether.scene.mesh.IMesh.Primitive;
import ch.fhnw.ether.scene.mesh.MeshUtilities;
import ch.fhnw.ether.scene.mesh.geometry.DefaultGeometry;
import ch.fhnw.ether.scene.mesh.material.EmptyMaterial;
import ch.fhnw.util.color.RGBA;

public final class ShadowVolumes {
	private static final IMesh OVERLAY_MESH = new DefaultMesh(Primitive.TRIANGLES, new EmptyMaterial(), DefaultGeometry.createV(MeshUtilities.DEFAULT_QUAD_TRIANGLES));

	private ShadowVolumeShader volumeShader;
	private Renderable overlay;

	private int lightIndex;
	private float extrudeDistance = 1000;
	private RGBA volumeColor = new RGBA(1, 0, 0, 0.2f);
	private RGBA overlayColor = new RGBA(0, 0, 0, 0.9f);

	public ShadowVolumes(Map<IAttribute, Supplier<?>> globals) {
		volumeShader = ShaderBuilder.create(new ShadowVolumeShader(() -> lightIndex, () -> extrudeDistance, () -> volumeColor), null, globals);
		overlay = new Renderable(new TrivialDeviceSpaceShader(() -> overlayColor), OVERLAY_MESH, globals);
	}

	// http://ogldev.atspace.co.uk/www/tutorial40/tutorial40.html
	public void render(GL3 gl, IMesh.Queue pass, List<Renderable> renderables, int numLights) {
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_ZERO, GL.GL_SRC_ALPHA);
		gl.glDepthMask(false);
		gl.glEnable(GL3.GL_DEPTH_CLAMP);
		
		overlay.update(gl, OVERLAY_MESH.getMaterial().getData(), OVERLAY_MESH.getTransformedGeometryData());

		for (lightIndex = 0; lightIndex < numLights; ++lightIndex) {
			gl.glClear(GL.GL_STENCIL_BUFFER_BIT);

			gl.glColorMask(false, false, false, false);

			gl.glEnable(GL.GL_STENCIL_TEST);

			gl.glStencilFuncSeparate(GL.GL_FRONT, GL.GL_ALWAYS, 0, 0xffffffff);
			gl.glStencilOpSeparate(GL.GL_FRONT, GL.GL_KEEP, GL.GL_DECR_WRAP, GL.GL_KEEP);

			gl.glStencilFuncSeparate(GL.GL_BACK, GL.GL_ALWAYS, 0, 0xffffffff);
			gl.glStencilOpSeparate(GL.GL_BACK, GL.GL_KEEP, GL.GL_INCR_WRAP, GL.GL_KEEP);

			volumeShader.update(gl, null);
			volumeShader.enable(gl);
			for (Renderable renderable : renderables) {
				if (renderable.containsFlag(Flag.DONT_CAST_SHADOW))
					continue;
				if (renderable.getQueue() != pass)
					continue;

				volumeShader.render(gl, renderable.getBuffer());
			}
			volumeShader.disable(gl);

			gl.glColorMask(true, true, true, true);

			gl.glStencilFunc(GL.GL_NOTEQUAL, 0x0, 0xffffffff);
			gl.glStencilOp(GL.GL_KEEP, GL.GL_KEEP, GL.GL_KEEP);

			overlay.render(gl);

			gl.glDisable(GL.GL_STENCIL_TEST);
		}
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL.GL_BLEND);
		gl.glDepthMask(true);
		gl.glDisable(GL3.GL_DEPTH_CLAMP);
	}
}
