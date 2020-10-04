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
	
	public HeatMap(File file) throws FileNotFoundException {
		try {
			var fileReader = new Scanner(file);
			var heatMapRaw = processFileContents(fileReader);
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
			var saveFile = new FileWriter(filename);
			saveFile.write(heatMap.toJson());
			saveFile.close();
		} catch (IOException e) {
			throw new IOException("Failed to create new file " + filename);
		}

	}
	
	// Create a heat map from an ArrayList of integers
	private FeatureCollection createHeatMap(ArrayList<Integer> heatMapGridRaw) {
		var heatMapGrid = createEmptyGrid(WIDTH, HEIGHT);
	
		for (int i = 0; i < heatMapGridRaw.size(); i++) {
			var currentFeature = heatMapGrid.get(i);
			var hexColor = getHexColor(heatMapGridRaw.get(i));
			
			currentFeature.addNumberProperty("fill-opacity", 0.75);
			currentFeature.addStringProperty("rgb-string", hexColor);
			currentFeature.addStringProperty("fill", hexColor);
		}
		
		return FeatureCollection.fromFeatures(heatMapGrid);
	}
	
	// Convert file contents into an ArrayList of integers
	private ArrayList<Integer> processFileContents(Scanner fileReader) {
		var heatMapGridRaw = new ArrayList<Integer>();
		
		while (fileReader.hasNextLine()) {
			var currentLine = fileReader.nextLine().split(",");
			
			for (var item : currentLine) {
				item = item.strip();
				heatMapGridRaw.add(Integer.parseInt(item));
			}
		}
		
		return heatMapGridRaw;
	}
	
	private ArrayList<Feature> createEmptyGrid(int width, int height) {
		var emptyGrid = new ArrayList<Feature>();
		
		var longitude_step = (MAX_LONGITUDE - MIN_LONGITUDE) / width;
		var latitude_step = (MAX_LATITUDE - MIN_LATITUDE) / height;
		
		// Initialise the latitude values for the top row
		var top_latitude = MAX_LATITUDE;
		var bottom_latitude = MAX_LATITUDE - latitude_step;
		
		for (int i = 0; i < height; i++) {
      // Initialise the longitude values for the start of the row
      var left_longitude = MIN_LONGITUDE;
      var right_longitude = MIN_LONGITUDE + longitude_step;
      
			for (int j = 0; j < width; j++) {
		    // Add the four points of a cell
		    var cellPoints = new ArrayList<Point>();
				cellPoints.add(Point.fromLngLat(left_longitude, top_latitude));
				cellPoints.add(Point.fromLngLat(left_longitude, bottom_latitude));
				cellPoints.add(Point.fromLngLat(right_longitude, bottom_latitude));
				cellPoints.add(Point.fromLngLat(right_longitude, top_latitude));
				
				// Convert to feature
				var cellPolygon = Polygon.fromLngLats(List.of(cellPoints));			
				var cellGeometry = (Geometry) cellPolygon;
				var cellFeature = Feature.fromGeometry(cellGeometry);
				emptyGrid.add(cellFeature);
				
				// Shift to next grid cell on the same latitude (row)
				left_longitude = right_longitude;
				right_longitude += longitude_step;
			}
			
			// Move latitude down (go to next row down)
			top_latitude = bottom_latitude;
			bottom_latitude -= latitude_step;
		}
		
		return emptyGrid;
	}
	
	private String getHexColor(double value) {
		if (0 <= value && value < 32) {
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
