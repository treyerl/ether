package ch.fhnw.ether.audio.fx;

import ch.fhnw.ether.audio.AudioFrame;
import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.Parameter;
import ch.fhnw.ether.media.RenderCommandException;

public class BeatDetect extends AbstractRenderCommand<IAudioRenderTarget> {
	private static final Parameter MAX_BPM = new Parameter("bpm",  "Max. BPM",    40,  180,   130);
	private static final Parameter SENS    = new Parameter("sens", "Sensitivity", 0f,    2f,  1f);

	private       double      holdTime;
	private       double      lastBeat;
	private       double      avgBPM;
	private final OnsetDetect onset;
	private final AutoGain    aGain;
	private       int         beatCounter;
	private       float       lastThresh;

	public BeatDetect(AutoGain gain, OnsetDetect onset) {
		super(MAX_BPM, SENS);
		this.aGain = gain;
		this.onset = onset;
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		final AudioFrame frame = target.getFrame();

		double hold = 60 / getVal(MAX_BPM);
		if(frame.playOutTime > holdTime) {
			
			float energy0 = AudioUtilities.energy(frame.samples);
			float energy1 = (getVal(SENS) * aGain.gain()) / 10f;
						
			if(onset.threshold() - lastThresh > 0 && energy0 > energy1) {
				beatCounter++;
				holdTime = frame.playOutTime + hold;
				double bpm = 60 / (frame.playOutTime - lastBeat);
				avgBPM = avgBPM * 0.8 + (0.2 * bpm);
				lastBeat = frame.playOutTime;
			}
		} else if(holdTime - (1.1 * hold) > frame.playOutTime)
			holdTime = 0;
		lastThresh = onset.threshold();
	}

	public int beatCounter() {
		return beatCounter;
	}

	public double bpm() {
		return avgBPM;
	}
}
