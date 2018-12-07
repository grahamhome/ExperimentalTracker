package code;

import code.ExperimentModel.TextObject;
import code.ExperimentModel.WaypointObject;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/**
 * Defines attributes common to all visual objects.
 * @author Graham Home
 */
abstract class GraphicalObject {
	private TextObject baseIcon;
	public Text graphicalIcon;
	public double x, y;
	
	/**
	 * Creates a new visual object from a TextObject.
	 * @param icon : The TextObject representing the object to be displayed graphically.
	 */
	public GraphicalObject(TextObject icon) {
		x = (icon.x*(TrackingActivity.mapWidth/ExperimentModel.x))+TrackingActivity.mapOffsetX;
		y = (icon.y*(TrackingActivity.mapHeight/ExperimentModel.y))+TrackingActivity.mapOffsetY;
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
			text.setFont(Font.loadFont(TrackingActivity.iconFontURL.toString(), textObject.size));
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
			TrackingActivity.root.getChildren().add(text);
		}
		return text;
	}
}
