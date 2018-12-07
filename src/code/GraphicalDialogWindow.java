package code;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * A dialog window which contains a text display and a button.
 * @author Graham Home
 *
 */
class GraphicalDialogWindow {
	private VBox window;
	private Rectangle background = new Rectangle(TrackingActivity.stageWidth, TrackingActivity.stageHeight, Color.BLACK);
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
				window.relocate((TrackingActivity.stageWidth-900)/2, (TrackingActivity.stageHeight-700)/2);
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
				TrackingActivity.root.getChildren().addAll(background, window);
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
				TrackingActivity.root.getChildren().removeAll(background, window);
			}
		});
	}
}