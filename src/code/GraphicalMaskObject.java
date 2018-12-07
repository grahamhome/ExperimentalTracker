package code;

import java.util.ArrayList;

import code.ExperimentModel.ScreenMaskEvent;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A visual representation of a 'mask event' which 
 * appears and disappears at specified times, obscuring 
 * the map when it appears.
 */
class GraphicalMaskObject extends SchedulableEvent {
	private ImageView mask;
	private ScreenMaskEvent maskEvent;
	private Rectangle maskBackground;
	public ArrayList<GraphicalQueryObject> concurrentEvents = new ArrayList<>();
	
	/**
	 * Constructs a graphical representation of a mask event and
	 * schedules its appearance and disappearance.
	 * @param event : The object representing the mask event to be depicted visually.
	 */
	public GraphicalMaskObject(ScreenMaskEvent event) {
		maskEvent = event;
		/* Create the visual elements of the mask */
		Image maskImage = new Image(event.image.toURI().toString());
		mask = new ImageView(maskImage);
		mask.setPreserveRatio(true);
		loopNumber = maskEvent.loopNumber;
		delay = (long)maskEvent.startTime;
		duration = (long)(maskEvent.endTime-maskEvent.startTime);
		scheduledTermination = true;
	}
	
	@Override
	public void execute() {
		super.execute();
		// Execute all scheduled events which occur concurrently with this mask
		concurrentEvents.forEach(e -> e.execute());
	}
	
	@Override
	public void show() {
		/* Position mask on screen over map */
		maskBackground = new Rectangle(TrackingActivity.stageWidth, TrackingActivity.stageHeight);
		maskBackground.setFill(Color.BLACK);
		mask.setFitWidth(TrackingActivity.mapWidth);
		mask.setFitHeight(TrackingActivity.mapHeight);
		mask.setX(TrackingActivity.mapOffsetX+((TrackingActivity.mapWidth-mask.getLayoutBounds().getWidth())/2));
		mask.setY(TrackingActivity.mapOffsetY+((TrackingActivity.mapHeight-mask.getLayoutBounds().getHeight())/2));
		Platform.runLater(() -> {
			TrackingActivity.root.getChildren().addAll(maskBackground, mask);
		});
		ReportWriter.reportMask(maskEvent, true);
	}
	
	@Override
	public void hide() {
		Platform.runLater(() -> {
			TrackingActivity.root.getChildren().removeAll(maskBackground, mask);
			ReportWriter.reportMask(maskEvent, false);
		});
		concurrentEvents.forEach(e -> e.hide());
	}
		
}
