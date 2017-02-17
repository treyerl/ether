package ch.fhnw.ether.scene.mesh;

import ch.fhnw.ether.render.gl.FloatArrayBuffer;

/**Allows for mutable mesh implementations presumably using glMapBuffer() or glBufferSubData() 
 * to synchronize vertex data changes with the GPU. Use FloatArrayBuffer's load() method to
 * implement GL calls.
 * 
 * @author treyerl
 *
 */
public interface IMutableMesh extends IMesh {
	float[][] getUpdatedTransformedGeometryData();
	FloatArrayBuffer getFloatArrayBuffer();
	void clearUpdates();
}
