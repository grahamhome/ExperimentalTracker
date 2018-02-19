package code;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import code.ExperimentModel.Connector;
import code.ExperimentModel.MaskEvent;
import code.ExperimentModel.TextObject;
import code.ExperimentModel.MovingObjectLabel;
import code.ExperimentModel.Query;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.WaypointObject;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
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
		map.scheduleMaskAppearances();
		map.scheduleQueryAppearances();
		stage.show();
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
		
		public void scheduleMaskAppearances() {
			for (MaskEvent event : ExperimentModel.maskEvents) {
				Rectangle maskBackground = new Rectangle(stageWidth, stageHeight);
				maskBackground.setFill(Color.BLACK);
				Image maskImage = new Image(event.image.toURI().toString());
				ImageView mask = new ImageView(maskImage);
				mask.setPreserveRatio(true);
				mask.setFitWidth(mapWidth);
				mask.setFitHeight(mapHeight);
				mask.setX(mapOffsetX+((mapWidth-mask.getLayoutBounds().getWidth())/2));
				mask.setY(mapOffsetY+((mapHeight-mask.getLayoutBounds().getHeight())/2));
				maskBackground.toFront();
				mask.toFront();
				maskBackground.setOpacity(0);
				mask.setOpacity(0);
				root.getChildren().addAll(maskBackground, mask);
				ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
				service.schedule(new Runnable() {
					@Override
					public void run() {
						maskBackground.setOpacity(1);
						mask.setOpacity(1);
						try {
							Thread.sleep((long)(event.endTime-event.startTime));
							maskBackground.setOpacity(0);
							mask.setOpacity(0);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}, (long)event.startTime, TimeUnit.MILLISECONDS);
			}
		}
		
		public void scheduleQueryAppearances() {
			for (Query query : ExperimentModel.queries) {
				VBox queryBox = new VBox(5);
				queryBox.setAlignment(Pos.CENTER);
				queryBox.setMaxWidth(Region.USE_PREF_SIZE);
				queryBox.setMaxHeight(Region.USE_PREF_SIZE);
				Label queryInstructions = new Label(query.text);
				queryInstructions.setBackground(new Background(new BackgroundFill(Color.WHITE,null,null)));
				queryInstructions.setMinWidth(query.text.length()*7.52);
				queryInstructions.setMaxWidth(query.text.length()*7.52);
				queryBox.getChildren().add(queryInstructions);
				queryBox.setVisible(false);
				TextField queryField = new TextField();
				if (query.acceptsText) {
					
					queryBox.getChildren().add(queryField);
				}
				queryBox.relocate(((query.x*(mapWidth/ExperimentModel.x))+mapOffsetX)-(query.text.length()*3.76), 
						((query.y*(mapHeight/ExperimentModel.y))+mapOffsetY)-(query.acceptsText ? 20 : 10));
				root.getChildren().add(queryBox);
				queryBox.toFront();
				ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
				service.schedule(new Runnable() {
					@Override
					public void run() {
						queryBox.setVisible(true);
						if (!query.wait) {
							try {
								Thread.sleep((long)(query.endTime-query.startTime));
								queryBox.setVisible(false);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							if (query.acceptsText) {
								queryField.setOnKeyPressed(e -> {
									if (e.getCode().equals(KeyCode.ENTER)) {
										queryBox.setVisible(false);
									}
								});
							} else {
								root.setOnMouseClicked(e -> {
									queryBox.setVisible(false);
								});
							}
						}
					}
				}, (long)query.startTime, TimeUnit.MILLISECONDS);
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
			Text text = null;
			if (textObject.value != null) {
				text = new Text(textObject.value);
				text.setFill(textObject.color);
				if (textObject instanceof MovingObjectLabel) {
					text.setFont(Font.loadFont(textFontURL.toString(), textObject.size));
				} else {
					text.setFont(Font.loadFont(iconFontURL.toString(), textObject.size));
				}
			} else {
				if (textObject instanceof WaypointObject) {
					text = new Text("");
				}
			}
			if (text != null) {
				text.setBoundsType(TextBoundsType.VISUAL);
				text.setWrappingWidth(200);
				text.setX(x-(text.getLayoutBounds().getWidth()/2));
				text.setY(y+(text.getLayoutBounds().getHeight()/2));
				root.getChildren().add(text);
			}
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
				iconPath.getElements().add(new LineTo(waypoint.x, waypoint.y));
				if (labelText != null) {
					double[] coords = getLabelRelativePosition(waypoint.graphicalIcon.getX(), waypoint.graphicalIcon.getY());
					labelPath.getElements().add(new LineTo(coords[0], coords[1]));
					if (labelBackground != null) {
						labelBackgroundPath.getElements().add(new LineTo(coords[0], coords[1]));
					}
				}
				previous = waypointObject;
			}
			PathTransition iconPathTransition = new PathTransition();
			iconPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
			iconPathTransition.setPath(iconPath);
			iconPathTransition.setNode(graphicalIcon);
			iconPathTransition.setInterpolator(Interpolator.LINEAR);
			iconPathTransition.setDuration(Duration.minutes((distance/object.speed)*60));
			masterTransition.getChildren().add(iconPathTransition);
			if (labelText != null) {
				PathTransition labelPathTransition = new PathTransition();
				labelPathTransition.setPath(labelPath);
				labelPathTransition.setNode(labelText);
				labelPathTransition.setInterpolator(Interpolator.LINEAR);
				labelPathTransition.setDuration(Duration.minutes((distance/object.speed)*60));
				masterTransition.getChildren().add(labelPathTransition);
				if (labelBackground != null) {
					PathTransition labelBackgroundPathTransition = new PathTransition();
					labelBackgroundPathTransition.setPath(labelPath);
					labelBackgroundPathTransition.setNode(labelBackground);
					labelBackgroundPathTransition.setInterpolator(Interpolator.LINEAR);
					labelBackgroundPathTransition.setDuration(Duration.minutes((distance/object.speed)*60));
					masterTransition.getChildren().add(labelBackgroundPathTransition);
				}
			}
		}
		
		public double[] getLabelRelativePosition(double targetX, double targetY) {
			switch (label.position) {
				case RIGHT:
					return new double[] {targetX+((graphicalIcon.getLayoutBounds().getWidth()+labelText.getLayoutBounds().getWidth())/1.9), targetY};
				case LEFT:
					return new double [] {targetX-((graphicalIcon.getLayoutBounds().getWidth()+labelText.getLayoutBounds().getWidth())/1.9), targetY};
				case ABOVE:
					return new double[] {targetX, targetY-((graphicalIcon.getLayoutBounds().getHeight()+labelText.getLayoutBounds().getHeight())/1.9)};
				case BELOW:
					return new double[] {targetX, targetY+((graphicalIcon.getLayoutBounds().getHeight()+labelText.getLayoutBounds().getHeight())/1.9)};
				default:
					return new double[] {targetX, targetY};
			}
		}
		
		public void setLabelRelativePosition() {
			double[] coords = getLabelRelativePosition(labelText.getX(), labelText.getY());
			labelText.setX(coords[0]);
			labelText.setY(coords[1]);
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
