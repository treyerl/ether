package ch.fhnw.ether.scene.mesh;

import org.lwjgl.opengl.GL11;

import ch.fhnw.ether.render.gl.IArrayBuffer;

/**Allows for mutable mesh implementations presumably using glBufferSubData() 
 * to synchronize vertex data changes with the GPU.
 * 
 * @author treyerl
 *
 */
public interface IIncrementalMesh extends IMesh {
	/**Allows a mesh to override the drawing procedure
	 * @param mode GL11.GL_POINTS, GL11.GL_LINES etc corresponding to this meshes Primitive type
	 * @param numVertices int
	 * @param shader IShader that is being used to draw this mesh. Use it to set shader uniforms, e.g. transformation.
	 */
	
	default void glDrawArrays(int mode, int numVertices){
		GL11.glDrawArrays(mode, 0, numVertices);
	}
	
	
	/**Allows to implement custom array buffers for mutable meshes, e.g. how to upload data to GPU
	 * @return
	 */
	IArrayBuffer getArrayBuffer();
}
