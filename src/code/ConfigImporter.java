package code;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import code.ExperimentModel.*;
import code.ExperimentModel.MovingObjectLabel.Position;
import javafx.scene.paint.Color;

/**
 * This class imports an experiment configuration specified by the user,
 * parses it to retrieve the experimental values, and creates a data model
 * containing the values specified in the configuration file.
 * @author Graham Home <grahamhome333@gmail.com>
 *
 */
public class ConfigImporter {
	
	private static final String CONFIG_FILE_NAME = "config.csv";
	private static final String COMMENT_INDICATOR = "#";
	private static final String PRIMARY_SEPARATOR = ",";
	private static final String SECONDARY_SEPARATOR = ":";
	private static final String WAYPOINT_PREFIX = "PT: ";
	private static final String CONNECTOR_PREFIX = "CT: ";
	private static final String MOVER_PREFIX = "MV: ";
	private static final String LABEL_PREFIX = "LB: ";
	private static final String MASK_PREFIX = "MK: ";
	private static final String QUERY_PREFIX = "QR: ";
	private static final String IMG_DIR = "\\images\\";
	private static final List<String> VALID_IMG_TYPES = Arrays.asList(new String[] {"jpg", "jpeg", "png" });
	
	private static HashMap<Integer, String> configLines = new HashMap<>();
	public static ArrayList<String> errors = new ArrayList<>();
	private static int lineNumber = 0;
	
	private static File directory;
	
	/**
	 * Given a path to a configuration directory, imports 
	 * the configuration and creates a data model from it.
	 * @param configPath : The path to the configuration.
	 * @return : An instance of ExperimentModel containing the
	 * values imported from the specified configuration, or null if 
	 * any errors were encountered in the config file.
	 * @throws FileNotFound exception if the config file is not found in
	 * the directory provided.
	 * @throws IOException if something goes wrong while reading the file.
	 */
	public static ExperimentModel run(File configDirectory) throws FileNotFoundException, IOException {
		directory = configDirectory;
		readLines();
		return buildModel();
	}
	
	private static ExperimentModel buildModel() {
		errors = new ArrayList<>();
		ExperimentModel model = new ExperimentModel();
		ArrayList<Integer> lineNumberList = new ArrayList<>();
		lineNumberList.addAll(configLines.keySet());
		Collections.sort(lineNumberList);
		Iterator<Integer> lineNumbers = lineNumberList.iterator();
		
		if (!lineNumbers.hasNext()) { report("File contains no values"); return null; }
		if ((ExperimentModel.name = configLines.get(lineNumber = lineNumbers.next())).isEmpty()) {
			report("Configuration name may not be empty");
		}
		
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		String[] mapValues = configLines.get(lineNumber = lineNumbers.next()).split(PRIMARY_SEPARATOR);
		boolean valid = true;
		if (mapValues.length != 3) { 
			report("Map data must contain 3 values"); 
			valid = false;
		} else {
			try {
				if (((ExperimentModel.x = Float.parseFloat(mapValues[0])) < 0) || (ExperimentModel.y = Float.parseFloat(mapValues[1])) < 0) {
					report("Map dimensions must be greater than 0");
					valid = false;
				}
			} catch (NumberFormatException e) { report("One or more map dimension values are not a number"); }
			if (mapValues[2].startsWith("#") ) {
				try {
					ExperimentModel.mapColor = javafx.scene.paint.Color.valueOf(mapValues[2]);
				} catch (IllegalArgumentException e) {
					report("Map color value is not a valid color code");
					valid = false;
				}
			} else {
				String imageName = mapValues[2];
				if (!(ExperimentModel.mapImage = new File(directory.toString() + IMG_DIR + imageName)).exists() || !ExperimentModel.mapImage.isFile()) {
					report("Image file " + imageName + " not found in the " + IMG_DIR + " folder of this configuration folder");
					valid = false;
				} else {
					int i = imageName.lastIndexOf(".");
					String ext = null;
					if (i != 0) {
						ext = imageName.substring(i+1).toLowerCase();
					}
					if (!VALID_IMG_TYPES.contains(ext)) {
						report("Image files must be in one of the following formats: " + VALID_IMG_TYPES.toString().replaceAll("\\[|\\]", ""));
						valid = false;
					}
				}
			}
		}
		if (!valid) { return null; } // Continuing with an invalid map would cause most other config lines to fail
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		try {
			ExperimentModel.updateRate = Float.parseFloat(configLines.get(lineNumber = lineNumbers.next()));
		} catch (NumberFormatException e) { report("Screen refresh rate is not a number"); }
		
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		ExperimentModel.duration = parseTime(configLines.get(lineNumber = lineNumbers.next()));
		
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		try {
			double clickRadius = (Double.parseDouble(configLines.get(lineNumber = lineNumbers.next())));
			if (clickRadius <= 0 || clickRadius > 100) {
				report("Click radius must be greater than 0 and no more than 100");
			} else {
				ExperimentModel.clickRadius = clickRadius;
			}
		} catch (NumberFormatException e) {
			report("Click radius must be a number");
		}
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		String introFileName = configLines.get(lineNumber = lineNumbers.next());
		File introFile = new File(directory.toString() + "\\" + introFileName);
		if (!introFile.exists() || !introFile.isFile()) {
			report("Introduction message file " + introFileName + " not found in this configuration folder");
		} else {
			int i = introFileName.lastIndexOf(".");
			String ext = null;
			if (i != 0) {
				ext = introFileName.substring(i+1).toLowerCase();
			}
			if (!ext.equals("txt")) {
				report("Introduction message file must be in .txt format");
			} else {
				try {
					FileReader introFileReader = new FileReader(introFile);
					BufferedReader introReader = new BufferedReader(introFileReader);
					StringBuilder intro = new StringBuilder();
					String line;
					while ((line = introReader.readLine()) != null) {
						if (intro.length() > 0) intro.append("\n");
						intro.append(line);
					}
					introReader.close();
					introFileReader.close();
					ExperimentModel.introduction = intro.toString();
				} catch (IOException e) {
					report("Error reading from introduction file. Please close any text editors using the file and try again");
				}
			}
		}
		
		String line = "";
		if (lineNumbers.hasNext()) {
			line = configLines.get(lineNumber = lineNumbers.next());
		}
		while (line.startsWith(WAYPOINT_PREFIX) || line.startsWith(CONNECTOR_PREFIX)) {
			valid = true;
			if (line.startsWith(WAYPOINT_PREFIX)) {
				String[] waypointData = line.replace(WAYPOINT_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (waypointData.length != 6 && waypointData.length != 3) { 
					report("Waypoint data must contain either 6 values (for visible waypoints) or 3 values (for invisible waypoints)"); 
				} else {
					WaypointObject waypoint = new WaypointObject();
					if (ExperimentModel.waypoints.containsKey(waypoint.name = waypointData[0])) {
						report("A waypoint with this name already exists");
						valid = false;
					}
					try {
						waypoint.x = Float.parseFloat(waypointData[1]); 
						waypoint.y = Float.parseFloat(waypointData[2]);
					} catch (NumberFormatException e) { 
						report("Waypoint coordinates must be numeric values"); 
						valid = false;
					}
					if (waypointData.length == 6) {
						try {
							waypoint.setValue(waypointData[3]);
						} catch (IllegalArgumentException e) {
							report("Invalid character code for waypoint symbol");
							valid = false;
						}
						try {
							waypoint.size = Float.parseFloat(waypointData[4]);
						} catch (NumberFormatException e) {
							report("Waypoint size must be a numeric value");
							valid = false;
						}
						try {
							waypoint.color = Color.valueOf(waypointData[5]);
						} catch (IllegalArgumentException e) {
							report("Waypoint color value must be a valid color code");
							valid = false;
						}
					}
					if (valid) {
						if (waypoint.alreadyExists()) {
							report("A waypoint with these coordinates already exists");
						} else {
							ExperimentModel.waypoints.put(waypoint.name, waypoint);
						}
					}
				}
			} else {
				String[] connectorData = line.replace(CONNECTOR_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (connectorData.length != 4) { 
					report("Connector data must contain exactly 4 values"); 
				} else {
					Connector connector = new Connector();
					valid = true;
					WaypointObject source;
					if ((source = ExperimentModel.waypoints.get(connectorData[0])) == null
							|| (connector.destination = ExperimentModel.waypoints.get(connectorData[1])) == null) 
					{
						report("One or both of the waypoints to be connected do not exist in this configuration");
						valid = false;
					} else if (source.equals(connector.destination)) { 
						report("Connector must connect two unique waypoints"); 
						valid = false;
					} else if (source.value == null || connector.destination.value == null) {
						report("Connectors may only be used to connect visible waypoints");
						valid = false;
					}
					try {
						connector.color = javafx.scene.paint.Color.valueOf(connectorData[3]);
					} catch (IllegalArgumentException e) {
						report("Connector color value is not a valid color code");
						valid = false;
					}
					try {
						if ((connector.width = Integer.parseInt(connectorData[2])) < 0 || connector.width > Connector.maxWidth) {
							report("Connector width must be between 0 and " + Connector.maxWidth);
							valid = false;
						}
					} catch ( NumberFormatException e) {
						report("Connector width must be a numeric value");
						valid = false;
					}
					if (valid) {
						if (source.isConnected(connector.destination)) {
							report("A connector between these waypoints already exists");
						} else {
							source.connectors.add(connector);
						}
					}
				}
			}
			if (lineNumbers.hasNext()) {
				line = configLines.get(lineNumber = lineNumbers.next());
			} else {
				line = "";
			}
		}
		while (line.startsWith(MOVER_PREFIX)) {
			String[] moverData = line.replace(MOVER_PREFIX, "").split(PRIMARY_SEPARATOR);
			if (moverData.length != 8) {
				report("Moving object data must contain exactly 8 values");
			} else {
				MovingObject mover = new MovingObject();
				valid = true;
				mover.name = moverData[0];
				try {
					mover.setValue(moverData[1]);
				} catch (IllegalArgumentException e) {
					report("Invalid character code for moving object symbol");
					valid = false;
				}
				try {
					mover.color = Color.valueOf(moverData[2]);
				} catch (IllegalArgumentException e) {
					report("Moving object color value is not a valid color code");
					valid = false;
				}
				try {
					mover.size = Float.parseFloat(moverData[3]);
				} catch (NumberFormatException e) {
					report("Moving object size must be a numeric value");
					valid = false;
				}
				try {
					if ((mover.speed = Float.parseFloat(moverData[4])) < 0) {
						report("Moving object speed must be greater than 0 knots");
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Moving object speed must be a numeric value");
					valid = false;
				}
				try {
					if ((mover.leaderLength = Integer.parseInt(moverData[5])) < 0 || mover.leaderLength > MovingObject.maxLeaderLength) {
						report("Leader line length must be between 0 and " + MovingObject.maxLeaderLength);
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Leader line length must be a numeric value");
				}
				try {
					if ((mover.numDots = Integer.parseInt(moverData[6])) < 0 || mover.numDots > MovingObject.maxDots) {
						report("Number of history dots must be between 0 and " + MovingObject.maxDots);
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Number of history dots must be a numeric value");
					valid = false;
				}
				String[] waypoints = moverData[7].split(SECONDARY_SEPARATOR);
				for (String waypointName : waypoints) {
					WaypointObject waypoint = ExperimentModel.waypoints.get(waypointName);
					if (waypoint == null) {
						report("The waypoint " + waypointName + " was not specified in this configuration");
						valid = false;
					} else {
						mover.pathPoints.add(waypoint);
					}
				}
				if (mover.pathPoints.size() < 2) {
					report("At least 2 valid waypoints must be specified for each moving object");
					valid = false;
				}
				if (valid) {
					if (mover.alreadyExists()) {
						report("A moving object with the same speed and path already exists");
					} else {
						mover.x = mover.pathPoints.get(0).x;
						mover.y = mover.pathPoints.get(0).y;
						ExperimentModel.objects.put(mover.name, mover);
					}
				}
			}
			if (lineNumbers.hasNext()) {
				line = configLines.get(lineNumber = lineNumbers.next());
			} else {
				line = "";
			}
		}
		
		while (line.startsWith(LABEL_PREFIX)) {
			String[] labelData = line.replace(LABEL_PREFIX, "").split(PRIMARY_SEPARATOR);
			if (labelData.length != 6) {
				report("Label data must contain exactly 6 values");
			} else {
				valid = true;
				MovingObjectLabel label = new MovingObjectLabel();
				MovingObject mover = ExperimentModel.objects.get(labelData[0]);
				if (mover == null) {
					report("The moving object " + labelData[0] + " was not specified in this configuration");
					valid = false;
				}
				switch (labelData[1]) {
					case "left":
						label.position = Position.LEFT;
						break;
					case "right":
						label.position = Position.RIGHT;
						break;
					case "above":
						label.position = Position.ABOVE;
						break;
					case "below":
						label.position = Position.BELOW;
						break;
					default:
						report("Label position must be either \"left\", \"right\", \"above\", or \"below\"");
						valid = false;
				}
				
				if (labelData[2].length() == 0) {
					label.backgroundColor = Color.TRANSPARENT;
				} else {
					try {
						label.backgroundColor = Color.valueOf(labelData[2]);
					} catch (IllegalArgumentException e) {
						report("Label background color is not a valid color value");
						valid = false;
					}
				}
				if (labelData[3].length() == 0) {
					label.color = Color.TRANSPARENT;
				} else {
					try {
						label.color = Color.valueOf(labelData[3]);
					} catch (IllegalArgumentException e) {
						report("Label text color is not a valid color value");
						valid = false;
					}
				}
				try {
					label.size = Float.valueOf(labelData[4]);
				} catch (NumberFormatException e) {
					report("Label size must be a numeric value");
					valid = false;
				}
				label.setValue(labelData[5]);
				if (label.value.isEmpty()) {
					report("Label may not be empty");
					valid = false;
				}
				if (valid) {
					mover.label = label;
				}
			}
			if (lineNumbers.hasNext()) {
				line = configLines.get(lineNumber = lineNumbers.next());
			} else {
				line = "";
			}
		}
		
		while (line.startsWith(MASK_PREFIX) || line.startsWith(QUERY_PREFIX)) {
			if (line.startsWith(MASK_PREFIX)) {
				String[] maskData = line.replace(MASK_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (maskData.length != 3) {
					report("Mask event data must contain exactly 3 values");
				} else {
					MaskEvent maskEvent = new MaskEvent();
					valid = true;
					String imageName = maskData[0];
					maskEvent.image = new File(directory.toString() + IMG_DIR + imageName);
					if (!maskEvent.image.exists() || !maskEvent.image.isFile()) {
						report("Mask image file " + imageName + " not found in the " + IMG_DIR + " folder of this configuration folder");
						valid = false;
					} else {
						int i = imageName.lastIndexOf(".");
						String ext = null;
						if (i != 0) {
							ext = imageName.substring(i+1).toLowerCase();
						}
						if (!VALID_IMG_TYPES.contains(ext)) {
							report("Image files must be in one of the following formats: " + VALID_IMG_TYPES.toString().replaceAll("\\[|\\]", ""));
							valid = false;
						}
					}
					maskEvent.startTime = parseTime(maskData[1]);
					maskEvent.endTime = parseTime(maskData[2]);
					if (maskEvent.startTime < 0 || maskEvent.endTime > ExperimentModel.duration) {
						report("Mask appearances must start before the beginning and finish before the end of the experiment");
						valid = false;
					}
					if (maskEvent.conflictsWithOther()) {
						report("Mask event would conflict with another mask event which already exists");
						valid = false;
					}
					if (valid) {
						ExperimentModel.maskEvents.add(maskEvent);
					}
				}
			} else {
				String[] queryData = line.replace(QUERY_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (queryData.length != 6) {
					report("Query event data must contain exactly 6 values");
				} else {
					valid = true;
					Query query = new Query();
					switch (queryData[0]) {
						case "click":
							query.acceptsText = false;
							break;
						case "text":
							query.acceptsText = true;
							break;
						default:
							report("Query type must be either \"click\" or \"text\"");
							valid = false;
					}
					
					query.startTime = parseTime(queryData[1]);
					String endTime = queryData[2];
					if (endTime.equals("wait")) {
						query.wait = true;
					} else {
						if ((query.endTime = parseTime(endTime)) == -1) {
							valid = false;
						} else {
							if (query.endTime-query.startTime > ExperimentModel.duration) {
								report("Query events must end before the end of the experiment");
								valid = false;
							}
						}
					}
					try {
						if ((query.x = Float.parseFloat(queryData[3])) > ExperimentModel.x ||
								(query.y = Float.parseFloat(queryData[4])) > ExperimentModel.y) {
							report("Query must be positioned within map boundaries");
							valid = false;
						}
						
					} catch (NumberFormatException e) {
						report("Query position values must be numeric values");
						valid = false;
					}
					query.text = queryData[5];
					if (valid) {
						ExperimentModel.queries.add(query);
					}
				}
			}
			if (lineNumbers.hasNext()) {
				line = configLines.get(lineNumber = lineNumbers.next());
			} else {
				line = "";
			}
		}
		if (lineNumbers.hasNext()) {
			report("Unrecognized configuration data detected after mask and query tasks");
		}
		return (errors.isEmpty() ? model : null);
	}
	
	/**
	 * Parses a time value and returns the time in milliseconds, 
	 * or -1 if the time value is invalid.
	 */
	private static double parseTime(String timeValueString) {
		String[] timeValues = timeValueString.split(SECONDARY_SEPARATOR);
		if (timeValues.length != 4) { 
			report("Four time values are required in the format hours:minutes:seconds:milliseconds");
			return -1;
		} else {
			try {
				int hours, minutes, seconds, milliseconds;
				if ((hours = Integer.parseInt(timeValues[0])) < 0) {
					report("Hours value must be greater than or equal to 0");
					return -1;
				}
				if ((minutes = Integer.parseInt(timeValues[1])) < 0 || minutes > 60) {
					report("Minutes value must be between 0 and 60");
					return -1;
				}
				if ((seconds = Integer.parseInt(timeValues[2])) < 0 || seconds > 60) {
					report("Seconds value must be between 0 and 60");
					return -1;
				}
				if ((milliseconds = Integer.parseInt(timeValues[3])) < 0 || milliseconds > 1000) {
					report("Milliseconds value must be between 0 and 1000");
					return -1;
				}
				return (hours * 60 * 60 * 1000) +
						(minutes * 60 * 1000) +
						(seconds * 1000) +
						milliseconds;
			} catch (NumberFormatException e) { 
				report("One or more of the time values is not a number"); 
				return -1;
			}
		}
	}
		
	/**
	 * This method simplifies the reporting of errors found in the configuration file.
	 * @param message : A message describing the error which was encountered.
	 */
	public static void report(String message) {
			errors.add("Line " + lineNumber + ": " + message + ".");
	}
	
	/**
	 * Reads in all non-blank and non-comment lines from the given configuration file
	 * and stores them with their line number.
	 * @param configDirectory : The directory containing the configuration file.
	 * @throws ConfigException : If the configuration file is not found or is empty.
	 * @throws IOException : If an error occurs while reading the config file which prevents it from being read.
	 */
	private static void readLines() throws FileNotFoundException, IOException {
		configLines = new HashMap<>();
		FileReader reader;
		reader = new FileReader(directory + "/" + CONFIG_FILE_NAME);
		BufferedReader bReader = new BufferedReader(reader);
		int lineNumber = 0;
		String line = bReader.readLine();
		while (line != null) {
			lineNumber++;
			if (!line.startsWith(COMMENT_INDICATOR) && line.length() > 0) {
				configLines.put(lineNumber, line);
			}
			line = bReader.readLine();
		}
		try {
			bReader.close();
			reader.close();
		} catch (IOException e) {/* No need to inform the user; the file has already been read */}
	}
}
