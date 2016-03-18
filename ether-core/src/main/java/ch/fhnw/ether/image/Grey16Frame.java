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

/**
 * 16 bit greyscale - LITTLE ENDIAN
 */

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

import ch.fhnw.util.BufferUtilities;

import com.jogamp.opengl.GL3;

public final class Grey16Frame extends Frame {
	public float[] originalMinMax = { 0.0f, 1.0f };

	boolean hasAlpha = false;

	protected Grey16Frame() {
		super(2);
	}

	public Grey16Frame(int width, int height) {
		super(2);
		init(width, height);
	}

	public Grey16Frame(int width, int height, ByteBuffer data) {
		super(width, height, data, 2);
	}

	public Grey16Frame(Frame frame) {
		this(frame.width, frame.height);
		if (pixelSize == frame.pixelSize)
			BufferUtilities.arraycopy(frame.pixels, 0, pixels, 0, pixels.capacity());
		else if (frame instanceof FloatFrame) {
			FloatBuffer src = ((FloatFrame) frame).buffer;
			final float min = ((FloatFrame) frame).getMinMax()[0];
			final float rng = ((FloatFrame) frame).getMinMax()[1] - min;

			final ByteBuffer dst = pixels;
			int spos = 0;
			dst.position(0);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					int val = (byte) ((65535f * (src.get(spos) - min)) / rng);
					// assume little endian
					dst.put((byte) val);
					dst.put((byte) (val >> 8));
					spos++;
				}
			}
		} else {
			final ByteBuffer src = frame.pixels;
			final ByteBuffer dst = pixels;
			int sps = frame.pixelSize;
			int spos = 0;
			dst.position(0);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					int val = src.get(spos) & 0xFF;
					val += src.get(spos + 1) & 0xFF;
					val += src.get(spos + 2) & 0xFF;
					val /= 3;
					dst.put((byte) val);
					dst.put((byte) val);
					spos += sps;
				}
			}
		}
		this.hasAlpha = frame.hasAlpha();
	}

	@Override
	public Grey16Frame create(int width, int height) {
		return new Grey16Frame(width, height);
	}

	@Override
	public void setRGB(int x, int y, byte[] rgb) {
		pixels.position((y * width + x) * pixelSize);
		final byte grey = (byte) (((rgb[0] & 0xFF) + (rgb[1] & 0xFF) + (rgb[2] & 0xFF)) / 3);
		pixels.put(grey);
		pixels.put(grey);
	}

	@Override
	public void setARGB(int x, int y, int argb) {
		pixels.position((y * width + x) * pixelSize);
		final byte grey = (byte) ((((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3);
		pixels.put(grey);
		pixels.put(grey);
	}

	@Override
	public BufferedImage toBufferedImage() {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
		final ByteBuffer src = pixels.asReadOnlyBuffer();
		src.clear();
		short[] line = new short[width];
		for (int j = height; --j >= 0;) {
			for (int i = 0; i < line.length; i++) {
				// assume little endian
				short tmp = (short) (src.get() & 0xFF);
				tmp |= (short) ((src.get() & 0xFF) << 8);
				line[i] = tmp;
			}
			result.getRaster().setDataElements(0, j, width, 1, line);
		}
		return result;
	}

	@Override
	public void getRGB(int x, int y, byte[] rgb) {
		byte val = pixels.get((y * width + x) * pixelSize + 1);

		rgb[0] = val;
		rgb[1] = val;
		rgb[2] = val;
	}

	@Override
	public float getFloatComponent(int x, int y, int unused) {
		byte hi = pixels.get((y * width + x) * pixelSize + 1);
		byte lo = pixels.get((y * width + x) * pixelSize);
		return ((hi << 8 | (lo & 0xFF)) & 0xFFFF) / 65535f;
	}

	@Override
	public float getBrightnessBilinear(double u, double v) {
		return getComponentBilinear(u, v, 0);
	}

	@Override
	public Frame copy() {
		Frame result = new Grey16Frame(this);
		return result;
	}

	@Override
	public Frame alloc() {
		return new Grey16Frame(width, height);
	}

	@Override
	public void setPixels(int x, int y, int w, int h, BufferedImage img, int flags) {
		setPixels_(x, y, w, h, img, flags, Double.NaN);
	}

	private void setPixels_(int x, int y, int w, int h, BufferedImage img, int flags, double noDataValue) {
		if (img.getType() == BufferedImage.TYPE_CUSTOM || img.getType() == BufferedImage.TYPE_BYTE_BINARY) {
			float[] minMax = Arrays.copyOf(originalMinMax, originalMinMax.length + 1);
			minMax[originalMinMax.length] = flags;
			img = ImageScaler.copy(img, new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_USHORT_GRAY), minMax);
			originalMinMax[0] = minMax[0];
			originalMinMax[1] = minMax[1];
		}
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
					final byte grey = (byte) (((src[srcyoff + i + 1] & 0xFF) + (src[srcyoff + i + 2] & 0xFF) + (src[srcyoff + i + 3] & 0xFF)) / 3);

					dst.put(grey);
					dst.put(grey);
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_INT_BGR:
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_INT_ARGB_PRE: {
			final int[] src = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < w; i++) {
					final int rgb = src[srcyoff + i];
					final byte grey = (byte) ((((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3);

					dst.put(grey);
					dst.put(grey);
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
					final byte grey = (byte) (((src[srcyoff + i + 0] & 0xFF) + (src[srcyoff + i + 1] & 0xFF) + (src[srcyoff + i + 2] & 0xFF)) / 3);

					dst.put(grey);
					dst.put(grey);
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
			for (int _h = h; _h > 0; _h--) {
				dst.position(dstyoff + x * pixelSize);
				for (int i = 0; i < w; i++) {
					final int rgb = cModel.getRGB(src[srcyoff + i] & 0xFF);
					final byte grey = (byte) ((((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3);

					dst.put(grey);
					dst.put(grey);
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_USHORT_555_RGB:
		case BufferedImage.TYPE_USHORT_565_RGB: {
			final short[] src = ((DataBufferUShort) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			for (; h > 0; h--) {
				dst.position(dstyoff + x * pixelSize);
				if (img.getType() == BufferedImage.TYPE_USHORT_555_RGB) {
					for (int i = 0; i < w; i++) {
						int val = src[srcyoff + i] & 0x7FFF;
						int grey = val & 0x1F;
						grey += (val >> 5) & 0x1F;
						grey += (val >> 10) & 0x1F;
						grey <<= 3;
						grey /= 3;
						dst.put((byte) grey);
						dst.put((byte) grey);
					}
				} else {
					for (int i = 0; i < w; i++) {
						int val = src[srcyoff + i] & 0x7FFF;
						int grey = val & 0x1F;
						grey += (val >> 6) & 0x1F;
						grey += (val >> 11) & 0x1F;
						grey <<= 3;
						grey /= 3;
						dst.put((byte) grey);
						dst.put((byte) grey);
					}
				}
				srcyoff += srcll;
				dstyoff -= dstll;
			}
			break;
		}
		case BufferedImage.TYPE_USHORT_GRAY: {
			final short[] src = ((DataBufferUShort) img.getRaster().getDataBuffer()).getData();
			final int srcll = img.getWidth();
			int srcyoff = srcll * y + x;
			if (originalMinMax[0] == 0 && originalMinMax[1] == 1) {
				int[] minMax = { 0xFFFFF, 0xFFFFF, 0, 0 };
				for (int _h = h; _h > 0; _h--) {
					for (int i = 0; i < w; i++) {
						final int v = src[srcyoff + i] & 0xFFFF;
						if (v < minMax[0])
							minMax[0] = v;
						if (v < minMax[1] && v > minMax[0])
							minMax[1] = v;

						if (v > minMax[3])
							minMax[3] = v;
						if (v > minMax[2] && v < minMax[3])
							minMax[2] = v;
					}
					srcyoff += srcll;
					dstyoff -= dstll;
				}

				int r = 0, alphaValue = 0xFFFF;

				if (!Double.isNaN(noDataValue)) {
					hasAlpha = true;
					alphaValue = (int) (noDataValue + 0.5);

					if (minMax[0] == alphaValue)
						minMax[0] = minMax[1];
					if (minMax[3] == alphaValue)
						minMax[3] = minMax[2];
				} else if ((flags & ImageScaler.DETECT_NO_DATA) != 0) {

					r = minMax[2] - minMax[1];

					float tol = 10;

					float topDelta = minMax[3] - minMax[2], bottomDelta = minMax[1] - minMax[0];

					if (topDelta > bottomDelta) {
						if (topDelta > tol) {
							hasAlpha = true;
							alphaValue = minMax[3];
							minMax[3] = minMax[2];
						}
					} else if (bottomDelta > tol) {
						hasAlpha = true;
						alphaValue = minMax[0];
						minMax[0] = minMax[1];
					}
				}

				r = minMax[3] - minMax[0];
				int min = minMax[0];

				dstyoff = dstll * ((height - 1) - y);
				srcyoff = srcll * y + x;

				if ((flags & ImageScaler.NORMALIZE) != 0) {
					for (int _h = h; _h > 0; _h--) {
						dst.position(dstyoff + x * pixelSize);
						for (int i = 0; i < w; i++) {
							int v_ = (src[srcyoff + i] & 0xFFFF), v;

							if (hasAlpha) {
								if (v_ == 0xFFFF && alphaValue != 0xFFFF)
									v_ = 0xFFFE;

								if (alphaValue == v_)
									v = 0xFFFF;
								else
									v = ((v_ - min) << 16) / r;
							} else
								v = ((v_ - min) << 16) / r;

							dst.put((byte) (v));
							dst.put((byte) (v >> 8));
						}
						srcyoff += srcll;
						dstyoff -= dstll;
					}
				} else {
					for (int _h = h; _h > 0; _h--) {
						dst.position(dstyoff + x * pixelSize);
						for (int i = 0; i < w; i++) {
							final short v = src[srcyoff + i];
							dst.put((byte) (v));
							dst.put((byte) (v >> 8));
						}
						srcyoff += srcll;
						dstyoff -= dstll;
					}
				}
				originalMinMax[0] = minMax[0];
				originalMinMax[1] = minMax[3];
			} else {
				for (; h > 0; h--) {
					dst.position(dstyoff + x * pixelSize);
					for (int i = 0; i < w; i++) {
						dst.put((byte) (src[srcyoff + i]));
						dst.put((byte) (src[srcyoff + i] >> 8));
					}
					srcyoff += srcll;
					dstyoff -= dstll;
				}
			}
			break;
		}
		default:
			throw new RuntimeException("Unsupported image type " + img.getType());
		}

		modified();
	}

	@Override
	public float getBrightness(int x, int y) {
		return getFloatComponent(x, y, 0);
	}

	@Override
	public void getRGBBilinear(double u, double v, byte[] rgb) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getARGB(int x, int y) {
		int rgb = pixels.get((y * width + x) * pixelSize) & 0xFF;
		return rgb << 16 | rgb << 8 | rgb | 0xFF000000;
	}

	@Override
	public Grey16Frame getSubframe(int x, int y, int width, int height) {
		Grey16Frame result = new Grey16Frame(width, height);
		getSubframeImpl(x, y, result);
		return result;
	}

	@Override
	public void setSubframe(int x, int y, Frame src) {
		if (src.getClass() != getClass())
			src = new Grey16Frame(src);
		setSubframeImpl(x, y, src);
	}

	@Override
	public boolean hasAlpha() {
		return hasAlpha;
	}

	@Override
	public float getAlphaComponent(int x, int y) {
		byte hi = pixels.get(((y * width) + x) * pixelSize + 1);
		byte lo = pixels.get(((y * width) + x) * pixelSize);
		return ((hi << 8 | (lo & 0xFF)) & 0xFFFF) == 0xFFFF ? 0 : 1;
	}

	public Frame normalizeSetAlpha(double alphaValue) {

		if (alphaValue > 2 << 16 || alphaValue < 0)
			alphaValue = Double.NaN;

		Grey16Frame out = new Grey16Frame(width, height);
		out.setPixels_(0, 0, width, height, toBufferedImage(), ImageScaler.NORMALIZE, alphaValue);

		return out;
	}

	@Override
	protected void loadTexture(GL3 gl) {
		gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_RED, width, height, 0, GL3.GL_RED, GL3.GL_UNSIGNED_SHORT, pixels);
	}
}
