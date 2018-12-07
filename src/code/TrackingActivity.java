package code;

import java.net.URL;
import java.util.HashMap;

import code.ExperimentModel.Query;
import code.ExperimentModel.MovingObject;
import code.ExperimentModel.WaypointObject;
import javafx.animation.ParallelTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Responsible for displaying animated map, interruptions & queries and collecting interaction data.
 * @author Graham Home
 *
 */
public class TrackingActivity extends Application {
	
	private static Stage stage;
	private static Scene scene;
	static Group root;
	static Map map = new Map();
	private static Rectangle2D bounds;
	static double stageWidth;
	static double stageHeight;
	static double mapOffsetX;
	static double mapOffsetY;
	static double mapHeight;
	static double mapWidth;
	static HashMap<WaypointObject, GraphicalStationaryObject> waypoints = new HashMap<>();
	static HashMap<MovingObject, GraphicalMovingObject> objects = new HashMap<>();
	static URL iconFontURL = TrackingActivity.class.getResource("/Font-Awesome-5-Free-Solid-900.otf");
	static URL textFontURL = TrackingActivity.class.getResource("/segoeui.ttf");
	static ParallelTransition masterTransition = new ParallelTransition();
	static HashMap<Query, GraphicalQueryObject> queries = new HashMap<>();
	static double experimentStartTime;
	static int loop = 0;
	static GraphicalQueryObject activeQuery;
	
	/**
	 * Creates all elements of the object tracking display.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Set the stage
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
		// Setup map
		determineMapSize();
		map.drawMap();
		map.drawWaypoints();
		map.drawConnectors();
		map.drawObjects();
		/* Move map frame to front so it will hide all objects which are outside map boundaries */
		map.frame.toFront();
		// Show intro text
		showIntroTextScreen();
	}
	
	/**
	 * Determines map dimensions based on screen size.
	 */
	private void determineMapSize() {
		if (stageWidth > stageHeight) {
			mapWidth = (mapHeight = stageHeight*0.98);
			mapOffsetX = (stageWidth-mapWidth)/2;
			mapOffsetY = stageHeight*0.01;
		} else {
			mapHeight = (mapWidth = stageWidth*0.98);
			mapOffsetX = stageWidth*0.01;
			mapOffsetY = (stageHeight-mapHeight)/2;
		}
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
			// Start experiment
			startWindow.hide();
			experimentStartTime = System.currentTimeMillis();
			ReportWriter.reportStatus(true);
			ReportWriter.reportLoop(loop+1);
			// Start object animations
			masterTransition.play();
			// Execute the first scheduled event
			SchedulableEvent event = ExperimentModel.events.get(loop);
			if (event != null) {
				if (event instanceof GraphicalMaskObject) {
					// Mask objects have their own execute() method which overrides the default
					((GraphicalMaskObject)event).execute();
				} else {
					event.execute();
				}
			}
			// Set action to be executed when object animations complete
			masterTransition.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					masterTransition.stop();
					if (++loop > ExperimentModel.loopCount) {
						// Maximum number of loops reached
						// End experiment
						ReportWriter.reportStatus(false);
						GraphicalDialogWindow endWindow = new GraphicalDialogWindow("The experiment has ended.", "Exit");
						endWindow.setAction((e2) -> {
							try {
								ConfigImportActivity.exit();
							} catch (Exception e) {
								e.printStackTrace();
							}
									
						});
						endWindow.show();
					} else {
						// Start next loop
						if (activeQuery != null) {
							activeQuery.hide();
						}
						ReportWriter.reportLoop(loop+1);
						// Restart object animations
						masterTransition.play();
						// Execute the first scheduled event
						SchedulableEvent nextEvent = ExperimentModel.events.get(loop);
						if (nextEvent != null) {
							if (nextEvent instanceof GraphicalMaskObject) {
								// Mask objects have their own execute() method which overrides the default
								((GraphicalMaskObject)nextEvent).execute();
							} else {
								nextEvent.execute();
							}
						}
					}
					
				}
			});
			
		});
		// Show start window and stage
		startWindow.show();
		stage.show();
	}
}