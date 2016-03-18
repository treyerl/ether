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

package ch.fhnw.ether.render.deferred;

import java.util.IdentityHashMap;
import java.util.Map;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import ch.fhnw.ether.render.AbstractRenderer;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.view.IView;


/**
 * Deferred renderer otw...
 * http://learnopengl.com/#!Advanced-Lighting/Deferred-Shading
 *
 * @author radar
 */
// XXX currently defunct, just a few tests....
public final class DeferredRenderer extends AbstractRenderer {
	
	private final Map<IView, GeometryBuffer> geometryBuffers = new IdentityHashMap<>();

	public DeferredRenderer() {
	}

	@Override
	protected void render(GL3 gl, IRenderTargetState state) {
		globals.viewInfo.setCameraSpace(gl);

		// 1. DEPTH QUEUE (DEPTH WRITE&TEST ENABLED, BLEND OFF)
		
		// create/update geometry buffer
		GeometryBuffer geometryBuffer = geometryBuffers.get(state.getView());
		if (geometryBuffer == null) {
			geometryBuffer = new GeometryBuffer();
			geometryBuffers.put(state.getView(), geometryBuffer);
		}
		geometryBuffer.update(gl, state);
		
		
		// 1.1 GEOMETRY PASS

		// FIXME: where do we deal with two-sided vs one-sided? mesh options?
		// shader dependent?
		// gl.glEnable(GL.GL_CULL_FACE);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1, 3);
		
		geometryBuffer.enable(gl);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		
		renderObjects(gl, state, Queue.DEPTH);
		
		geometryBuffer.disable(gl);
		
		gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
		// gl.glDisable(GL.GL_CULL_FACE);
		
		
		// blit
		geometryBuffer.blit(gl);

		
		// 1.2 LIGHT PASS


		
		
		// 2. TRANSPARENCY QUEUE (DEPTH WRITE DISABLED, DEPTH TEST ENABLED, BLEND ON)
		gl.glEnable(GL.GL_BLEND);
		gl.glDepthMask(false);
		renderObjects(gl, state, Queue.TRANSPARENCY);

		// 3. OVERLAY QUEUE (DEPTH WRITE&TEST DISABLED, BLEND ON)
		gl.glDisable(GL.GL_DEPTH_TEST);
		renderObjects(gl, state, Queue.OVERLAY);

		// 4. DEVICE SPACE OVERLAY QUEUE (DEPTH WRITE&TEST DISABLED, BLEND ON)
		globals.viewInfo.setOrthoDeviceSpace(gl);
		renderObjects(gl, state, Queue.DEVICE_SPACE_OVERLAY);

		// 5. SCREEN SPACE OVERLAY QUEUE(DEPTH WRITE&TEST DISABLED, BLEND ON)
		globals.viewInfo.setOrthoScreenSpace(gl);
		renderObjects(gl, state, Queue.SCREEN_SPACE_OVERLAY);

		// 6. CLEANUP: RETURN TO DEFAULTS
		gl.glDisable(GL.GL_BLEND);
		gl.glDepthMask(true);
	}
}
