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

package ch.fhnw.ether.audio.fx;

import java.util.Arrays;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.BlockBuffer;
import ch.fhnw.ether.audio.ButterworthFilter;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;

public class OnsetDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final Parameter SENS       = new Parameter("sens",   "Sensitivity",    0f,    100f,    100-25);
	private static final Parameter BAND_DECAY = new Parameter("bDecay", "Per band decay", 0.88f, 0.9999f, 0.9f);
	private static final Parameter AVG_DECAY  = new Parameter("aDecay", "Average decay",  0.88f, 0.9999f, 0.999f);

	private static final float[] BANDS      = { 80, 2000, 6000, 13000, 16000 };
	private static final double  CHUNK_SIZE = 0.02;
	private static final float   ATTACK     = 0.9f;

	private final BandsButterworth      bands;
	private float[]                     lastBands;
	private float[]                     bandsa;
	private float[]                     fluxBands;
	private float[]                     thresholds;
	private ButterworthFilter[]         filters;
	private float                       flux;
	private float                       threshold;
	private BlockBuffer                 buffer;

	public OnsetDetect(BandsButterworth bands) {
		this.bands = bands;
	}

	@Override
	protected void init(IAudioRenderTarget target) throws RenderCommandException {
		this.buffer = new BlockBuffer((int) (target.getSampleRate() * CHUNK_SIZE), false, Window.RECTANGLE);
		if(OnsetDetect.this.bands == null) {
			lastBands  = new float[BANDS.length - 1];
			bandsa     = new float[BANDS.length - 1];
			fluxBands  = new float[BANDS.length - 1];
			thresholds = new float[BANDS.length - 1];
			filters   = new ButterworthFilter[BANDS.length - 1];
			for(int i = 0; i < filters.length; i++)
				filters[i] = ButterworthFilter.getBandpassFilter(target.getSampleRate(), BANDS[i], BANDS[i+1]);
		} else {
			lastBands  = new float[OnsetDetect.this.bands.numBands()];
			bandsa     = new float[OnsetDetect.this.bands.numBands()];
			fluxBands  = new float[OnsetDetect.this.bands.numBands()];
			thresholds = new float[OnsetDetect.this.bands.numBands()];
			filters    = new ButterworthFilter[OnsetDetect.this.bands.numBands()];
		}
	}
	private void processBand(final int band, final float decay, final float sens) {
		float d = bandsa[band] - lastBands[band];
		fluxBands[band] = d;
		if(d > thresholds[band]) {
			thresholds[band] = thresholds[band] * ATTACK + (1-ATTACK) * d;
			flux += d / (sens * thresholds[band]);
		}
		else
			thresholds[band] *= decay;
	}

	public float[] fluxBands() {
		return fluxBands;
	}

	public float[] thresholds() {
		return thresholds;
	}

	public float flux() {
		return flux;
	}

	public float threshold() {
		return threshold;
	}

	public float onset() {
		float result = flux() / threshold();
		if(result > 1) result = 1;
		return result;
	}

	public void reset() {
		threshold = 0;
		Arrays.fill(bandsa, 0f);
		Arrays.fill(lastBands, 0f);
		Arrays.fill(fluxBands, 0f);
		Arrays.fill(thresholds, 0f);
	}

	public OnsetDetect() {
		super(SENS, BAND_DECAY, AVG_DECAY);
		this.bands = null;
	}

	@Override
	protected void run(final IAudioRenderTarget target) throws RenderCommandException {
		final AudioFrame frame = target.getFrame();
		final float decay = getVal(BAND_DECAY);
		final float sens  = Math.max(0.1f, getMax(SENS) - getVal(SENS));
		if(OnsetDetect.this.bands == null) {
			final float[] monoSamples = frame.getMonoSamples();
			buffer.add(monoSamples);

			flux = 0;
			for(;;) {
				float[] block = buffer.nextBlock();
				if(block == null) break;
				for(int band = 0; band < BANDS.length - 1; band++) {
					final float[] samples = block.clone();
					if(samples.length > 5) {
						filters[band].processBand(samples);
						bandsa[band] = AudioUtilities.energy(samples);
						processBand(band, decay, sens);
					}
				}
			}
		} else {
			OnsetDetect.this.bands.power(bandsa);
			for(int band = 0; band < bandsa.length; band++)
				processBand(band, decay, sens);
		}

		if(flux > threshold)
			threshold = threshold * ATTACK + (1-ATTACK) * flux;
		else
			threshold *= getVal(AVG_DECAY);

		System.arraycopy(bandsa, 0, lastBands, 0, bandsa.length );
	}	
}
