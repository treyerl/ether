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

public final class AVDecoder {
	
	public class Profile {
		public Profile(boolean video, boolean audio, int audioFrameSize, int audioSampleRate) {
			
		}
	}
	

	private long nativeHandle;

	private final URL url;
	private final double duration;
	private final double videoFrameRate;
	private final int videoWidth;
	private final int videoHeight;

	public AVDecoder(URL url) {
		this.url = url;

		nativeHandle = Avion.decoderCreate(url.toString());
		if (nativeHandle == 0)
			throw new IllegalArgumentException("cannot create av decoder from " + url);

		duration = Avion.decoderGetDuration(nativeHandle);
		videoFrameRate = Avion.decoderGetVideoFrameRate(nativeHandle);
		videoWidth = Avion.decoderGetVideoWidth(nativeHandle);
		videoHeight = Avion.decoderGetVideoHeight(nativeHandle);
	}

	public void dispose() {
		Avion.decoderDispose(nativeHandle);
		nativeHandle = 0;
	}

	public URL getURL() {
		return url;
	}

	public double getDuration() {
		return duration;
	}

	public double getVideoFrameRate() {
		return videoFrameRate;
	}

	public int getVideoWidth() {
		return videoWidth;
	}

	public int getVideoHeight() {
		return videoHeight;
	}

	public ByteBuffer getNextAudioFrame(double[] pts) {
		return Avion.decoderGetNextAudioFrame(nativeHandle, pts);
	}

	public ByteBuffer getNextVideoFrame(double[] pts) {
		return Avion.decoderGetNextVideoFrame(nativeHandle, pts);
	}

	public void seek(double time) {
		Avion.decoderSeek(nativeHandle, time);
	}

	@Override
	public String toString() {
		return getURL() + " (d=" + getDuration() + " fr=" + getVideoFrameRate() + " w=" + getVideoWidth() + " h=" + getVideoHeight() + ")";
	}
}
