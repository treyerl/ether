package ch.fhnw.ether.examples.threed;

import ch.fhnw.ether.platform.Platform;
import ch.fhnw.ether.view.IWindow;
import ch.fhnw.util.math.Vec2;

public class LowLevelRenderingExperiments {
	public static void main(String[] args) {
		new LowLevelRenderingExperiments();
	}

	public LowLevelRenderingExperiments() {
		Platform.get().init();
		
		IWindow window = IWindow.create(new Vec2(500, 500), "Test Window", true);
		
		window.setVisible(true);
		
		Platform.get().run();
	}

}
