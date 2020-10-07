package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.ArrayIndexOutOfBoundsException;
import java.lang.NumberFormatException;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

public class HeatMapGrid {
  // Height and width of heat map grid
  private final int HEIGHT;
  private final int WIDTH;

  // Define the boundary of the heat map grid
  private final double MAX_LATITUDE = 55.946233;
  private final double MIN_LATITUDE = 55.942617;
  private final double MAX_LONGITUDE = -3.184319;
  private final double MIN_LONGITUDE = -3.192473;

  private FeatureCollection heatMapGrid;

  public HeatMapGrid(File file, int width, int height) throws FileNotFoundException {
    // Store heat map dimensions
    WIDTH = width;
    HEIGHT = height;

    try {
      // Extract the contents of the file into an ArrayList of Integers
      var fileReader = new Scanner(file);
      var heatMapRaw = processFileContents(fileReader);
      fileReader.close();

      // Create the heat map as a FeatureCollection
      heatMapGrid = createHeatMap(heatMapRaw);
    } catch (FileNotFoundException e) {
      throw new FileNotFoundException("The file " + file.getName() + " does not exist.");
    }
  }

  // Save heat map grid in the current directory with the given file name
  public void save(String filename) throws IOException {
    try {
      var saveFile = new FileWriter(filename);
      saveFile.write(heatMapGrid.toJson());
      saveFile.close();
    } catch (IOException e) {
      throw new IOException("Failed to create the new file " + filename);
    }
  }

  // Process file contents into a flattened grid (row-wise)
  private ArrayList<Integer> processFileContents(Scanner fileReader) {
    var heatMapGridRaw = new ArrayList<Integer>();

    // Keeps track of the number of lines read
    var currentLineNumber = 0;

    while (fileReader.hasNextLine()) {
      // Only occurs if file have more than HEIGHT number of lines
      if (currentLineNumber == HEIGHT) {
        System.out.println("File has too many lines. Trimming...");
        break;
      }

      var heatMapGridRawRow = processFileLine(fileReader.nextLine(), currentLineNumber);
      heatMapGridRaw.addAll(heatMapGridRawRow);
      
      currentLineNumber += 1;
    }

    // Pad with extra rows if file doesn't have enough lines
    if (currentLineNumber < HEIGHT) {
      System.out.println("Not enough lines in file. Padding with rows of -1...");
      var paddedRow = Collections.nCopies(WIDTH, -1);

      while (currentLineNumber < HEIGHT) {
        heatMapGridRaw.addAll(paddedRow);
        currentLineNumber += 1;
      }
    }

    return heatMapGridRaw;
  }

  // Process a file line string into an integer ArrayList of length WIDTH
  private ArrayList<Integer> processFileLine(String fileLine, int line_number) {
    var heatMapGridRow = new ArrayList<Integer>();

    var currentLine = fileLine.split(",");

    // Let user know if the length of the line is incorrect
    if (currentLine.length < WIDTH) {
      System.out.println("Line " + line_number + " is too short. Padding...");
    } else if (currentLine.length > WIDTH) {
      System.out.println("Line " + line_number + " is too long. Trimming...");
    }

    // Construct a row of length WIDTH
    for (var i = 0; i < WIDTH; i++) {
      try {
        var currentItem = currentLine[i].strip();
        var currentInteger = Integer.parseInt(currentItem);
        heatMapGridRow.add(currentInteger);
      } catch(ArrayIndexOutOfBoundsException e) {
        // If line is too short, pad with -1
        heatMapGridRow.add(-1);
      } catch (NumberFormatException e) {
        // If currentItem is not a valid integer, replace with -1
        System.out.println("Encountered an invalid number. Replacing with -1...");
        heatMapGridRow.add(-1);
      }
    }

    return heatMapGridRow;
  }

  // Populate a (row-wise) flattened empty grid with information based on the input array
  private FeatureCollection createHeatMap(ArrayList<Integer> heatMapGridRaw) {
    // Get the row-wise flattened grid  
    var emptyHeatMapGrid = createEmptyGrid(WIDTH, HEIGHT);

    // Populate each cell based on the corresponding integer in the argument array
    for (int i = 0; i < heatMapGridRaw.size(); i++) {
      // Get the colour value associated with the integer
      var hexColor = getHexColor(heatMapGridRaw.get(i));

      // Set feature properties
      var currentFeature = emptyHeatMapGrid.get(i);
      currentFeature.addNumberProperty("fill-opacity", 0.75);
      currentFeature.addStringProperty("rgb-string", hexColor);
      currentFeature.addStringProperty("fill", hexColor);
    }

    return FeatureCollection.fromFeatures(emptyHeatMapGrid);
  }

  // Create an empty Geo-JSON grid based on the width and height
  private ArrayList<Feature> createEmptyGrid(int width, int height) {
    var emptyGrid = new ArrayList<Feature>();

    // Calculate the size of the cells
    var cell_width = (MAX_LONGITUDE - MIN_LONGITUDE) / width;
    var cell_height = (MAX_LATITUDE - MIN_LATITUDE) / height;

    // Initialise for the top row
    var cell_top = MAX_LATITUDE;
    var cell_bottom = MAX_LATITUDE - cell_height;

    for (int i = 0; i < height; i++) {
      // Initialise for the first column in the row
      var cell_left = MIN_LONGITUDE;
      var cell_right = MIN_LONGITUDE + cell_width;

      for (int j = 0; j < width; j++) {
        // Add the four points of a cell
        var cellPoints = new ArrayList<Point>();
        cellPoints.add(Point.fromLngLat(cell_left, cell_top));
        cellPoints.add(Point.fromLngLat(cell_left, cell_bottom));
        cellPoints.add(Point.fromLngLat(cell_right, cell_bottom));
        cellPoints.add(Point.fromLngLat(cell_right, cell_top));

        // Convert to feature
        var cellPolygon = Polygon.fromLngLats(List.of(cellPoints));			
        var cellGeometry = (Geometry) cellPolygon;
        var cellFeature = Feature.fromGeometry(cellGeometry);
        emptyGrid.add(cellFeature);

        // Shift to next grid cell on the same row
        cell_left = cell_right;
        cell_right += cell_width;
      }

      // Go to the next row down
      cell_top = cell_bottom;
      cell_bottom -= cell_height;
    }

    return emptyGrid;
  }

  // Defines the mapping between colour and sensor values
  private String getHexColor(int value) {
    if (0 <= value && value < 32) {
      return "#00ff00";
    }
    else if (32 <= value && value < 64) {
      return "#40ff00";
    }
    else if (64 <= value && value < 96) {
      return "#80ff00";
    }
    else if (96 <= value && value < 128) {
      return "#c0ff00";
    }
    else if (128 <= value && value < 160) {
      return "#ffc000";
    }
    else if (160 <= value && value < 192) {
      return "#ff8000";
    }
    else if (192 <= value && value < 224) {
      return "#ff4000";
    }
    else if (224 <= value && value < 256) {
      return "#ff0000";
    }

    // Set the default to black to make it easy to see the affected cells
    return "#000000";
  }
}
