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

import code.TrackingActivity.SchedulableEvent;
import javafx.scene.paint.Color;

/**
 * This class represents the configuration for an MIT experiment.
 * @author Graham Home <grahamhome333@gmail.com>
 */
public class ExperimentModel {
	
	public static String name;
	public static String participantId;
	public static float x, y, largestFontSize;
	public static int loopCount;
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
	public static ArrayList<SchedulableEvent> events = new ArrayList<>();
	private static StringBuilder report = new StringBuilder();
	private static long startTime;
	private static String lastClickTime;
	
	private static String getReportFileName() {
		Calendar cal = Calendar.getInstance();
		return new StringBuilder()
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
		events = new ArrayList<>();
	}
	
	/**
	 * Add the elapsed time to the current line of the experiment report.
	 */
	private static String reportTime() {
		long elapsedMillis = System.currentTimeMillis()-startTime;
		long hours = elapsedMillis/3600000;
		long remainder = elapsedMillis%3600000;
		long minutes = remainder/60000;
		remainder = remainder%60000;
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
	
	public static void reportFreeze(boolean frozen) {
		reportTime();
		report.append("Moving Objects ")
		.append(frozen ? "Frozen" : "Unfrozen")
		.append(System.lineSeparator());
	}
	
	public static void reportStatus(boolean started) {
		if (started) {
			startTime = System.currentTimeMillis();
			report.append("00:00:00:000,");
		} else {
			reportTime();
		}
		report.append("Experiment ")
		.append(started ? "Started" : "Stopped")
		.append(System.lineSeparator());
	}
	
	public static void reportLoop(int loopNumber) {
		reportTime();
		report.append("Loop ")
		.append(loopNumber)
		.append(" Started")
		.append(",")
		.append(System.lineSeparator());
	}
	
	/**
	 * Add the appearance or disappearance of a screen mask to the experiment report.
	 * @param mask : The ScreenMaskEvent to report.
	 * @param start : True to log the mask appearance, false to log the mask disappearance.
	 */
	public static void reportMask(ScreenMaskEvent mask, boolean show) {
		reportTime();
		report.append("Mask ")
		.append(show ? "Appearance" : "Disappearance")
		.append(",")
		.append(mask.image.getName())
		.append(System.lineSeparator());
	}
	
	/**
	 * Add the appearance or disappearance of an identity mask to the experiment report.
	 * @param mask : The IdentityMaskEvent to report.
	 * @param start : True to log the mask appearance, false to log the mask disappearance.
	 */
	public static void reportIdentityMask(boolean show) {
		reportTime();
		report.append("Identity Mask ")
		.append(show ? "Appearance" : "Disappearance")
		.append(System.lineSeparator());
	}
	
	/**
	 * Add the appearance or disappearance of a query to the experiment report.
	 * @param query : The QueryEvent to report.
	 * @param show : True to log the query appearance, false to log the query disappearance.
	 */
	public static void reportQuery(Query query, boolean show) {
		reportTime();
		report.append(query instanceof FindQuery ? "Click Query " : 
			query instanceof TextResponseQuery ? "Text Query " : "Yes/No Query ")
		.append(show ? "Appearance" : "Disappearance")
		.append(",")
		.append(query.text)
		.append(System.lineSeparator());
	}
	
	/**
	 * Add a text entry to the experiment report.
	 * @param value : The text entered by the user.
	 */
	public static void reportTextEntry(TextResponseQuery query) {
		reportTime();
		report.append("Text Entry")
		.append(",")
		.append(query.value.replaceAll(",", ""))
		.append(System.lineSeparator());
	}
	
	/**
	 * Add a click event to the experiment report.
	 * @param click : The Click to report.
	 */
	public static void reportClick(FindQuery query) {
		lastClickTime = reportTime();
		report.append("Click")
		.append(",")
		.append(String.format("%2.3f", query.x))
		.append(",")
		.append(String.format("%2.3f",query.y))
		.append(System.lineSeparator());
	}
	
	/**
	 * Add a yes/no response event to the experiment report.
	 */
	public static void reportBinaryQueryResponse(BinaryQuery query) {
		reportTime();
		report.append("Yes/No Response")
		.append(",")
		.append(query.response ? "Yes" : "No")
		.append(System.lineSeparator());
	}
	
	/**
	 * Add an Object Hit event to the experiment report.
	 * @param label : The text of the object's label.
	 * @param distance : The distance to the object from the click location.
	 */
	public static void reportObjectHit(String label, double distance) {
		report.append(lastClickTime)
		.append("Object Hit")
		.append(",")
		.append(label)
		.append(",")
		.append(String.format("%2.3f", distance))
		.append(System.lineSeparator());
	}
	
	/**
	 * Add an Identity Viewed event to the experiment report.
	 * @param label : The text of the object's label.
	 */
	public static void reportIdentityViewed(String label) {
		reportTime();
		report.append("Object Identity Viewed")
		.append(",")
		.append(label)
		.append(System.lineSeparator());
	}
	
	/**
	 * Writes the report to the report file.
	 */
	public static void writeReport() {
		String commonHeader = "Time (all events), Event Type (all events)" + System.lineSeparator();
		String maskHeader = ",Mask Image Name (mask events)" + System.lineSeparator();
		String queryHeader = ",,Query Text (query events)" + System.lineSeparator();
		String textEntryHeader = ",,Text Entered (text entry events)" + System.lineSeparator();
		String clickHeader = ",,X Value in Nautical Miles (click events),Y value in Nautical Miles (click events)" + System.lineSeparator();
		String hitHeader =",,Label of Hit Object (object hit events),Distance to Object in Nautical Miles (object hit events)" + System.lineSeparator();
		String identityViewedEvent = ",,Label of Object Viewed (identity viewed events)" + System.lineSeparator();
		String binaryResponseHeader = ",,Response" + System.lineSeparator();
		try {
			PrintWriter reportWriter = new PrintWriter(getReportFileName(), "UTF-8");
			for (String header : Arrays.asList(commonHeader, maskHeader, queryHeader, textEntryHeader, clickHeader, binaryResponseHeader, hitHeader, identityViewedEvent)) {
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
				return ((x == waypoint.x && y == waypoint.y));
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
		public int loopNumber;
		
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
		public double startTime, endTime;
		public int loopNumber;
	}
	
	/**
	 * Represents a "query", a prompt which appears on-screen at a specific time for a specific duration 
	 * and prompts the user to click somewhere on the screen or to enter a text response.
	 * @author Graham
	 *
	 */
	public static class Query {
		public String text;
		public double startTime, endTime, responseTime;
		public boolean wait = false;
		public int loopNumber;
		public boolean freeze = false; // Whether or not the moving objects should be frozen during the query
		public float positionX, positionY;
		public boolean maskIdentities = false;
		
		/**
		 * Determines if one Query conflicts (overlaps) with any other by comparing their start and end times.
		 */
		public boolean conflictsWithOther() {
			return queries.stream().anyMatch(e -> 
					((!e.wait) && ((startTime < e.startTime) && (endTime > e.startTime))) ||
					((!wait) && ((startTime > e.endTime) && (endTime < e.startTime))));
		}
		
		public void setResponseTime(double time) {
			responseTime = time;
		}
	}
	
	public static class FindQuery extends Query {
		public float x, y;
		public ArrayList<TextObject> nearbyObjects = new ArrayList<>();
		
		public void respond(float x, float y, double time) {
			this.x = x;
			this.y = y;
			super.setResponseTime(time);
		}
	}
	
	public static class TextResponseQuery extends Query {
		public String value;
		
		public void respond(String value, double time) {
			this.value = value;
			super.setResponseTime(time);
		}
	}
	
	public static class BinaryQuery extends Query {
		public Boolean response;
		
		public void respond(boolean result, double time) {
			this.response = result;
			super.setResponseTime(time);
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
