package code;

import java.util.HashMap;

import code.ExperimentModel.Connector;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.Waypoint;
import javafx.animation.Interpolator;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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
	
	public TrackingActivity(ExperimentModel model) {
		this.model = model;
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.hide();
		stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
		root = new Group();
		stage.setFullScreen(true);
		scene = new Scene(root, stage.getWidth(), stage.getHeight(), Color.BLACK);
		stage.setScene(scene);
		bounds = Screen.getPrimary().getBounds();
		stageWidth = bounds.getWidth();
		stageHeight = bounds.getHeight();
		map.drawMap();
		stage.show();
		map.drawWaypoints();
		map.drawConnectors();
		map.drawObjects();
	}
	
	/**
	 * Represents the map containing waypoints, connectors and movers.
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
			// TODO: use waypoint name instead of integer, add import rule to ensure it's a single character
			// & add Wingdings support
			Integer i = 1;
			for (Waypoint waypoint: model.waypoints) {
				waypoints.put(waypoint, drawText(waypoint.x, waypoint.y, (i++).toString().charAt(0)));
			}
		}
		
		private void drawObjects() {
			// TODO: use object name instead of integer, add import rule to ensure it's a single character
			// & add Wingdings support
			Integer i = 6;
			for (MovingObject object : model.objects) {
				Text text = drawText(object.pathPoints.get(0).x, object.pathPoints.get(0).y, (i++).toString().charAt(0));
				Path path = new Path();
				path.getElements().add(new MoveTo(text.getX(), text.getY()));
				double x = text.getX();
				double y = text.getY();
				double distance = 0;
				for (int j=1; j<object.pathPoints.size(); j++) { // TODO: Calculate distances correctly, and calibrate object positions
					Waypoint waypoint = object.pathPoints.get(j); // also TODO: use concurrent transitions to build overall animation, possibly sequential transitions for individual animations
					double newX = waypoints.get(waypoint).getX(); // also, also TODO: build up a hashmap of all paths between all points & use them to create animations rather than making duplicate paths?
					double newY =  waypoints.get(waypoint).getY();
					path.getElements().add(new LineTo(newX, newY));
					distance += Math.sqrt(Math.pow((newX-x), 2) + Math.pow(newY-y, 2));
					x = newX;
					y = newY;
				}
				PathTransition transition = new PathTransition();
				transition.setDuration(Duration.millis(i%2==0 ? 16000 : 12000)); // TODO: replace with object speed/object distance
				transition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
				transition.setPath(path);
				transition.setNode(text);
				transition.setInterpolator(Interpolator.LINEAR);
				transition.play();
			}
		}
		
		private void drawConnectors() {
			for (Connector connector: model.connectors) {
				Path path = new Path();
				path.getElements().add(new MoveTo(waypoints.get(connector.point1).getX(), waypoints.get(connector.point1).getY()));
				path.getElements().add(new LineTo(waypoints.get(connector.point2).getX(), waypoints.get(connector.point2).getY()));
				path.setStroke(connector.color);
				path.setStrokeWidth(6);
				root.getChildren().add(path);
			}
		}
		
		private Text drawText(double x, double y, Character symbol) {
			x = (x*(mapWidth/model.x))+mapOffsetX;
			y = (y*(mapHeight/model.y))+mapOffsetY;
			Text t = new Text(x, y, (symbol).toString());
			t.setFont(new Font(FONT_SIZE));
			t.setFill(Color.WHITE);
			t.setBoundsType(TextBoundsType.VISUAL);
			t.setWrappingWidth(200);
			root.getChildren().add(t);
			Path p = new Path();
			p.getElements().add(new MoveTo(0,0));
			p.getElements().add(new LineTo(x,y));
			PathTransition pt = new PathTransition();
			pt.setPath(p);
			pt.setNode(t);
			pt.play();
			return t;
		}
	}
}
