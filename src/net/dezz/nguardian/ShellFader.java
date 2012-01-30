package net.dezz.nguardian;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class ShellFader {
	
	private class FadeTimer extends Timer {
		private boolean cancelled = false;
		
		public FadeTimer() {
		}
		
		public synchronized boolean isCancelled() {
			return this.cancelled;
		}
		
		public void setCancelled(boolean flag) {
			this.cancelled = flag;
		}
		
		@Override
		public void cancel() {
			if (!isCancelled()) {
				super.cancel();
				setCancelled(true);
			}
		}
	}

	private class FadeShellTimerTask extends TimerTask implements Cloneable {
		
		private Timer timer;
		private int delta;
		private Shell shell;
		private int startAlpha;
		private int alpha;
		private Runnable fadeComplete;
		
		private static final int MAX_ALPHA = 255;
		private static final int MIN_ALPHA = 0;
		
		public FadeShellTimerTask(Shell shell, int startAlpha, int delta, Runnable completeListener) {
			this.startAlpha = startAlpha;
			this.alpha = startAlpha;
			this.delta = delta;
			this.shell = shell;
			this.fadeComplete = completeListener;
		}
		
		public FadeShellTimerTask clone() {
			return new FadeShellTimerTask(shell, startAlpha, delta, fadeComplete);
		}
		
		public void setTimer(Timer timer) {
			this.timer = timer;
		}
	
		@Override
		public void run() {
			if (Display.getDefault() == null) {
				return;
			}
			
			alpha += delta;
			if (delta > 0 && alpha >= MAX_ALPHA) {
				alpha = MAX_ALPHA;
			} else if (delta < 0 && alpha <= MIN_ALPHA) {
				alpha = MIN_ALPHA;
			}
			
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					shell.setAlpha(alpha);
					if (alpha == MIN_ALPHA || alpha == MAX_ALPHA) {
						timer.cancel();
						if (fadeComplete != null) {
							fadeComplete.run();
						}
					}
				}
			});
		}
	}
	
	private FadeShellTimerTask task;
	private int delay;
	private Timer timer;
	
	public ShellFader(Shell shell, int delay, int startAlpha, int delta, Runnable completeListener) {
		this.task = new FadeShellTimerTask(shell, startAlpha, delta, completeListener);
		this.delay = delay;
	}
	
	public void stop() {
		if (timer != null) {
			timer.cancel();
		}
	}
	
	public void start() {
		timer = new FadeTimer();
		task = task.clone();
		task.setTimer(timer);
		timer.schedule(task, delay, 50);
	}
}
