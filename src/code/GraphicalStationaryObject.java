package code;

import code.ExperimentModel.Connector;
import code.ExperimentModel.WaypointObject;
import javafx.scene.shape.Line;

/**
 * Visually represents a waypoint.
 */
public class GraphicalStationaryObject extends GraphicalObject {
	
	/**
	 * Creates a visual representation of a waypoint from a WaypointObject.
	 * @param waypoint : The WaypointObject representing the waypoint to be depicted graphically.
	 */
	public GraphicalStationaryObject(WaypointObject waypoint) {
		super(waypoint);
	}
	
	/**
	 * Draws all Connectors leading from this Waypoint to others.
	 * @param waypoint : The Waypoint which this object is built from.
	 */
	void drawConnectors(WaypointObject waypoint) {
		for (Connector connector: waypoint.connectors) {
			GraphicalStationaryObject destination = TrackingActivity.waypoints.get(connector.destination);
			Line line = new Line(x, y, destination.x, destination.y);
			line.setStroke(connector.color);
			line.setStrokeWidth(connector.width);
			TrackingActivity.root.getChildren().add(line);
			destination.graphicalIcon.toFront();
		}
		// Move the waypoint in front of its connectors in the view hierarchy
		graphicalIcon.toFront();
	}
}
