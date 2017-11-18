package code;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class TrackingActivity extends Application {
	
	private Stage stage;
	private Scene scene;
	private Group root;

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.setFullScreen(true);
		root = new Group();
		drawMap();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
	
	private void drawMap() {
		root.getChildren().add(new Rectangle(scene.getWidth(), scene.getHeight(), Paint.valueOf("4286f4")));
		new Alert(AlertType.ERROR, "An error occurred while reading the configuration file. Please try again.", ButtonType.OK).showAndWait();
	}
}
