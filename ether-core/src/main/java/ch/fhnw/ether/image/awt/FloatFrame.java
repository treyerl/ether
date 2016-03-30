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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;

import ch.fhnw.util.BufferUtilities;

public final class FloatFrame extends Frame {
	private static final float[] MIN_MAX_0_1 = { 0f, 1f };
	public float[] originalMinMax = MIN_MAX_0_1;
	FloatBuffer buffer;

	protected FloatFrame() {
		super(4);
		buffer = pixels.asFloatBuffer();
	}

	public FloatFrame(Frame frame) {
		this(frame, MIN_MAX_0_1);
	}

	public FloatFrame(FloatFrame frame) {
		this(frame, frame.originalMinMax);
	}

	public FloatFrame(int width, int height) {
		super(4);
		buffer = pixels.asFloatBuffer();
		init(width, height);
	}

	public FloatFrame(int width, int height, ByteBuffer frameBuffer) {
		super(width, height, frameBuffer, 4);
		pixels.rewind();
		buffer = pixels.asFloatBuffer();
	}

	@Override
	public FloatFrame create(int width, int height) {
		return new FloatFrame(width, height);
	}
	
	public FloatFrame(Frame frame, float[] minMax) {
		this(frame.width, frame.height);
		if (pixelSize == frame.pixelSize && frame instanceof FloatFrame)
			BufferUtilities.arraycopy(frame.pixels, 0, pixels, 0, pixels.capacity());
		else {
			Grey16Codec encoder = new Grey16Codec(minMax);
			final ByteBuffer src = frame.pixels;
			int sps = frame.pixelSize;
			if (frame instanceof Grey16Frame) {
				int spos = 0;
				int dpos = 0;
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						buffer.put(dpos++, encoder.decode(src.get(spos + 1), src.get(spos)));
						spos += sps;
					}
				}
			} else {
				int spos = 0;
				int dpos = 0;
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						int val = src.get(spos) & 0xFF;
						val += src.get(spos) & 0xFF;
						val += src.get(spos) & 0xFF;
						val /= 3;
						buffer.put(dpos++, encoder.decode(val, val));
						spos += sps;
					}
				}
			}
		}
		originalMinMax = minMax;
	}

	@Override
	protected void init(int width, int height) {
		super.init(width, height);
		buffer = pixels.asFloatBuffer();
	}

	@Override
	public void setPixels(int x, int y, int w, int h, BufferedImage img, int flags) {
		final int dstll = width;
		int dstyoff = dstll * ((height - 1) - y);

		if (img.getType() != BufferedImage.TYPE_USHORT_GRAY) {
			BufferedImage tmp = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
			Graphics g = tmp.getGraphics();
			g.drawImage(img, x, y, x + w, y + h, x, y, x + w, y + h, AWTImageSupport.AWT_OBSERVER);
			g.dispose();
			img = tmp;
		}

		Grey16Codec encoder = new Grey16Codec(new float[] { .0f, 1.0f });

		final short[] src = ((DataBufferUShort) img.getRaster().getDataBuffer()).getData();
		final int srcll = img.getWidth();
		int srcyoff = srcll * y + x;
		for (; h > 0; h--) {
			buffer.position(dstyoff + x);
			for (int i = 0; i < w; i++)
				buffer.put(encoder.decode(src[srcyoff + i]));
			srcyoff += srcll;
			dstyoff -= dstll;
		}
	}

	@Override
	public BufferedImage toBufferedImage() {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
		short[] line = new short[width];

		Grey16Codec encoder = new Grey16Codec(getMinMax());

		int y = 0;
		for (int j = height; --j >= 0; y++) {
			for (int i = 0; i < line.length; i++)
				line[i] = encoder.encode(buffer.get(j * width + i));

			result.getRaster().setDataElements(0, y, width, 1, line);
		}
		return result;
	}

	@Override
	public Frame copy() {
		Frame result = new FloatFrame(this);
		return result;
	}

	@Override
	public Frame alloc() {
		return new FloatFrame(width, height);
	}

	@Override
	public float getBrightnessBilinear(double u, double v) {
		return getComponentBilinear(u, v, 0);
	}

	@Override
	public float getFloatComponent(int x, int y, int component) {
		float out = buffer.get(y * width + x);

		if (component == 3)
			return Float.isNaN(out) ? 0 : 1;

		return out;
	}

	public float[] getMinMax() {
		float minMax[] = { Float.MAX_VALUE, -Float.MAX_VALUE };
		buffer.clear();
		for (int i = buffer.capacity(); --i >= 0;) {
			float val = buffer.get();

			if (Float.isNaN(val))
				continue;

			minMax[0] = Math.min(minMax[0], val);
			minMax[1] = Math.max(minMax[1], val);
		}
		return minMax;
	}

	private static class Grey16Codec {
		private static int MAX_USHORT = 0xffff;

		private final float min;
		private final float max;
		private final float range;

		public Grey16Codec(float[] minMax) {
			this.min = minMax[0];
			this.max = minMax[1];
			this.range = minMax[1] - minMax[0];
		}

		public short encode(float value) {
			int ival = Math.round(((value - min) / range) * MAX_USHORT);

			return (short) (ival & 0xffff);
		}

		public float decode(int val) {
			int ival = val & 0xffff;
			return linearInterpolate(min, max, ival / (float) MAX_USHORT);
		}

		public float decode(int msb, int lsb) {
			return decode(msb << 8 & 0xff00 | lsb & 0xff);
		}
	}

	@Override
	public float getBrightness(int x, int y) {
		return buffer.get(y * width + x);
	}

	@Override
	public void setARGB(int x, int y, int argb) {
		buffer.put((y * width) + x, ((((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 765f));
	}

	public void setBrightness(int x, int y, float value) {
		buffer.put(y * width + x, value);
	}

	@Override
	public int getARGB(int x, int y) {
		int rgb = (int) (buffer.get(y * width + x) * 255f) & 0xFF;
		return rgb << 16 | rgb << 8 | rgb | 0xFF000000;
	}

	@Override
	public void getRGBBilinear(double u, double v, byte[] rgb) {
		rgb[0] = rgb[1] = rgb[2] = (byte) (getComponentBilinear(u, v, 0) * 255f);
	}

	@Override
	public FloatFrame getSubframe(int x, int y, int width, int height) {
		FloatFrame result = new FloatFrame(width, height);
		getSubframeImpl(x, y, result);
		return result;
	}

	@Override
	public void setSubframe(int x, int y, Frame src) {
		if (src.getClass() != getClass())
			src = new FloatFrame(src);
		setSubframeImpl(x, y, src);
	}

	@Override
	protected void loadTexture() {
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RED, width, height, 0, GL11.GL_RED, GL11.GL_FLOAT, pixels);
	}

	public void normalize() {
		float max = 0;
		buffer.clear();
		for(int i = buffer.capacity(); --i >= 0;)
			max = Math.max(max, buffer.get());
		buffer.clear();
		for(int i = buffer.capacity(); --i >= 0;)
			buffer.put(i, buffer.get(i) / max);		
	}	
}
