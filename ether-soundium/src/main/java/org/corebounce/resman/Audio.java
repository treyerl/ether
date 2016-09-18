package org.corebounce.resman;

import java.io.IOException;
import java.net.URL;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.IAudioSource;
import ch.fhnw.ether.audio.JavaSoundSource;
import ch.fhnw.ether.audio.JavaSoundTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.BandsButterworth;
import ch.fhnw.ether.audio.fx.BeatDetect;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.OnsetDetect;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.util.ArrayUtilities;
import ch.fhnw.util.Log;

public class Audio extends Subsystem {
	private static final Log log = Log.create();

	private final IAudioSource     src;
	private final DCRemove         dcrmv = new DCRemove();
	private final AutoGain         gain  = new AutoGain();
	private final BandsButterworth bands = new BandsButterworth(40, 8000, 40, 5, 1);
	private final OnsetDetect      onset = new OnsetDetect(); 
	private final BeatDetect       beat  = new BeatDetect(gain, onset); 
	private final JavaSoundTarget  dst   = new JavaSoundTarget();
	private final RenderProgram<IAudioRenderTarget> audio;
	
	public Audio(String ... args) throws RenderCommandException, IOException {
		super(CFG_PREFIX, args);
		
		
		URL url = null;
		try {
			url = new URL(configuration.get("in"));
		} catch(Throwable t) {}
		
		src = url == null ? new JavaSoundSource(2, 44100, 1024) : new URLAudioSource(url);
		
		audio = new RenderProgram<>(src, dcrmv, gain, bands, onset, beat);
		
		dst.useProgram(audio);
		dst.start();

		log.info("Audio started.");
	}

	public BandsButterworth getBands() {
		return bands;
	}

	public OnsetDetect getOnset() {
		return onset;
	}
	
	public void addLast(AbstractRenderCommand<IAudioRenderTarget> cmd) {
		audio.addLast(cmd);
	}

	public static String   CFG_PREFIX = "au";
	public static String[] CFG_OPTIONS = {
			"param", "Display parameter window",
			"in=<url>","URL of audio file",
	};
	
	static {
		int i = 0;
		for(String source : JavaSoundSource.getSources())
			CFG_OPTIONS = ArrayUtilities.cat(CFG_OPTIONS, new String[] {"in="+i++, source});
	}

	public BeatDetect getBeat() {
		return beat;
	}

}
