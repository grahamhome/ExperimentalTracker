package code;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import code.ExperimentModel.Connector;
import code.ExperimentModel.IdentityMaskEvent;
import code.ExperimentModel.ScreenMaskEvent;
import code.ExperimentModel.TextObject;
import code.ExperimentModel.MovingObjectLabel;
import code.ExperimentModel.Query;
import code.ExperimentModel.Query.Click;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.WaypointObject;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.application.Application;
import javafx.application.Platform;
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
	
	private Stage stage;
	private Scene scene;
	private Group root;
	private Map map = new Map();
	private Rectangle2D bounds;
	private double stageWidth, stageHeight, mapOffsetX, mapOffsetY, mapHeight, mapWidth;
	private HashMap<WaypointObject, GraphicalStationaryObject> waypoints = new HashMap<>();
	private HashMap<MovingObject, GraphicalMovingObject> objects = new HashMap<>();
	private URL iconFontURL = TrackingActivity.class.getResource("/Font-Awesome-5-Free-Solid-900.otf");
	private URL textFontURL = TrackingActivity.class.getResource("/segoeui.ttf");
	private ParallelTransition masterTransition = new ParallelTransition();
	private GraphicalQueryObject activeQuery;
	private HashMap<Query, GraphicalQueryObject> queries = new HashMap<>();
	private double experimentStartTime;
	

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
			map.scheduleMaskAppearances();
			map.scheduleQueryAppearances();
			scheduleExperimentEnd();
			masterTransition.play();
			experimentStartTime = System.currentTimeMillis();
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
					masterTransition.play();
				}
			}
		}, (long)ExperimentModel.duration, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Represents the map containing waypoints, connectors, moving objects, queries and information displays.
	 */
	private class Map {
		
		private Rectangle mapShape;
		
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
				Image mapImage = new Image(ExperimentModel.mapImage.toURI().toString());
				ImageView map = new ImageView(mapImage);
				map.setPreserveRatio(true);
				map.setFitWidth(mapWidth);
				map.setFitHeight(mapHeight);
				map.setX(mapOffsetX);
				map.setY(mapOffsetY);
				root.getChildren().add(map);
			}
			/* Draw obscuring frame around map to ensure moving & stationary objects will only appear inside map itself */
			frame = Rectangle.subtract(new Rectangle(stageWidth, stageHeight, Color.BLACK), new Rectangle(mapOffsetX,mapOffsetY,mapWidth,mapHeight));
			root.getChildren().add(frame);
			/* 
			 * Adjust map dimensions to ensure waypoints & objects will be placed entirely within map, 
			 * even if they are placed at its edges (e.g. at (0,0)).
			 */
			mapWidth -= ExperimentModel.largestFontSize;
			mapHeight -= ExperimentModel.largestFontSize;
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
			for (IdentityMaskEvent mask : ExperimentModel.identityMaskEvents) {
				new GraphicalIdentityMaskObject(mask);
				
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
	private abstract class GraphicalObject {
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
	private class GraphicalMovingObject extends GraphicalObject {
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
				label.setMinWidth(objectLabel.value.length()*objectLabel.size/2.15);
				label.setMaxWidth(objectLabel.value.length()*objectLabel.size/2.15);
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
		
		/**
		 * Shows or hides the object's label.
		 * @param show : True to show the label, false to mask it.
		 */
		public void maskLabel(boolean show) {
			if (label != null) {
				if (show) {
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
				} else {
					label.setTextFill(objectLabel.color);
					label.setBackground(new Background(new BackgroundFill(objectLabel.backgroundColor, null, null)));
					label.setOnMouseMoved(null);
					graphicalIcon.setOnMouseMoved(null);
				}
			}
		}
	}
	
	/**
	 * Visually represents a waypoint.
	 */
	private class GraphicalStationaryObject extends GraphicalObject {
		
		/**
		 * Creates a visual representation of a waypoint from a WaypointObject.
		 * @param waypoint : The WaypointObject representing the waypoint to be depicted graphically.
		 */
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
	
	/**
	 * A visual representation of a 'mask event' which 
	 * appears and disappears at specified times, obscuring 
	 * the map when it appears.
	 */
	private class GraphicalMaskObject {
		private ImageView mask;
		private ScreenMaskEvent maskEvent;
		
		/**
		 * Constructs a graphical representation of a mask event and
		 * schedules its appearance and disappearance.
		 * @param event : The object representing the mask event to be depicted visually.
		 */
		private GraphicalMaskObject(ScreenMaskEvent event) {
			maskEvent = event;
			/* Create the visual elements of the mask */
			Rectangle maskBackground = new Rectangle(stageWidth, stageHeight);
			maskBackground.setFill(Color.BLACK);
			Image maskImage = new Image(event.image.toURI().toString());
			mask = new ImageView(maskImage);
			mask.setPreserveRatio(true);
			mask.setFitWidth(mapWidth);
			mask.setFitHeight(mapHeight);
			/* Position mask on screen over map */
			mask.setX(mapOffsetX+((mapWidth-mask.getLayoutBounds().getWidth())/2));
			mask.setY(mapOffsetY+((mapHeight-mask.getLayoutBounds().getHeight())/2));
			/* Create delayed task to add mask elements to screen */
			ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
			service.schedule(new Runnable() {
				@Override
				public void run() {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							root.getChildren().addAll(maskBackground, mask);
							map.queries.stream().forEach(q -> q.bringToFront());
							ExperimentModel.reportMask(event, true);
						}
					});
					/* Remove mask elements after specified delay */
					try {
						Thread.sleep((long)((event.endTime-event.startTime)+(ExperimentModel.duration*(event.loopNumber-1))));
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								root.getChildren().removeAll(maskBackground, mask);
								ExperimentModel.reportMask(event, false);
							}
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			/* Set delay before mask appears */
			}, (long)(event.startTime+(ExperimentModel.duration*(event.loopNumber-1))), TimeUnit.MILLISECONDS);
		}
	}
	
	
	/**
	 * A visual representation of an "identity mask", which appears and disappears at
	 * specified times, obscuring the moving object labels when it appears.
	 */
	private class GraphicalIdentityMaskObject {
		private IdentityMaskEvent event;
		
		private GraphicalIdentityMaskObject(IdentityMaskEvent maskEvent) {
			event = maskEvent;
			ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
			service.schedule(new Runnable() {
				@Override
				public void run() {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							for (GraphicalMovingObject object : objects.values()) {
								object.maskLabel(true);
							}
							ExperimentModel.reportIdentityMask(event, true);
						}
					});
					/* Remove mask elements after specified delay */
					try {
						Thread.sleep((long)((event.endTime-event.startTime)+(ExperimentModel.duration*(event.loopNumber-1))));
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								for (GraphicalMovingObject object : objects.values()) {
									object.maskLabel(false);
								}
								ExperimentModel.reportIdentityMask(event, false);
							}
						});
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			/* Set delay before mask appears */
			}, (long)(event.startTime+(ExperimentModel.duration*(event.loopNumber-1))), TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * A visual representation of a 'text entry' or 'click object' query which
	 * appears and disappears at specified times.
	 */
	private class GraphicalQueryObject {
		
		private VBox queryBox;
		private Query query;
		
		/**
		 * Constructs a graphical representation of a given Query object and
		 * schedules its appearance and disappearance.
		 * @param query : The Query object to be represented graphically.
		 */
		private GraphicalQueryObject(Query query) {
			/* Create the visual elements of the query */
			queryBox = new VBox(5);
			queryBox.setPadding(new Insets(5));
			queryBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
			queryBox.setAlignment(Pos.CENTER);
			queryBox.setMaxWidth(Region.USE_PREF_SIZE);
			queryBox.setMaxHeight(Region.USE_PREF_SIZE);
			Label queryInstructions = new Label(query.text);
			queryInstructions.setMinWidth(Region.USE_PREF_SIZE);
			queryInstructions.setMaxWidth(Region.USE_PREF_SIZE);
			queryInstructions.setFont(Font.loadFont(textFontURL.toString(), 15));
			queryBox.getChildren().add(queryInstructions);
			/* Only 'text input' queries have a text input field */
			TextField queryField = new TextField();
			if (query.acceptsText) {
					queryBox.getChildren().add(queryField);
			}
			/* Position query elements on screen using specified coordinates and length of query string */
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					queryBox.relocate(((query.x*(mapWidth/ExperimentModel.x))+mapOffsetX)-(queryBox.getWidth()/2), 
							((query.y*(mapHeight/ExperimentModel.y))+mapOffsetY)-(query.acceptsText ? 20 : 10));
				}
			});
			this.query = query;
			queries.put(query, this);
			/* Create delayed task to add query elements to view at specified time */
			ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
			service.schedule(new Runnable() {
				
				/* Adds query elements to screen */
				@Override
				public void run() {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							if (activeQuery != null) {
								activeQuery.remove();
							}
							root.getChildren().add(queryBox);
							activeQuery = queries.get(query);
							ExperimentModel.reportQuery(query, true);
						}
					});
					/* Allow 'text entry' query to be closed by pressing the 'enter' button */
					if (query.acceptsText) {
						queryField.setOnKeyPressed(e -> {
							if (e.getCode().equals(KeyCode.ENTER)) {
								query.responseText = query.new TextEntry(queryField.getText(), query.startTime-(experimentStartTime-System.currentTimeMillis()));
								ExperimentModel.reportTextEntry(query.responseText.value);
								Platform.runLater(removeQuery);
								service.shutdownNow();
							}
						});
					} else {
						/* Allow 'click object' query to be closed by clicking the screen */
						root.setOnMouseClicked(e -> {
							// Ensure click is within map boundaries
							if (map.mapShape.contains(new Point2D(e.getX(), e.getY()))) {
								System.out.println("Clicked: " + (float)e.getSceneX() + ", " + (float)e.getSceneY());
								query.responseClick = query.new Click((float)e.getSceneX(), (float)e.getSceneY(), System.currentTimeMillis()-experimentStartTime);
								Circle selectedArea = new Circle(e.getX(), e.getY(), Math.sqrt((((ExperimentModel.clickRadius/100)*mapHeight*mapWidth))/Math.PI));
								// Check waypoints
								for (Entry<WaypointObject, GraphicalStationaryObject> waypointEntry : waypoints.entrySet()) {
									if (selectedArea.contains(new Point2D(waypointEntry.getValue().x, waypointEntry.getValue().y))) {
										query.responseClick.nearbyObjects.add(waypointEntry.getKey());
									}
								} // Check moving objects
								for (Entry<MovingObject, GraphicalMovingObject> objectEntry : objects.entrySet()) {
									Text objectIcon = objectEntry.getValue().graphicalIcon;
									double x = objectIcon.getX() + objectIcon.getTranslateX();
									double y  = objectIcon.getY() + objectIcon.getTranslateY();
									System.out.println((objectEntry.getValue().label != null ? objectEntry.getValue().label.getText() + " ": "Unlabelled ") + (x) + ", " + (y));
									if (selectedArea.contains(new Point2D(objectIcon.getX() + objectIcon.getTranslateX(), objectIcon.getY() + objectIcon.getTranslateY()))) { 
										query.responseClick.nearbyObjects.add(objectEntry.getKey());
										System.out.println("Hit: " + (objectEntry.getValue().label != null ? objectEntry.getValue().label.getText() : "Unlabelled"));
									}
								}
								System.out.println("---");
								Platform.runLater(removeQuery);
								root.setOnMouseClicked(null);
								ExperimentModel.reportClick(query.responseClick);
								for (TextObject hitObject: query.responseClick.nearbyObjects) {
									GraphicalMovingObject movingObj = objects.get(hitObject);
									if (movingObj != null) {
										ExperimentModel.reportObjectHit((movingObj.label != null ? movingObj.label.getText() : "No label"), 0);
									}
								}
								service.shutdownNow();
							}
						});
					}
					if (!query.wait) {
						/* Close query after specified delay */
						try {
							Thread.sleep((long)((query.endTime-query.startTime)+(ExperimentModel.duration*(query.loopNumber-1))));
							Platform.runLater(removeQuery);
							root.setOnMouseClicked(null);
							service.shutdownNow();
						} catch (InterruptedException e) { /* This is expected when the service is terminated early via shutdownNow() */ }
					}
				}
				
				/* A sub-task which removes the query elements from the screen when called */
				Runnable removeQuery = new Runnable() {
					@Override
					public void run() {
						remove();
					}
				};
			/* Set delay before query appears */
			}, (long)(query.startTime*+(ExperimentModel.duration*(query.loopNumber-1))), TimeUnit.MILLISECONDS);
		}
		
		/**
		 * Removes the query
		 */
		public void remove() {
			root.getChildren().remove(queryBox);
			activeQuery = null;
			ExperimentModel.reportQuery(query, false);
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
			window.setMaxWidth(500);
			window.setMinWidth(500);
			window.setMaxHeight(280);
			window.setMinHeight(280);
			TextArea textDisplay = new TextArea(dialogText);
			textDisplay.setEditable(false);
			textDisplay.setWrapText(true);
			actionButton = new Button(buttonText);
			actionButton.setMinWidth(100);
			window.getChildren().addAll(textDisplay, actionButton);
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					window.relocate((stageWidth-500)/2, (stageHeight-280)/2);
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
