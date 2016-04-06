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

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

import ch.fhnw.util.BufferUtilities;

final class ByteImage extends AbstractHostImage {
	
	ByteImage(int width, int height, ComponentFormat componentFormat, AlphaMode alphaMode) {
		super(width, height, ComponentType.BYTE, componentFormat, alphaMode);
	}

	ByteImage(int width, int height, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		super(width, height, ComponentType.BYTE, componentFormat, alphaMode, pixels);
	}
	
	@Override
	public IHostImage copy() {
		IHostImage image = allocate();
		BufferUtilities.arraycopy(getPixels(), 0, image.getPixels(), 0, getPixels().capacity());
		return image;
	}

	@Override
	public IHostImage allocate() {
		return new ByteImage(getWidth(), getHeight(), getComponentFormat(), getAlphaMode());
	}
	
	@Override
	public IHostImage allocate(int width, int height) {
		return new ByteImage(width, height, getComponentFormat(), getAlphaMode());
	}
	
	@Override
	public byte[] getPixel(int x, int y, byte[] dst) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			dst[i] = getPixels().get(pos + i);
		return dst;
	}

	@Override
	public void setPixel(int x, int y, byte[] src) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			getPixels().put(pos + i, src[i]);
	}

	@Override
	public float[] getPixel(int x, int y, float[] dst) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			dst[i] = getPixels().get(pos + i) / 255f;
		return dst;
	}

	@Override
	public void setPixel(int x, int y, float[] src) {
		int pos = pos(x, y);
		for (int i = 0; i < getComponentFormat().getNumComponents(); ++i)
			getPixels().put(pos + i, (byte)(src[i] * 255f));
	}

	@Override
	public byte getComponentByte(int x, int y, int component) {
		return getPixels().get(pos(x, y) + component);
	}

	@Override
	public void setComponentByte(int x, int y, int component, byte value) {
		getPixels().put(pos(x, y) + component, value);
	}

	@Override
	public float getComponentFloat(int x, int y, int component) {
		return getComponentByte(x, y, component) / 255f;
	}

	@Override
	public void setComponentFloat(int x, int y, int component, float value) {
		setComponentByte(x, y, component, (byte)(value * 255f));
	}

	@Override
	protected void loadTexture() {
		int internalFormat = 0;
		int format = 0;
		switch (getComponentFormat()) {
		case G:
			internalFormat = GL11.GL_RED;
			format = GL11.GL_RED;
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_R, GL11.GL_RED);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_G, GL11.GL_RED);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_B, GL11.GL_RED);
			break;
		case GA:
			internalFormat = GL30.GL_RG;
			format = GL30.GL_RG;
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_R, GL11.GL_RED);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_G, GL11.GL_RED);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_B, GL11.GL_RED);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL33.GL_TEXTURE_SWIZZLE_A, GL11.GL_GREEN);
			break;
		case RGB:
			internalFormat = GL11.GL_RGB;
			format = GL11.GL_RGB;
			break;
		case RGBA:
			internalFormat = GL11.GL_RGBA;
			format = GL11.GL_RGBA;
			break;
		}
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, getWidth(), getHeight(), 0, format, GL11.GL_UNSIGNED_BYTE, getPixels());
	}
}
