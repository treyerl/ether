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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;

import ch.fhnw.ether.image.IImage.AlphaMode;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;

public class STBImageSupport implements IImageSupport {

	@Override
	public IHostImage readHost(InputStream in, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return (IHostImage)read(in, componentType, componentFormat, alphaMode, true);
	}
	
	@Override
	public IGPUImage readGPU(InputStream in, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) throws IOException {
		return (IGPUImage)read(in, componentType, componentFormat, alphaMode, false);
	}
	
	public IImage read(InputStream in, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, boolean host) throws IOException {
		// TODO: HDR support (stb read as float)
		// TODO: Pre-multiplied alpha support
		int numComponentsRequested = componentFormat != null ? componentFormat.getNumComponents() : 0;
		byte[] bytes = getBytesFromInputStream(in);
		ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
		buffer.put(bytes).flip();
		
		IntBuffer width = BufferUtils.createIntBuffer(1);
		IntBuffer height = BufferUtils.createIntBuffer(1);
		IntBuffer numComponents = BufferUtils.createIntBuffer(1);
		
		STBImage.stbi_set_flip_vertically_on_load(1);
		ByteBuffer pixels = STBImage.stbi_load_from_memory(buffer, width, height, numComponents, numComponentsRequested);
		if (pixels == null)
			throw new IOException("can't load image: " + STBImage.stbi_failure_reason());
		
		if (componentType == null)
			componentType = ComponentType.BYTE;
		else if (componentType == ComponentType.FLOAT)
			throw new UnsupportedOperationException("float types unsupported");
		
		if (componentFormat == null)
			componentFormat = ComponentFormat.get(numComponents.get(0));
		
		if (alphaMode == null)
			alphaMode = AlphaMode.POST_MULTIPLIED;
		else if (alphaMode != AlphaMode.POST_MULTIPLIED)
			throw new UnsupportedOperationException("premultiplied alpha unsupported");

		IImage image = null;
		
		if (host) {
			// note: we need to create a copy here in order to explicity release the STB-managed buffer.
			ByteBuffer copy = BufferUtils.createByteBuffer(pixels.capacity());
			pixels.rewind();
			copy.put(pixels);
			image = IHostImage.create(width.get(0), height.get(0), componentType, componentFormat, alphaMode, copy);
		} else {
			// no copy required for gpu-fast path
			image = IGPUImage.create(width.get(0), height.get(0), componentType, componentFormat, alphaMode, pixels);
		}

		pixels.rewind();
		STBImage.stbi_image_free(pixels);
		
		return image;
	}
	
	@Override
	public void write(IImage frame, OutputStream out, FileFormat format) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public IHostImage resize(IHostImage image, int width, int height) {
		if (image.getWidth() == width && image.getHeight() == height)
			return image;
		
		ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * image.getNumBytesPerPixel());
		
		switch (image.getComponentType()) {
		case BYTE:
			STBImageResize.stbir_resize_float(image.getPixels(), image.getWidth(), image.getHeight(), 0, pixels, width, height, 0, image.getComponentFormat().getNumComponents());
			break;
		case FLOAT:
			STBImageResize.stbir_resize_uint8(image.getPixels(), image.getWidth(), image.getHeight(), 0, pixels, width, height, 0, image.getComponentFormat().getNumComponents());
			break;
		}
		return IHostImage.create(width, height, image.getComponentType(), image.getComponentFormat(), image.getAlphaMode(), pixels);
	}
	
	private static byte[] getBytesFromInputStream(InputStream in) throws IOException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[0xffff];
			for (int len; (len = in.read(buffer)) != -1;)
				os.write(buffer, 0, len);
			os.flush();
			return os.toByteArray();
		}
	}
}
