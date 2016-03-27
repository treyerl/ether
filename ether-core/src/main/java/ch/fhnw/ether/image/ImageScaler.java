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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

public final class ImageScaler {
	public static final int NORMALIZE = 1;
	public static final int DETECT_NO_DATA = 2;

	public static BufferedImage toType(BufferedImage src, int type) {
		if (src.getType() == type)
			return src;
		BufferedImage result = new BufferedImage(src.getWidth(), src.getHeight(), type);
		Graphics g = result.getGraphics();
		g.drawImage(src, 0, 0, AWTFrameSupport.AWT_OBSERVER);
		g.dispose();
		return result;
	}

	/**
	 * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
	 *
	 * @param img
	 *            the original image to be scaled
	 * @param targetWidth
	 *            the desired width of the scaled instance, in pixels
	 * @param targetHeight
	 *            the desired height of the scaled instance, in pixels
	 * @param hint
	 *            one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality
	 *            if true, this method will use a multi-step scaling technique that provides higher quality than the
	 *            usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
	 *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
	 *            {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static Frame getScaledInstance(Frame img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {
		return AWTFrameSupport.createFrame(getScaledInstance(img.toBufferedImage(), targetWidth, targetHeight, hint, higherQuality, 0x00808080));
	}

	/**
	 * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
	 *
	 * @param img
	 *            the original image to be scaled
	 * @param targetWidth
	 *            the desired width of the scaled instance, in pixels
	 * @param targetHeight
	 *            the desired height of the scaled instance, in pixels
	 * @param hint
	 *            one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality
	 *            if true, this method will use a multi-step scaling technique that provides higher quality than the
	 *            usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
	 *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
	 *            {@code BILINEAR} hint is specified)
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality) {
		return getScaledInstance(img, targetWidth, targetHeight, hint, higherQuality, 0x00808080);
	}

	/**
	 * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
	 *
	 * @param img
	 *            the original image to be scaled
	 * @param targetWidth
	 *            the desired width of the scaled instance, in pixels
	 * @param targetHeight
	 *            the desired height of the scaled instance, in pixels
	 * @param hint
	 *            one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality
	 *            if true, this method will use a multi-step scaling technique that provides higher quality than the
	 *            usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
	 *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
	 *            {@code BILINEAR} hint is specified)
	 * @return blendARGB the color to blend non-fully transparent pixels with.
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static Frame getScaledInstance(Frame img, int targetWidth, int targetHeight, Object hint, boolean higherQuality, int blendARGB) {
		return AWTFrameSupport.createFrame(getScaledInstance(img.toBufferedImage(), targetWidth, targetHeight, hint, higherQuality, blendARGB));
	}
	
	/**
	 * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
	 *
	 * @param img
	 *            the original image to be scaled
	 * @param targetWidth
	 *            the desired width of the scaled instance, in pixels
	 * @param targetHeight
	 *            the desired height of the scaled instance, in pixels
	 * @param hint
	 *            one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
	 *            {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
	 *            {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
	 * @param higherQuality
	 *            if true, this method will use a multi-step scaling technique that provides higher quality than the
	 *            usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or
	 *            {@code targetHeight} is smaller than the original dimensions, and generally only when the
	 *            {@code BILINEAR} hint is specified)
	 * @return blendARGB the color to blend non-fully transparent pixels with.
	 * @return a scaled version of the original {@code BufferedImage}
	 */
	public static BufferedImage getScaledInstance(BufferedImage img, int targetWidth, int targetHeight, Object hint, boolean higherQuality, int blendARGB) {
		int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = img;
		int w, h;

		if (higherQuality && targetWidth < img.getWidth())
			w = img.getWidth();
		else
			w = targetWidth;

		if (higherQuality && targetHeight < img.getHeight())
			h = img.getHeight();
		else
			h = targetHeight;

		do {
			if (higherQuality && w > targetWidth) {
				w /= 2;
				if (w < targetWidth) {
					w = targetWidth;
				}
			}

			if (higherQuality && h > targetHeight) {
				h /= 2;
				if (h < targetHeight) {
					h = targetHeight;
				}
			}

			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			int[] background = new int[tmp.getWidth() * tmp.getHeight()];

			if (type == BufferedImage.TYPE_INT_ARGB) {
				g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, higherQuality ? RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
						: RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
				g2.drawImage(ret, 0, 0, w, h, null);
				tmp.getRGB(0, 0, tmp.getWidth(), tmp.getHeight(), background, 0, tmp.getWidth());
				for (int i = 0; i < background.length; i++) {
					background[i] = background[i] >>> 24 <= 1 ? blendARGB & 0xFFFFFF : blendARGB;
				}
			} else
				Arrays.fill(background, blendARGB);

			tmp.setRGB(0, 0, tmp.getWidth(), tmp.getHeight(), background, 0, tmp.getWidth());
			g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, higherQuality ? RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
					: RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();

			ret = tmp;
		} while (w != targetWidth || h != targetHeight);

		return ret;
	}

	public static Frame getScaledLimitedInstance(Frame src, float scaleW, float scaleH, int maxDim, Object hint, boolean higherQuality) {
		return AWTFrameSupport.createFrame(getScaledLimitedInstance(src.toBufferedImage(), scaleW, scaleH, maxDim, hint, higherQuality));
	}
	
	public static BufferedImage getScaledLimitedInstance(BufferedImage src, float scaleW, float scaleH, int maxDim, Object hint, boolean higherQuality) {
		int targetWidth = (int) (src.getWidth() * scaleW);
		int targetHeight = (int) (src.getHeight() * scaleH);
		if (maxDim > 0) {
			if (targetWidth > maxDim) {
				targetHeight = (int) ((float) targetHeight * (float) maxDim / targetWidth);
				targetWidth = maxDim;
			}
			if (targetHeight > maxDim) {
				targetWidth = (int) ((float) targetWidth * (float) maxDim / targetHeight);
				targetHeight = maxDim;
			}
		}
		if (targetWidth > 0 && targetHeight > 0) // sanitize
			return ImageScaler.getScaledInstance(src, targetWidth, targetHeight, hint, higherQuality);
		return null;
	}

	public static BufferedImage copy(BufferedImage src, BufferedImage dst) {
		return copy(src, dst, new float[] { 0, 1 });
	}

	public static BufferedImage copy(BufferedImage src, BufferedImage dst, float[] origMinMax) {
		if (isCompatible(src, dst))
			dst.setData(src.getData());
		else {
			boolean normalize = false;
			boolean detectNoData = false;

			if (origMinMax.length > 2) {
				normalize = ((int) origMinMax[2] & NORMALIZE) != 0;
				detectNoData = ((int) origMinMax[2] & DETECT_NO_DATA) != 0;
			}

			Graphics g = dst.getGraphics();
			DataBuffer sdb = src.getData().getDataBuffer();
			DataBuffer ddb = dst.getData().getDataBuffer();
			try {
				float[] tmp = null;
				float[] minMax = { Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
				switch (sdb.getDataType()) {
				case DataBuffer.TYPE_FLOAT:
					for (int y = src.getNumYTiles(); --y >= 0;) {
						for (int x = src.getNumXTiles(); --x >= 0;) {
							WritableRaster raster = src.getWritableTile(x, y);
							final int w = raster.getWidth();
							final int h = raster.getHeight();
							tmp = new float[w * h * raster.getNumDataElements()];
							raster.getDataElements(0, 0, w, h, tmp);
							for (int i = 0; i < tmp.length; i++) {
								final float v = tmp[i];
								if (v < minMax[0])
									minMax[0] = v;
								if (v < minMax[1] && v > minMax[0])
									minMax[1] = v;

								if (v > minMax[3])
									minMax[3] = v;
								if (v > minMax[2] && v < minMax[3])
									minMax[2] = v;
							}
						}
					}

					float noData = 0;
					if (detectNoData) {
						float r = minMax[2] - minMax[1];
						if (minMax[3] - minMax[2] > r) {
							noData = minMax[3];
							minMax[3] = minMax[2];
						} else if (minMax[1] - minMax[0] > r) {
							noData = minMax[0];
							minMax[0] = minMax[1];
						}
					}

					float csmin = Float.MAX_VALUE;
					float csmax = -Float.MAX_VALUE;
					ColorSpace cs = src.getColorModel().getColorSpace();
					for (int i = cs.getNumComponents(); --i >= 0;) {
						csmin = Math.min(csmin, cs.getMinValue(i));
						csmax = Math.max(csmin, cs.getMaxValue(i));
					}

					float min = minMax[0];
					float max = minMax[3];

					if (min >= csmin && max <= csmax) {
						min = csmin;
						max = csmax;
					}

					origMinMax[0] = minMax[0];
					origMinMax[1] = minMax[3];
					final float range = minMax[3] - minMax[0];

					// special treatment for ArcMap terrain output (32bit float tiffs) : hardcoded linear mapping to
					// USHORT
					// the g.drawImage() way results in wrong scaling, no idea why [dec]
					if (ddb.getDataType() == DataBuffer.TYPE_USHORT && sdb.getDataType() == DataBuffer.TYPE_FLOAT && dst.getNumXTiles() == src.getNumXTiles()
							&& dst.getNumYTiles() == dst.getNumYTiles()) {
						final float USHORTMAX = ((1 << 16) - 1);

						for (int y = src.getNumYTiles(); --y >= 0;) {
							for (int x = src.getNumXTiles(); --x >= 0;) {
								WritableRaster raster = src.getWritableTile(x, y);
								final int w = raster.getWidth();
								final int h = raster.getHeight();
								if (tmp == null || src.getNumXTiles() > 1 || src.getNumYTiles() > 1) {
									tmp = new float[w * h * raster.getNumDataElements()];
									raster.getDataElements(0, 0, w, h, tmp);
								}

								WritableRaster dstRaster = dst.getWritableTile(x, y);

								if (normalize) {
									if (detectNoData) {
										for (int i = 0; i < tmp.length; i++) {
											if (tmp[i] == noData)
												tmp[i] = min;
											else
												tmp[i] = (float) Math.rint((tmp[i] - min) / range * USHORTMAX);
										}
									} else {
										for (int i = 0; i < tmp.length; i++)
											tmp[i] = (float) Math.rint((tmp[i] - min) / range * USHORTMAX);
									}
								}

								dstRaster.setPixels(x, y, w, h, tmp);
							}
						}
					} else {
						for (int y = src.getNumYTiles(); --y >= 0;) {
							for (int x = src.getNumXTiles(); --x >= 0;) {
								WritableRaster raster = src.getWritableTile(x, y);
								final int w = raster.getWidth();
								final int h = raster.getHeight();
								if (tmp == null || src.getNumXTiles() > 1 || src.getNumYTiles() > 1) {
									tmp = new float[w * h * raster.getNumDataElements()];
									raster.getDataElements(0, 0, w, h, tmp);
								}
								if (normalize) {
									if (detectNoData) {
										for (int i = 0; i < tmp.length; i++) {
											if (tmp[i] == noData)
												tmp[i] = min;
											else
												tmp[i] = (tmp[i] - min) / range;
										}
									} else {
										for (int i = 0; i < tmp.length; i++)
											tmp[i] = (tmp[i] - min) / range;
									}
								}

								raster.setDataElements(0, 0, w, h, tmp);
							}
						}
						g.drawImage(src, 0, 0, AWTFrameSupport.AWT_OBSERVER);
					}
					break;
				default:
					g.drawImage(src, 0, 0, AWTFrameSupport.AWT_OBSERVER);
					break;
				}
			} catch (Throwable t) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, src.getWidth(), src.getHeight());
				g.setColor(Color.RED);
				g.drawRect(0, 0, src.getWidth() - 1, src.getHeight() - 1);
				g.setColor(Color.BLACK);
				g.drawString("Image Format not supported", 4, 16);
			} finally {
				g.dispose();
			}

		}
		return dst;
	}

	private static boolean isCompatible(BufferedImage src, BufferedImage dst) {
		return isCompatible(src.getSampleModel(), dst.getSampleModel()) && isCompatible(src.getColorModel(), dst.getColorModel());
	}

	private static boolean isCompatible(SampleModel src, SampleModel dst) {
		if (src.getNumBands() == dst.getNumBands()) {
			for (int i = 0; i < src.getNumBands(); i++)
				if (src.getSampleSize(i) != dst.getSampleSize(i))
					return false;
			return true;
		}
		return false;
	}

	private static boolean isCompatible(ColorModel src, ColorModel dst) {
		return src.hasAlpha() == dst.hasAlpha() && isCompatible(src.getColorSpace(), dst.getColorSpace());
	}

	private static boolean isCompatible(ColorSpace src, ColorSpace dst) {
		if (src.getType() == dst.getType() && src.getNumComponents() == dst.getNumComponents()) {
			for (int i = src.getNumComponents(); --i >= 0;) {
				if (src.getMaxValue(i) != dst.getMaxValue(i))
					return false;
				if (src.getMinValue(i) != dst.getMinValue(i))
					return false;
				if (!src.getName(i).equals(dst.getName(i)))
					return false;
			}
			return true;
		}
		return false;
	}
}
