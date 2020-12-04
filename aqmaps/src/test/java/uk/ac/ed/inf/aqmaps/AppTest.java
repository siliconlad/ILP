package uk.ac.ed.inf.aqmaps;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;

import uk.ac.ed.inf.aqmaps.drone.Sensor;
import uk.ac.ed.inf.aqmaps.webserver.WebServer;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class AppTest {
  private static final String PORT = "9898";
  
  WebServer webServer = new WebServer("http", "localhost", PORT);
  Random rand = new Random();
  
  String year = rand.nextInt(2) == 1 ? "2020" : "2021";
  String month = String.format("%02d", rand.nextInt(12) + 1);
  String day = String.format("%02d", rand.nextInt(28) + 1);
  
  String startLat = "55.944425";
  String startLng = "-3.188396";
  
  ArrayList<Sensor> sensors;
  ArrayList<Sensor> visitedSensors = new ArrayList<Sensor>();
  ArrayList<Double> dronePathLats = new ArrayList<Double>();
  ArrayList<Double> dronePathLngs = new ArrayList<Double>();
  
  @BeforeAll
  void initAll() {
    System.out.println(this.year + "/" + this.month + "/" + this.day);
    assertAll("app",
        () -> {
          // Get sensors
          this.sensors = webServer.getSensors(this.year, this.month, this.day);
          
          // Run application
          String[] args = {this.day, this.month, this.year, this.startLat, this.startLng, "9", PORT};
          App.main(args);
        }
    );
  }
  
  @Test
  @Order(1)
  void testFlightPath() {
    assertAll("flight path",
        () -> {
          // Get file
          File flightPathFile = new File("flightpath-" + this.day + "-" + this.month + "-" + this.year + ".txt");
          assertTrue(flightPathFile.exists());
          
          Scanner flightPathReader = new Scanner(flightPathFile);
          int numOfLines = 0;
          
          var prevLat = startLat;
          this.dronePathLats.add(Double.valueOf(prevLat));
          var prevLng = startLng;
          this.dronePathLngs.add(Double.valueOf(prevLng));
          
          while (flightPathReader.hasNextLine()) {
            String currLine = flightPathReader.nextLine();
            numOfLines += 1;
            
            // String should contain no spaces
            assertFalse(currLine.contains(" "), "Must have no spaces.");
            
            String[] currLineComponents = currLine.split(",");
            assertEquals(7, currLineComponents.length, "Line should have 7 components separated by commas.");
            
            // Check Line number
            assertEquals(numOfLines, Integer.valueOf(currLineComponents[0]), "Line number must match.");
            
            // Check starting position
            assertEquals(prevLng, currLineComponents[1], "Must equal previous longitude.");
            assertEquals(prevLat, currLineComponents[2], "Must equal previous latitude.");
            
            // Check direction
            assertFalse(currLineComponents[3].contains("."), "Direction must be an integer.");
            assertFalse(currLineComponents[3].contains("-"), "Direction must be positive.");
            assertTrue(currLineComponents[3].endsWith("0"), "Direction must be multiple of 10.");
            int direction = Integer.valueOf(currLineComponents[3]).intValue();
            assertTrue(0 <= direction && direction <= 350, "Direction must be between 0 and 350 (inclusive).");
            assertTrue(direction % 10 == 0, "Direction must be multiple of 10.");
            
            // Check end position
            assertEquals(Double.valueOf(prevLng) + 0.0003 * Math.cos(Math.toRadians(direction)), Double.valueOf(currLineComponents[4]), "Longitude movement is not correct.");
            assertEquals(Double.valueOf(prevLat) + 0.0003 * Math.sin(Math.toRadians(direction)), Double.valueOf(currLineComponents[5]), "Latitude movement is not correct.");
            prevLng = currLineComponents[4];
            this.dronePathLngs.add(Double.valueOf(prevLng));
            prevLat = currLineComponents[5];
            this.dronePathLats.add(Double.valueOf(prevLat));
            
            // Check sensor
            boolean sensorExists = false;
            for (var sensor : this.sensors) {
              if (sensor.location.equals(currLineComponents[6])) {
                sensorExists = true;
                
                // Check distance
                var sensorInfo = webServer.getWhat3WordsDetails(sensor.location);
                var dLng = sensorInfo.coordinates.lng - Double.valueOf(prevLng);
                var dLat = sensorInfo.coordinates.lat - Double.valueOf(prevLat);
                
                var dst = Math.sqrt(Math.pow(dLng, 2) + Math.pow(dLat, 2));
                assertTrue(dst < 0.0002, "Drone is too far away from sensor");
                
                this.visitedSensors.add(sensor);
              }
            }
            
            if (currLineComponents[6].equals("null")) {
              sensorExists = true;
            }
            assertTrue(sensorExists, "Sensor component is invalid.");
          }
          
          assertTrue(0 < numOfLines && numOfLines <= 150, "Number of lines must be between 1 and 150 (inclusive).");
          flightPathReader.close();
        }
    );
  }
  
  @Test
  @Order(2)
  void testReadings() {
    assertAll("readings",
        () -> {
          File geojsonFile = new File("readings-" + this.day + "-" + this.month + "-" + this.year + ".geojson");
          assertTrue(geojsonFile.exists());
          
          Scanner geojsonFileReader = new Scanner(geojsonFile);
          var mapFeatureCollection = FeatureCollection.fromJson(geojsonFileReader.nextLine());
          geojsonFileReader.close();
          
          var dronePathFeature = mapFeatureCollection.features().get(0);
          var dronePathGeometry = (LineString) dronePathFeature.geometry();
          var dronePath = dronePathGeometry.coordinates();
          
          for (var i=0; i < dronePath.size(); i++) {
            // Round to 7 decimal places
            assertEquals(Math.round(this.dronePathLngs.get(i) * 10000000) / 10000000.0, dronePath.get(i).longitude());
            assertEquals(Math.round(this.dronePathLats.get(i) * 10000000) / 10000000.0, dronePath.get(i).latitude());
          }
          
          var sensorFeatures = mapFeatureCollection.features().subList(1, 34);
          for (var sensorFeature : sensorFeatures) {
            // Check basic sensor properties
            assertEquals(sensorFeature.getStringProperty("rgb-string"), sensorFeature.getStringProperty("marker-color"), "Sensor doesn't have the same rbg-string and marker-color properties.");
            
            var sensorVisited = false;
            for (var visitedSensor : this.visitedSensors) {
              if (visitedSensor.location.equals(sensorFeature.getStringProperty("location"))) {
                sensorVisited = true;
                
                // Check visited sensor properties
                if (visitedSensor.battery < 10.0) {
                  assertEquals("cross", sensorFeature.getStringProperty("marker-symbol"), "Sensor has wrong symbol (low battery).");
                  assertEquals("#000000", sensorFeature.getStringProperty("rgb-string"), "Sensor has wrong color (low battery)"); 
                } else {
                  var sensorReading = Double.valueOf(visitedSensor.reading);
                  String sensorColor = "";
                  String sensorMarker = "";
                  if (0 <= sensorReading && sensorReading < 32) {
                    sensorColor = "#00ff00";
                    sensorMarker = "lighthouse";
                  } else if (sensorReading < 64) {
                    sensorColor = "#40ff00";
                    sensorMarker = "lighthouse";
                  } else if (sensorReading < 96) {
                    sensorColor = "#80ff00";
                    sensorMarker = "lighthouse";
                  } else if (sensorReading < 128) {
                    sensorColor = "#c0ff00";
                    sensorMarker = "lighthouse";
                  } else if (sensorReading < 160) {
                    sensorColor = "#ffc000";
                    sensorMarker = "danger";
                  } else if (sensorReading < 192) {
                    sensorColor = "#ff8000";
                    sensorMarker = "danger";
                  } else if (sensorReading < 224) {
                    sensorColor = "#ff4000";
                    sensorMarker = "danger";
                  } else if (sensorReading < 256) {
                    sensorColor = "#ff0000";
                    sensorMarker = "danger";
                  }
                  
                  assertEquals(sensorColor, sensorFeature.getStringProperty("rgb-string"), "Sensor has wrong color (visited).");
                  assertEquals(sensorMarker, sensorFeature.getStringProperty("marker-symbol"), "Sensor has wrong marker (visited)");
                }
              }
            }
            
            // Make sure sensor is marked as not visited
            if (!sensorVisited) {
              assertEquals("", sensorFeature.getStringProperty("marker-symbol"), "Sensor has wrong marker (not visited).");
              assertEquals("#aaaaaa", sensorFeature.getStringProperty("rgb-string"), "Sensor has wrong color (not visited).");
            }
          }
          
        }
    );
  }
  
  @AfterAll
  void tearDownAll() {
    // Delete files
    File flightPathFile = new File("flightpath-" + this.day + "-" + this.month + "-" + this.year + ".txt");
    flightPathFile.delete();
    
    File sensorReadingsFile = new File("readings-" + this.day + "-" + this.month + "-" + this.year + ".geojson");
    sensorReadingsFile.delete();
  }
}
