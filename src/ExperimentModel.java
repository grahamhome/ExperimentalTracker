import java.util.ArrayList;

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
	
	public static enum Shape {
		CIRCLE,
		SQUARE,
		TRIANGLE,
		STAR,
		DEFAULT,
		NO_MATCH;
		
		public static Shape getShape(String shapeName) {
			switch(shapeName) {
				case "circle" : return CIRCLE;
				case "square" : return SQUARE;
				case "triangle" : return TRIANGLE;
				case "star" : return STAR;
				case "" : return DEFAULT;
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
			switch(colorName) {
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
		
		public Waypoint(String name, Shape shape, float x, float y, boolean visible) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.shape = shape;
			this.visible = visible;
		}
		
		@Override
		public boolean equals(Object waypoint) {
			if (!(waypoint instanceof Waypoint)) return false;
			Waypoint point = (Waypoint) waypoint;
			return ((x == point.x && y == point.y) || (x == y && x == point.y && y == point.x));
		}
	}
	
	public static class Connector {
		public Waypoint point1, point2;
		public int width;
		public Color color;
		
		public Connector(Waypoint point1, Waypoint point2, int width, Color color) {
			this.point1 = point1;
			this.point2 = point2;
			this.width = width;
			this.color = color;
		}
		
		@Override
		public boolean equals(Object connector) {
			if (!(connector instanceof Connector)) return false;
			Connector connect = (Connector) connector;
			return ((point1.equals(connect.point1) && point2.equals(connect.point2)) || (point1.equals(connect.point2) && point2.equals(connect.point1)));
		}
	}
	
	public static class Mover {
		public String name, label;
		public Shape shape;
		public int angle, leaderLength, numDots;
		public float speed;
		public ArrayList<Waypoint> pathPoints;
		
		public Mover(String name, Shape shape, String label, int angle, int leaderLength, int numDots, ArrayList<Waypoint> pathPoints, float speed) {
			this.name = name;
			this.shape = shape;
			this.label = label;
			this.angle = angle;
			this.leaderLength = leaderLength;
			this.numDots = numDots;
			this.pathPoints = pathPoints;
			this.speed = speed;
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
}
