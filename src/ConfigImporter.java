import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

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
	
	private static final HashMap<Integer, String> configLines = new HashMap<>();
	private static final ArrayList<String> errors = new ArrayList<>();
	private static int lineNumber = 0;
	
	/**
	 * Given a path to an experimental configuration directory,
	 * imports the configuration and creates a data model from it.
	 * @param configPath : The path to the configuration.
	 * @return : An instance of ExperimentModel containing the
	 * values imported from the specified configuration.
	 * @throws ConfigException if a malformed configuration is encountered.
	 * @throws IOException if something goes wrong while reading the file.
	 */
	public static ExperimentModel run(File configDirectory) throws IOException {
		readLines(configDirectory);
		return buildModel();
	}
	
	private static ExperimentModel buildModel() {
		ExperimentModel model = new ExperimentModel();
		ArrayList<Integer> lineNumberList = new ArrayList<>();
		lineNumberList.addAll(configLines.keySet());
		Collections.sort(lineNumberList);
		Iterator<Integer> lineNumbers = lineNumberList.iterator();
		if (!lineNumbers.hasNext()) { report("File contains no values"); return null; }
		model.name = configLines.get(lineNumber = lineNumbers.next());
		
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
		String[] timeValues = configLines.get(lineNumber = lineNumbers.next()).split(SECONDARY_SEPARATOR);
		if (timeValues.length != 4) { 
			report("Four duration values are required"); 
		} else {
			try {
				model.duration = (Integer.parseInt(timeValues[0]) * 60 * 60 * 1000) +
						(Integer.parseInt(timeValues[1]) * 60 * 1000) +
						(Integer.parseInt(timeValues[2]) * 1000) +
						Integer.parseInt(timeValues[3]);
			} catch (NumberFormatException e) { report("One or more duration values are not a number"); }
		}
		
		String line = configLines.get(lineNumber = lineNumbers.next());
		while (line != null && (line.startsWith(WAYPOINT_PREFIX) || line.startsWith(CONNECTOR_PREFIX))) {
			if (line.startsWith(WAYPOINT_PREFIX)) {
				String[] waypointData = line.replace(WAYPOINT_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (waypointData.length != 5) { 
					report("Waypoint data must contain 5 values"); 
				} else {
					boolean valid = true;
					if (model.getWaypoint(waypointData[0]) != null) {
						report("A waypoint with this name already exists");
						valid = false;
					}
					boolean visible = false;
					switch(waypointData[4]) {
						case "visible" : visible = true; break;
						case "invisible" : break;
						default : { report("Waypoint visibility value must be 'visible' or 'invisible'"); valid = false; }
					}
					ExperimentModel.Shape shape = ExperimentModel.Shape.getShape(waypointData[1]);
					if (shape == ExperimentModel.Shape.NO_MATCH) { 
						report("Invalid shape name");
						valid = false;
					}
					if (valid) {
						try {
							ExperimentModel.Waypoint waypoint = new ExperimentModel.Waypoint(waypointData[0], shape, Float.parseFloat(waypointData[2]), Float.parseFloat(waypointData[3]), visible);
							if (model.waypoints.contains(waypoint)) {
								report("A waypoint with these coordinates already exists");
							} else {
								model.waypoints.add(waypoint);
							}
						} catch (NumberFormatException e) { report("Waypoint coordinates must be numeric values"); }
					}
				}
			} else {
				String[] connectorData = line.replace(CONNECTOR_PREFIX, "").split(PRIMARY_SEPARATOR);
				if (connectorData.length != 4) { 
					report("Connector data must contain 4 values"); 
				} else {
					boolean valid = true;
					if (connectorData[0].equals(connectorData[1])) { 
						report("Connector must connect two unique waypoints"); 
						valid = false;
					}
					if (model.getWaypoint(connectorData[0]) == null || model.getWaypoint(connectorData[1]) == null) {
						report("One or both of the waypoints to be connected do not exist in this configuration");
						valid = false;
					}
					ExperimentModel.Color color = ExperimentModel.Color.getColor(connectorData[3]);
					if (color == ExperimentModel.Color.NO_MATCH) {
						report("Invalid color option");
						valid = false;
					}
					if (valid) {
						try {
							ExperimentModel.Connector connector = new ExperimentModel.Connector(model.getWaypoint(connectorData[0]), model.getWaypoint(connectorData[1]), Integer.parseInt(connectorData[2]), color);
							if (model.connectors.contains(connector)) {
								report("A connector between these waypoints already exists");
							} else {
								model.connectors.add(connector);
							}
						} catch ( NumberFormatException e) {
							report("Connector width must be a numeric value");
						}
					}
				}
			}
		}
		line = configLines.get(lineNumber = lineNumbers.next());
		while (line != null && (line.startsWith(MOVER_PREFIX))) {
			String[] moverData = line.replace(MOVER_PREFIX, "").split(SECONDARY_SEPARATOR);
			if (moverData.length != 8) {
				report("Mover data must contain 8 values");
			} else {
				boolean valid = true;
			}
		}
		
		
		
		// TODO: finish parsing config file, also put this in a repo
		return model;
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
	private static void readLines(File configDirectory) throws FileNotFoundException, IOException {
		FileReader reader;
		reader = new FileReader(configDirectory);
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
