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

/**
 * This class imports an experiment configuration specified by the user,
 * parses it to retrieve the experimental values, and creates a data model
 * containing the values.
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
	private static final String INTERRUPTION_PREFIX = "INT: ";
	private static final String QUERY_PREFIX = "QUE: ";
	private static final String IMG_DIR = "images/";
	private static final List<String> VALID_IMG_TYPES = Arrays.asList(new String[] {"jpg", "jpeg", "png" });
	
	private static final HashMap<Integer, String> configLines = new HashMap<>();
	public static ArrayList<String> errors = new ArrayList<>();
	private static int lineNumber = 0;
	
	private static File directory;
	
	/**
	 * Given a path to an experimental configuration directory,
	 * imports the configuration and creates a data model from it.
	 * @param configPath : The path to the configuration.
	 * @return : An instance of ExperimentModel containing the
	 * values imported from the specified configuration.
	 * @throws ConfigException if a malformed configuration is encountered.
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
		if ((model.name = configLines.get(lineNumber = lineNumbers.next())).isEmpty()) {
			report("Configuration name may not be empty");
		}
		
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		String[] dimensions = configLines.get(lineNumber = lineNumbers.next()).split(PRIMARY_SEPARATOR);
		if (dimensions.length != 2) { 
			report("Two map dimension values are required"); 
		} else {
			try {
				model.x = Float.parseFloat(dimensions[0]);
				model.y = Float.parseFloat(dimensions[1]);
			} catch (NumberFormatException e) { report("One or more map dimension values are not a number"); }
		}
		
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		try {
			model.updateRate = Float.parseFloat(configLines.get(lineNumber = lineNumbers.next()));
		} catch (NumberFormatException e) { report("Screen refresh rate is not a number"); }
		
		if (!lineNumbers.hasNext()) { report("No values found after this line"); return null; }
		model.duration = parseTime(configLines.get(lineNumber = lineNumbers.next()));
		
		String line = configLines.get(lineNumber = lineNumbers.next());
		while (line != null && (line.startsWith(WAYPOINT_PREFIX) || line.startsWith(CONNECTOR_PREFIX))) {
			if (line.startsWith(WAYPOINT_PREFIX)) {
				String[] waypointData = line.replace(WAYPOINT_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (waypointData.length != 5) { 
					report("Waypoint data must contain exactly 5 comma-separated values"); 
				} else {
					Waypoint waypoint = new Waypoint();
					boolean valid = true;
					if (model.getWaypoint((waypoint.name = waypointData[0])) != null) {
						report("A waypoint with this name already exists");
						valid = false;
					}
					switch (waypointData[4]) {
						case "visible" : waypoint.visible = true; break;
						case "invisible" : waypoint.visible = false; break;
						default : report("Waypoint visibility value must be 'visible' or 'invisible'"); valid = false;
					}
					if ((waypoint.shape = Shape.getShape(waypointData[1])) == Shape.NO_MATCH) { 
						report("Invalid shape name");
						valid = false;
					}
					try {
						if ((waypoint.x = Float.parseFloat(waypointData[2])) < 0 
								|| waypoint.x > model.x 
								|| (waypoint.y = Float.parseFloat(waypointData[3])) < 0 
								|| waypoint.y > model.y) 
						{
							report("Waypoint coordinates must be within map boundaries");
							valid = false;
						} 
					} catch (NumberFormatException e) { 
						report("Waypoint coordinates must be numeric values"); 
						valid = false;
					}
					if (valid) {
						if (model.waypoints.contains(waypoint)) {
							report("A waypoint with these coordinates already exists");
						} else {
							model.waypoints.add(waypoint);
						}
					}
				}
			} else {
				String[] connectorData = line.replace(CONNECTOR_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (connectorData.length != 4) { 
					report("Connector data must contain exactly 4 comma-separated values"); 
				} else {
					Connector connector = new Connector();
					boolean valid = true;
					if ((connector.point1 = model.getWaypoint(connectorData[0])) == null 
							|| (connector.point2 = model.getWaypoint(connectorData[1])) == null) 
					{
						report("One or both of the waypoints to be connected do not exist in this configuration");
						valid = false;
					} else if (connector.point1.equals(connector.point2)) { 
						report("Connector must connect two unique waypoints"); 
						valid = false;
					}
					if ((connector.color = Color.getColor(connectorData[3])) == Color.NO_MATCH) {
						report("Invalid color option");
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
						if (model.connectors.contains(connector)) {
							report("A connector between these waypoints already exists");
						} else {
							model.connectors.add(connector);
						}
					}
				}
			}
			line = configLines.get(lineNumber = lineNumbers.next());
		}
		line = configLines.get(lineNumber = lineNumbers.next());
		while (line != null && (line.startsWith(MOVER_PREFIX))) {
			String[] moverData = line.replace(MOVER_PREFIX, "").split(SECONDARY_SEPARATOR);
			if (moverData.length != 7) {
				report("Mover data must contain exactly 7 comma-separated values");
			} else {
				Mover mover = new Mover();
				boolean valid = true;
				Shape shape = Shape.getShape(moverData[0]);
				if ((mover.shape = Shape.getShape(moverData[0])) == Shape.NO_MATCH) {
					report("Invalid shape name");
					valid = false;
				}
				mover.label = moverData[1];
				try {
					if (!Mover.labelAngles.contains((mover.angle = Integer.parseInt(moverData[2])))) {
						report("Label angle must be one of the following values: " + Mover.labelAngles.toString().replaceAll("\\[|\\]", ""));
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Label angle must be a numeric value");
					valid = false;
				}
				try {
					if ((mover.leaderLength = Integer.parseInt(moverData[3])) < 0 || mover.leaderLength > Mover.maxLeaderLength) {
						report("Leader line length must be between 0 and " + Mover.maxLeaderLength);
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Leader line length must be a numeric value");
				}
				try {
					if ((mover.numDots = Integer.parseInt(moverData[4])) < 0 || mover.numDots > Mover.maxDots) {
						report("Number of history dots must be between 0 and " + Mover.maxDots);
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Number of history dots must be a numeric value");
					valid = false;
				}
				String[] waypoints = moverData[5].split(SECONDARY_SEPARATOR);
				for (String waypointName : waypoints) {
					Waypoint waypoint = model.getWaypoint(waypointName);
					if (waypoint == null) {
						report("The waypoint " + waypointName + " was not specified in this configuration");
						valid = false;
					} else {
						mover.pathPoints.add(waypoint);
					}
				}
				if (mover.pathPoints.size() < 2) {
					report("At least 2 valid waypoints must be specified for each mover");
					valid = false;
				}
				try {
					if ((mover.speed = Float.parseFloat(moverData[6])) <= 0) {
						report("Mover speed must be greater than 0 knots");
						valid = false;
					}
				} catch (NumberFormatException e) {
					report("Mover speed must be a numeric value");
					valid = false;
				}
				if (valid) {
					if (model.movers.contains(mover)) {
						report("A mover with the same speed and path already exists");
					} else {
						model.movers.add(mover);
					}
				}
			}
			line = configLines.get(lineNumber = lineNumbers.next());
		}
		line = configLines.get(lineNumber = lineNumbers.next());
		while (line != null && (line.startsWith(INTERRUPTION_PREFIX) || line.startsWith(QUERY_PREFIX))) {
			if (line.startsWith(INTERRUPTION_PREFIX)) {
				String[] interruptionData = line.replace(INTERRUPTION_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (interruptionData.length != 3) {
					report("Interruption event data must contain exactly 3 comma-separated values");
				} else {
					Interruption interruption = new Interruption();
					boolean valid = true;
					String imageName = interruptionData[0];
					interruption.image = new File(directory.toString() + IMG_DIR + imageName);
					if (!interruption.image.exists() || !interruption.image.isFile()) {
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
					interruption.startTime = parseTime(interruptionData[1]);
					interruption.duration = parseTime(interruptionData[2]);
					if (interruption.startTime == -1 || interruption.duration == -1) {
						valid = false;
					} else if (interruption.startTime + interruption.duration >= model.duration) {
						report("Interruption tasks must begin and end before the end of the experiment");
						valid = false;
					}
					if (valid) {
						model.interruptions.add(interruption);
					}
				}
			} else {
				String[] queryData = line.replace(QUERY_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (queryData.length != 4) {
					report("Query event data must contain exactly 4 comma-separated values");
				} else {
					boolean valid = true;
					Query query = new Query();
					query.text = queryData[0];
					switch (queryData[1]) {
						case "visual": query.visual = true; break;
						case "audio": query.visual = false; break;
						default: report("Query format must be either \"audio\" or \"visual\""); valid = false;
					}
					query.startTime = parseTime(queryData[2]);
					query.duration = parseTime(queryData[3]);
					if (query.startTime == -1 || query.duration == -1) {
						valid = false;
					} else if (query.startTime + query.duration >= model.duration) {
						report("Query tasks must begin and end before the end of the experiment");
						valid = false;
					}
					if (valid) {
						model.queries.add(query);
					}
				}
			}
			
			line = configLines.get(lineNumber = lineNumbers.next());
		}
		if (lineNumbers.hasNext()) {
			report("Additional lines detected after interruption/query tasks, which violates config file specification");
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
				if ((minutes = Integer.parseInt(timeValues[1])) < 0 || minutes > 0) {
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
						(minutes * 1000) +
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
		FileReader reader;
		reader = new FileReader(directory + CONFIG_FILE_NAME);
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
