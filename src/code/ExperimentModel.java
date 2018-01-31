package code;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javafx.scene.paint.Color;

/**
 * This class represents the configuration for an MIT experiment.
 * @author Graham Home <grahamhome333@gmail.com>
 *
 */
public class ExperimentModel {
	
	public String name;
	public float x, y, updateRate;
	public javafx.scene.paint.Color mapColor;
	public File mapImage;
	public double duration;
	public double clickRadius;
	public String introduction	;
	public ArrayList<Waypoint> waypoints = new ArrayList<>();
	public ArrayList<Connector> connectors = new ArrayList<>();
	public ArrayList<MovingObject> objects = new ArrayList<>();
	public ArrayList<MaskEvent> maskEvents = new ArrayList<>();
	public ArrayList<Query> queries = new ArrayList<>();
	
	public static class Waypoint {
		public String name;
		public float x, y;
		public String icon;
		public float size;
		public Color color;
		
		@Override
		public boolean equals(Object waypointToCompare) {
			if (!(waypointToCompare instanceof Waypoint)) return false;
			Waypoint waypoint = (Waypoint) waypointToCompare;
			return ((x == waypoint.x && y == waypoint.y) || (x == y && x == waypoint.y && y == waypoint.x));
		}
	}
	
	public Waypoint getWaypoint(String name) {
		for (Waypoint point : waypoints) {
			if (point.name.equals(name)) {
				return point;
			}
		}
		return null;
	}
	
	public MovingObject getMovingObject(String name) {
		for (MovingObject mover : objects) {
			if (mover.name.equals(name)) {
				return mover;
			}
		}
		return null;
	}
	
	public static class Connector {
		public Waypoint point1, point2;
		public int width;
		public javafx.scene.paint.Color color;
		
		public static final int maxWidth = 10;
		
		@Override
		public boolean equals(Object connectorToCompare) {
			if (!(connectorToCompare instanceof Connector)) return false;
			Connector connector = (Connector) connectorToCompare;
			return ((point1.equals(connector.point1) && point2.equals(connector.point2)) || (point1.equals(connector.point2) && point2.equals(connector.point1)));
		}
	}
	
	public static class MovingObject {
		public String name, icon;
		public Color color;
		public int numDots;
		public float speed, leaderLength;
		public ArrayList<Waypoint> pathPoints = new ArrayList<>();
		public Label label;
		
		public static final int maxLeaderLength = 10;
		public static final int maxDots = 10;
		
		@Override
		public boolean equals(Object moverToCompare) {
			if (!(moverToCompare instanceof MovingObject)) return false;
			MovingObject mover = (MovingObject) moverToCompare;
			if (mover.speed != speed || mover.pathPoints.size() != pathPoints.size()) return false;
			Iterator<Waypoint> pathPointsIterator1 = mover.pathPoints.iterator();
			Iterator<Waypoint> pathPointsIterator2 = pathPoints.iterator();
			while (pathPointsIterator1.hasNext() && pathPointsIterator2.hasNext()) {
				if (!pathPointsIterator1.next().equals(pathPointsIterator2.next())) {
					return false;
				}
			}
			return true;
		}
	}
	
	public static class Label {
		public enum Position {
			LEFT, RIGHT, ABOVE, BELOW
		}
		public Position position;
		public Color backgroundColor, foregroundColor;
		public String text;
	}
	
	public static class MaskEvent {
		public File image;
		public double startTime, endTime;
	}
	
	public static class Query {
		public String text;
		public boolean acceptsText; // If false, accepts a mouse click as input
		public double startTime, endTime;
		public boolean wait = false;
	}
}
