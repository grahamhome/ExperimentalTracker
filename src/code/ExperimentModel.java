package code;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javafx.scene.paint.Color;

/**
 * This class represents the configuration for an MIT experiment.
 * @author Graham Home <grahamhome333@gmail.com>
 */
public class ExperimentModel {
	
	public static String name;
	private static String participantId;
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
	private static StringBuilder report = new StringBuilder();
	private static long startTime = System.currentTimeMillis();
	private static String lastClickTime = "";
	private static String reportFileName;
	static {
		Calendar cal = Calendar.getInstance();
		reportFileName = new StringBuilder()
				.append(cal.get(Calendar.MONTH)+1)
				.append("-")
				.append(cal.get(Calendar.DAY_OF_MONTH))
				.append("-")
				.append(cal.get(Calendar.YEAR))
				.append("-")
				.append(name)
				.append("-")
				.append(participantId)
				.append(".csv").toString();
	}
	
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
	 * Add the elapsed time to the current line of the experiment report.
	 */
	private static String reportTime() {
		long elapsedMillis = System.currentTimeMillis()-startTime;
		long hours = elapsedMillis/3600000;
		long remainder = elapsedMillis%3600000;
		long minutes = remainder/60000;
		remainder = remainder%6000;
		long seconds = remainder/1000;
		long millis = Math.round((double)remainder%1000);
		
		StringBuilder time = new StringBuilder();
		for (long value : Arrays.asList(hours, minutes, seconds)) {
			time.append(value > 0 ? value : "00");
			time.append(":");
		}
		time.append(millis > 0 ? millis : "000");
		time.append(",");
		report.append(time.toString());
		return time.toString();
	}
	
	/**
	 * Add the appearance or disappearance of a screen mask to the experiment report.
	 * @param mask : The ScreenMaskEvent to report.
	 * @param start : True to log the mask appearance, false to log the mask disappearance.
	 */
	public static void reportMask(ScreenMaskEvent mask, boolean show) {
		reportTime();
		report.append("Mask Event");
		report.append(",");
		report.append(show ? "Appearance" : "Disappearance");
		report.append(",");
		report.append(mask.image.getName());
		report.append(System.lineSeparator());
	}
	
	/**
	 * Add the appearance or disappearance of a query to the experiment report.
	 * @param query : The QueryEvent to report.
	 * @param show : True to log the query appearance, false to log the query disappearance.
	 */
	public static void reportQuery(Query query, boolean show) {
		String queryTime = reportTime();
		report.append(query.acceptsText ? "Text Query " : "Click Query ");
		report.append(show ? "Appearance" : "Disappearance");
		report.append(",");
		report.append(query.text.replaceAll(",", ""));
		report.append(System.lineSeparator());
		if (query.mask) {
			report.append(queryTime);
			report.append("Identity Mask Event");
			report.append(",");
			report.append(show ? "Appearance" : "Disappearance");
			report.append(System.lineSeparator());
		}
	}
	
	/**
	 * Add a text entry to the experiment report.
	 * @param value : The text entered by the user.
	 */
	public static void reportTextEntry(String value) {
		reportTime();
		report.append("Text Entry");
		report.append(",");
		report.append(value.replaceAll(",", ""));
		report.append(System.lineSeparator());
	}
	
	/**
	 * Add a click event to the experiment report.
	 * @param click : The Click to report.
	 */
	public static void reportClick(Query.Click click) {
		lastClickTime = reportTime();
		report.append("Click");
		report.append(",");
		report.append(click.x);
		report.append(",");
		report.append(click.y);
		report.append(System.lineSeparator());
	}
	
	/**
	 * Add an Object Hit event to the experiment report.
	 * @param label : The text of the object's label.
	 * @param distance : The distance to the object from the click location.
	 */
	public static void reportObjectHit(String label, float distance) {
		report.append(lastClickTime);
		report.append("Object Hit");
		report.append(",");
		report.append(label);
		report.append(",");
		report.append(distance);
		report.append(System.lineSeparator());
	}
	
	/**
	 * Add an Identity Viewed event to the experiment report.
	 * @param label : The text of the object's label.
	 */
	public static void reportIdentityViewed(String label) {
		reportTime();
		report.append("Object Identity Viewed");
		report.append(",");
		report.append(label);
		report.append(System.lineSeparator());
	}
	
	/**
	 * Writes the report to the report file.
	 */
	public static void writeReport() {
		String commonHeader = "Time (all events), Event Type (all events)" + System.lineSeparator();
		String maskHeader = ",,Appearance/Disappearance (mask events),Mask Image Name (mask events)" + System.lineSeparator();
		String queryHeader = ",,Appearance/Disappearance (query events),Query Text (query events)" + System.lineSeparator();
		String identityMaskHeader = ",,Appearance/Disappearance (identity mask events)" + System.lineSeparator();
		String textEntryHeader = ",,Text Entered (text entry events)" + System.lineSeparator();
		String clickHeader = ",,X Value in Nautical Miles (click events),Y value in Nautical Miles (click events)" + System.lineSeparator();
		String hitHeader =",,Label of Hit Object (object hit events),Distance to Object Center in Nautical Miles (object hit events)" + System.lineSeparator();
		String identityViewedEvent = ",,Label of Object Viewed (identity viewed events)" + System.lineSeparator();
		try {
			PrintWriter reportWriter = new PrintWriter(reportFileName, "UTF-8");
			for (String header : Arrays.asList(commonHeader, maskHeader, queryHeader, identityMaskHeader, textEntryHeader, clickHeader, hitHeader, identityViewedEvent)) {
				reportWriter.write(header);
			}
			reportWriter.write(report.toString());
			reportWriter.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
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
		public Click responseClick;
		public TextEntry responseText;
		public HashMap<MovingObject, Double> mousedOverMovingObjects = new HashMap<>();
		public boolean mask = false;
		
		/**
		 * Determines if one Query conflicts (overlaps) with any other by comparing their start and end times.
		 */
		public boolean conflictsWithOther() {
			return queries.stream().anyMatch(e -> 
					((!e.wait) && ((startTime < e.startTime) && (endTime > e.startTime))) ||
					((!wait) && ((startTime > e.endTime) && (endTime < e.startTime))));
		}
		
		/**
		 * Represents a click performed in response to a query.
		 */
		public class Click {
			public double time;
			public float x, y;
			public ArrayList<TextObject> nearbyObjects;
			
			public Click(float x, float y, double time) {
				this.x = x;
				this.y = y;
				this.time = time - startTime;
				nearbyObjects = new ArrayList<>();
			}
		}
		
		/**
		 * Represents a text entry given in response to a query.
		 */
		public class TextEntry {
			public double time;
			public String value;
			
			public TextEntry(String value, double time) {
				this.value = value;
				this.time = time;
			}
		}
		
	}
	
	
	
	/**
	 * Determines the font size of the largest icon in the experimental model.
	 */
	public static void setLargestFontSize() {
		waypoints.values().stream().forEach(w -> { if (w.size > largestFontSize) { largestFontSize = w.size; } });
		objects.values().stream().forEach(o -> { if (o.size > largestFontSize) { largestFontSize = o.size; } });
	}
}
