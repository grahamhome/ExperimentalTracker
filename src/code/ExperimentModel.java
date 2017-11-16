package code;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents the configuration for an MIT experiment.
 * @author Graham Home <grahamhome333@gmail.com>
 *
 */
public class ExperimentModel {
	
	public String name;
	public float x, y, updateRate;
	public double duration;
	public ArrayList<Waypoint> waypoints = new ArrayList<>();
	public ArrayList<Connector> connectors = new ArrayList<>();
	public ArrayList<Mover> movers = new ArrayList<>();
	public ArrayList<Interruption> interruptions = new ArrayList<>();
	public ArrayList<Query> queries = new ArrayList<>();
	
	public static enum Shape {
		CIRCLE,
		SQUARE,
		TRIANGLE,
		STAR,
		NO_MATCH;
		
		public static Shape getShape(String shapeName) {
			switch (shapeName) {
				case "circle" : return CIRCLE;
				case "square" : return SQUARE;
				case "triangle" : return TRIANGLE;
				case "star" : return STAR;
				default : return NO_MATCH;
			}
		}
	}
	
	public static enum Color {
		RED,
		ORANGE,
		YELLOW,
		GREEN,
		BLUE,
		PURPLE,
		BLACK,
		WHITE,
		NO_MATCH;
		
		public static Color getColor(String colorName) {
			switch (colorName) {
				case "red" : return RED;
				case "orange" : return ORANGE;
				case "yellow" : return YELLOW;
				case "green" : return GREEN;
				case "blue" : return BLUE;
				case "purple" : return PURPLE;
				case "black" : return BLACK;
				case "white" : return WHITE;
				default : return NO_MATCH;
			}
		}
	}
	
	public static class Waypoint {
		public String name;
		public float x, y;
		public Shape shape;
		public boolean visible;
		
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
	
	public static class Connector {
		public Waypoint point1, point2;
		public int width;
		public Color color;
		
		public static final int maxWidth = 10;
		
		@Override
		public boolean equals(Object connectorToCompare) {
			if (!(connectorToCompare instanceof Connector)) return false;
			Connector connector = (Connector) connectorToCompare;
			return ((point1.equals(connector.point1) && point2.equals(connector.point2)) || (point1.equals(connector.point2) && point2.equals(connector.point1)));
		}
	}
	
	public static class Mover {
		public String label;
		public Shape shape;
		public int angle, leaderLength, numDots;
		public float speed;
		public ArrayList<Waypoint> pathPoints = new ArrayList<>();
		
		public static final List<Integer> labelAngles = Arrays.asList(new Integer[] {0, 45, 90, 135, 180, 225, 270, 315, 360});
		public static final int maxLeaderLength = 10;
		public static final int maxDots = 10;
		
		@Override
		public boolean equals(Object moverToCompare) {
			if (!(moverToCompare instanceof Mover)) return false;
			Mover mover = (Mover) moverToCompare;
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
	
	public static class Interruption {
		public File image;
		public double startTime;
		public double duration;
	}
	
	public static class Query {
		public String text;
		public boolean visual; // If false: audio
		public double startTime;
		public double duration;
		public boolean wait = false;
	}
}
