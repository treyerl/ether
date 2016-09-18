package org.corebounce.resman;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.BeatDetect;
import ch.fhnw.ether.audio.fx.OnsetDetect;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.Log;
import ch.fhnw.util.math.MathUtilities;

public class AudioPanel implements PaintListener, ControlListener {
	private static final Log log = Log.create();
	
	private final Audio                      audio;
	private 	  AtomicReference<ImageData> buffer = new AtomicReference<>(new ImageData(1, 100, 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF)));
	private       Canvas                     canvas;
	private       int                        x;
	private       int                        lastCount;

	public AudioPanel(Audio audio) {
		this.audio = audio;

		audio.addLast(new AbstractRenderCommand<IAudioRenderTarget>() {
			@Override
			protected void run(IAudioRenderTarget target) throws RenderCommandException {
				ImageData buffer = AudioPanel.this.buffer.get();
				if(x >= buffer.width) x = 0;
				int   off   = x * 3;
				final int   line  = (buffer.bytesPerLine) - 3;
				final float scale = buffer.height;

				OnsetDetect onset   = audio.getOnset();
				BeatDetect  beat    = audio.getBeat();
				float[]     bands   = onset.fluxBands();
				final int   y2bands = buffer.height / bands.length;

				int flux   = (int)(onset.flux() * scale);
				int thresh = (int)(onset.threshold() * scale);
				if(beat.beatCounter() != lastCount) {
					lastCount = beat.beatCounter();

					for(int y = buffer.height; --y >= 0;) {
						buffer.data[off++] = (byte) 255;
						buffer.data[off++] = 0;
						buffer.data[off++] = 0;
						off += line;
					}
				} else {
					for(int y = buffer.height; --y >= 0;) {
						byte r = 0;
						byte g = 0;
						byte b = (byte)MathUtilities.clamp((Math.abs(bands[y / y2bands])) * 10000, 0, 255);


						if(y < flux) {
							g = (byte) 200;
						}
						if(y == thresh) {
							r = (byte) 200;
						}
						buffer.data[off++] = r;
						buffer.data[off++] = g;
						buffer.data[off++] = b;
						off += line;
					}
				}
				x++;
			}
		});
	}

	public Composite createPartControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		result.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));	
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		result.setLayout(layout);

		Composite ui = new Composite(result, SWT.NONE);
		ui.setLayoutData(new GridData(SWT.DEFAULT, SWT.DEFAULT));		
		layout = new GridLayout(2, false);
		layout.marginWidth  = 0;
		layout.marginHeight = 0;
		ui.setLayout(layout);
		ParameterWindow.createUI(ui, audio.getBeat(), false);

		canvas = new Canvas(result, SWT.NONE);
		canvas.addPaintListener(this);
		canvas.addControlListener(this);
		canvas.setSize(SWT.DEFAULT, buffer.get().height);
		canvas.setLayoutData(GridDataFactory.fill(true, false, SWT.DEFAULT, buffer.get().height));

		new Repeating(500, 10, ()->{if(valid()) canvas.redraw();});

		return result;
	}

	@Override
	public void controlMoved(ControlEvent e) {}

	@Override
	public void controlResized(ControlEvent e) {
		if(valid()) {
			ImageData buffer = AudioPanel.this.buffer.get();
			this.buffer.set(buffer.scaledTo(canvas.getSize().x, buffer.height));
			x = 0;
		}
	}

	@Override
	public void paintControl(PaintEvent e) {
		try {
			ImageData buffer = this.buffer.get();
			int       x      = this.x;
			Image image = new Image(e.display, buffer);
			e.gc.drawImage(image, 
					0,                0, x, buffer.height, 
					buffer.width - x, 0, x, buffer.height); 
			e.gc.drawImage(image, 
					x, 0, buffer.width - x, buffer.height, 
					0, 0, buffer.width - x, buffer.height); 
			image.dispose();
		} catch(Throwable t) {
			log.severe(t);
		}
	}

	private boolean valid() {
		return canvas != null && !canvas.isDisposed() && canvas.getSize().x > 0;
	}
}
