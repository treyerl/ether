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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.examples.raytracing.util.IntersectResult;
import ch.fhnw.ether.image.Frame;
import ch.fhnw.ether.image.RGBA8Frame;
import ch.fhnw.ether.media.AbstractFrameSource;
import ch.fhnw.ether.media.IRenderTarget;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.scene.camera.Camera;
import ch.fhnw.ether.scene.camera.ICamera;
import ch.fhnw.ether.scene.light.ILight;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.video.IVideoRenderTarget;
import ch.fhnw.ether.video.IVideoSource;
import ch.fhnw.ether.video.VideoFrame;
import ch.fhnw.util.Log;
import ch.fhnw.util.color.RGB;
import ch.fhnw.util.color.RGBA;
import ch.fhnw.util.math.Vec3;
import ch.fhnw.util.math.geometry.Line;

public class RayTracer extends AbstractFrameSource implements IVideoSource {
	private static final Log LOG = Log.create();

	private static final RGBA BACKGROUND_COLOR   = RGBA.WHITE;
	private static final int  BACKGROUND_COLOR_I = RGBA.WHITE.toRGBA32();

	private final List<RayTraceMesh> meshes = new ArrayList<>();

	private int                   w = 1, h = 1;
	private ICamera               camera = new Camera();
	private List<ILight>          lights = new ArrayList<>();

	public void setSize(int width, int height) {
		this.w = width;
		this.h = height;
	}

	public void setCamera(ICamera camera) {
		this.camera = camera;
	}

	public void addMesh(IMesh mesh) {
		if(mesh instanceof RayTraceMesh)
			meshes.add((RayTraceMesh) mesh);
		else
			LOG.info("Ignored non-RayTraceMesh: " + mesh);
	}

	public void removeMesh(IMesh mesh) {
		meshes.remove(mesh);
	}

	private void intersection(final int x, final int y, final Line ray, final ILight light, final ByteBuffer pixels) {
		// find nearest intersection point in scene
		IntersectResult nearest = new IntersectResult(null, null, BACKGROUND_COLOR, Float.POSITIVE_INFINITY);
		for (RayTraceMesh r : meshes) {
			IntersectResult intersect = r.intersect(ray);
			if (intersect.isValid() && intersect.dist < nearest.dist) {
				nearest = intersect;
			}
		}

		// no object found
		if (!nearest.isValid()) {
			pixels.position((y * w + x) * 4);
			pixels.put((byte)(BACKGROUND_COLOR_I >> 24));
			pixels.put((byte)(BACKGROUND_COLOR_I >> 16));
			pixels.put((byte)(BACKGROUND_COLOR_I >> 8));
			pixels.put((byte)(BACKGROUND_COLOR_I));
			return;
		}

		// create vector which is sure over surface
		Vec3 positionOnSurface = nearest.position.subtract(ray.getDirection().scale(0.01f));
		positionOnSurface = positionOnSurface.add(nearest.surface.getNormalAt(nearest.position).scale(0.0001f));

		float distanceToLight = light.getPosition().subtract(nearest.position).length();

		// check if path to light is clear
		Line lightRay = Line.fromRay(positionOnSurface, light.getPosition().subtract(positionOnSurface));
		for (RayTraceMesh r : meshes) {
			IntersectResult intersect = r.intersect(lightRay);
			if (intersect.isValid() && intersect.dist < distanceToLight) {
				pixels.position((y * w + x) * 4);
				pixels.put((byte)0);
				pixels.put((byte)0);
				pixels.put((byte)0);
				pixels.put(Frame.B255);
				return;
			}
		}

		// diffuse color
		float f = Math.max(0, lightRay.getDirection().dot(nearest.surface.getNormalAt(nearest.position)));
		RGBA c = nearest.color;
		RGB  lc = light.getLightSource().getColor();

		pixels.position((y * w + x) * 4);
		pixels.put((byte)(f * c.r * lc.r * 255f));
		pixels.put((byte)(f * c.g * lc.g * 255f));
		pixels.put((byte)(f * c.b * lc.b * 255f));
		pixels.put((byte)(c.a * 255f));
	}

	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public int getHeight() {
		return h;
	}

	@Override
	public float getFrameRate() {
		return FRAMERATE_UNKNOWN;
	}

	@Override
	public long getLengthInFrames() {
		return FRAMECOUNT_UNKNOWN;
	}

	@Override
	public double getLengthInSeconds() {
		return LENGTH_INFINITE;
	}

	@Override
	protected void run(IRenderTarget<?> target) throws RenderCommandException {
		if(lights.isEmpty()) return;

		RGBA8Frame frame = new RGBA8Frame(w, h);

		final int   w     = frame.width;
		final int   h     = frame.height;

		final ILight light      = lights.get(0);
		final Vec3   camPos     = camera.getPosition();

		final float aspect      = (float)w / (float)h;

		final float planeWidth  = (float) (2 * Math.tan(camera.getFov() / 2) * camera.getNear());
		final float planeHeight = planeWidth / aspect;

		final float deltaX = planeWidth / w;
		final float deltaY = planeHeight / h;

		final Vec3    lookVector = camera.getTarget().subtract(camera.getPosition()).normalize();
		final Vec3    upVector   = camera.getUp().normalize();
		final Vec3    sideVector = lookVector.cross(upVector).normalize();

		frame.processLines((pixels, line)->{
			final int j = line - (h / 2);
			for (int i = -w / 2; i < w / 2; ++i) {
				final Vec3 x     = sideVector.scale(i * deltaX);
				final Vec3 y     = upVector.scale(j * deltaY);
				final Vec3 dir   = lookVector.add(x).add(y);
				final Line ray   = Line.fromRay(camPos, dir);
				intersection((i + w / 2), (j + h / 2), ray, light, pixels);
			}
		});
		try {
			((IVideoRenderTarget)target).setFrame(this, new VideoFrame(frame));
		} catch(Throwable t) {
			throw new RenderCommandException(t);
		}
	}

	public void setLights(List<ILight> lights) {
		this.lights = new ArrayList<>(lights);
	}

	public void addLight(ILight light) {
		lights.add(light);
	}

	public void removeLight(ILight light) {
		lights.remove(light);
	}	
}
