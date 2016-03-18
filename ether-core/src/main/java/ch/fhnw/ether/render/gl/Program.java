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

package ch.fhnw.ether.render.gl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.render.shader.IShader;
import ch.fhnw.ether.render.shader.base.AbstractShader;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;

/**
 * GLSL shader program abstraction.
 *
 * @author radar
 */
// NOTE: currently we do not plan to dispose programs
public final class Program {
	public enum ShaderType {
		//@formatter:off
		VERTEX(GL3.GL_VERTEX_SHADER),
		TESS_CONTROL(GL4.GL_TESS_CONTROL_SHADER),
		TESS_EVAL(GL4.GL_TESS_EVALUATION_SHADER),
		GEOMETRY(GL3.GL_GEOMETRY_SHADER),
		FRAGMENT(GL3.GL_FRAGMENT_SHADER);
		//@formatter:on

		ShaderType(int glType) {
			this.glType = glType;
		}

		int getGLType() {
			return glType;
		}

		private final int glType;
	}

	private final static Class<?> LIBRARY_BASE = IShader.class;
	private final static String LIBRARY_PATH = "glsl/lib";

	private static final class Shader {
		static final Map<String, Shader> SHADERS = new HashMap<>();

		final Class<?> root;
		final String path;
		int shaderObject;

		Shader(GL3 gl, Class<?> root, String path, ShaderType type, PrintStream out) throws IOException {
			this.root = root;
			this.path = path;

			StringBuilder code = new StringBuilder();
			if(path.startsWith(AbstractShader.INLINE))
				code.append(path);
			else {
				URL url = root.getResource(path);
				if (url == null) {
					out.println("file not found: " + this);
					throw new FileNotFoundException("file not found: " + this);
				}
				new GLSLReader(LIBRARY_BASE, LIBRARY_PATH, url, code, out);
			}

			shaderObject = gl.glCreateShader(type.glType);

			gl.glShaderSource(shaderObject, 1, new String[] { code.toString() }, new int[] { code.length() }, 0);
			gl.glCompileShader(shaderObject);

			if (!checkStatus(gl, shaderObject, GL3.GL_COMPILE_STATUS, out)) {
				out.println("failed to compile shader: " + this);
				throw new IllegalArgumentException("failed to compile shader: " + this);
			}
		}

		@Override
		public String toString() {
			return root.getSimpleName() + ":" + path;
		}

		static Shader create(GL3 gl, Class<?> root, String path, ShaderType type, PrintStream out) throws IOException {
			String key = key(root, path);
			Shader shader = SHADERS.get(key);
			if (shader == null) {
				shader = new Shader(gl, root, path, type, out);
				SHADERS.put(key, shader);
			}
			return shader;
		}

		static String key(Class<?> root, String path) {
			return root.hashCode() + "/" + path;
		}
	}

	private static final Map<String, Program> PROGRAMS = new HashMap<>();

	private final String id;
	private final GLObject programObject;

	private Program(GL3 gl, PrintStream out, Shader... shaders) {
		programObject = new GLObject(gl, Type.PROGRAM);

		String id = "";
		for (Shader shader : shaders) {
			if (shader != null) {
				gl.glAttachShader(programObject.getId(), shader.shaderObject);
				id += shader.path + " ";
			}
		}
		this.id = id;

		gl.glLinkProgram(programObject.getId());
		if (!checkStatus(gl, programObject.getId(), GL3.GL_LINK_STATUS, out)) {
			out.println("failed to link program: " + this);
			throw new IllegalArgumentException("failed to link program: " + this);
		}

		gl.glValidateProgram(programObject.getId());
		if (!checkStatus(gl, programObject.getId(), GL3.GL_VALIDATE_STATUS, out)) {
			out.println("failed to validate program: " + this);
			throw new IllegalArgumentException("failed to validate program: " + this);
		}
	}

	public void enable(GL3 gl) {
		gl.glUseProgram(programObject.getId());
	}

	public void disable(GL3 gl) {
		gl.glUseProgram(0);
	}

	public void setUniform(GL3 gl, int index, boolean value) {
		if(index >= 0)
			gl.glUniform1i(index, value ? 1 : 0);
	}

	public void setUniform(GL3 gl, int index, int value) {
		if(index >= 0)
			gl.glUniform1i(index, value);
	}

	public void setUniform(GL3 gl, int index, float value) {
		if(index >= 0)
			gl.glUniform1f(index, value);
	}

	public void setUniformVec2(GL3 gl, int index, float[] value) {
		if(value != null && index >= 0)
			gl.glUniform2fv(index, 1, value, 0);
	}

	public void setUniformVec3(GL3 gl, int index, float[] value) {
		if (value != null && index >= 0)
			gl.glUniform3fv(index, 1, value, 0);
	}

	public void setUniformVec4(GL3 gl, int index, float[] value) {
		if (value != null && index >= 0)
			gl.glUniform4fv(index, 1, value, 0);
	}

	public void setUniformMat3(GL3 gl, int index, float[] value) {
		if (value != null && index >= 0)
			gl.glUniformMatrix3fv(index, 1, false, value, 0);
	}

	public void setUniformMat4(GL3 gl, int index, float[] value) {
		if (value != null && index >= 0)
			gl.glUniformMatrix4fv(index, 1, false, value, 0);
	}

	public void setUniformSampler(GL3 gl, int index, int unit) {
		if(index >= 0)
			setUniform(gl, index, unit);
	}

	public int getAttributeLocation(GL3 gl, String name) {
		return gl.glGetAttribLocation(programObject.getId(), name);
	}

	public int getUniformLocation(GL3 gl, String name) {
		return gl.glGetUniformLocation(programObject.getId(), name);
	}

	public int getUniformBlockIndex(GL3 gl, String name) {
		return gl.glGetUniformBlockIndex(programObject.getId(), name);
	}

	public void bindUniformBlock(GL3 gl, int index, int bindingPoint) {
		gl.glUniformBlockBinding(programObject.getId(), index, bindingPoint);
	}

	@Override
	public String toString() {
		return id;
	}

	public static Program create(GL3 gl, Class<?> root, String vertShader, String fragShader, String geomShader, PrintStream out) throws IOException {
		String key = key(root, vertShader, fragShader, geomShader);
		Program program = PROGRAMS.get(key);
		if (program == null) {
			Shader vert = Shader.create(gl, root, vertShader, ShaderType.VERTEX, out);
			Shader frag = Shader.create(gl, root, fragShader, ShaderType.FRAGMENT, out);
			Shader geom = null;
			if (geomShader != null && root.getResource(geomShader) != null) {
				geom = Shader.create(gl, root, geomShader, ShaderType.GEOMETRY, out);
			}
			program = new Program(gl, out, vert, frag, geom);
			PROGRAMS.put(key, program);
		}
		return program;
	}

	private static String key(Class<?> root, String... paths) {
		String key = "" + root.hashCode();
		for (String path : paths) {
			if (path != null)
				key += ":" + path;
		}
		return key;
	}

	private static boolean checkStatus(GL3 gl3, int object, int statusType, PrintStream out) {
		int[] status = { 0 };

		if (statusType == GL3.GL_COMPILE_STATUS)
			gl3.glGetShaderiv(object, statusType, status, 0);
		else if (statusType == GL3.GL_LINK_STATUS || statusType == GL3.GL_VALIDATE_STATUS)
			gl3.glGetProgramiv(object, statusType, status, 0);

		if (status[0] != 1) {
			gl3.glGetShaderiv(object, GL3.GL_INFO_LOG_LENGTH, status, 0);
			if(status[0] > 0) {
				byte[] infoLog = new byte[status[0]];
				if (statusType == GL3.GL_COMPILE_STATUS)
					gl3.glGetShaderInfoLog(object, status[0], status, 0, infoLog, 0);
				else if (statusType == GL3.GL_LINK_STATUS)
					gl3.glGetProgramInfoLog(object, status[0], status, 0, infoLog, 0);
				out.println(new String(infoLog));
			} else
				out.println("Unknonw (" + (statusType == GL3.GL_COMPILE_STATUS ? "GL_COMPILE_STATUS" : "GL_LINK_STATUS") + ")");
			return false;
		}
		return true;
	}
}
