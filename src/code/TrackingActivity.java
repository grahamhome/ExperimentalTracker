package code;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.javafx.geom.BaseBounds.BoundsType;
import com.sun.javafx.image.impl.BaseIntToByteConverter;

import code.ExperimentModel.Connector;
import code.ExperimentModel.TextObject;
import code.ExperimentModel.MovingObjectLabel;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.WaypointObject;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
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
	private Map map = new Map();
	private Rectangle2D bounds;
	private double stageWidth, stageHeight, mapOffsetX, mapOffsetY, mapHeight, mapWidth;
	private HashMap<WaypointObject, GraphicalStationaryObject> waypoints = new HashMap<>();
	private static final int FONT_SIZE = 30; // Determines the size of the waypoints & moving objects
	private URL iconFontURL,textFontURL;
	private ParallelTransition masterTransition = new ParallelTransition();
	
	public TrackingActivity() {
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
		map.drawObjects();
		stage.show();
		// TODO: Show intro message here
		masterTransition.play();
		// TODO: Show outro message here
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
			if (ExperimentModel.mapColor != null) {
				Rectangle map = new Rectangle(mapOffsetX,mapOffsetY,mapWidth,mapHeight);
				map.setFill(ExperimentModel.mapColor);
				root.getChildren().add(map);
			} else {
				Image mapImage = new Image(ExperimentModel.mapImage.toURI().toString());
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
			for (WaypointObject waypoint: ExperimentModel.waypoints.values()) {
				waypoints.put(waypoint, new GraphicalStationaryObject(waypoint));
			}
		}
		
		private void drawObjects() {
			for (MovingObject object : ExperimentModel.objects.values()) {
				new GraphicalMovingObject(object);
			}
		}
	}
	
	private abstract class GraphicalObject {
		private TextObject baseIcon;
		public Text graphicalIcon;
		public double x, y;
		
		public GraphicalObject(TextObject icon) {
			x = (icon.x*(mapWidth/ExperimentModel.x))+mapOffsetX;
			y = (icon.y*(mapHeight/ExperimentModel.y))+mapOffsetY;
			this.baseIcon = icon;
			this.graphicalIcon = drawText(baseIcon);
		}
		
		public Text drawText(TextObject textObject) {
			Text text;
			if (textObject.value != null) {
				text = new Text(textObject.value);
				text.setFill(textObject.color);
				if (textObject instanceof MovingObjectLabel) {
					text.setFont(Font.loadFont(textFontURL.toString(), textObject.size));
				} else {
					text.setFont(Font.loadFont(iconFontURL.toString(), textObject.size));
				}
			} else {
				text = new Text("");
			}
			text.setBoundsType(TextBoundsType.VISUAL);
			text.setWrappingWidth(200);
			text.setX(x-(text.getLayoutBounds().getWidth()/2));
			text.setY(y+(text.getLayoutBounds().getHeight()/2));
			root.getChildren().add(text);
			return text;
		}
	}
		
	private class GraphicalMovingObject extends GraphicalObject {
		private MovingObject object;
		private MovingObjectLabel label;
		private Text labelText;
		private Rectangle labelBackground;
		
		public GraphicalMovingObject(MovingObject object) {
			super(object);
			this.object = object;
			label = object.label;
			if (label != null) {
				labelText = drawText(label);
				setLabelRelativePosition();
				if (label.backgroundColor != Color.TRANSPARENT) {
					labelBackground = new Rectangle(
							labelText.getLayoutBounds().getMinX(), 
							labelText.getLayoutBounds().getMinY(),
							labelText.getLayoutBounds().getWidth(),
							labelText.getLayoutBounds().getHeight());
					labelBackground.setFill(label.backgroundColor);
					root.getChildren().add(labelBackground);
					labelText.toFront();
				}
			}
			generatePaths(object.pathPoints);
		}
		
		public void generatePaths(ArrayList<WaypointObject> pathPoints) {
			Path iconPath = new Path();
			iconPath.getElements().add(new MoveTo(graphicalIcon.getX(), graphicalIcon.getY()));
			Path labelPath = new Path();
			Path labelBackgroundPath = new Path();
			if (labelText != null) {
				labelPath.getElements().add(new MoveTo(labelText.getX(), labelText.getY()));
				if (labelBackground != null) {
					labelBackgroundPath.getElements().add(new MoveTo(labelBackground.getX(), labelBackground.getY()));
				}
			}
			double distance = 0;
			WaypointObject previous = pathPoints.get(0);
			for (WaypointObject waypointObject : pathPoints) {
				GraphicalStationaryObject waypoint = waypoints.get(waypointObject);
				distance += Math.sqrt(Math.pow(waypointObject.x-previous.x,2)+Math.pow(waypointObject.y-previous.y,2));
				previous = waypointObject;
				iconPath.getElements().add(new LineTo(waypoint.x, waypoint.y));
				/*if (labelText != null) {
					double[] coords = getLabelRelativePosition(newX, newY);
					labelPath.getElements().add(new LineTo(coords[0], coords[1]));
					if (labelBackground != null) {
						labelBackgroundPath.getElements().add(new LineTo(coords[0], coords[1]));
					}
				}*/
			}
			PathTransition iconPathTransition = new PathTransition();
			iconPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
			iconPathTransition.setPath(iconPath);
			iconPathTransition.setNode(graphicalIcon);
			iconPathTransition.setInterpolator(Interpolator.LINEAR);
			System.out.println("Distance: " + distance);
			System.out.println("Speed: " + object.speed);
			System.out.println("Duration (mins): " + (distance/object.speed)*60);
			iconPathTransition.setDuration(Duration.minutes((distance/object.speed)*60));
			masterTransition.getChildren().add(iconPathTransition);
			/*if (labelText != null) {
				PathTransition labelPathTransition = new PathTransition();
				labelPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
				labelPathTransition.setPath(labelPath);
				labelPathTransition.setNode(labelText);
				labelPathTransition.setInterpolator(Interpolator.LINEAR);
				labelPathTransition.setDuration(new Duration((distance/speed)*60*60*1000));
				masterTransition.getChildren().add(labelPathTransition);
				if (labelBackground != null) {
					PathTransition labelBackgroundPathTransition = new PathTransition();
					labelBackgroundPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
					labelBackgroundPathTransition.setPath(labelPath);
					labelBackgroundPathTransition.setNode(labelText);
					labelBackgroundPathTransition.setInterpolator(Interpolator.LINEAR);
					labelBackgroundPathTransition.setDuration(new Duration((distance/speed)*60*60*1000));
					masterTransition.getChildren().add(labelBackgroundPathTransition);
				}
			}*/
		}
		
		public void setLabelRelativePosition() {
			switch (label.position) {
				case RIGHT:
					labelText.setX(labelText.getX()-(label.size*label.value.length()/2.5));
					break;
				case LEFT:
					labelText.setX(labelText.getX()+(label.size*label.value.length()/3.2));
					break;
				case ABOVE:
					labelText.setY(labelText.getY()-label.size);
					break;
				case BELOW:
					labelText.setY(labelText.getY()+label.size);
					break;
			}
		}
	}
	
	/**
	 * Visually represents a waypoint.
	 */
	private class GraphicalStationaryObject extends GraphicalObject {
		public GraphicalStationaryObject(WaypointObject waypoint) {
			super(waypoint);
			drawConnectors(waypoint);
		}
		
		/**
		 * Draws all Connectors leading from this Waypoint to others.
		 * @param waypoint : The Waypoint which this object is built from.
		 */
		private void drawConnectors(WaypointObject waypoint) {
			for (Connector connector: waypoint.connectors) {
				GraphicalStationaryObject destination = waypoints.get(connector.destination);
				Line line = new Line(x, y, destination.x, destination.y);
				line.setStroke(connector.color);
				line.setStrokeWidth(connector.width);
				root.getChildren().add(line);
				destination.graphicalIcon.toFront();
			}
			graphicalIcon.toFront();
		}
	}
}
