package ch.fhnw.ether.examples.threed;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.ether.view.IWindow.IContext;
import ch.fhnw.util.math.Vec2;

public class LowLevelRenderingExperiments {
	private final IWindow window;
	
	
	public static void main(String[] args) {
		new LowLevelRenderingExperiments();
	}

	public LowLevelRenderingExperiments() {
		Platform.get().init();
		
		window = IWindow.create(new Vec2(500, 500), "Test Window", true);
		
		window.setVisible(true);
		
		Thread renderer = new Thread(this::renderer);
		renderer.setDaemon(true);
		renderer.start();
		
		Platform.get().run();
	}

	void renderer() {
		long nvgContext = 0;
		NVGColor color = null;
		
		while (!window.isDestroyed()) {
			try (IContext ctx = window.acquireContext()) {
				GL11.glClearColor(0, 0, 0, 1);
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
				Vec2 v = window.getFramebufferSize();

				GL11.glViewport(0, 0, (int)v.x, (int)v.y);
				
				if (nvgContext == 0) {
					nvgContext = NanoVGGL3.nvgCreateGL3(0);
					color = NVGColor.create();
					NanoVG.nvgRGBAf(1f, 0.8f, 0f, 1f, color);
				}
				NanoVG.nvgBeginFrame(nvgContext, (int)v.x, (int)v.y, 1);
				NanoVG.nvgBeginPath(nvgContext);
				NanoVG.nvgRect(nvgContext, 100, 100, 120, 30);
				NanoVG.nvgFillColor(nvgContext, color);
				NanoVG.nvgFill(nvgContext);
				NanoVG.nvgEndFrame(nvgContext);
				
			} catch (Exception e) {
				
			}
			window.swapBuffers();
		}
	}
}
