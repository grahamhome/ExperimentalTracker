package code;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
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
public class ConfigImportActivity extends Application {
	
	private Stage stage;
	private StackPane root;
	
	private File showConfigSelector() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Select Existing Configuration");
		File selection = chooser.showDialog(stage);
		return selection;
	}
	
	private void importConfiguration() throws Exception {
		File selection = showConfigSelector();
		if (selection != null) {
			try {
				ConfigImporter.run(selection);
				if (!ConfigImporter.errors.isEmpty()) {
					StringBuilder errors = new StringBuilder("The following errors were encountered in the selected configuration file:");
					for (String error : ConfigImporter.errors) {
						errors.append("\n");
						errors.append(error);
					}
					TextArea errorDisplay = new TextArea();
					errorDisplay.setEditable(false);
					errorDisplay.setText(errors.toString());
					Alert alert = new Alert(AlertType.ERROR, null, ButtonType.OK);
					alert.getDialogPane().setContent(errorDisplay);
					alert.showAndWait();
				} else {
					// TODO: Show subject number dialog box here
					new TrackingActivity().start(stage);
				}
			} catch (FileNotFoundException e) {
				new Alert(AlertType.ERROR, "No configuration file was found in the selected directory. Please try a different directory.", ButtonType.OK).showAndWait();
			} catch (IOException e) {
				new Alert(AlertType.ERROR, "An error occurred while reading the configuration file. Please try again.", ButtonType.OK).showAndWait();
			} finally {
				ExperimentModel.reset();
			}
		}
	}
	
	private void buildStartupScreen() {
		Button importConfigBtn = new Button();
		importConfigBtn.setText("Select Existing Configuration");
		importConfigBtn.setOnAction((e) -> {
			try {
				importConfiguration();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		root.getChildren().add(importConfigBtn);
	}
	
	/**
	 * Starts the user interface for configuration importing.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.setTitle("MIT Tracker - Configuration");
		root = new StackPane();
		buildStartupScreen();
		stage.setScene(new Scene(root, 300, 250));
		stage.show();
	}

	/**
	 * Launches this activity.
	 */
	public static void run() {
		launch();
	}
}
