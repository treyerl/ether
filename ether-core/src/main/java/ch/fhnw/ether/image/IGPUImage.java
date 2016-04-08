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

package ch.fhnw.ether.image;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import ch.fhnw.ether.platform.Platform;

public interface IGPUImage extends IImage {
	public static final IGPUImage TRANSPARENT_1x1 = create(1, 1, ComponentType.BYTE, ComponentFormat.RGBA, AlphaMode.POST_MULTIPLIED);
	
	long getGPUHandle();
	
	IHostImage createHostImage();
	
	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat) {
		return create(width, height, componentType, componentFormat, AlphaMode.POST_MULTIPLIED, null);
	}

	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, ByteBuffer pixels) {
		return create(width, height, componentType, componentFormat, AlphaMode.POST_MULTIPLIED, pixels);
	}

	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) {
		return create(width, height, componentType, componentFormat, alphaMode, null);
	}

	static IGPUImage create(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		return new GLGPUImage(width, height, componentType, componentFormat, alphaMode, pixels);
	}

	static IGPUImage read(InputStream in, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return Platform.get().getImageSupport().readGPU(in, componentType, componentFormat, alphaMode);
	}

	static IGPUImage read(InputStream in) throws IOException {
		return read(in, null, null, null);
	}
	
	static IGPUImage read(File file, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return read(new FileInputStream(file), componentType, componentFormat, alphaMode);
	}

	static IGPUImage read(File file) throws IOException {
		return read(new FileInputStream(file), null, null, null);
	}

	static IGPUImage read(URL url, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return read(url.openStream(), componentType, componentFormat, alphaMode);
	}

	static IGPUImage read(URL url) throws IOException {
		return read(url.openStream(), null, null, null);
	}
}
