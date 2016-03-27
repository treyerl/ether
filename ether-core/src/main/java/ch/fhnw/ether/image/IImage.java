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

import ch.fhnw.ether.scene.mesh.material.Texture;

public interface IImage {
	
	enum ComponentType {
		BYTE(8),
		FLOAT(32);
		
		private final int size;
		
		ComponentType(int size) {
			this.size = size;
		}
		
		public int getSize() {
			return size;
		}
	}
	
	enum AlphaMode {
		NO_ALPHA,
		POST_MULTIPLIED,
		PRE_MULTIPLIED
	}

	void clear();
	
	IImage copy();

	IImage alloc();

	int getWidth();
	
	int getHeight();
	
	int getNumComponents();
	
	ComponentType getComponentType();
	
	AlphaMode getAlphaMode();
	
	int getPixel(int x, int y);
	
	void setPixel(int x, int y, int pixel);
	
	byte[] getPixel(int x, int y, byte[] dst);

	void setPixel(int x, int y, byte[] src);
	
	float[] getPixel(int x, int y, float[] dst);
	
	void setPixel(int x, int y, float[] src);
	
	byte getComponentInt32(int x, int y, int component);
	
	void setComponentInt32(int x, int y, int component, byte value);
	
	float getComponentFloat(int x, int y, int component);
	
	void setComponentFloat(int x, int y, int component, float value);
	
	IImage getSubframe(int x, int y, int width, int height);

	void setSubframe(int x, int y, IImage frame);
	
	ByteBuffer getPixels();

	Texture getTexture();
}
