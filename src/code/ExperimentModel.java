package code;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javafx.scene.paint.Color;

/**
 * This class represents the configuration for an MIT experiment.
 * @author Graham Home <grahamhome333@gmail.com>
 */
public class ExperimentModel {
	
	public static String name;
	public static float x, y, updateRate, largestFontSize;
	public static javafx.scene.paint.Color mapColor;
	public static File mapImage;
	public static double duration;
	public static double clickRadius;
	public static String introduction;
	public static HashMap<String, WaypointObject> waypoints = new HashMap<>();
	public static HashMap<String, MovingObject> objects = new HashMap<>();
	public static ArrayList<ScreenMaskEvent> screenMaskEvents = new ArrayList<>();
	public static ArrayList<IdentityMaskEvent> identityMaskEvents = new ArrayList<>();
	public static ArrayList<Query> queries = new ArrayList<>();
	
	/**
	 * Resets the configuration to its initial state.
	 */
	public static void reset() {
		name = null;
		x = 0;
		y = 0;
		updateRate = 0;
		mapColor = null;
		mapImage = null;
		duration = 0;
		clickRadius = 0;
		largestFontSize = 0;
		introduction = null;
		waypoints = new HashMap<>();
		objects = new HashMap<>();
		screenMaskEvents = new ArrayList<>();
		queries = new ArrayList<>();
	}
	
	/**
	 * Contains attributes common to all objects depicted as text or icons.
	 */
	public abstract static class TextObject {
		public float x,y,size;
		public String value;
		public Color color;
	}
	
	/**
	 * Represents a waypoint: a non-moving object optionally depicted as an icon.
	 * Waypoints have zero or more Connectors, which visually connect them to other Waypoints.
	 */
	public static class WaypointObject extends TextObject {
		public String name;
		public ArrayList<Connector> connectors = new ArrayList<>();
		
		/**
		 * Determines if one waypoint is equal to another.
		 */
		@Override
		public boolean equals(Object waypointToCompare) {
			if (!(waypointToCompare instanceof WaypointObject)) {
				return false;
			} else {
				WaypointObject waypoint = (WaypointObject) waypointToCompare;
				return ((x == waypoint.x && y == waypoint.y) || (x == y && (x == waypoint.y || y == waypoint.x)));
			}
		}
		
		/**
		 * Determines if a waypoint is equal to any other.
		 */
		public boolean alreadyExists() {
			return waypoints.values().stream().anyMatch(w -> this.equals(w));
		}
		
		/**
		 * Determines whether or not two Waypoints are connected.
		 */
		public Boolean isConnected(WaypointObject waypointToCheck) {
			return connectors.stream().anyMatch(c -> c.destination.equals(waypointToCheck)) ||
					waypointToCheck.connectors.stream().anyMatch(c -> c.destination.equals(this));
		}
		
		/**
		 * Sets the icon of the waypoint.
		 * @param value : A string value containing the hexadecimal representation of an icon.
		 * @throws IllegalArgumentException: if the given string does not contain a hexadecimal value which denotes a Unicode character.
		 */
		public void setValue(String value) throws IllegalArgumentException {
			this.value = Character.toString(Character.toChars(Integer.parseInt(value, 16))[0]);
		}
		
	}
	
	/**
	 * Represents a moving object, which is depicted as an icon and moves between a sequence of Waypoints.
	 */
	public static class MovingObject extends WaypointObject {
		public int numDots;
		public double speed;
		public float leaderLength;
		public ArrayList<WaypointObject> pathPoints = new ArrayList<>();
		public MovingObjectLabel label;
		
		public static final int maxLeaderLength = 10;
		public static final int maxDots = 10;
		
		/**
		 * Determines if one MovingObject is equal to any other by comparing their speeds and paths.
		 */
		public boolean alreadyExists() {
			for (MovingObject object : objects.values()) {
				if (object.speed == speed && object.pathPoints.size() == pathPoints.size()) {
					boolean exists = true;
					Iterator<WaypointObject> pathPointsIterator1 = object.pathPoints.iterator();
					Iterator<WaypointObject> pathPointsIterator2 = pathPoints.iterator();
					while (pathPointsIterator1.hasNext() && pathPointsIterator2.hasNext()) {
						if (!pathPointsIterator1.next().equals(pathPointsIterator2.next())) {
							exists = false;
						}
					}
					if (exists) return exists;
				}
			}
			return false;
		}
	}
	
	/**
	 * Represents the label of a MovingObject, which is depicted as a text string with an optional colored background,
	 * is positioned relative to the MovingObject to which it belongs, and moves between the same Waypoints as the MovingObject
	 * to which it belongs, at the same speed. 
	 */
	public static class MovingObjectLabel extends WaypointObject {
		public enum Position {
			LEFT, RIGHT, ABOVE, BELOW
		}
		public Position position;
		public Color backgroundColor;
		
		/**
		 * Sets the string value of the label.
		 */
		public void setValue(String text) {
			this.value = text;
		}
	}
	
	/**
	 * Represents a connector from one Waypoint to another. Has a color and width.
	 */
	public static class Connector {
		public WaypointObject destination;
		public int width;
		public javafx.scene.paint.Color color;
		
		public static final int maxWidth = 10;
	}
	
	/**
	 * Represents a "screen mask event", in which an image appears over the map at a specific time for a specific duration.
	 */
	public static class ScreenMaskEvent {
		public File image;
		public double startTime, endTime;
		
		/**
		 * Determines if one MaskEvent conflicts (overlaps) with any other by comparing their start and end times.
		 */
		public boolean conflictsWithOther() {
			return screenMaskEvents.stream().anyMatch(e -> ((startTime < e.startTime) && (endTime > e.startTime)) ||
					((startTime > e.endTime) && (endTime < e.startTime)));
		}
	}
	
	/**
	 * Represents an "identity mask event", in which the labels of the moving objects are blacked out
	 * until they detect a mouse-over event.
	 */
	public static class IdentityMaskEvent {
		public double startTime;
	}
	
	/**
	 * Represents a "query", a prompt which appears on-screen at a specific time for a specific duration 
	 * and prompts the user to click somewhere on the screen or to enter a text response.
	 * @author Graham
	 *
	 */
	public static class Query {
		public String text;
		public boolean acceptsText; // If false, accepts a mouse click as input
		public double startTime, endTime;
		public boolean wait = false;
		public float x, y;
	}
	
	/**
	 * Determines the font size of the largest icon in the experimental model.
	 */
	public static void setLargestFontSize() {
		waypoints.values().stream().forEach(w -> { if (w.size > largestFontSize) { largestFontSize = w.size; } });
		objects.values().stream().forEach(o -> { if (o.size > largestFontSize) { largestFontSize = o.size; } });
	}
}
