package code;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.javafx.geom.BaseBounds.BoundsType;

import code.ExperimentModel.Connector;
import code.ExperimentModel.Icon;
import code.ExperimentModel.Label;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.Waypoint;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextBoundsType;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Responsible for displaying animated map, interruptions & queries and collecting interaction data.
 * @author Graham Home
 *
 */
public class TrackingActivity extends Application {
	
	private Stage stage;
	private Scene scene;
	private Group root;
	private ExperimentModel model;
	private Map map = new Map();
	private Rectangle2D bounds;
	private double stageWidth, stageHeight, mapOffsetX, mapOffsetY, mapHeight, mapWidth;
	private HashMap<Waypoint, Text> waypoints = new HashMap<>();
	private static final int FONT_SIZE = 30; // Determines the size of the waypoints & moving objects
	private URL iconFontURL,textFontURL;
	private ParallelTransition masterTransition = new ParallelTransition();
	
	public TrackingActivity(ExperimentModel model) {
		this.model = model;
		iconFontURL = TrackingActivity.class.getResource("/Font-Awesome-5-Free-Solid-900.otf");
		textFontURL = TrackingActivity.class.getResource("/segoeui.ttf");
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.hide();
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		stage.setFullScreen(true);
		root = new Group();
		scene = new Scene(root, stage.getWidth(), stage.getHeight(), Color.BLACK);
		stage.setScene(scene);
		bounds = Screen.getPrimary().getBounds();
		stageWidth = bounds.getWidth();
		stageHeight = bounds.getHeight();
		map.drawMap();
		map.drawWaypoints();
		map.drawConnectors();
		map.drawObjects();
		stage.show();
		// TODO: Show intro message here
		masterTransition.play();
	}
	
	/**
	 * Represents the map containing waypoints, connectors and moving objects.
	 *
	 */
	private class Map {
		
		private void drawMap() {
			if (stageWidth > stageHeight) {
				mapWidth = (mapHeight = stageHeight*0.98);
				mapOffsetX = (stageWidth-mapWidth)/2;
				mapOffsetY = stageHeight*0.01;
			} else {
				mapHeight = (mapWidth = stageWidth*0.98);
				mapOffsetX = stageWidth*0.01;
				mapOffsetY = (stageHeight-mapHeight)/2;
			}
			if (model.mapColor != null) {
				Rectangle map = new Rectangle(mapOffsetX,mapOffsetY,mapWidth,mapHeight);
				map.setFill(model.mapColor);
				root.getChildren().add(map);
			} else {
				Image mapImage = new Image(model.mapImage.toURI().toString());
				ImageView map = new ImageView(mapImage);
				map.setPreserveRatio(true);
				map.setFitWidth(mapWidth);
				map.setFitHeight(mapHeight);
				map.setX(mapOffsetX);
				map.setY(mapOffsetY);
				root.getChildren().add(map);
			}
				
			// Adjust map dimensions to allow waypoints & objects to be placed correctly
			mapWidth -= FONT_SIZE;
			mapHeight -= FONT_SIZE;
			mapOffsetY += FONT_SIZE/2;
			mapOffsetX += FONT_SIZE/2;
		}
		
		private void drawWaypoints() {
			for (Waypoint waypoint: model.waypoints) {
				new GraphicalStationaryObject(waypoint);
			}
		}
		
		private void drawObjects() {
			for (MovingObject object : model.objects) {
				new GraphicalMovingObject(object);
			}
		}
		
		private void drawConnectors() {
			for (Connector connector: model.connectors) {
				Line line = new Line(waypoints.get(connector.point1).getX(),
						waypoints.get(connector.point1).getY(), 
						waypoints.get(connector.point2).getX(), 
						waypoints.get(connector.point2).getY());
				line.setStroke(connector.color);
				line.setStrokeWidth(connector.width);
				root.getChildren().add(line);
			}
			for (Text t : waypoints.values()) {
				t.toFront();
			}
		}
	}
	
	private abstract class GraphicalObject {
		private Icon icon;
		public double x, y;
		
		public GraphicalObject(Icon icon) {
			this.icon = icon;
			x = (icon.x*(mapWidth/model.x))+mapOffsetX;
			y = (icon.y*(mapHeight/model.y))+mapOffsetY;
		}
		
		public Text drawIcon(Icon icon) {
			Text t;
			if (icon.iconCode != null) {
				t = new Text(x, y, Character.toString(Character.toChars(Integer.parseInt(icon.iconCode, 16))[0]));
				t.setFill(icon.color);
			} else {
				t = new Text(x, y, "");
			}
			t.setFont(Font.loadFont(iconFontURL.toString(), icon.size));
			t.setWrappingWidth(200);
			t.setBoundsType(TextBoundsType.VISUAL);
			placeText(t);
			root.getChildren().add(t);
			return t;
		}
		
		public void placeText(Text text) {
			// This is needed because Shapes are placed differently when they are moved along a path
			// TODO: replace this with logic to place the node using its center 
			Path path = new Path();
			path.getElements().add(new MoveTo(text.getX()-1, text.getY()-1));
			path.getElements().add(new LineTo(text.getX(), text.getY()));
			PathTransition pt = new PathTransition(Duration.millis(1), path, text);
			pt.play();
		}
	}
		
	private class GraphicalMovingObject extends GraphicalObject {
		private MovingObject object;
		private Label label;
		private Text icon;
		private Text labelText;
		private Rectangle labelBackground;
		private float speed;
		
		public GraphicalMovingObject(MovingObject object) {
			super(object);
			this.object = object;
			this.icon = drawIcon(object);
			label = object.label;
			if (label != null) {
				double[] labelCoords = getLabelRelativePosition(x, y);
				labelText = new Text(labelCoords[0], labelCoords[1], label.text);
				placeText(labelText);
				labelText.setFont(Font.loadFont(textFontURL.toString(), label.size));
				labelText.setWrappingWidth(200);
				labelText.setBoundsType(TextBoundsType.VISUAL);
				labelText.setTextAlignment(TextAlignment.CENTER);
				labelText.setFill(label.foregroundColor);
				root.getChildren().add(labelText);
				if (label.backgroundColor != Color.TRANSPARENT) {
					Rectangle labelBackground = new Rectangle(
							labelText.getLayoutBounds().getMinX(), 
							labelText.getLayoutBounds().getMinY(),
							labelText.getLayoutBounds().getWidth(),
							labelText.getLayoutBounds().getHeight());
					labelBackground.setFill(label.backgroundColor);
					Path path = new Path();
					path.getElements().add(new MoveTo(labelText.getX()-1, labelText.getY()-1));
					path.getElements().add(new LineTo(labelText.getX(), labelText.getY()));
					PathTransition pt = new PathTransition(Duration.millis(1), path, labelBackground);
					pt.play();
					root.getChildren().add(labelBackground);
					labelText.toFront();
				}
			}
			//generatePaths(object.pathPoints);
		}
		
		public void generatePaths(ArrayList<Waypoint> pathPoints) {
			Path iconPath = new Path();
			iconPath.getElements().add(new MoveTo(x, y));
			Path labelPath = new Path();
			Path labelBackgroundPath = new Path();
			if (labelText != null) {
				labelPath.getElements().add(new MoveTo(labelText.getX(), labelText.getY()));
				if (labelBackground != null) {
					labelBackgroundPath.getElements().add(new MoveTo(labelBackground.getX(), labelBackground.getY()));
				}
			}
			double distance = 0;
			for (Waypoint waypoint : pathPoints) {
				double newX = waypoints.get(waypoint).getX(); // TODO: build up a hashmap of all paths between all points & use them to create animations rather than making duplicate paths?
				double newY = waypoints.get(waypoint).getY();
				distance += getRealDistance(x, y, newX, newY);
				iconPath.getElements().add(new LineTo(newX, newY));
				if (labelText != null) {
					double[] coords = getLabelRelativePosition(newX, newY);
					labelPath.getElements().add(new LineTo(coords[0], coords[1]));
					if (labelBackground != null) {
						labelBackgroundPath.getElements().add(new LineTo(coords[0], coords[1]));
					}
				}
				x = newX;
				y = newY;
			}
			PathTransition iconPathTransition = new PathTransition();
			iconPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
			iconPathTransition.setPath(iconPath);
			iconPathTransition.setNode(icon);
			iconPathTransition.setInterpolator(Interpolator.LINEAR);
			iconPathTransition.setDuration(new Duration(distance/speed));
			masterTransition.getChildren().add(iconPathTransition);
			if (labelText != null) {
				PathTransition labelPathTransition = new PathTransition();
				labelPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
				labelPathTransition.setPath(labelPath);
				labelPathTransition.setNode(labelText);
				labelPathTransition.setInterpolator(Interpolator.LINEAR);
				labelPathTransition.setDuration(new Duration(distance/speed));
				masterTransition.getChildren().add(labelPathTransition);
				if (labelBackground != null) {
					PathTransition labelBackgroundPathTransition = new PathTransition();
					labelBackgroundPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
					labelBackgroundPathTransition.setPath(labelPath);
					labelBackgroundPathTransition.setNode(labelText);
					labelBackgroundPathTransition.setInterpolator(Interpolator.LINEAR);
					labelBackgroundPathTransition.setDuration(new Duration(distance/speed));
					masterTransition.getChildren().add(labelPathTransition);
				}
			}
		}
		
		public double[] getLabelRelativePosition(double targetX, double targetY) {
			switch (label.position) {
				case RIGHT:
					return new double[] {targetX+label.size*label.text.length()/2.5, targetY};
				case LEFT:
					return new double[] {targetX-label.size*label.text.length()/3.2, targetY};
				case ABOVE:
					return new double[] {targetX, targetY-label.size};
				case BELOW:
					return new double[] {targetX, targetY+label.size};
				default:
					return new double[] {targetX, targetY};
			}
		}
	}
	
	private class GraphicalStationaryObject extends GraphicalObject {
		public GraphicalStationaryObject(Waypoint waypoint) {
			super(waypoint);
			waypoints.put(waypoint, drawIcon(waypoint));
		}
	}
	
	/**
	 * Given a set of coordinates in pixels, returns the distance between them in nautical miles.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public double getRealDistance(double x1, double y1, double x2, double y2) {
		if (model.x > model.y) {
			return Math.sqrt(Math.pow(((x2-x1)*(model.x/model.y)), 2)+Math.pow((y1-y2),2));
		} else if (model.y > model.x) {
			return Math.sqrt(Math.pow((x2-x1), 2)+Math.pow(((y1-y2)*(model.y/model.x)),2));
		} else {
			return Math.sqrt(Math.pow((x2-x1), 2)+Math.pow((y1-y2),2));
		}
	}
}
