package code;

import code.ExperimentModel.Waypoint;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;

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
	private static final int FONT_SIZE = 22;
	
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
		scene = new Scene(root, stage.getWidth(), stage.getHeight());
		stage.setScene(scene);
		bounds = Screen.getPrimary().getBounds();
		stageWidth = bounds.getWidth();
		stageHeight = bounds.getHeight();
		map.drawMap();
		map.drawWaypoints();
		stage.show();
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
			mapWidth -= (FONT_SIZE*3/4);
			mapHeight -= FONT_SIZE;
		}
		
		private void drawWaypoints() {
			Integer i = 1;
			for (Waypoint waypoint: model.waypoints) {
				double x = (waypoint.x*(mapWidth/model.x))+mapOffsetX;
				double y = (waypoint.y*(mapHeight/model.y))+mapOffsetY+FONT_SIZE;
				Text t = new Text(x, y, (i++).toString());
				t.setFont(new Font(30));
				t.setWrappingWidth(200);
				t.setTextAlignment(TextAlignment.LEFT);
				root.getChildren().add(t);
				
				/*switch (waypoint.shape) {
				case CIRCLE:
					Circle circle = new Circle(waypoint.x+(bounds.getWidth()*0.1), waypoint.y+(bounds.getHeight()*0.1), 10);
					circle.setFill(Color.RED);
					root.getChildren().add(circle);
					break;
				case SQUARE:
					Rectangle rectangle = new Rectangle((waypoint.x*10)+(bounds.getWidth()*0.1), (waypoint.y*10)+(bounds.getHeight()*0.1), 10, 10);
					rectangle.setFill(Color.RED);
					root.getChildren().add(rectangle);
					break;
				case STAR:
					break;
				case TRIANGLE:
					break;
				default:
					break;
				
				}*/
			}
		}
	}
}
