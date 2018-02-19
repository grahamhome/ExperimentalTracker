package code;

import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
	private Rectangle introBackground;
	private VBox introBox;
	private TextArea introDisplay;
	private Button introButton;
	
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
		buildIntroTextScreen();
		stage.show();
		introBox.relocate((stageWidth-introBox.getWidth())/2, (stageHeight-introBox.getHeight())/2);
		introBox.setVisible(true);
	}
	
	private void buildIntroTextScreen() { // TODO: use scene.setRoot to add these elements. Try using Platform.runLater to relocate introBox in this method.
		introBackground = new Rectangle(stageWidth, stageHeight);
		introBackground.setFill(Color.BLACK);
		introBox = new VBox(5);
		introBox.setPadding(new Insets(5));
		introBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
		introBox.setAlignment(Pos.CENTER);
		introBox.setMaxWidth(Region.USE_PREF_SIZE);
		introBox.setMaxHeight(Region.USE_PREF_SIZE);
		introDisplay = new TextArea();
		introDisplay.setEditable(false);
		introDisplay.setText(ExperimentModel.introduction);
		introDisplay.setWrapText(true);
		introButton = new Button("Start");
		introButton.setOnMouseReleased((e) -> {
			root.getChildren().removeAll(introBackground, introBox);
			masterTransition.play();
			map.scheduleMaskAppearances();
			map.scheduleQueryAppearances();
			scheduleExperimentEnd();
		});
		introBox.getChildren().addAll(introDisplay, introButton);
		introBox.setVisible(false);
		root.getChildren().addAll(introBackground, introBox);
	}
	
	public void scheduleExperimentEnd() {
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(new Runnable() {
			@Override
			public void run() {
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						masterTransition.stop();
						introDisplay.setText("The experiment has ended.");
						introButton.setOnMouseReleased((e2) -> {
							try {
								System.exit(0);
							} catch (Exception e1) {
								e1.printStackTrace();
							}
							
						});
						scene.setRoot(new Group(introBackground, introBox));
					}
				});
				
			}
			
		}, (long)ExperimentModel.duration, TimeUnit.MILLISECONDS);
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
		
		public void scheduleMaskAppearances() { // TODO: use Platform.runLater to add & completely remove masks from root
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
		
		public void scheduleQueryAppearances() { // TODO: use Platform.runLater to add & completely remove queries from root
			for (Query query : ExperimentModel.queries) {
				VBox queryBox = new VBox(5);
				queryBox.setPadding(new Insets(5));
				queryBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
				queryBox.setAlignment(Pos.CENTER);
				queryBox.setMaxWidth(Region.USE_PREF_SIZE);
				queryBox.setMaxHeight(Region.USE_PREF_SIZE);
				Label queryInstructions = new Label(query.text);
				queryInstructions.setMinWidth(query.text.length()*7.52);
				queryInstructions.setMaxWidth(query.text.length()*7.52);
				queryInstructions.setFont(Font.loadFont(textFontURL.toString(), 15));
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
				text.setFont(Font.loadFont(iconFontURL.toString(), textObject.size));
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
		private MovingObjectLabel objectLabel;
		private Label label;
		
		public GraphicalMovingObject(MovingObject object) {
			super(object);
			this.object = object;
			objectLabel = object.label;
			if (objectLabel != null) {
				label = new Label(objectLabel.value);
				label.setTextFill(objectLabel.color);
				label.setBackground(new Background(new BackgroundFill(objectLabel.backgroundColor, null, null)));
				label.setMinWidth(objectLabel.value.length()*objectLabel.size/2.15);
				label.setMaxWidth(objectLabel.value.length()*objectLabel.size/2.15);
				label.setMaxHeight(objectLabel.size);
				label.setFont(Font.loadFont(textFontURL.toString(), objectLabel.size));
				root.getChildren().add(label);
			}
			generatePaths(object.pathPoints);
		}
		
		public void generatePaths(ArrayList<WaypointObject> pathPoints) {
			Path iconPath = new Path();
			iconPath.getElements().add(new MoveTo(graphicalIcon.getX(), graphicalIcon.getY()));
			Path labelPath = new Path();
			if (label != null) {
				double[] coords = getLabelRelativePosition(graphicalIcon);
				labelPath.getElements().add(new MoveTo(coords[0], coords[1]));
			}
			double distance = 0;
			WaypointObject previous = pathPoints.get(0);
			for (WaypointObject waypointObject : pathPoints) {
				GraphicalStationaryObject waypoint = waypoints.get(waypointObject);
				distance += Math.sqrt(Math.pow(waypointObject.x-previous.x,2)+Math.pow(waypointObject.y-previous.y,2));
				iconPath.getElements().add(new LineTo(waypoint.x, waypoint.y));
				if (label != null) {
					double[] coords = getLabelRelativePosition(waypoint.graphicalIcon);
					labelPath.getElements().add(new LineTo(coords[0], coords[1]));
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
			if (label != null) {
				PathTransition labelPathTransition = new PathTransition();
				labelPathTransition.setPath(labelPath);
				labelPathTransition.setNode(label);
				labelPathTransition.setInterpolator(Interpolator.LINEAR);
				labelPathTransition.setDuration(Duration.minutes((distance/object.speed)*60));
				masterTransition.getChildren().add(labelPathTransition);
			}
		}
		
		public double[] getLabelRelativePosition(Text target) {
			switch (objectLabel.position) {
				case RIGHT:
					return new double[] {target.getX()+(objectLabel.value.length()*objectLabel.size/4.3)+(graphicalIcon.getLayoutBounds().getWidth()/2)+5, target.getY()-objectLabel.size/4};
				case LEFT:
					return new double [] {target.getX()-(objectLabel.value.length()*objectLabel.size/4.3)-(graphicalIcon.getLayoutBounds().getWidth()/2)-5, target.getY()-objectLabel.size/4};
				case ABOVE:
					return new double[] {target.getX(), target.getY()-(objectLabel.size*2)};
				case BELOW:
					return new double[] {target.getX(), target.getY()+(graphicalIcon.getLayoutBounds().getHeight())};
				default:
					return new double[] {target.getX(), target.getY()};
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
