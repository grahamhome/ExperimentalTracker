package code;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import code.ExperimentModel.BinaryQuery;
import code.ExperimentModel.Connector;
import code.ExperimentModel.FindQuery;
import code.ExperimentModel.IdentityMaskEvent;
import code.ExperimentModel.ScreenMaskEvent;
import code.ExperimentModel.TextObject;
import code.ExperimentModel.TextResponseQuery;
import code.ExperimentModel.MovingObjectLabel;
import code.ExperimentModel.Query;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.WaypointObject;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
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
	
	private static Stage stage;
	private static Scene scene;
	private static Group root;
	private static Map map = new Map();
	private static Rectangle2D bounds;
	private static double stageWidth;
	private static double stageHeight;
	private static double mapOffsetX;
	private static double mapOffsetY;
	private static double mapHeight;
	private static double mapWidth;
	private static HashMap<WaypointObject, GraphicalStationaryObject> waypoints = new HashMap<>();
	private static HashMap<MovingObject, GraphicalMovingObject> objects = new HashMap<>();
	private static URL iconFontURL = TrackingActivity.class.getResource("/Font-Awesome-5-Free-Solid-900.otf");
	private static URL textFontURL = TrackingActivity.class.getResource("/segoeui.ttf");
	private static ParallelTransition masterTransition = new ParallelTransition();
	private static HashMap<Query, GraphicalQueryObject> queries = new HashMap<>();
	private static double experimentStartTime;
	private static int loop = 0;
	private static GraphicalQueryObject activeQuery;
	
	/**
	 * Calls methods to create all elements of the object tracking display.
	 */
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
		/* Move map frame to front so it will hide all objects which are outside map boundaries */
		map.frame.toFront();
		showIntroTextScreen();
	}
	
	/**
	 * Shows a dialog window with the specified instructional text. 
	 * Starts the experiment when the button in the dialog box is pressed.
	 */
	private void showIntroTextScreen() {
		/* Create dialog */
		GraphicalDialogWindow startWindow = new GraphicalDialogWindow(ExperimentModel.introduction, "Start");
		/* Set dialog action to start the experiment */
		startWindow.setAction((e) -> {
			startWindow.hide();
			experimentStartTime = System.currentTimeMillis();
			ExperimentModel.reportStatus(true);
			ExperimentModel.reportLoop(loop+1);
			masterTransition.play();
			SchedulableEvent event = ExperimentModel.events.get(loop);
			if (event != null) {
				if (event instanceof GraphicalMaskObject) {
					((GraphicalMaskObject)event).execute();
				} else {
					event.execute();
				}
			}
			masterTransition.setOnFinished(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					masterTransition.stop();
					if (++loop > ExperimentModel.loopCount) {
						ExperimentModel.reportStatus(false);
						GraphicalDialogWindow endWindow = new GraphicalDialogWindow("The experiment has ended.", "Exit");
						endWindow.setAction((e2) -> {
							try {
								ConfigImportActivity.exit();
							} catch (Exception e1) {
								e1.printStackTrace();
							}
									
						});
						endWindow.show();
					} else {
						if (activeQuery != null) {
							activeQuery.hide();
						}
						ExperimentModel.reportLoop(loop+1);
						masterTransition.play();
						SchedulableEvent nextEvent = ExperimentModel.events.get(loop);
						if (nextEvent != null) {
							if (nextEvent instanceof GraphicalMaskObject) {
								((GraphicalMaskObject)nextEvent).execute();
							} else {
								nextEvent.execute();
							}
						}
					}
					
				}
			});
			
		});
		startWindow.show();
		/* The stage is not shown until here so that the map is not visible (even for a split-second) before the intro dialog box is shown. */
		stage.show();
	}
	/**
	 * Creates a delayed action to end the experiment at the specified time.
	 */
	public void scheduleExperimentEnd() {
		/* Create delayed action to stop animations and show dialog window */
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(new Runnable() {
			@Override
			public void run() {
				masterTransition.stop();
				if (--ExperimentModel.loopCount == 0) {
					GraphicalDialogWindow endWindow = new GraphicalDialogWindow("The experiment has ended.", "Exit");
					endWindow.setAction((e2) -> {
						try {
							ConfigImportActivity.exit();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
								
					});
					endWindow.show();
				} else {
					scheduleExperimentEnd();
					map.scheduleMaskAppearances();
					map.scheduleQueryAppearances();
					masterTransition.play();
				}
			}
		}, (long)ExperimentModel.duration, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Represents the map containing waypoints, connectors, moving objects, queries and information displays.
	 */
	private static class Map {
		
		public Rectangle mapShape;
		public ImageView mapImage;
		
		/* A visual element which surrounds the map image or shape to hide any object positioned outside the map itself. */
		public Shape frame;
		
		/* A list of queries which are currently being displayed */
		private ArrayList<GraphicalQueryObject> queries = new ArrayList<>();
		
		/**
		 * Draws a map using a specified image or color. Draws map as large as possible for given screen size.
		 */
		private void drawMap() {
			/* Determine map width & height based on screen size */
			if (stageWidth > stageHeight) {
				mapWidth = (mapHeight = stageHeight*0.98);
				mapOffsetX = (stageWidth-mapWidth)/2;
				mapOffsetY = stageHeight*0.01;
			} else {
				mapHeight = (mapWidth = stageWidth*0.98);
				mapOffsetX = stageWidth*0.01;
				mapOffsetY = (stageHeight-mapHeight)/2;
			}
			/* Fill map with specified color (if any) */
			mapShape = new Rectangle(mapOffsetX,mapOffsetY,mapWidth,mapHeight);
			if (ExperimentModel.mapColor != null) {
				mapShape.setFill(ExperimentModel.mapColor);
				root.getChildren().add(mapShape);
			} else {
				/* Fill map with specified image, preserving image ratio */
				Image map = new Image(ExperimentModel.mapImage.toURI().toString());
				mapImage = new ImageView(map);
				mapImage.setPreserveRatio(true);
				mapImage.setFitWidth(mapWidth);
				mapImage.setFitHeight(mapHeight);
				mapImage.setX(mapOffsetX);
				mapImage.setY(mapOffsetY);
				root.getChildren().add(mapImage);
			}
			/* Draw obscuring frame around map to ensure moving & stationary objects will only appear inside map itself */
			frame = Rectangle.subtract(new Rectangle(stageWidth, stageHeight, Color.BLACK), new Rectangle(mapOffsetX,mapOffsetY,mapWidth,mapHeight));
			root.getChildren().add(frame);
			/* 
			 * Adjust map dimensions to ensure waypoints & objects will be placed entirely within map, 
			 * even if they are placed at its edges (e.g. at (0,0)).
			 */
			mapWidth -= ExperimentModel.largestFontSize;
			mapHeight -= ExperimentModel.largestFontSize*1.4;
			mapOffsetY += ExperimentModel.largestFontSize/2;
			mapOffsetX += ExperimentModel.largestFontSize/2;
		}
		
		/**
		 * Draws all waypoints on map.
		 */
		private void drawWaypoints() {
			for (WaypointObject waypoint: ExperimentModel.waypoints.values()) {
				waypoints.put(waypoint, new GraphicalStationaryObject(waypoint));
			}
		}
		
		/**
		 * Draws all waypoint connectors on map.
		 */
		private void drawConnectors() {
			for (Entry<WaypointObject,GraphicalStationaryObject> entry : waypoints.entrySet()) {
				entry.getValue().drawConnectors(entry.getKey());
			}
		}
		
		/**
		 * Draws all moving objects on map.
		 */
		private void drawObjects() {
			for (MovingObject object : ExperimentModel.objects.values()) {
				new GraphicalMovingObject(object);
			}
		}
		
		/**
		 * Creates all mask appearances.
		 */
		public void scheduleMaskAppearances() {
			for (ScreenMaskEvent mask : ExperimentModel.screenMaskEvents) {
				new GraphicalMaskObject(mask);
			}
		}
		
		/**
		 * Creates all query appearances.
		 */
		public void scheduleQueryAppearances() {
			for (Query query : ExperimentModel.queries) {
				map.queries.add(new GraphicalQueryObject(query));
			}
		}
	}
	
	/**
	 * Contains attributes common to all visual objects.
	 */
	private abstract static class GraphicalObject {
		private TextObject baseIcon;
		public Text graphicalIcon;
		public double x, y;
		
		/**
		 * Creates a new visual object from a TextObject.
		 * @param icon : The TextObject representing the object to be displayed graphically.
		 */
		public GraphicalObject(TextObject icon) {
			x = (icon.x*(mapWidth/ExperimentModel.x))+mapOffsetX;
			y = (icon.y*(mapHeight/ExperimentModel.y))+mapOffsetY;
			this.baseIcon = icon;
			this.graphicalIcon = drawText(baseIcon);
		}
		
		/**
		 * Creates a visual text element from a TextObject. 
		 * @param textObject : The TextObject representing the text object to be depicted graphically.
		 * @return : The visual representation of the specified TextObject.
		 */
		public Text drawText(TextObject textObject) {
			Text text = null;
			if (textObject.value != null) {
				text = new Text(textObject.value);
				text.setFill(textObject.color);
				text.setFont(Font.loadFont(iconFontURL.toString(), textObject.size));
			} else {
				/* Invisible waypoints are represented 'visually' by an empty Text element */
				if (textObject instanceof WaypointObject) {
					text = new Text("");
				}
			}
			if (text != null) {
				text.setBoundsType(TextBoundsType.VISUAL);
				text.setWrappingWidth(200);
				/* Position visual text element and add to display */
				text.setX(x-(text.getLayoutBounds().getWidth()/2));
				text.setY(y+(text.getLayoutBounds().getHeight()/2));
				root.getChildren().add(text);
			}
			return text;
		}
	}
		
	/**
	 * Visually represents a moving object.
	 */
	private static class GraphicalMovingObject extends GraphicalObject {
		private MovingObject object;
		private MovingObjectLabel objectLabel;
		private Label label;
		private ArrayList<WaypointObject> pathPoints;
		
		/**
		 * Create a visual representation of a moving object from a MovingObject instance.
		 * @param object : The MovingObject instance representing the object to be depicted graphically.
		 */
		public GraphicalMovingObject(MovingObject object) {
			super(object);
			this.object = object;
			this.pathPoints = object.pathPoints;
			objectLabel = object.label;
			if (objectLabel != null) {
				/* Create label for moving object */
				label = new Label(objectLabel.value);
				label.setTextFill(objectLabel.color);
				label.setBackground(new Background(new BackgroundFill(objectLabel.backgroundColor, null, null)));
				label.setMinWidth(Region.USE_PREF_SIZE);
				label.setMaxWidth(Region.USE_PREF_SIZE);
				label.setMaxHeight(objectLabel.size);
				label.setFont(Font.loadFont(textFontURL.toString(), objectLabel.size));
				root.getChildren().add(label);
			}
			generatePaths();
			objects.put(object, this);
		}
		
		/**
		 * Create animations for moving an object and its label (if present) between
		 * its specified list of waypoints.
		 * @param pathPoints
		 */
		public void generatePaths() {
			/* Create path for moving object icon */
			Path iconPath = new Path();
			iconPath.getElements().add(new MoveTo(graphicalIcon.getX(), graphicalIcon.getY()));
			/* Create path for label, if present */
			Path labelPath = new Path();
			if (label != null) {
				double[] coords = getLabelRelativePosition(graphicalIcon);
				labelPath.getElements().add(new MoveTo(coords[0], coords[1]));
			}
			double distance = 0;
			WaypointObject previous = pathPoints.get(0);
			/* Include all waypoints in object and label paths */
			for (WaypointObject waypointObject : pathPoints) {
				GraphicalStationaryObject waypoint = waypoints.get(waypointObject);
				/* 
				 * Calculate distance between the current & next waypoints in the path &
				 * add it to the total distance travelled.
				 */
				distance += Math.sqrt(Math.pow(waypointObject.x-previous.x,2)+Math.pow(waypointObject.y-previous.y,2));
				iconPath.getElements().add(new LineTo(waypoint.x, waypoint.y));
				if (label != null) {
					double[] coords = getLabelRelativePosition(waypoint.graphicalIcon);
					labelPath.getElements().add(new LineTo(coords[0], coords[1]));
				}
				previous = waypointObject;
			}
			/* Create transition for object path & add to master transition */
			PathTransition iconPathTransition = new PathTransition();
			iconPathTransition.setOrientation(PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
			iconPathTransition.setPath(iconPath);
			iconPathTransition.setNode(graphicalIcon);
			iconPathTransition.setInterpolator(Interpolator.LINEAR);
			/* Determine duration of transition based on distance travelled and object speed */
			Duration travelTime = Duration.minutes((distance/object.speed)*60);
			iconPathTransition.setDuration(travelTime);
			masterTransition.getChildren().add(iconPathTransition);
			/* Create transition for label path (if present) & add to master transition */
			if (label != null) {
				PathTransition labelPathTransition = new PathTransition();
				labelPathTransition.setPath(labelPath);
				labelPathTransition.setNode(label);
				labelPathTransition.setInterpolator(Interpolator.LINEAR);
				labelPathTransition.setDuration(travelTime);
				masterTransition.getChildren().add(labelPathTransition);
			}
		}
		
		/**
		 * Returns the coordinates of a label placed relative to a target object.
		 * @param target : The Text object which the label is to be placed relative to.
		 * @return : The coordinates of the label when placed relative to the target object.
		 */
		public double[] getLabelRelativePosition(Text target) {
			switch (objectLabel.position) {
				case RIGHT:
					return new double[] {target.getX()+(objectLabel.value.length()*objectLabel.size/3.8)+(graphicalIcon.getLayoutBounds().getWidth()/2)+5, target.getY()-objectLabel.size/4};
				case LEFT:
					return new double [] {target.getX()-(objectLabel.value.length()*objectLabel.size/3.8)-(graphicalIcon.getLayoutBounds().getWidth()/2)-5, target.getY()-objectLabel.size/4};
				case ABOVE:
					return new double[] {target.getX(), target.getY()-(objectLabel.size*2)};
				case BELOW:
					return new double[] {target.getX(), target.getY()+(graphicalIcon.getLayoutBounds().getHeight())};
				default:
					return new double[] {target.getX(), target.getY()};
			}
		}
		
		/**
		 * Shows or hides the object's label.
		 * @param show : True to show the label, false to mask it.
		 */
		public void maskLabel(boolean show) {
			if (label != null) {
				if (show) {
					Platform.runLater(() -> {
						label.setTextFill(Color.BLACK);
						label.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
						EventHandler<MouseEvent> listener = new EventHandler<MouseEvent>() {

							@Override
							public void handle(MouseEvent event) {
								maskLabel(false);
								ExperimentModel.reportIdentityViewed(objectLabel.value);
							}
							
						};
						label.setOnMouseMoved(listener);
						graphicalIcon.setOnMouseMoved(listener);
					});
				} else {
					Platform.runLater(() -> {
						label.setTextFill(objectLabel.color);
						label.setBackground(new Background(new BackgroundFill(objectLabel.backgroundColor, null, null)));
						label.setOnMouseMoved(null);
						graphicalIcon.setOnMouseMoved(null);
					});
				}
			}
		}
	}
	
	/**
	 * Visually represents a waypoint.
	 */
	private static class GraphicalStationaryObject extends GraphicalObject {
		
		/**
		 * Creates a visual representation of a waypoint from a WaypointObject.
		 * @param waypoint : The WaypointObject representing the waypoint to be depicted graphically.
		 */
		public GraphicalStationaryObject(WaypointObject waypoint) {
			super(waypoint);
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
	
	public abstract static class SchedulableEvent {
		public SchedulableEvent next = null;
		public int loopNumber;
		public long delay;
		public long duration;
		public boolean scheduledTermination = false;
		public boolean responseReceived = false;
		public void execute() {
			// Schedule event appearance
			ScheduledExecutorService addEventService = Executors.newSingleThreadScheduledExecutor();
			addEventService.schedule(new Runnable() {
				@Override
				public void run() {
					if (loop == loopNumber) {
						if (activeQuery != null) {
							activeQuery.hide();
						}
						show();
					}
					if (scheduledTermination) {
						// Schedule event removal
						ScheduledExecutorService removeMaskService = Executors.newSingleThreadScheduledExecutor();
						removeMaskService.schedule(new Runnable() {
							@Override
							public void run() {
								if (!responseReceived) {
									hide();
									if (next != null && loopNumber == loop) {
										if (next instanceof GraphicalMaskObject) {
											((GraphicalMaskObject)next).execute();
										} else {
											next.execute();
										}
									}
								}
							}
						}, duration, TimeUnit.MILLISECONDS);
					}
				}
			}, delay, TimeUnit.MILLISECONDS);
		}
		public void show() {}
		public void hide() {}
	}
	
	/**
	 * A visual representation of a 'mask event' which 
	 * appears and disappears at specified times, obscuring 
	 * the map when it appears.
	 */
	public static class GraphicalMaskObject extends SchedulableEvent {
		private ImageView mask;
		private ScreenMaskEvent maskEvent;
		private Rectangle maskBackground;
		public ArrayList<GraphicalQueryObject> concurrentEvents = new ArrayList<>();
		
		/**
		 * Constructs a graphical representation of a mask event and
		 * schedules its appearance and disappearance.
		 * @param event : The object representing the mask event to be depicted visually.
		 */
		public GraphicalMaskObject(ScreenMaskEvent event) {
			maskEvent = event;
			/* Create the visual elements of the mask */
			Image maskImage = new Image(event.image.toURI().toString());
			mask = new ImageView(maskImage);
			mask.setPreserveRatio(true);
			loopNumber = maskEvent.loopNumber;
			delay = (long)maskEvent.startTime;
			duration = (long)(maskEvent.endTime-maskEvent.startTime);
			scheduledTermination = true;
		}
		
		@Override
		public void execute() {
			super.execute();
			concurrentEvents.stream().forEach(e -> e.execute());
		}
		
		@Override
		public void show() {
			/* Position mask on screen over map */
			maskBackground = new Rectangle(stageWidth, stageHeight);
			maskBackground.setFill(Color.BLACK);
			mask.setFitWidth(mapWidth);
			mask.setFitHeight(mapHeight);
			mask.setX(mapOffsetX+((mapWidth-mask.getLayoutBounds().getWidth())/2));
			mask.setY(mapOffsetY+((mapHeight-mask.getLayoutBounds().getHeight())/2));
			Platform.runLater(() -> {
				root.getChildren().addAll(maskBackground, mask);
			});
			ExperimentModel.reportMask(maskEvent, true);
		}
		
		@Override
		public void hide() {
			Platform.runLater(() -> {
				root.getChildren().removeAll(maskBackground, mask);
				ExperimentModel.reportMask(maskEvent, false);
			});
			concurrentEvents.stream().forEach(e -> e.hide());
		}
			
	}
	
	/**
	 * A visual representation of a 'text entry' or 'click object' query which
	 * appears and disappears at specified times.
	 */
	public static class GraphicalQueryObject extends SchedulableEvent {
		
		private VBox queryBox;
		private Query query;
		private TextField queryField;
		
		/**
		 * Constructs a graphical representation of a given Query object and
		 * schedules its appearance and disappearance.
		 * @param query : The Query object to be represented graphically.
		 */
		public GraphicalQueryObject(Query query) {
			/* Create the visual elements of the query */
			queryBox = new VBox(5);
			queryBox.setPadding(new Insets(5));
			queryBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
			queryBox.setAlignment(Pos.CENTER);
			queryBox.setMaxWidth(Region.USE_PREF_SIZE);
			queryBox.setMaxHeight(Region.USE_PREF_SIZE);
			Label queryInstructions = new Label(query.text + (query instanceof BinaryQuery ? System.lineSeparator() + "Left-click for \"yes\", right-click for \"no\"" : ""));
			queryInstructions.setMinWidth(Region.USE_PREF_SIZE);
			queryInstructions.setMaxWidth(Region.USE_PREF_SIZE);
			queryInstructions.setFont(Font.loadFont(textFontURL.toString(), 15));
			queryBox.getChildren().add(queryInstructions);
			/* Only 'text input' queries have a text input field */
			queryField = new TextField();
			if (query instanceof TextResponseQuery) {
					queryBox.getChildren().add(queryField);
			}
			loopNumber = query.loopNumber;
			delay = (long)query.startTime;
			if (!query.wait) {
				duration = (long)(query.endTime-query.startTime);
				scheduledTermination = true;
			}
			this.query = query;
			queries.put(query, this);
		}
		
		@Override
		public void execute() {
			super.execute();
		}
		
		@Override
		public void show() {
			ExperimentModel.reportQuery(query, true);
			activeQuery = this;
			/* Position query elements on screen using specified coordinates and length of query string */
			Platform.runLater(() -> { 
				queryBox.relocate(((query.positionX*(mapWidth/ExperimentModel.x))+mapOffsetX)-(queryBox.getWidth()/2), 
						((query.positionY*(mapHeight/ExperimentModel.y))+mapOffsetY)-(query instanceof TextResponseQuery ? 20 : 10));
				root.getChildren().add(queryBox);
				queryBox.toFront();
			});
			if (query.maskIdentities) {
				for (GraphicalMovingObject object : objects.values()) {
					object.maskLabel(true);
				}
				ExperimentModel.reportIdentityMask(true);
			}
			if (query.freeze) {
				ExperimentModel.reportFreeze(true);
				masterTransition.pause();
			}
			/* Allow 'text entry' query to be closed by pressing the 'enter' button */
			if (query instanceof TextResponseQuery) {
				queryField.setOnKeyPressed(e -> {
					if (e.getCode().equals(KeyCode.ENTER)) {
						((TextResponseQuery)query).respond(queryField.getText(), query.startTime-(experimentStartTime-System.currentTimeMillis()));
						ExperimentModel.reportTextEntry((TextResponseQuery)query);
						responseReceived = true;
						hide();
						if (next != null && loopNumber == loop) {
							next.execute();
						}
					}
				});
			} else {
				/* Allow 'click object' query to be closed by clicking the screen */
				root.setOnMouseClicked(e -> {
					if (query instanceof FindQuery) {
						// Ensure click is within map boundaries
						if ((map.mapImage == null ? map.mapShape : map.mapImage).contains(new Point2D(e.getX(), e.getY()))) {
							double nmX = ((e.getX()-mapOffsetX)/map.mapShape.getWidth())*ExperimentModel.x;
							double nmY = ((e.getY()-mapOffsetY)/map.mapShape.getHeight())*ExperimentModel.y;
							((FindQuery)query).respond((float)nmX, (float)nmY, query.startTime-(experimentStartTime-System.currentTimeMillis()));
							Circle selectedArea = new Circle(e.getX(), e.getY(), Math.sqrt((((ExperimentModel.clickRadius/100)*mapHeight*mapWidth))/Math.PI));
							// Check waypoints
							for (Entry<WaypointObject, GraphicalStationaryObject> waypointEntry : waypoints.entrySet()) {
								if (selectedArea.contains(new Point2D(waypointEntry.getValue().x, waypointEntry.getValue().y))) {
									((FindQuery)query).nearbyObjects.add(waypointEntry.getKey());
								}
							} // Check moving objects
							for (Entry<MovingObject, GraphicalMovingObject> objectEntry : objects.entrySet()) {
								Text objectIcon = objectEntry.getValue().graphicalIcon;
								double x = objectIcon.getX() + objectIcon.getTranslateX();
								double y  = objectIcon.getY() + objectIcon.getTranslateY();
								if (selectedArea.contains(new Point2D(objectIcon.getX() + objectIcon.getTranslateX(), objectIcon.getY() + objectIcon.getTranslateY()))) { 
									((FindQuery)query).nearbyObjects.add(objectEntry.getKey());
								}
							}
							ExperimentModel.reportClick((FindQuery)query);
							System.out.println(nmX + "," + nmY);
							for (TextObject hitObject: ((FindQuery)query).nearbyObjects) {
								GraphicalMovingObject movingObj = objects.get(hitObject);
								if (movingObj != null) {
									ExperimentModel.reportObjectHit((movingObj.label != null ? movingObj.label.getText() : "No label"), Math.sqrt(Math.pow((nmX-hitObject.x),2) + Math.pow((nmY-hitObject.y), 2)));
								}
							}
							responseReceived = true;
							hide();
							if (next != null && loopNumber == loop) {
								next.execute();
							}
						}
					} else {
						// Record left or right mouse button click
						((BinaryQuery)query).respond(e.getButton().equals(MouseButton.PRIMARY), query.startTime-(experimentStartTime-System.currentTimeMillis()));
						ExperimentModel.reportBinaryQueryResponse((BinaryQuery)query);
						responseReceived = true;
						hide();
						if (next != null && loopNumber == loop) {
							next.execute();
						}
					}
					
				});
			}
		}
		
		@Override
		public void hide() {
			root.setOnMouseClicked(null);
			activeQuery = null;
			remove();
		}
		
		/**
		 * Removes the query
		 */
		public void remove() {
			if (root.getChildren().contains(queryBox)) {
				Platform.runLater(() -> {
					root.getChildren().remove(queryBox);
				});
				ExperimentModel.reportQuery(query, false);
				if (query.maskIdentities) {
					for (GraphicalMovingObject object : objects.values()) {
						object.maskLabel(false);
					}
					ExperimentModel.reportIdentityMask(false);
				}
				if (query.freeze) {
					ExperimentModel.reportFreeze(false);
					masterTransition.play();
				}
			}
		}
		
		/**
		 * Brings the query to the front of the view hierarchy.
		 */
		public void bringToFront() {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					queryBox.toFront();
				}
			});
		}
	}
	
	/**
	 * A dialog window which contains a text display and a button.
	 * @author Graham
	 *
	 */
	private class GraphicalDialogWindow {
		private VBox window;
		private Rectangle background = new Rectangle(stageWidth, stageHeight, Color.BLACK);
		private Button actionButton;
		
		/**
		 * Creates a dialog window with the specified text and button.
		 */
		public GraphicalDialogWindow (String dialogText, String buttonText) {
			window = new VBox(5);
			window.setPadding(new Insets(5));
			window.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
			window.setAlignment(Pos.CENTER);
			window.setMaxWidth(900);
			window.setMinWidth(900);
			window.setMaxHeight(700);
			window.setMinHeight(700);
			TextArea textDisplay = new TextArea(dialogText);
			textDisplay.setEditable(false);
			textDisplay.setWrapText(true);
			textDisplay.setMinSize(850, 600);
			actionButton = new Button(buttonText);
			actionButton.setMinWidth(100);
			window.getChildren().addAll(textDisplay, actionButton);
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					window.relocate((stageWidth-900)/2, (stageHeight-700)/2);
				}
			});
		}
		
		/**
		 * Allows the dialog box's button action to be set.
		 */
		public void setAction(EventHandler<MouseEvent> action) {
			actionButton.setOnMouseReleased(action);
		}
		
		/**
		 * Shows the dialog window.
		 */
		public void show() {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					root.getChildren().addAll(background, window);
				}
			});
		}
		
		/**
		 * Hides the dialog window.
		 */
		public void hide() {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					root.getChildren().removeAll(background, window);
				}
			});
		}
	}
}
