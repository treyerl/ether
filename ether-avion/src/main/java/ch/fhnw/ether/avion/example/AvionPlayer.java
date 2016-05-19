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

package ch.fhnw.ether.avion.example;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import ch.fhnw.ether.avion.AVDecoder;
import ch.fhnw.ether.avion.Avion;

public final class AvionPlayer {
	public static void mainXXX(String[] args) {
		try {
			System.out.println("avion test flight: " + Avion.isReady());

			int audioSize = 1024;

			AVDecoder decoder = Avion.createDecoder(
					new URL("file:///Users/radar/Desktop/simian_mobile_disco-audacity_of_huge_(2009).mp4"), true, true,
					audioSize, false, 44100);

			// wrapper->seek(60);

			int size = decoder.getVideoWidth() * decoder.getVideoHeight() * 4;
			ByteBuffer image = ByteBuffer.allocateDirect(size);

			FloatBuffer samples = ByteBuffer.allocateDirect(4 * audioSize).asFloatBuffer();

			int error = 0;
			double[] pts = new double[1];
			while (error != -1) {
				error = decoder.decodeVideo(image, pts);
				System.out.println("got video frame " + pts + " " + error + " " + pts[0]);

				error = decoder.decodeAudio(samples, pts);
				System.out.println("got audio frame " + pts + " " + error + " " + pts[0]);
			}

			decoder.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static final String[] VERTEX_SHADER = {
		"#version 330\n",
		"\n",
		"in vec2 position;\n",
		"in vec2 texCoord;\n",
		"\n",
		"out vec2 vsTexCoord;\n",
		"\n",
		"void main(void) {\n",
		"  vsTexCoord = texCoord;\n",
		"  gl_Position = vec4(position.x, position.y, 0.0, 1.0);\n",
		"}\n"
	};

	private static final String[] FRAGMENT_SHADER = {
		"#version 330\n",
		"\n",
		"uniform sampler2D tex;\n",
		"\n",
		"in vec2 vsTexCoord;\n",
		"\n",
		"out vec4 color;\n",
		"\n",
		"void main(void) {\n",
		"  vec2 x = vsTexCoord;",
		"  color = vec4(1, 0, 0, 0); //texture(tex, vsTexCoord);\n",
		"}\n"
	};
	
	private static final float[] TRIANGLES = {
		-1, -1,		0, 0,
		1, -1, 		1, 0,
		1, 1,		1, 1,
		-1, -1,		0, 0,
		1, 1,		1, 1,
		-1, 1,		0, 1		
	};
	
	long window;
	int width = 1024;
	int height = 768;
	GLFWErrorCallback errCallback;
	GLFWFramebufferSizeCallback fbCallback;

	int program;
	int positionAttrib;
	int texCoordAttrib;
	int textureUniform;
	int vao;
	int vbo;

	void init() throws IOException {
		GLFW.glfwSetErrorCallback(errCallback = new GLFWErrorCallback() {
			GLFWErrorCallback delegate = GLFWErrorCallback.createPrint(System.err);

			@Override
			public void invoke(int error, long description) {
				if (error == GLFW.GLFW_VERSION_UNAVAILABLE)
					System.err.println("This demo requires OpenGL 3.3 or higher.");
				delegate.invoke(error, description);
			}

			@Override
			public void free() {
				delegate.free();
			}
		});

		if (!GLFW.glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

		window = GLFW.glfwCreateWindow(32, 32, "Avion Video Decoding Demo", 0, 0);
		if (window == 0)
			throw new RuntimeException("Failed to create the GLFW window");

		GLFW.glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				AvionPlayer.this.width = width;
				AvionPlayer.this.height = height;
			}
		});
		
		GLFW.glfwSetWindowSize(window, width, height);

		GLFW.glfwMakeContextCurrent(window);
		GLFW.glfwSwapInterval(0);
		GLFW.glfwShowWindow(window);
		GL.createCapabilities();

		GL11.glClearColor(0, 0, 0, 1);

		// Create all needed GL resources
		createProgram();
		createBuffers();
	}

	void createProgram() throws IOException {
		int program = GL20.glCreateProgram();
		int vshader = createShader(GL20.GL_VERTEX_SHADER, VERTEX_SHADER);
		int fshader = createShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
		GL20.glAttachShader(program, vshader);
		GL20.glAttachShader(program, fshader);
		GL20.glLinkProgram(program);
		int linked = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
		String programLog = GL20.glGetProgramInfoLog(program);
		if (programLog.trim().length() > 0)
			System.err.println(programLog);
		if (linked == 0)
			throw new AssertionError("Could not link program");
		this.program = program;
		GL20.glUseProgram(program);
		
		positionAttrib = GL20.glGetAttribLocation(program, "position");
		texCoordAttrib = GL20.glGetAttribLocation(program, "texCoord");
		textureUniform = GL20.glGetUniformLocation(program, "tex");
		GL20.glUseProgram(0);
	}
	
	void createBuffers() {
		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);

		vbo = GL15.glGenBuffers();

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, TRIANGLES, GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(positionAttrib, 2, GL11.GL_FLOAT, false, 4 * 4, 0);
		GL20.glEnableVertexAttribArray(positionAttrib);
		GL20.glVertexAttribPointer(texCoordAttrib, 2, GL11.GL_FLOAT, false, 4 * 4, 2 * 4);
		GL20.glEnableVertexAttribArray(texCoordAttrib);

		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
	}

	void render() {
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		
		GL20.glUseProgram(program);
		GL30.glBindVertexArray(vao);
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
		GL30.glBindVertexArray(0);
		//GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
		GL20.glUseProgram(0);
		
		int error = GL11.glGetError();
		if (error != 0)
			System.err.println("got gl error: " + error);
	}

	void loop() {
		while (!GLFW.glfwWindowShouldClose(window)) {
			GLFW.glfwPollEvents();
			GL11.glViewport(0, 0, width, height);
			render();
			GLFW.glfwSwapBuffers(window);
		}
	}

	void run() {
		try {
			init();
			loop();

			errCallback.free();
			fbCallback.free();
			GLFW.glfwDestroyWindow(window);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			GLFW.glfwTerminate();
		}
	}

	public static void main(String[] args) {
		new AvionPlayer().run();
	}

	static int createShader(int type, String[] lines) throws IOException {
		int shader = GL20.glCreateShader(type);
		GL20.glShaderSource(shader, lines);
		GL20.glCompileShader(shader);
		int compiled = GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
		String shaderLog = GL20.glGetShaderInfoLog(shader);
		if (shaderLog.trim().length() > 0)
			System.err.println(shaderLog);
		if (compiled == 0)
			throw new RuntimeException("Could not compile shader");
		return shader;
	}
}
