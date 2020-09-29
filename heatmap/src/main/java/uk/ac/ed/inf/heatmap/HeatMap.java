package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

public class HeatMap {
	// Height and width of heat map fixed by specification
	private final int HEIGHT = 10;
	private final int WIDTH = 10;
	
	// Define the boundary of the drone confinement area
	private final double MAX_LATITUDE = 55.946233;
	private final double MIN_LATITUDE = 55.942617;
	private final double MAX_LONGITUDE = -3.184319;
	private final double MIN_LONGITUDE = -3.192473;
	
	private FeatureCollection heatMap;
	private ArrayList<Integer> heatMapRaw;
	
	public HeatMap(File file) throws FileNotFoundException {
		Scanner fileReader;
		try {
			fileReader = new Scanner(file);
			heatMapRaw = processFileContents(fileReader);
			heatMap = createHeatMap(heatMapRaw);
			fileReader.close();
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("The file " + file.getName() + " does not exist.");
		}
	}
	
    // Return heat map as a FeatureCollection
    public FeatureCollection getFeatureCollection() {
    	return heatMap;
    }
	
    // Return heat map in JSON format
	public String getJson() {
		return heatMap.toJson();
	}
	
	// Save heat map in a file
	public void save(String filename) throws IOException {
		try {
			FileWriter saveFile = new FileWriter(filename);
			saveFile.write(heatMap.toJson());
			saveFile.close();
		} catch (IOException e) {
			throw new IOException("Failed to create new file " + filename);
		}

	}
	
	// Create a heat map from an ArrayList of integers
	private FeatureCollection createHeatMap(ArrayList<Integer> heatMapGridRaw) {
		ArrayList<Feature> heatMapGrid = createEmptyGrid(WIDTH, HEIGHT);
		
		Feature currentFeature;
		String hexColor;
		for (int i = 0; i < heatMapGridRaw.size(); i++) {
			currentFeature = heatMapGrid.get(i);
			hexColor = getHexColor(heatMapGridRaw.get(i));
			currentFeature.addNumberProperty("fill-opacity", 0.75);
			currentFeature.addStringProperty("rgb-string", hexColor);
			currentFeature.addStringProperty("fill", hexColor);
		}
		
		return FeatureCollection.fromFeatures(heatMapGrid);
	}
	
	// Convert file contents into an ArrayList of integers
	private ArrayList<Integer> processFileContents(Scanner fileReader) {
		ArrayList<Integer> heatMapGridRaw = new ArrayList<Integer>();
		
		String[] currentLine;
		while (fileReader.hasNextLine()) {
			currentLine = fileReader.nextLine().split(",");
			
			for (String item : currentLine) {
				item = item.strip();
				heatMapGridRaw.add(Integer.parseInt(item));
			}
		}
		
		return heatMapGridRaw;
	}
	
	private ArrayList<Feature> createEmptyGrid(int width, int height) {
		ArrayList<Feature> emptyGrid = new ArrayList<Feature>();
		
		double longitude_step = (MAX_LONGITUDE - MIN_LONGITUDE) / width;
		double latitude_step = (MAX_LATITUDE - MIN_LATITUDE) / height;
		
		// Initialise the four latitude and longitude values for the first grid
		double left_longitude = MIN_LONGITUDE;
		double right_longitude = MIN_LONGITUDE + longitude_step;
		double top_latitude = MAX_LATITUDE;
		double bottom_latitude = MAX_LATITUDE - latitude_step;
		
		for (int i = 0; i < height; i++) {			
			for (int j = 0; j < width; j++) {	
				ArrayList<Point> cellPoints = new ArrayList<Point>();
				
				// Add the four points of a cell
				cellPoints.add(Point.fromLngLat(left_longitude, top_latitude));
				cellPoints.add(Point.fromLngLat(left_longitude, bottom_latitude));
				cellPoints.add(Point.fromLngLat(right_longitude, bottom_latitude));
				cellPoints.add(Point.fromLngLat(right_longitude, top_latitude));
				
				// Convert to feature
				Polygon cellPolygon = Polygon.fromLngLats(List.of(cellPoints));			
				Geometry cellGeometry = (Geometry) cellPolygon;
				Feature  cellFeature = Feature.fromGeometry(cellGeometry);
				emptyGrid.add(cellFeature);
				
				// Shift to next grid cell on the same latitude (row)
				left_longitude = right_longitude;
				right_longitude += longitude_step;
			}
			
			// Move latitude down (go to next row down)
			top_latitude = bottom_latitude;
			bottom_latitude -= latitude_step;
			
			// Reset longitude values
			left_longitude = MIN_LONGITUDE;
			right_longitude = MIN_LONGITUDE + longitude_step;
		}
		
		return emptyGrid;
	}
	
	private String getHexColor(double value) {
		if (value < 0) {
			return "#000000";
		}
		else if (value < 32) {
			return "#00ff00";
		}
		else if (value < 64) {
			return "#40ff00";
		}
		else if (value < 96) {
			return "#80ff00";
		}
		else if (value < 128) {
			return "#c0ff00";
		}
		else if (value < 160) {
			return "#ffc000";
		}
		else if (value < 192) {
			return "#ff8000";
		}
		else if (value < 224) {
			return "#ff4000";
		}
		else if (value < 256) {
			return "#ff0000";
		}
		
		return "#000000";
	}
}
