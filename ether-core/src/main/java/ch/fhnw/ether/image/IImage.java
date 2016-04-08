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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import ch.fhnw.ether.image.IImageSupport.FileFormat;
import ch.fhnw.ether.platform.Platform;

/**
 * IImage is the opaque base type of all images. Besides size and format we
 * don't know anything about the image.
 * 
 * @author radar
 *
 */
public interface IImage {

	enum ComponentType {
		BYTE(1), FLOAT(4);

		private final int size;

		private ComponentType(int size) {
			this.size = size;
		}

		public int getSize() {
			return size;
		}
	}

	enum ComponentFormat {
		G(1), GA(2), RGB(3), RGBA(4);

		private final int numComponents;

		private ComponentFormat(int numComponents) {
			this.numComponents = numComponents;
		}

		public int getNumComponents() {
			return numComponents;
		}

		public static ComponentFormat get(int numComponents) {
			return ComponentFormat.values()[numComponents - 1];
		}
	}

	enum AlphaMode {
		POST_MULTIPLIED, PRE_MULTIPLIED
	}

	int getWidth();

	int getHeight();

	ComponentType getComponentType();

	ComponentFormat getComponentFormat();

	AlphaMode getAlphaMode();

	int getNumBytesPerPixel();
	
	static void write(IImage image, OutputStream out, FileFormat format) throws IOException {
		Platform.get().getImageSupport().write(image, out, format);
	}

	static void write(IImage image, File file, FileFormat format) throws IOException {
		write(image, new FileOutputStream(file), format);
	}
}
