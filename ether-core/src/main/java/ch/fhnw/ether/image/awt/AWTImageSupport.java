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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Icon;

import ch.fhnw.ether.image.IHostImage;
import ch.fhnw.ether.image.IImage;
import ch.fhnw.ether.image.IImage.AlphaMode;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;
import ch.fhnw.ether.image.IImageSupport;

public final class AWTImageSupport implements IImageSupport {

	@Override
	public IHostImage read(InputStream in, ComponentFormat componentFormat, ComponentType componentType, AlphaMode alphaMode) throws IOException {
		return null;
	}

	@Override
	public void write(IImage image, OutputStream out, FileFormat format) throws IOException {
	}

	@Override
	public IHostImage scale(IHostImage image, int width, int height) {
		return null;
	}

	public static ImageObserver AWT_OBSERVER = new ImageObserver() {
		@Override
		public boolean imageUpdate(java.awt.Image img, int infoflags, int x, int y, int width, int height) {
			return (infoflags & (ALLBITS | ERROR | ABORT)) == 0;
		}
	};

	public static Frame createFrame(BufferedImage img) {
		return createFrame(img, 0);
	}

	public static Frame createFrame(BufferedImage img, int flags) {
		Frame result = null;
		switch (img.getType()) {
		case BufferedImage.TYPE_BYTE_BINARY:
		case BufferedImage.TYPE_CUSTOM:
			if (img.getColorModel().getNumColorComponents() == 1)
				result = new Grey16Frame(img.getWidth(), img.getHeight());
			else {
				if (img.getColorModel().hasAlpha())
					result = new RGBA8Frame(img.getWidth(), img.getHeight());
				else
					result = new RGB8Frame(img.getWidth(), img.getHeight());
			}
			break;
		case BufferedImage.TYPE_4BYTE_ABGR:
		case BufferedImage.TYPE_INT_ARGB:
		case BufferedImage.TYPE_4BYTE_ABGR_PRE:
		case BufferedImage.TYPE_INT_ARGB_PRE:
		case BufferedImage.TYPE_BYTE_INDEXED:
			result = new RGBA8Frame(img.getWidth(), img.getHeight());
			break;
		case BufferedImage.TYPE_USHORT_555_RGB:
		case BufferedImage.TYPE_USHORT_565_RGB:
		case BufferedImage.TYPE_INT_RGB:
		case BufferedImage.TYPE_3BYTE_BGR:
			result = new RGB8Frame(img.getWidth(), img.getHeight());
			break;
		case BufferedImage.TYPE_BYTE_GRAY:
		case BufferedImage.TYPE_USHORT_GRAY:
			result = new Grey16Frame(img.getWidth(), img.getHeight());
			break;
		default:
			throw new RuntimeException("Unsupported image type " + img.getType());
		}

		result.setPixels(0, 0, img.getWidth(), img.getHeight(), img, flags);
		return result;
	}

	public static Frame createFrame(Icon icon) {
		BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.getGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();
		return AWTImageSupport.createFrame(img);
	}

	public static Frame createFrame(Image image, int targetType) {
		BufferedImage result = new BufferedImage(image.getWidth(AWTImageSupport.AWT_OBSERVER),
				image.getHeight(AWTImageSupport.AWT_OBSERVER), targetType);
		if (image instanceof BufferedImage)
			return AWTImageSupport.createFrame(ImageScaler.copy((BufferedImage) image, result));

		Graphics g = result.getGraphics();
		g.drawImage(image, 0, 0, AWTImageSupport.AWT_OBSERVER);
		g.dispose();
		return AWTImageSupport.createFrame(result);
	}

	public static Frame readFrame(File file) throws IOException {
		return createFrame(ImageIO.read(file));
	}

	public static Frame readFrame(URL url) throws IOException {
		return createFrame(ImageIO.read(url));
	}

	public static Frame readFrame(InputStream in) throws IOException {
		return createFrame(ImageIO.read(in));
	}

	public void writeFrame(Frame frame, File file, FileFormat format) throws IOException {
		ImageIO.write(frame.toBufferedImage(), format.toString(), file);
	}

	public void writeFrame(Frame frame, OutputStream out, FileFormat format) throws IOException {
		ImageIO.write(frame.toBufferedImage(), format.toString(), out);
	}
}
