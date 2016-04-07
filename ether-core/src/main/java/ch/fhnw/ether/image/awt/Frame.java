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

package ch.fhnw.ether.image.awt;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import ch.fhnw.ether.image.ILineProcessor;
import ch.fhnw.ether.render.gl.GLObject;
import ch.fhnw.ether.render.gl.Texture;
import ch.fhnw.ether.render.gl.GLObject.Type;
import ch.fhnw.ether.view.gl.GLContextManager;
import ch.fhnw.ether.view.gl.GLContextManager.IGLContext;
import ch.fhnw.util.BufferUtilities;
import ch.fhnw.util.Log;
import ch.fhnw.util.TextUtilities;

public abstract class Frame {
	private static final Log LOG = Log.create();

	public enum FileFormat {PNG,JPEG}

	public static final byte        B0    = 0;
	public static final byte        B255  = (byte) 255;

	private static final ByteBuffer EMPTY = BufferUtils.createByteBuffer(0);

	public ByteBuffer pixels = EMPTY;
	public int        width;
	public int        height;
	public int        pixelSize;
	private int       modCount;
	private Texture   texture;

	protected Frame(int pixelSize) {
		this.pixelSize = pixelSize;
	}

	protected Frame(int width, int height, byte[] frameBuffer, int pixelSize) {
		this.pixels = BufferUtils.createByteBuffer(frameBuffer.length);
		this.pixels.put(frameBuffer);
		this.pixelSize = pixelSize;
		init(width, height);
	}

	protected Frame(int width, int height, ByteBuffer frameBuffer, int pixelSize) {
		if (frameBuffer.isDirect()) {
			this.pixels = frameBuffer;
		} else {
			this.pixels = BufferUtils.createByteBuffer(frameBuffer.capacity());
			this.pixels.put(frameBuffer);
		}
		this.pixelSize = pixelSize;
		init(width, height);
	}

	@Override
	public int hashCode() {
		return width << 18 | height << 2 | pixelSize;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Frame) {
			Frame other = (Frame) obj;
			pixels.rewind();
			other.pixels.rewind();
			return width == other.width && height == other.height && pixelSize == other.pixelSize && pixels.equals(other.pixels);
		}
		return false;
	}

	@Override
	public String toString() {
		return TextUtilities.getShortClassName(this) + ":" + width + "x" + height;
	}

	protected void init(int width, int height) {
		if(width < 1 || height < 1) throw new IllegalArgumentException("width or height < 1");
		this.width  = width;
		this.height = height;
		int bufsize = width * height * pixelSize;
		if (this.pixels.capacity() < bufsize)
			this.pixels = BufferUtils.createByteBuffer(bufsize);
	}

	public abstract Frame create(int width, int height);

	public static Frame create(Texture texture) {
		try(IGLContext ctx = GLContextManager.acquireContext()) {
			Frame result = null;
			final int target = GL11.GL_TEXTURE_2D;
			GL11.glBindTexture(target, texture.getGlObject().getId());
			int internalFormat = GL11.glGetTexLevelParameteri(target, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
			switch(internalFormat) {
			case GL11.GL_RGB8:
				internalFormat = GL11.GL_RGB;
				/* fall-thru */
			case GL11.GL_RGB:
				result = new RGB8Frame(texture.getWidth(), texture.getHeight());
				break;
			case GL11.GL_RGBA8:
				internalFormat = GL11.GL_RGBA;
				/* fall-thru */
			case GL11.GL_RGBA:
				result = new RGBA8Frame(texture.getWidth(), texture.getHeight());
				break;
			default:
				throw new IllegalArgumentException("Unsupported format:" + internalFormat);
			}
			result.pixels.clear();
			GL11.glGetTexImage(target, 0, internalFormat, GL11.GL_UNSIGNED_BYTE, result.pixels);
			GL11.glBindTexture(target, 0);
			return result;
		} catch(Throwable t) {
			LOG.severe(t);
			return null;
		}
	}

	public final void setPixels(int x, int y, int width, int height, BufferedImage img) {
		setPixels(x, y, width, height, img, 0);
	}

	public abstract void setPixels(int x, int y, int width, int height, BufferedImage img, int flags);

	public abstract Frame getSubframe(int x, int y, int width, int height);

	public abstract void setSubframe(int x, int y, Frame frame);

	protected void getSubframeImpl(int x, int y, Frame dst) {
		if (x + dst.width > width)
			throw new IllegalArgumentException("i(" + x + ")+dst.w(" + dst.width + ") > w(" + width + ")");
		if (y + dst.height > height)
			throw new IllegalArgumentException("j(" + y + ")+dst.h(" + dst.height + ") > h(" + height + ")");
		int slnsize = width;
		int dlnsize = dst.width;
		for (int jj = 0; jj < dst.height; jj++) {
			BufferUtilities.arraycopy(pixels, ((y + jj) * slnsize + x) * pixelSize, dst.pixels, jj * dlnsize * pixelSize, dlnsize * pixelSize);
		}
		modified();
	}

	protected void setSubframeImpl(int x, int y, Frame src) {
		if (x + src.width > width)
			throw new IllegalArgumentException("i(" + x + ")+src.w(" + src.width + ") > w(" + width + ")");
		if (y + src.height > height)
			throw new IllegalArgumentException("j(" + y + ")+src.h(" + src.height + ") > h(" + height + ")");
		int slnsize = src.width;
		int dlnsize = width;
		for (int jj = 0; jj < src.height; jj++)
			BufferUtilities.arraycopy(src.pixels, jj * slnsize * pixelSize, pixels, ((y + jj) * dlnsize + x) * pixelSize, slnsize * pixelSize);
		modified();
	}	

	public static Frame copyTo(Frame src, Frame dst) {
		if (src.getClass() == dst.getClass()) {
			for (int j = Math.min(src.height, dst.height); --j >= 0;)
				BufferUtilities.arraycopy(src.pixels, (j * src.width) * src.pixelSize, dst.pixels, (j * dst.width) * dst.pixelSize, Math.min(src.width, dst.width)
						* src.pixelSize);
		} else {
			for (int j = Math.min(src.height, dst.height); --j >= 0;)
				for (int i = Math.min(src.width, dst.width); --i >= 0;)
					dst.setARGB(i, j, src.getARGB(i, j));
		}
		dst.modified();
		return dst;
	}

	public abstract BufferedImage toBufferedImage();

	public void modified() {
		modCount++;
		texture = null;
	}

	public int getModCount() {
		return modCount;
	}

	public abstract Frame copy();

	public abstract Frame alloc();

	public void getRGBUnsigned(int x, int y, int[] rgb) {
		int irgb = getARGB(x, y);
		rgb[0] = (irgb >> 16) & 0xFF;
		rgb[1] = (irgb >> 8) & 0xFF;
		rgb[2] = irgb & 0xFF;
	}

	public void getRGB(int x, int y, byte[] rgb) {
		int irgb = getARGB(x, y);
		rgb[0] = (byte) (irgb >> 16);
		rgb[1] = (byte) (irgb >> 8);
		rgb[2] = (byte) irgb;
	}

	public void setRGB(int x, int y, byte[] rgb) {
		int argb = (rgb[0] & 0xFF) << 16;
		argb |= (rgb[1] & 0xFF) << 8;
		argb |= (rgb[2] & 0xFF);
		argb |= 0xFF000000;
		setARGB(x, y, argb);
	}

	public final float getComponentBilinear(double u, double v, int component) {
		// bilinear interpolation
		final int width_ = width - 1;
		final int height_ = height - 1;

		int i0 = (int) (u * width_);
		int j0 = (int) (v * height_);

		if (i0 < 0)
			i0 = 0;
		else if (i0 > width_)
			i0 = width_;
		if (j0 < 0)
			j0 = 0;
		else if (j0 > height_)
			j0 = height_;

		int i1 = i0 + 1;
		int j1 = j0 + 1;

		if (i1 < 0)
			i1 = 0;
		else if (i1 > width_)
			i1 = width_;
		if (j1 < 0)
			j1 = 0;
		else if (j1 > height_)
			j1 = height_;

		// interpolate
		final double w = (u - i0 / (double) width_) * width_;
		final double h = (v - j0 / (double) height_) * height_;

		float c00 = getFloatComponent(i0, j0, component);
		float c01 = getFloatComponent(i0, j1, component);
		float c10 = getFloatComponent(i1, j0, component);
		float c11 = getFloatComponent(i1, j1, component);

		float c = (float) (h * ((1 - w) * c01 + w * c11) + (1 - h) * ((1 - w) * c00 + w * c10));

		return c;
	}

	public boolean hasAlpha() {
		return false;
	}

	public float getAlphaBilinear(double u, double v) {
		// bilinear interpolation
		final int width_  = width - 1;
		final int height_ = height - 1;

		int i0 = (int) (u * width_);
		int j0 = (int) (v * height_);

		if (i0 < 0)
			i0 = 0;
		else if (i0 > width_)
			i0 = width_;
		if (j0 < 0)
			j0 = 0;
		else if (j0 > height_)
			j0 = height_;

		int i1 = i0 + 1;
		int j1 = j0 + 1;

		if (i1 < 0)
			i1 = 0;
		else if (i1 > width_)
			i1 = width_;
		if (j1 < 0)
			j1 = 0;
		else if (j1 > height_)
			j1 = height_;

		// interpolate
		final double w = (u - i0 / (double) width_) * width_;
		final double h = (v - j0 / (double) height_) * height_;

		float c00 = getAlphaComponent(i0, j0);
		float c01 = getAlphaComponent(i0, j1);
		float c10 = getAlphaComponent(i1, j0);
		float c11 = getAlphaComponent(i1, j1);

		float c = (float) (h * ((1 - w) * c01 + w * c11) + (1 - h) * ((1 - w) * c00 + w * c10));

		return c;
	}

	public float getAlphaComponent(int x, int y) {
		throw new Error("Alpha not defined");
	}

	public void clear() {
		BufferUtilities.fill(pixels, 0, pixels.capacity(), B0);
	}

	public abstract float getBrightness(int x, int y);

	public abstract float getBrightnessBilinear(double u, double v);

	public abstract void getRGBBilinear(double u, double v, byte[] rgb);

	public abstract float getFloatComponent(int x, int y, int component);

	public abstract void setARGB(int x, int y, int argb);

	public abstract int getARGB(int x, int y);

	protected static float linearInterpolate(float low, float high, float weight) {
		return low + ((high - low) * weight);
	}

	protected abstract void loadTexture();

	static final ExecutorService POOL       = Executors.newCachedThreadPool();
	static final int             NUM_CHUNKS = Runtime.getRuntime().availableProcessors(); 

	final static class Chunk implements Runnable {
		private final int            from;
		private final int            to;
		private final ByteBuffer     pixels;
		private final ILineProcessor processor;
		private final int            lineLength;

		Chunk(Frame frame, int from, int to, ILineProcessor processor) {
			this.from       = from;
			this.to         = to;
			this.pixels     = frame.pixels.duplicate();
			this.processor  = processor;
			this.lineLength = frame.width * frame.pixelSize;
		}

		@Override
		public void run() {
			for(int j = from; j < to; j++) {
				pixels.position(j * lineLength);
				processor.process(pixels, j);
			}
		}
	}

	public final void processLines(ILineProcessor processor) {
		List<Future<?>> result = new ArrayList<>(NUM_CHUNKS + 1);
		int inc  = Math.max(32, height / NUM_CHUNKS);
		for(int from = 0; from < height; from += inc)
			result.add(POOL.submit(new Chunk(this, from, Math.min(from + inc, height), processor)));
		try {
			for(Future<?> f : result)
				f.get();
		} catch(Throwable t) {
			t.printStackTrace();
		}
		modified();
	}

	public final void position(ByteBuffer pixels, int x, int y) {
		pixels.position((y * width + x) * pixelSize);
	}

	public synchronized Texture getTexture() {
		if(texture == null) {
			try(IGLContext ctx = GLContextManager.acquireContext()) {
				texture = new Texture(new GLObject(Type.TEXTURE), width, height);
				final int target = GL11.GL_TEXTURE_2D;
				GL11.glBindTexture(target, texture.getGlObject().getId());
				pixels.clear();
				loadTexture();
				GL30.glGenerateMipmap(target);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(target, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
				GL11.glFinish();
			} catch(Throwable t) {
				LOG.warning(t);
			}
		}
		return texture;
	}
}
