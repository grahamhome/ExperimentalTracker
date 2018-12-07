package code;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import code.ExperimentModel.BinaryQuery;
import code.ExperimentModel.FindQuery;
import code.ExperimentModel.Query;
import code.ExperimentModel.ScreenMaskEvent;
import code.ExperimentModel.TextResponseQuery;

/**
 * Builds a report of all events which occur during an experiment and writes it to disk.
 * @author Graham Home
 *
 */
public class ReportWriter {
	
	private static StringBuilder report = new StringBuilder();

	private static String getReportFileName() {
		Calendar cal = Calendar.getInstance();
		return new StringBuilder()
				.append(cal.get(Calendar.MONTH)+1)
				.append("-")
				.append(cal.get(Calendar.DAY_OF_MONTH))
				.append("-")
				.append(cal.get(Calendar.YEAR))
				.append("-")
				.append(ExperimentModel.name)
				.append("-")
				.append(ExperimentModel.participantId)
				.append(".csv").toString();
	}
	
	/**
	 * Add the elapsed time to the current line of the experiment report.
	 */
	private static String reportTime() {
		long elapsedMillis = System.currentTimeMillis()-ExperimentModel.startTime;
		long hours = elapsedMillis/3600000;
		long remainder = elapsedMillis%3600000;
		long minutes = remainder/60000;
		remainder = remainder%60000;
		long seconds = remainder/1000;
		long millis = Math.round((double)remainder%1000);
		
		StringBuilder time = new StringBuilder();
		for (long value : Arrays.asList(hours, minutes, seconds)) {
			time.append(value > 0 ? value : "00");
			time.append(":");
		}
		time.append(millis > 0 ? millis : "000");
		time.append(",");
		report.append(time.toString());
		return time.toString();
	}
	
	/**
	 * Report that the moving objects have been frozen or unfrozen.
	 * @param frozen : True to report freeze, False to report un-freeze.
	 */
	public static void reportFreeze(boolean frozen) {
		reportTime();
		report.append("Moving Objects ")
		.append(frozen ? "Frozen" : "Unfrozen")
		.append(System.lineSeparator());
	}
	
	/**
	 * Report that the experiment has started or ended.
	 * @param started : True to report start, False to report end.
	 */
	public static void reportStatus(boolean started) {
		if (started) {
			ExperimentModel.startTime = System.currentTimeMillis();
			report.append("00:00:00:000,");
		} else {
			reportTime();
		}
		report.append("Experiment ")
		.append(started ? "Started" : "Stopped")
		.append(System.lineSeparator());
	}
	
	/**
	 * Report that a loop has started.
	 * @param loopNumber : The number of the loop to report.
	 */
	public static void reportLoop(int loopNumber) {
		reportTime();
		report.append("Loop ")
		.append(loopNumber)
		.append(" Started")
		.append(",")
		.append(System.lineSeparator());
	}
	
	/**
	 * Report the appearance or disappearance of a screen mask.
	 * @param mask : The ScreenMaskEvent to report.
	 * @param start : True to log the mask appearance, False to log the mask disappearance.
	 */
	public static void reportMask(ScreenMaskEvent mask, boolean show) {
		reportTime();
		report.append("Mask ")
		.append(show ? "Appearance" : "Disappearance")
		.append(",")
		.append(mask.image.getName())
		.append(System.lineSeparator());
	}
	
	/**
	 * Report the appearance or disappearance of an identity mask.
	 * @param mask : The IdentityMaskEvent to report.
	 * @param start : True to log the mask appearance, False to log the mask disappearance.
	 */
	public static void reportIdentityMask(boolean show) {
		reportTime();
		report.append("Identity Mask ")
		.append(show ? "Appearance" : "Disappearance")
		.append(System.lineSeparator());
	}
	
	/**
	 * Report the appearance or disappearance of a query.
	 * @param query : The QueryEvent to report.
	 * @param show : True to log the query appearance, False to log the query disappearance.
	 */
	public static void reportQuery(Query query, boolean show) {
		reportTime();
		report.append(query instanceof FindQuery ? "Click Query " : 
			query instanceof TextResponseQuery ? "Text Query " : "Yes/No Query ")
		.append(show ? "Appearance" : "Disappearance")
		.append(",")
		.append(query.text)
		.append(System.lineSeparator());
	}
	
	/**
	 * Report a text entry.
	 * @param value : The text entered by the user.
	 */
	public static void reportTextEntry(TextResponseQuery query) {
		reportTime();
		report.append("Text Entry")
		.append(",")
		.append(query.value.replaceAll(",", ""))
		.append(System.lineSeparator());
	}
	
	/**
	 * Report a click event.
	 * @param click : The Click to report.
	 */
	public static void reportClick(FindQuery query) {
		ExperimentModel.lastClickTime = reportTime();
		report.append("Click")
		.append(",")
		.append(String.format("%2.3f", query.x))
		.append(",")
		.append(String.format("%2.3f",query.y))
		.append(System.lineSeparator());
	}
	
	/**
	 * Report a yes/no response event.
	 */
	public static void reportBinaryQueryResponse(BinaryQuery query) {
		reportTime();
		report.append("Yes/No Response")
		.append(",")
		.append(query.response ? "Yes" : "No")
		.append(System.lineSeparator());
	}
	
	/**
	 * Report an Object Hit event.
	 * @param label : The text of the object's label.
	 * @param distance : The distance to the object from the click location.
	 */
	public static void reportObjectHit(String label, double distance) {
		report.append(ExperimentModel.lastClickTime)
		.append("Object Hit")
		.append(",")
		.append(label)
		.append(",")
		.append(String.format("%2.3f", distance))
		.append(System.lineSeparator());
	}
	
	/**
	 * Report an Identity Viewed event.
	 * @param label : The text of the label which was viewed.
	 */
	public static void reportIdentityViewed(String label) {
		reportTime();
		report.append("Object Identity Viewed")
		.append(",")
		.append(label)
		.append(System.lineSeparator());
	}
	
	/**
	 * Writes the report to the report file.
	 */
	public static void writeReport() {
		String commonHeader = "Time (all events), Event Type (all events)" + System.lineSeparator();
		String maskHeader = ",Mask Image Name (mask events)" + System.lineSeparator();
		String queryHeader = ",,Query Text (query events)" + System.lineSeparator();
		String textEntryHeader = ",,Text Entered (text entry events)" + System.lineSeparator();
		String clickHeader = ",,X Value in Nautical Miles (click events),Y value in Nautical Miles (click events)" + System.lineSeparator();
		String hitHeader =",,Label of Hit Object (object hit events),Distance to Object in Nautical Miles (object hit events)" + System.lineSeparator();
		String identityViewedEvent = ",,Label of Object Viewed (identity viewed events)" + System.lineSeparator();
		String binaryResponseHeader = ",,Response" + System.lineSeparator();
		try {
			PrintWriter reportWriter = new PrintWriter(getReportFileName(), "UTF-8");
			for (String header : Arrays.asList(commonHeader, maskHeader, queryHeader, textEntryHeader, clickHeader, binaryResponseHeader, hitHeader, identityViewedEvent)) {
				reportWriter.write(header);
			}
			reportWriter.write(report.toString());
			reportWriter.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}
}
