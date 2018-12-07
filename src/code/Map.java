package code;

import java.util.ArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Represents a map containing waypoints, connectors, moving objects, masks and queries.
 * @author Graham Home
 */
class Map {
	
	public Rectangle mapShape;
	public ImageView mapImage;
	
	/* A visual element which surrounds the map image or shape to hide any object positioned outside the map itself. */
	public Shape frame;
	
	/* A list of queries which are currently being displayed */
	private ArrayList<GraphicalQueryObject> queries = new ArrayList<>();
	
	/**
	 * Draws a map using a specified image or color. Draws map as large as possible for given screen size.
	 */
	public void drawMap() {
		/* Fill map with specified color (if any) */
		mapShape = new Rectangle(TrackingActivity.mapOffsetX,TrackingActivity.mapOffsetY,TrackingActivity.mapWidth,TrackingActivity.mapHeight);
		if (ExperimentModel.mapColor != null) {
			mapShape.setFill(ExperimentModel.mapColor);
			TrackingActivity.root.getChildren().add(mapShape);
		} else {
			/* Fill map with specified image, preserving image ratio */
			Image map = new Image(ExperimentModel.mapImage.toURI().toString());
			mapImage = new ImageView(map);
			mapImage.setPreserveRatio(true);
			mapImage.setFitWidth(TrackingActivity.mapWidth);
			mapImage.setFitHeight(TrackingActivity.mapHeight);
			mapImage.setX(TrackingActivity.mapOffsetX);
			mapImage.setY(TrackingActivity.mapOffsetY);
			TrackingActivity.root.getChildren().add(mapImage);
		}
		/* Draw obscuring frame around map to ensure moving & stationary objects will only appear inside map itself */
		frame = Rectangle.subtract(new Rectangle(TrackingActivity.stageWidth, TrackingActivity.stageHeight, Color.BLACK), new Rectangle(TrackingActivity.mapOffsetX,TrackingActivity.mapOffsetY,TrackingActivity.mapWidth,TrackingActivity.mapHeight));
		TrackingActivity.root.getChildren().add(frame);
		/* 
		 * Adjust map dimensions to ensure waypoints & objects will be placed entirely within map, 
		 * even if they are placed at its edges (e.g. at (0,0)).
		 */
		TrackingActivity.mapWidth -= ExperimentModel.largestFontSize;
		TrackingActivity.mapHeight -= ExperimentModel.largestFontSize*1.4;
		TrackingActivity.mapOffsetY += ExperimentModel.largestFontSize/2;
		TrackingActivity.mapOffsetX += ExperimentModel.largestFontSize/2;
	}
	
	/**
	 * Draws all waypoints on map.
	 */
	void drawWaypoints() {
		ExperimentModel.waypoints.values().forEach(w -> {
			TrackingActivity.waypoints.put(w, new GraphicalStationaryObject(w));
		});
	}
	
	/**
	 * Draws all waypoint connectors on map.
	 */
	void drawConnectors() {
		TrackingActivity.waypoints.entrySet().forEach(e -> {
			e.getValue().drawConnectors(e.getKey());
		});
	}
	
	/**
	 * Draws all moving objects on map.
	 */
	void drawObjects() {
		ExperimentModel.objects.values().forEach(o -> {
			new GraphicalMovingObject(o);
		});
	}
	
	/**
	 * Creates all scheduled mask appearances.
	 */
	public void scheduleMaskAppearances() {
		ExperimentModel.screenMaskEvents.forEach(m -> {
			new GraphicalMaskObject(m);
		});
	}
	
	/**
	 * Creates all scheduled query appearances.
	 */
	public void scheduleQueryAppearances() {
		ExperimentModel.queries.forEach(q -> {
			queries.add(new GraphicalQueryObject(q));
		});
	}
}