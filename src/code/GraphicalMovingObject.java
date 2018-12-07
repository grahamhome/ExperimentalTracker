package code;

import java.util.ArrayList;

import code.ExperimentModel.MovingObject;
import code.ExperimentModel.MovingObjectLabel;
import code.ExperimentModel.WaypointObject;
import code.GraphicalStationaryObject;
import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Visual representation of a moving object.
 * @author Graham Home
 */
class GraphicalMovingObject extends GraphicalObject {
	private MovingObject object;
	private MovingObjectLabel objectLabel;
	Label label;
	private ArrayList<WaypointObject> pathPoints;
	
	/**
	 * Create a visual representation of a moving object from a MovingObject instance.
	 * @param object : The MovingObject instance representing the object to be depicted graphically.
	 */
	public GraphicalMovingObject(MovingObject object) {
		super(object);
		this.object = object;
		this.pathPoints = object.pathPoints;
		objectLabel = object.label;
		if (objectLabel != null) {
			/* Create label for moving object */
			label = new Label(objectLabel.value);
			label.setTextFill(objectLabel.color);
			label.setBackground(new Background(new BackgroundFill(objectLabel.backgroundColor, null, null)));
			label.setMinWidth(Region.USE_PREF_SIZE);
			label.setMaxWidth(Region.USE_PREF_SIZE);
			label.setMaxHeight(objectLabel.size);
			label.setFont(Font.loadFont(TrackingActivity.textFontURL.toString(), objectLabel.size));
			TrackingActivity.root.getChildren().add(label);
		}
		generatePaths();
		TrackingActivity.objects.put(object, this);
	}
	
	/**
	 * Create animations for moving an object and its label (if present) between
	 * its specified list of waypoints.
	 * @param pathPoints
	 */
	public void generatePaths() {
		/* Create path for moving object icon */
		Path iconPath = new Path();
		iconPath.getElements().add(new MoveTo(graphicalIcon.getX(), graphicalIcon.getY()));
		/* Create path for label, if present */
		Path labelPath = new Path();
		if (label != null) {
			double[] coords = getLabelRelativePosition(graphicalIcon);
			labelPath.getElements().add(new MoveTo(coords[0], coords[1]));
		}
		double distance = 0;
		WaypointObject previous = pathPoints.get(0);
		/* Include all waypoints in object and label paths */
		for (WaypointObject waypointObject : pathPoints) {
			GraphicalStationaryObject waypoint = TrackingActivity.waypoints.get(waypointObject);
			/* 
			 * Calculate distance between the current & next waypoints in the path &
			 * add it to the total distance travelled.
			 */
			distance += Math.sqrt(Math.pow(waypointObject.x-previous.x,2)+Math.pow(waypointObject.y-previous.y,2));
			iconPath.getElements().add(new LineTo(waypoint.x, waypoint.y));
			if (label != null) {
				double[] coords = getLabelRelativePosition(waypoint.graphicalIcon);
				labelPath.getElements().add(new LineTo(coords[0], coords[1]));
			}
			previous = waypointObject;
		}
		/* Create transition for object path & add to master transition */
		PathTransition iconPathTransition = new PathTransition();
		iconPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
		iconPathTransition.setPath(iconPath);
		iconPathTransition.setNode(graphicalIcon);
		iconPathTransition.setInterpolator(Interpolator.LINEAR);
		/* Determine duration of transition based on distance travelled and object speed */
		Duration travelTime = Duration.minutes((distance/object.speed)*60);
		iconPathTransition.setDuration(travelTime);
		TrackingActivity.masterTransition.getChildren().add(iconPathTransition);
		/* Create transition for label path (if present) & add to master transition */
		if (label != null) {
			PathTransition labelPathTransition = new PathTransition();
			labelPathTransition.setPath(labelPath);
			labelPathTransition.setNode(label);
			labelPathTransition.setInterpolator(Interpolator.LINEAR);
			labelPathTransition.setDuration(travelTime);
			TrackingActivity.masterTransition.getChildren().add(labelPathTransition);
		}
	}
	
	/**
	 * Returns the coordinates of a label placed relative to a target object.
	 * @param target : The Text object which the label is to be placed relative to.
	 * @return : The coordinates of the label when placed relative to the target object.
	 */
	public double[] getLabelRelativePosition(Text target) {
		switch (objectLabel.position) {
			case RIGHT:
				return new double[] {target.getX()+(objectLabel.value.length()*objectLabel.size/3.8)+(graphicalIcon.getLayoutBounds().getWidth()/2)+5, target.getY()-objectLabel.size/4};
			case LEFT:
				return new double [] {target.getX()-(objectLabel.value.length()*objectLabel.size/3.8)-(graphicalIcon.getLayoutBounds().getWidth()/2)-5, target.getY()-objectLabel.size/4};
			case ABOVE:
				return new double[] {target.getX(), target.getY()-(objectLabel.size*2)};
			case BELOW:
				return new double[] {target.getX(), target.getY()+(graphicalIcon.getLayoutBounds().getHeight())};
			default:
				return new double[] {target.getX(), target.getY()};
		}
	}
	
	/**
	 * Shows or hides the object's label.
	 * @param show : True to show the label, false to mask it.
	 */
	public void maskLabel(boolean show) {
		if (label != null) {
			if (show) {
				Platform.runLater(() -> {
					label.setTextFill(Color.BLACK);
					label.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
					EventHandler<MouseEvent> listener = new EventHandler<MouseEvent>() {

						@Override
						public void handle(MouseEvent event) {
							maskLabel(false);
							ReportWriter.reportIdentityViewed(objectLabel.value);
						}
						
					};
					label.setOnMouseMoved(listener);
					graphicalIcon.setOnMouseMoved(listener);
				});
			} else {
				Platform.runLater(() -> {
					label.setTextFill(objectLabel.color);
					label.setBackground(new Background(new BackgroundFill(objectLabel.backgroundColor, null, null)));
					label.setOnMouseMoved(null);
					graphicalIcon.setOnMouseMoved(null);
				});
			}
		}
	}
}
