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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import ch.fhnw.util.BufferUtilities;

public class RGBA8Frame extends RGB8Frame {

	public RGBA8Frame(int width, int height) {
		super(width, height, 4);
	}

	public RGBA8Frame(int width, int height, ByteBuffer frameBuffer) {
		super(width, height, frameBuffer, 4);
	}

	public RGBA8Frame(int width, int height, byte[] frameBuffer) {
		super(width, height, frameBuffer, 4);
	}

	public RGBA8Frame(Frame frame) {
		super(frame.width, frame.height, 4);
		if (pixelSize == frame.pixelSize && !(frame instanceof FloatFrame))
			BufferUtilities.arraycopy(frame.pixels, 0, pixels, 0, pixels.capacity());
		else {
			final ByteBuffer dst = pixels;
			if (frame instanceof Grey16Frame) {
				final ByteBuffer src = frame.pixels;
				int sps = frame.pixelSize;
				int spos = 0;
				spos++; // assume little endian
				dst.position(0);
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						byte val = src.get(spos);
						dst.put(val);
						dst.put(val);
						dst.put(val);
						dst.put(B255);
						spos += sps;
					}
				}
			} else if (frame instanceof FloatFrame) {
				final FloatBuffer src = ((FloatFrame) frame).buffer;
				final float min = ((FloatFrame) frame).getMinMax()[0];
				final float rng = ((FloatFrame) frame).getMinMax()[1] - min;

				int spos = 0;
				dst.position(0);
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						float fVal = src.get(spos);
						if (Float.isNaN(fVal)) {
							dst.put(B0);
							dst.put(B0);
							dst.put(B0);
							dst.put(B0);
						} else {
							byte val = (byte) ((255f * (fVal - min)) / rng);
							dst.put(val);
							dst.put(val);
							dst.put(val);
							dst.put(B255);
						}
						spos++;
					}
				}
			} else {
				final ByteBuffer src = frame.pixels;
				int sps = frame.pixelSize;
				int spos = 0;
				dst.position(0);
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						dst.put(src.get(spos));
						dst.put(src.get(spos + 1));
						dst.put(src.get(spos + 2));
						dst.put(B255);
						spos += sps;
					}
				}
			}
		}
	}

	@Override
	public RGBA8Frame create(int width, int height) {
		return new RGBA8Frame(width, height);
	}

	public byte getAlpha(int x, int y) {
		return pixels.get(((y * width) + x) * pixelSize + 3);
	}

	public void setAlpha(int x, int y, int alpha) {
		pixels.put(((y * width) + x) * pixelSize + 3, (byte) alpha);
	}

	@Override
	public void setARGB(int x, int y, int argb) {
		pixels.position((y * width + x) * pixelSize);
		pixels.put((byte) (argb >> 16));
		pixels.put((byte) (argb >> 8));
		pixels.put((byte) (argb));
		pixels.put((byte) (argb >> 24));
	}

	public void setRGBA(int x, int y, int rgba) {
		pixels.position((y * width + x) * pixelSize);
		pixels.put((byte) (rgba >> 24));
		pixels.put((byte) (rgba >> 16));
		pixels.put((byte) (rgba >> 8));
		pixels.put((byte) (rgba));
	}

	@Override
	public final int getARGB(int x, int y) {
		int idx = (y * width + x) * pixelSize;
		int result = pixels.get(idx + 3) & 0xFF;
		result <<= 8;
		result |= pixels.get(idx + 0) & 0xFF;
		result <<= 8;
		result |= pixels.get(idx + 1) & 0xFF;
		result <<= 8;
		result |= pixels.get(idx + 2) & 0xFF;
		return result;
	}

	@Override
	public void setRGB(int x, int y, byte[] rgb) {
		pixels.position((y * width + x) * pixelSize);
		pixels.put(rgb[0]);
		pixels.put(rgb[1]);
		pixels.put(rgb[2]);
		if (rgb.length > 3)
			pixels.put(rgb[3]);
	}

	@Override
	public RGBA8Frame getSubframe(int x, int y, int width, int height) {
		RGBA8Frame result = new RGBA8Frame(width, height);
		getSubframeImpl(x, y, result);
		return result;
	}

	@Override
	public BufferedImage toBufferedImage() {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] data = new int[width];
		final ByteBuffer src = pixels.asReadOnlyBuffer();
		src.clear();
		for (int j = height; --j >= 0;) {
			for (int i = 0; i < data.length; i++) {
				int tmp = (src.get() & 0xFF) << 16;
				tmp |= (src.get() & 0xFF) << 8;
				tmp |= (src.get() & 0xFF);
				tmp |= (src.get() & 0xFF) << 24;
				data[i] = tmp;
			}
			result.setRGB(0, j, width, 1, data, 0, width);
		}
		return result;
	}

	@Override
	public Frame copy() {
		Frame result = new RGBA8Frame(this);
		return result;
	}

	@Override
	public Frame alloc() {
		return new RGBA8Frame(width, height);
	}

	@Override
	public void setPixels(int x, int y, int w, int h, BufferedImage img, int flags) {
		if (img.getType() == BufferedImage.TYPE_CUSTOM || img.getType() == BufferedImage.TYPE_BYTE_BINARY)
			img = ImageScaler.copy(img, new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB));

		final ByteBuffer dst = pixels;
		final int dstll = width * pixelSize;
		int dstyoff = dstll * ((height - 1) - y);
		switch (img.getType()) {
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_4BYTE_ABGR_PRE: {
			final byte[] src = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth() * 4;
			int srcyoff = srcll * y + x * 4;
			final int copylen = w * 4;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < copylen; i += 4) {
					dst.put(src[srcyoff + i + 3]);
					dst.put(src[srcyoff + i + 2]);
					dst.put(src[srcyoff + i + 1]);
					dst.put(src[srcyoff + i + 0]);
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_INT_BGR: {
			final int[] src = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < w; i++) {
					final int rgb = src[srcyoff + i];

					dst.put((byte) rgb);
					dst.put((byte) (rgb >> 8));
					dst.put((byte) (rgb >> 16));
					dst.put((byte) (rgb >> 24));
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE: {
			final int[] src = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < w; i++) {
					final int rgb = src[srcyoff + i];

					dst.put((byte) (rgb >> 16));
					dst.put((byte) (rgb >> 8));
					dst.put((byte) rgb);
					dst.put((byte) (rgb >> 24));
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_3BYTE_BGR: {
			final byte[] src = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth() * 3;
			int srcyoff = srcll * y + x * 3;
			final int copylen = w * 3;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < copylen; i += 3) {
					dst.put(src[srcyoff + i + 2]);
					dst.put(src[srcyoff + i + 1]);
					dst.put(src[srcyoff + i + 0]);
					dst.put(B255);
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_BYTE_GRAY: {
			final byte[] src = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < w; i++) {
					final byte grey = src[srcyoff + i];
					dst.put(grey);
					dst.put(grey);
					dst.put(grey);
					dst.put(B255);
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_BYTE_INDEXED: {
			final byte[] src = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
			final ColorModel cModel = img.getColorModel();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < w; i++) {
					final int rgb = cModel.getRGB(src[srcyoff + i] & 0xFF);

					dst.put((byte) (rgb >> 16));
					dst.put((byte) (rgb >> 8));
					dst.put((byte) rgb);
					dst.put((byte) (rgb >> 24));
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_USHORT_GRAY:
		case BufferedImage.TYPE_USHORT_555_RGB:
		case BufferedImage.TYPE_USHORT_565_RGB:
			super.setPixels(x, y, w, h, img, flags);
			break;
		default:
			throw new RuntimeException("Unsupported image type " + img.getType());
		}

		modified();
	}

	@Override
	public float getFloatComponent(int x, int y, int component) {
		return (pixels.get(((y * width) + x) * pixelSize + component) & 0xFF) / 255f;
	}

	@Override
	public boolean hasAlpha() {
		return true;
	}

	@Override
	public float getAlphaComponent(int x, int y) {
		return getFloatComponent(x, y, 3);
	}

	public void add(Frame src) {
		if (src instanceof RGBA8Frame) {
			final ByteBuffer srcfb = src.pixels;
			final ByteBuffer dstfb = pixels;
			for (int j = Math.min(src.height, height); --j >= 0;) {
				srcfb.position(src.width * j * src.pixelSize);
				dstfb.position(width * j * pixelSize);
				for (int i = Math.min(src.width, width); --i >= 0;) {
					int srcr = srcfb.get() & 0xFF;
					int srcg = srcfb.get() & 0xFF;
					int srcb = srcfb.get() & 0xFF;
					int srca = srcfb.get() & 0xFF;

					dstfb.put((byte) ((srca * srcr + (255 - srca) * (dstfb.get(dstfb.position()) & 0xFF)) >> 8));
					dstfb.put((byte) ((srca * srcg + (255 - srca) * (dstfb.get(dstfb.position()) & 0xFF)) >> 8));
					dstfb.put((byte) ((srca * srcb + (255 - srca) * (dstfb.get(dstfb.position()) & 0xFF)) >> 8));
					dstfb.put((byte) ((srca * srca + (255 - srca) * (dstfb.get(dstfb.position()) & 0xFF)) >> 8));
				}
			}
			modified();
		} else
			Frame.copyTo(src, this);
	}

	@Override
	public void setSubframe(int x, int y, Frame src) {
		if (src.getClass() != getClass())
			src = new RGBA8Frame(src);
		setSubframeImpl(x, y, src);
	}

	@Override
	protected void loadTexture() {
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
	}	
}
