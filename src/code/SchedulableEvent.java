package code;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

abstract class SchedulableEvent {
	public SchedulableEvent next = null;
	public int loopNumber;
	public long delay;
	public long duration;
	public boolean scheduledTermination = false;
	public boolean responseReceived = false;
	public void execute() {
		// Schedule event appearance
		ScheduledExecutorService addEventService = Executors.newSingleThreadScheduledExecutor();
		addEventService.schedule(new Runnable() {
			@Override
			public void run() {
				if (TrackingActivity.loop == loopNumber) {
					if (TrackingActivity.activeQuery != null) {
						TrackingActivity.activeQuery.hide();
					}
					show();
				}
				if (scheduledTermination) {
					// Schedule event removal
					ScheduledExecutorService removeMaskService = Executors.newSingleThreadScheduledExecutor();
					removeMaskService.schedule(new Runnable() {
						@Override
						public void run() {
							if (!responseReceived) {
								hide();
								if (next != null && loopNumber == TrackingActivity.loop) {
									if (next instanceof GraphicalMaskObject) {
										((GraphicalMaskObject)next).execute();
									} else {
										next.execute();
									}
								}
							}
						}
					}, duration, TimeUnit.MILLISECONDS);
				}
			}
		}, delay, TimeUnit.MILLISECONDS);
	}
	public void show() {}
	public void hide() {}
}
