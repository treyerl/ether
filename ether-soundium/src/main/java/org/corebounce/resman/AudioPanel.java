package org.corebounce.resman;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.BeatDetect;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.ParameterWindow;
import ch.fhnw.util.Log;

public class AudioPanel implements PaintListener, ControlListener {
	private static final Log log = Log.create();

	private final Audio                      audio;
	private 	  AtomicReference<ImageData> buffer = new AtomicReference<>(new ImageData(1, 100, 24, new PaletteData(0xFF0000, 0x00FF00, 0x0000FF)));
	private       Canvas                     canvasUI;
	private       Label                      bpmUI;
	private       int                        x;
	private       int                        lastCount;
	private       int                        lastCountPLL;
	private       Color                      FLASH;

	public AudioPanel(Audio audio) {
		this.audio = audio;

		audio.addLast(new AbstractRenderCommand<IAudioRenderTarget>() {
			boolean trigger = true;
			@Override
			protected void run(IAudioRenderTarget target) throws RenderCommandException {
				ImageData  buffer = AudioPanel.this.buffer.get();
				BeatDetect beat   = audio.getBeat();


				if(beat.beatCounterPLL() != lastCountPLL && trigger) {
					trigger = false;
					x       = 5;
				}

				if(x >= buffer.width || trigger) {
					trigger = true;
					x = 0;
				}

				final int   line  = (buffer.bytesPerLine) - 3;
				final float scale = buffer.height;

				int value  = (int)(beat.value() * scale);
				int thresh = (int)(beat.threshold() * scale);

				int off = x * 3;
				for(int y = buffer.height; --y >= 0;) {
					byte r = 0;
					byte g = 0;
					byte b = 0;

					if(y < value)
						g = (byte) 200;
					if(y == thresh)
						r = (byte) 200;
					buffer.data[off++] = r;
					buffer.data[off++] = g;
					buffer.data[off++] = b;
					off += line;
				}


				if(beat.beatCounterPLL() != lastCountPLL) {
					off          = x * 3;
					lastCountPLL = beat.beatCounterPLL();
					for(int y = 2 * buffer.height / 3; y < buffer.height; y++) {
						buffer.data[off++] = -1;
						buffer.data[off++] = -1;
						buffer.data[off++] = -1;
						off += line;
					}
				} 

				if(beat.beatCounter() != lastCount) {
					off       = x * 3;
					lastCount = beat.beatCounter();
					for(int y = 2 * buffer.height / 3; y < buffer.height; y++) {
						buffer.data[off++] = -1;
						buffer.data[off++] = -1;
						buffer.data[off++] = 0;
						off += line;
					}
				}
				x++;
			}
		});
	}

	public Composite createPartControl(Composite parent) {
		Composite result = new Composite(parent, SWT.NONE);

		FLASH   = parent.getDisplay().getSystemColor(SWT.COLOR_YELLOW);

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
		bpmUI = new Label(ui, SWT.CENTER);
		bpmUI.setLayoutData(GridDataFactory.fill(true, false));
		ParameterWindow.createUI(ui, audio.getBeat(), false);

		canvasUI = new Canvas(result,  SWT.NO_REDRAW_RESIZE | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		canvasUI.addPaintListener(this);
		canvasUI.addControlListener(this);
		canvasUI.setSize(SWT.DEFAULT, buffer.get().height);
		canvasUI.setLayoutData(GridDataFactory.fill(true, false, SWT.DEFAULT, buffer.get().height));

		new Repeating(500, 16, ()->{if(valid()) canvasUI.redraw();});

		return result;
	}

	@Override
	public void controlMoved(ControlEvent e) {}

	@Override
	public void controlResized(ControlEvent e) {
		if(valid()) {
			ImageData buffer = AudioPanel.this.buffer.get();
			this.buffer.set(buffer.scaledTo(canvasUI.getSize().x, buffer.height));
			x = 0;
		}
	}

	int  lastBPM;
	int  lastBeat;
	long flashTimeout;
	@Override
	public void paintControl(PaintEvent e) {
		try {
			long now = System.currentTimeMillis();

			ImageData buffer = this.buffer.get();
			int       x      = this.x;
			Image image = new Image(e.display, buffer);
			/*
			e.gc.drawImage(image, 
					0,                0, x, buffer.height, 
					buffer.width - x, 0, x, buffer.height); 
			e.gc.drawImage(image, 
					x, 0, buffer.width - x, buffer.height, 
					0, 0, buffer.width - x, buffer.height);*/
			e.gc.drawImage(image, 0, 0);
			image.dispose();
			e.gc.setForeground(FLASH);
			e.gc.drawLine(x, 0, x, 16);
			
			int bpm  =  (int)audio.getBeat().bpm();
			int beat = audio.getBeat().beatCounterPLL();
			if(lastBPM != bpm) {
				bpmUI.setText(bpm + " BPM");
				lastBPM = bpm;
			}
			if(lastBeat != beat) {
				bpmUI.setBackground(FLASH);
				flashTimeout = now + 100;
				lastBeat     = beat;
			}
			if(now > flashTimeout) {
				bpmUI.setBackground(null);
				flashTimeout = Long.MAX_VALUE;
			}
		} catch(Throwable t) {
			log.severe(t);
		}
	}

	private boolean valid() {
		return canvasUI != null && !canvasUI.isDisposed() && canvasUI.getSize().x > 0;
	}
}
