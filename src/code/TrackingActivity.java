package code;

import java.util.HashMap;

import code.ExperimentModel.MovingObject;
import code.ExperimentModel.Waypoint;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
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
	private static final int FONT_SIZE = 30;
	
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
		map.drawObjects();
		
	}
	
	/**
	 * Represents the map containing waypoints, connectors and movers.
	 *
	 */
	private class Map {
		
		private Rectangle map;
		
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
			Rectangle map = new Rectangle(mapOffsetX,mapOffsetY,mapWidth,mapHeight);
			map.setFill(Color.LIGHTBLUE);
			root.getChildren().add(map);
			// Adjust map dimensions to allow waypoints & objects to be placed correctly
			mapWidth -= (FONT_SIZE*2/3);
			mapHeight -= FONT_SIZE;
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
				for (int j=1; j<object.pathPoints.size(); j++) {
					Waypoint waypoint = object.pathPoints.get(j);
					path.getElements().add(new LineTo(waypoints.get(waypoint).getX(), waypoints.get(waypoint).getY()));
				}
				PathTransition transition = new PathTransition();
				transition.setDuration(Duration.millis(8000)); //TODO: replace with object speed*object distance
				transition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
				transition.setPath(path);
				transition.setNode(text);
				transition.play();
			}
		}
		
		private Text drawText(double x, double y, Character symbol) {
			x = (x*(mapWidth/model.x))+mapOffsetX;
			y = (y*(mapHeight/model.y))+mapOffsetY+FONT_SIZE*4/5;
			Text t = new Text(x, y, (symbol).toString());
			t.setFont(new Font(FONT_SIZE));
			t.setBoundsType(TextBoundsType.VISUAL);
			t.setWrappingWidth(200);
			root.getChildren().add(t);
			return t;
		}
	}
}
