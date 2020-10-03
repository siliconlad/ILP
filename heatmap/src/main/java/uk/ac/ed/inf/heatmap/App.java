package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class App {
	public static void main(String[] args) {
		// File name is passed as a command-line argument
		String predictionsFileName = args[0];

		try {
			File predictionsFile = new File(predictionsFileName);
			HeatMap heatMap = new HeatMap(predictionsFile);
			heatMap.save("heatmap.geojson");
		} catch (FileNotFoundException e) {
			System.out.println("The file " + predictionsFileName + " could not be found.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
