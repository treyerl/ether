package ch.fhnw.ether.examples.threed;

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
		for (;;) {
			try (IContext ctx = window.acquireContext()) {
				GL11.glClearColor(1, 0, 0, 1);
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			} catch (Exception e) {
				
			}
			window.swapBuffers();
		}
	}
}
