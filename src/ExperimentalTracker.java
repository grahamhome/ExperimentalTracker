import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * This program is designed for use in 
 * Multiple Identity Tracking experiments.
 * Development begun 10/16/17.
 * @author Graham Home <grahamhome333@gmail.com>
 *
 */
public class ExperimentalTracker extends Application {
	
	private Stage stage;
	private StackPane root;
	
	private File showConfigSelector() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select Existing Configuration");
		File selection = chooser.showDialog(stage);
		return selection;
	}
	
	private void importConfiguration() {
		File selection = showConfigSelector();
		if (selection != null) {
			try {
				ConfigImporter.run(selection);
			} catch (ConfigImporter.ConfigException ex) {
				new Alert(AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
			} catch (IOException ex) {
				new Alert(AlertType.ERROR, "An error occurred while reading the configuration file. Please try again.", ButtonType.OK).showAndWait();
			}
		}
	}
	
	private void showStartupScreen() {
		Button importConfigBtn = new Button();
		importConfigBtn.setText("Select Existing Configuration");
		importConfigBtn.setOnAction((e) -> {
			importConfiguration();
		});
		root.getChildren().add(importConfigBtn);
	}
	
	/**
	 * Starts the user interface.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.setTitle("MIT Tracker - Configuration");
		root = new StackPane();
		showStartupScreen();
		stage.setScene(new Scene(root, 300, 250));
		stage.show();
	}
	
	/**
	 * Executes the program.
	 * @param args : not used
	 */
	public static void main(String[] args) {
		launch(args);
	}
}
