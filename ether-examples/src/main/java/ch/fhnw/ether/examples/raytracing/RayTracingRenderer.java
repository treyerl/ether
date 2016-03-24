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

package ch.fhnw.ether.examples.raytracing;

public class RayTracingRenderer /*implements IRenderer*/ {
/*
	private final RayTracer       rayTracer;
	private final ForwardRenderer renderer      = new ForwardRenderer();
	private final Texture         screenTexture;
	private final IMesh           plane;
	private long                  n = 0;

	public RayTracingRenderer(RayTracer rayTracer) {
		this.rayTracer = rayTracer;
		this.screenTexture = new Texture(rayTracer, false);
		this.plane         = createScreenPlane(-1, -1, 2, 2, screenTexture);
		this.renderer.addMesh(plane);
	}
	
	@Override
	public void render(IRenderProgram program) {
		long t = System.currentTimeMillis();
		Viewport viewport = view.getViewport();
		if (viewport.w != rayTracer.getWidth() || viewport.h != rayTracer.getHeight())
			rayTracer.setSize(viewport.w, viewport.h);
		rayTracer.setCamera(view.getCamera());
		rayTracer.setLights(view.getController().getScene().getLightInfo());
		
		screenTexture.update();

		renderer.render(gl, view);
		System.out.println((System.currentTimeMillis() - t) + "ms for " + ++n + "th frame");
	}

	private static IMesh createScreenPlane(float x, float y, float w, float h, Texture texture) {
		float[] vertices = { x, y, 0, x + w, y, 0, x + w, y + h, 0, x, y, 0, x + w, y + h, 0, x, y + h, 0 };
		IGeometry geometry = DefaultGeometry.createVM(Primitive.TRIANGLES, vertices, MeshLibrary.DEFAULT_QUAD_TEX_COORDS);

		return new DefaultMesh(new ColorMapMaterial(texture), geometry, Queue.DEVICE_SPACE_OVERLAY);
	}

	public void addMesh(IMesh mesh) {
		rayTracer.addMesh(mesh);
	}

	public void removeMesh(IMesh mesh) {
		rayTracer.removeMesh(mesh);
	}

	public void addLight(ILight light) {
		rayTracer.addLight(light);
	}

	public void removeLight(ILight light) {
		rayTracer.removeLight(light);
	}
*/
}
