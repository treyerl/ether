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

package ch.fhnw.ether.render;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Supplier;

import com.jogamp.opengl.GL3;

import ch.fhnw.ether.render.forward.ShadowVolumes;
import ch.fhnw.ether.scene.attribute.IAttribute;
import ch.fhnw.ether.scene.camera.IViewCameraState;
import ch.fhnw.ether.scene.mesh.IMesh;
import ch.fhnw.ether.scene.mesh.IMesh.Queue;
import ch.fhnw.ether.view.IView;
import ch.fhnw.ether.view.gl.GLContextManager;
import ch.fhnw.ether.view.gl.GLContextManager.IGLContext;

public abstract class AbstractRenderer implements IRenderer {
	
	private static final boolean DBG = false;

	public static final class RenderGlobals {
		public final Map<IAttribute, Supplier<?>> attributes = new IdentityHashMap<>();
		public final ViewInfo viewInfo = new ViewInfo();
		public final LightInfo lightInfo = new LightInfo();
		
		private RenderGlobals() {
			viewInfo.getAttributes(attributes);
			lightInfo.getAttributes(attributes);
		}
	}

	private static final int MAX_RENDER_QUEUE_SIZE = 3;

	private final Thread renderThread;
	private final BlockingQueue<Runnable> renderQueue = new ArrayBlockingQueue<>(MAX_RENDER_QUEUE_SIZE);

	protected final RenderGlobals globals = new RenderGlobals();

	private ShadowVolumes shadowVolumes;

	public AbstractRenderer() {
		this.renderThread = new Thread(this::runRenderThread, "renderthread");
		renderThread.start();
	}

	@Override
	public ExecutionPolicy getExecutionPolicy() {
		return ExecutionPolicy.DUAL_THREADED;
	}

	@Override
	public Renderable createRenderable(IMesh mesh) {
		return new Renderable(mesh, globals.attributes);
	}

	@Override
	public void submit(Supplier<IRenderState> supplier) {
		try {
			if (renderQueue.size() < MAX_RENDER_QUEUE_SIZE) {
				final IRenderState state = supplier.get();
				renderQueue.put(() -> render(state));
			} else {
				if (DBG)
					System.err.println("renderer: render queue full");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void render(IRenderState renderState) {
		// update renderables (only once for all views)
		// note that it's absolutely imperative that this is executed for
		// every render runnable created. otherwise scene-render state will
		// get out of sync resulting in ugly fails.
		try (IGLContext ctx = GLContextManager.acquireContext()) {
			renderState.getRenderUpdates().forEach(update -> update.update(ctx.getGL()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// render all views
		renderState.getRenderStates().forEach(targetState -> {
			IView view = targetState.getView();
			IViewCameraState vcs = targetState.getViewCameraState();
			targetState.getView().getWindow().display(drawable -> {
                try {
                	GL3 gl = drawable.getGL().getGL3();
                    render(gl, targetState, view, vcs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
				return true;
			});
		});
	}

	private void render(GL3 gl, IRenderTargetState renderState, IView view, IViewCameraState vcs) {
		try {
			// XXX: make sure we only render on render thread (e.g. jogl
			// will do repaints on other threads when resizing windows...)
			if (!isRenderThread()) {
				return;
			}

			// gl = new TraceGL3(gl, System.out);
			// gl = new DebugGL3(gl);

			// FIXME: currently we clear in DefaultView.display() ... needs
			// to move
			// gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT |
			// GL.GL_STENCIL_BUFFER_BIT);

			if (!view.isEnabled())
				return;

			// update views and lights
			globals.viewInfo.update(gl, vcs);
			globals.lightInfo.update(gl, vcs, renderState.getLights());

			// render everything
			render(gl, renderState);

			int error = gl.glGetError();
			if (error != 0)
				System.err.println("renderer returned with exisiting GL error 0x" + Integer.toHexString(error));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected abstract void render(GL3 gl, IRenderTargetState state);
	
	protected void renderObjects(GL3 gl, IRenderTargetState state, Queue pass) {
		for (Renderable renderable : state.getRenderables()) {
			if (renderable.getQueue() == pass) {
				renderable.render(gl);
			}
		}
	}

	protected void renderShadowVolumes(GL3 gl, IRenderTargetState state, Queue pass) {
		if (shadowVolumes == null) {
			shadowVolumes = new ShadowVolumes(globals.attributes);
		}
		shadowVolumes.render(gl, pass, state.getRenderables(), globals.lightInfo.getNumLights());
	}
	
	private void runRenderThread() {
		while (true) {
			try {
				renderQueue.take().run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean isRenderThread() {
		return Thread.currentThread().equals(renderThread);
	}
}
