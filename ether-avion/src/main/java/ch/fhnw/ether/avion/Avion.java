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

package ch.fhnw.ether.avion;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Avion {
	public static void main(String[] args) {
		System.out.println("welcome to avion: " + READY);
	}
	
	private static boolean READY = true;

	static {
		try {
			System.loadLibrary("avion");
		} catch (Throwable t) {
			READY = false;
		}
	}

	public static boolean isReady() {
		return READY;
	}

	private Avion() {
	}
	
	public static AVDecoder createDecoder(URL url, boolean decodeAudio, boolean decodeVideo, int audioBufferSize, boolean audioInterleaved, double audioSampleRate) {
		return new AVDecoder(url, decodeAudio, decodeVideo, audioBufferSize, audioInterleaved, audioSampleRate);
	}
	
	public static AVEncoder createEncoder(String path) {
		return new AVEncoder(path);
	}

	static native long decoderCreate(String url, boolean decodeAudio, boolean decodeVideo, int audioBufferSize, boolean audioInterleaved, double audioSampleRate);

	static native void decoderDispose(long nativeHandle);

    static native void decoderRange(long nativeHandle, double start, double end);

    static native boolean decoderHasAudio();

    static native boolean decoderHasVideo();
    
    static native double decoderGetDuration(long nativeHandle);

	static native double decoderGetVideoFrameRate(long nativeHandle);

	static native int decoderGetVideoWidth(long nativeHandle);

	static native int decoderGetVideoHeight(long nativeHandle);

	static native int decoderDecodeAudio(long nativeHandle, FloatBuffer buffer, double[] pts);

	static native int decoderDecodeVideo(long nativeHandle, ByteBuffer buffer, double[] pts);
}
