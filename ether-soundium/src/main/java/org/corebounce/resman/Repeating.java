package org.corebounce.resman;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;

public class Repeating implements Runnable {
	final int delay;
	final AtomicReference<Runnable> r;
	
	public Repeating(int initialDelay, int delay, Runnable r) {
		this.delay = delay;
		this.r     = new AtomicReference<>(r);
		Display.getDefault().timerExec(initialDelay, this);
	}
	
	@Override
	public void run() {
		Runnable r = this.r.get();
		if(r == null) return;
		r.run();
		Display.getDefault().timerExec(delay, this);
	}

	public synchronized void stop() {
		r.set(null);
	}
}
