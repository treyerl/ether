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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import ch.fhnw.ether.image.IImage.AlphaMode;
import ch.fhnw.ether.image.IImage.ComponentFormat;
import ch.fhnw.ether.image.IImage.ComponentType;

public interface IImageSupport {
	enum FileFormat {
		PNG, JPEG
	}

	IHostImage read(InputStream in, ComponentFormat componentFormat, ComponentType componentType, AlphaMode alphaMode) throws IOException;

	default IHostImage read(InputStream in) throws IOException {
		return read(in, null, null, null);
	}
	
	default IHostImage read(File file, ComponentFormat componentFormat, ComponentType componentType, AlphaMode alphaMode) throws IOException {
		return read(new FileInputStream(file), componentFormat, componentType, alphaMode);
	}

	default IHostImage read(File file) throws IOException {
		return read(new FileInputStream(file), null, null, null);
	}

	default IHostImage read(URL url, ComponentFormat componentFormat, ComponentType componentType, AlphaMode alphaMode) throws IOException {
		return read(url.openStream(), componentFormat, componentType, alphaMode);
	}

	default IHostImage read(URL url) throws IOException {
		return read(url.openStream(), null, null, null);
	}

	void write(IImage image, OutputStream out, FileFormat format) throws IOException;

	default void writeIImage(IImage image, File file, FileFormat format) throws IOException {
		write(image, new FileOutputStream(file), format);
	}

	IHostImage scale(IHostImage image, int width, int height);
}
