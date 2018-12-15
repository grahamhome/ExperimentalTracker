package code;

import code.ExperimentModel.BinaryQuery;
import code.ExperimentModel.FindQuery;
import code.ExperimentModel.Query;
import code.ExperimentModel.TextResponseQuery;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * A visual representation of a 'text entry' or 'click object' query which
 * appears and disappears at specified times.
 * @author Graham Home
 */
class GraphicalQueryObject extends SchedulableEvent {
	
	private VBox queryBox;
	private Query query;
	private TextField queryField;
	
	/**
	 * Constructs a graphical representation of a given Query object and
	 * schedules its appearance and disappearance.
	 * @param query : The Query object to be represented graphically.
	 */
	public GraphicalQueryObject(Query query) {
		/* Create the visual elements of the query */
		queryBox = new VBox(5);
		queryBox.setPadding(new Insets(5));
		queryBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		queryBox.setAlignment(Pos.CENTER);
		queryBox.setMaxWidth(Region.USE_PREF_SIZE);
		queryBox.setMaxHeight(Region.USE_PREF_SIZE);
		Label queryInstructions = new Label(query.text + (query instanceof BinaryQuery ? System.lineSeparator() + "Left-click for \"yes\", right-click for \"no\"" : ""));
		queryInstructions.setMinWidth(Region.USE_PREF_SIZE);
		queryInstructions.setMaxWidth(Region.USE_PREF_SIZE);
		queryInstructions.setFont(Font.loadFont(TrackingActivity.textFontURL.toString(), 15));
		queryBox.getChildren().add(queryInstructions);
		/* Only 'text input' queries have a text input field */
		queryField = new TextField();
		if (query instanceof TextResponseQuery) {
				queryBox.getChildren().add(queryField);
		}
		loopNumber = query.loopNumber;
		delay = (long)query.startTime;
		if (!query.wait) {
			duration = (long)(query.endTime-query.startTime);
			scheduledTermination = true;
		}
		this.query = query;
		TrackingActivity.queries.put(query, this);
	}
	
	/**
	 * Shows the query.
	 */
	@Override
	public void show() {
		ReportWriter.reportQuery(query, true);
		TrackingActivity.activeQuery = this;
		/* Position query elements on screen using specified coordinates and length of query string */
		Platform.runLater(() -> { 
			queryBox.relocate(((query.positionX*(TrackingActivity.mapWidth/ExperimentModel.x))+TrackingActivity.mapOffsetX)-(queryBox.getWidth()/2), 
					((query.positionY*(TrackingActivity.mapHeight/ExperimentModel.y))+TrackingActivity.mapOffsetY)-(query instanceof TextResponseQuery ? 20 : 10));
			TrackingActivity.root.getChildren().add(queryBox);
			queryBox.toFront();
		});
		if (query.maskIdentities) {
			// Mask all moving object labels
			TrackingActivity.objects.values().forEach(o -> {
				o.maskLabel(true);
			});
			ReportWriter.reportIdentityMask(true);
		}
		if (query.freeze) {
			// Pause object animation
			ReportWriter.reportFreeze(true);
			TrackingActivity.masterTransition.pause();
		}
		/* Allow 'text entry' query to be closed by pressing the 'enter' button */
		if (query instanceof TextResponseQuery) {
			queryField.setOnKeyPressed(e -> {
				if (e.getCode().equals(KeyCode.ENTER)) {
					((TextResponseQuery)query).respond(queryField.getText(), query.startTime-(TrackingActivity.experimentStartTime-System.currentTimeMillis()));
					ReportWriter.reportTextEntry((TextResponseQuery)query);
					responseReceived = true;
					hide();
					if (next != null && loopNumber == TrackingActivity.loop) {
						next.execute();
					}
				}
			});
		} else {
			/* Allow 'click object' query to be closed by clicking the screen */
			TrackingActivity.root.setOnMouseClicked(e -> {
				if (query instanceof FindQuery) {
					// Ensure click is within map boundaries
					if ((TrackingActivity.map.mapImage == null ? TrackingActivity.map.mapShape : TrackingActivity.map.mapImage).contains(new Point2D(e.getX(), e.getY()))) {
						double nmX = ((e.getX()-TrackingActivity.mapOffsetX)/TrackingActivity.map.mapShape.getWidth())*ExperimentModel.x;
						double nmY = ((e.getY()-TrackingActivity.mapOffsetY)/TrackingActivity.map.mapShape.getHeight())*ExperimentModel.y;
						((FindQuery)query).respond((float)nmX, (float)nmY, query.startTime-(TrackingActivity.experimentStartTime-System.currentTimeMillis()));
						Circle selectedArea = new Circle(e.getX(), e.getY(), Math.sqrt((((ExperimentModel.clickRadius/100)*TrackingActivity.mapHeight*TrackingActivity.mapWidth))/Math.PI));
						// Check waypoints
						TrackingActivity.waypoints.entrySet().forEach(entry -> {
							if (selectedArea.contains(new Point2D(entry.getValue().x, entry.getValue().y))) {
								((FindQuery)query).nearbyObjects.add(entry.getKey());
							}
						});
						// Check moving objects
						TrackingActivity.objects.entrySet().forEach(entry -> {
							Text objectIcon = entry.getValue().graphicalIcon;
							if (selectedArea.contains(new Point2D(objectIcon.getX() + objectIcon.getTranslateX(), objectIcon.getY() + objectIcon.getTranslateY()))) { 
								((FindQuery)query).nearbyObjects.add(entry.getKey());
							}
						});
						ReportWriter.reportClick((FindQuery)query);
						// Report hit objects
						((FindQuery)query).nearbyObjects.forEach(o -> {
							GraphicalMovingObject movingObj = TrackingActivity.objects.get(o);
							if (movingObj != null) {
								ReportWriter.reportObjectHit(
										(movingObj.label != null ? movingObj.label.getText() : "No label"), 
										Math.sqrt(Math.pow((nmX-o.x),2) + Math.pow((nmY-o.y), 2)));
							}
						});
						responseReceived = true;
						hide();
						// Execute the next scheduled event in the loop
						if (next != null && loopNumber == TrackingActivity.loop) {
							next.execute();
						}
					}
				} else {
					// Record left or right mouse button click
					((BinaryQuery)query).respond(e.getButton().equals(MouseButton.PRIMARY), query.startTime-(TrackingActivity.experimentStartTime-System.currentTimeMillis()));
					ReportWriter.reportBinaryQueryResponse((BinaryQuery)query);
					responseReceived = true;
					hide();
					if (next != null && loopNumber == TrackingActivity.loop) {
						next.execute();
					}
				}
				
			});
		}
	}
	
	/**
	 * Hides the query.
	 */
	@Override
	public void hide() {
		TrackingActivity.root.setOnMouseClicked(null);
		TrackingActivity.activeQuery = null;
		remove();
	}
	
	/**
	 * Removes the query.
	 */
	public void remove() {
		if (TrackingActivity.root.getChildren().contains(queryBox)) {
			sendToBack();
			Platform.runLater(() -> {
				TrackingActivity.root.getChildren().remove(queryBox);
			});
			ReportWriter.reportQuery(query, false);
			if (query.maskIdentities) {
				for (GraphicalMovingObject object : TrackingActivity.objects.values()) {
					object.maskLabel(false);
				}
				ReportWriter.reportIdentityMask(false);
			}
			if (query.freeze) {
				ReportWriter.reportFreeze(false);
				TrackingActivity.masterTransition.play();
			}
		}
	}
	
	/**
	 * Brings the query to the front of the view hierarchy.
	 */
	public void bringToFront() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				queryBox.toFront();
			}
		});
	}
	
	/**
	 * Sends the query to the back of the view hierarchy.
	 */
	public void sendToBack() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				queryBox.toBack();
			}
		});
	}
}
