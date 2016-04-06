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
import java.util.concurrent.atomic.AtomicReference;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.scene.mesh.material.Texture;
import ch.fhnw.ether.view.gl.GLContextManager;
import ch.fhnw.ether.view.gl.GLContextManager.IGLContext;
import ch.fhnw.util.BufferUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public abstract class AbstractHostImage implements IHostImage {
	private static final Log LOG = Log.create();

	private final int width;
	private final int height;
	private final ComponentType componentType;
	private final ComponentFormat componentFormat;
	private final AlphaMode alphaMode;
	private final int numBytesPerPixel;
	private final ByteBuffer pixels;
	private final AtomicReference<Texture> texture = new AtomicReference<>();
	
	protected AbstractHostImage(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode) {
		this(width, height, componentType, componentFormat, alphaMode, null);
	}

	protected AbstractHostImage(int width, int height, ComponentType componentType, ComponentFormat componentFormat, AlphaMode alphaMode, ByteBuffer pixels) {
		this.width = width;
		this.height = height;
		this.componentType = componentType;
		this.componentFormat = componentFormat;
		this.alphaMode = alphaMode;
		this.numBytesPerPixel = componentFormat.getNumComponents() * componentType.getSize();
		
		// XXX should we really copy the buffer here, or just check & assign?
		this.pixels = BufferUtils.createByteBuffer(width * height * numBytesPerPixel);
		if (pixels != null)
			this.pixels.put(pixels);
	}
	
	@Override
	public final void clear() {
		BufferUtilities.fill(pixels, 0, pixels.capacity(), (byte)0);
	}
	
	@Override
	public final int getWidth() {
		return width;
	}
	
	@Override
	public final int getHeight() {
		return height;
	}
	
	@Override
	public final ComponentType getComponentType() {
		return componentType;
	}
	
	@Override
	public final ComponentFormat getComponentFormat() {
		return componentFormat;
	}
	
	@Override
	public final AlphaMode getAlphaMode() {
		return alphaMode;
	}
	
	@Override
	public final IHostImage getSubImage(int x, int y, int width, int height) {
		IHostImage image = allocate(width, height);
		if (x + width > getWidth())
			throw new IllegalArgumentException("sub-image too wide");
		if (y + height > getHeight())
			throw new IllegalArgumentException("sub-image too high");
		int pixelSize = getNumBytesPerPixel();
		int srcRowSize = getWidth();
		int dstRowSize = width;
		for (int i = 0; i < height; ++i)
			BufferUtilities.arraycopy(getPixels(), ((y + i) * srcRowSize + x) * pixelSize, image.getPixels(), i * dstRowSize * pixelSize, dstRowSize * pixelSize);
		return image;
	}

	@Override
	public final void setSubImage(int x, int y, IHostImage image) {
		image = IHostImage.convert(image, getComponentType(), getComponentFormat(), getAlphaMode());
		if (x + image.getWidth() > getWidth())
			throw new IllegalArgumentException("sub-image too wide");
		if (y + image.getHeight() > getHeight())
			throw new IllegalArgumentException("sub-image too high");
		int pixelSize = getNumBytesPerPixel();
		int srcRowSize = image.getWidth();
		int dstRowSize = getWidth();
		for (int i = 0; i < image.getHeight(); ++i)
			BufferUtilities.arraycopy(image.getPixels(), i * srcRowSize * pixelSize, pixels, ((y + i) * dstRowSize + x) * pixelSize, srcRowSize * pixelSize);
	}

	@Override
	public final ByteBuffer getPixels() {
		return pixels;
	}
	
	@Override
	public final int getNumBytesPerPixel() {
		return numBytesPerPixel;
	}
	
	@Override
	public final synchronized Texture getTexture() {
		if (texture.get() == null) {
			try (IGLContext ctx = GLContextManager.acquireContext()) {
				Texture tex = new Texture(new GLObject(Type.TEXTURE), width, height);
				final int target = GL11.GL_TEXTURE_2D;
				GL11.glBindTexture(target, tex.getGlObject().getId());
				pixels.rewind();
				loadTexture();
				GL30.glGenerateMipmap(target);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
				GL11.glFinish();
				texture.set(tex);
			} catch (Throwable t) {
				LOG.warning(t);
			}
		}
		return texture.get();
	}	
	
	@Override
	public final int hashCode() {
		return width << 18 | height << 2 | getNumBytesPerPixel();
	}

	@Override
	public final boolean equals(Object obj) {
		if (obj instanceof IHostImage) {
			IHostImage other = (IHostImage) obj;
			getPixels().rewind();
			other.getPixels().rewind();
			return getWidth() == other.getWidth() && 
				getHeight() == other.getHeight() && 
				getComponentFormat() == other.getComponentFormat() && 
				getComponentType() == other.getComponentType() &&
				getAlphaMode() == other.getAlphaMode() &&
				getPixels().equals(other.getPixels());
		}
		return false;
	}

	@Override
	public String toString() {
		return TextUtilities.getShortClassName(this) + ": " + width + " x " + height + " " + getComponentType() + " " + getComponentFormat() + " " + getAlphaMode();
	}
	
	protected final int pos(int x, int y) {
		return (y * width + x) * numBytesPerPixel;		
	}
	
	protected final void modified() {
		texture.set(null);
	}
	
	protected abstract void loadTexture();
}
