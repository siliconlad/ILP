package uk.ac.ed.inf.aqmaps.drone;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class Route {
  
  private ArrayList<PathPoint> dronePath;
  private ArrayList<Sensor> skippedSensors;
  private ArrayList<Sensor> visitedSensors;
  private FeatureCollection noFlyZones;
  private FeatureCollection map;
  
  public Route(ArrayList<PathPoint> dronePath, ArrayList<Sensor> skippedSensors, FeatureCollection noFlyZones) {
    this.dronePath = dronePath;
    this.skippedSensors = skippedSensors;
    this.visitedSensors = this.getVisitedSensorsFromDronePath();
    this.noFlyZones = noFlyZones;
  }
  
  public void saveMap(String fileName) throws IOException {
    if (this.map == null) {
      throw new RuntimeException("Map not built. Call buildMap method first.");
    }

    var saveFile = new FileWriter(fileName);
    saveFile.write(this.map.toJson());
    saveFile.close();
  }
  
  public void saveRoute(String fileName) throws IOException{
    var routeFileWriter = new FileWriter(fileName);
    
    for (var i=0; i < this.dronePath.size(); i++) {
      routeFileWriter.write(Integer.toString(i+1) + "," + this.dronePath.get(i).toString() + '\n');
    }
    
    routeFileWriter.close();
  }
  
  public void buildMap(boolean showNoFlyZones) {
    var mapFeatures = new ArrayList<Feature>();
    
    // Add drone path
    var dronePathFeature = this.createPathFeature();
    mapFeatures.add(dronePathFeature);
    
    // Add visited sensor markers
    for (var sensor: this.visitedSensors) {
      var sensorMarker = this.createSensorMarker(sensor, true);
      mapFeatures.add(sensorMarker);
    }
    
    // Add skipped sensor markers
    for (var sensor: this.skippedSensors) {
      var sensorMarker = this.createSensorMarker(sensor, false);
      mapFeatures.add(sensorMarker);
    }
    
    // Show no fly zones
    if (showNoFlyZones) {
      mapFeatures.addAll(this.noFlyZones.features());
    }
    
    this.map = FeatureCollection.fromFeatures(mapFeatures);
  }
  
  private ArrayList<Sensor> getVisitedSensorsFromDronePath() {
    var visitedSensors = new ArrayList<Sensor>();
    
    for (var pathPoint : this.dronePath) {
      if (pathPoint.sensor != null) {
        visitedSensors.add(pathPoint.sensor);
      }
    }
    
    return visitedSensors;
  }
  
  private Feature createPathFeature() {
    var mapPathPoints = new ArrayList<Point>();
    
    if (this.dronePath.size() > 0) {
      var startPathPoint = Point.fromLngLat(this.dronePath.get(0).startPos.lng, this.dronePath.get(0).startPos.lat);
      mapPathPoints.add(startPathPoint);
    }

    for (var pathPoint : this.dronePath) {      
      var pathPointPoint = Point.fromLngLat(pathPoint.endPos.lng, pathPoint.endPos.lat);
      mapPathPoints.add(pathPointPoint);
    }
    
    var mapPathLineString = LineString.fromLngLats(mapPathPoints);
    var mapPathGeometry = (Geometry)mapPathLineString;
    var mapPathFeature = Feature.fromGeometry(mapPathGeometry);
    
    return mapPathFeature;
  }
  
  private Feature createSensorMarker(Sensor sensor, boolean visited) {
    String markerColor = this.getHexColor(sensor, visited);
    String markerSymbol = this.getMarkerSymbol(sensor, visited);

    var sensorPoint = Point.fromLngLat(sensor.coordinates.lng, sensor.coordinates.lat);
    var sensorGeometry = (Geometry)sensorPoint;
    var sensorFeature = Feature.fromGeometry(sensorGeometry);
    
    // Add properties
    sensorFeature.addStringProperty("location", sensor.location);
    sensorFeature.addStringProperty("rgb-string", markerColor);
    sensorFeature.addStringProperty("marker-color", markerColor);
    sensorFeature.addStringProperty("marker-symbol", markerSymbol);
  
    return sensorFeature;
  }
  
  private String getHexColor(Sensor sensor, boolean visited) {
    if (!visited) {
      return "#aaaaaa";
    }
    
    // Battery too low, hence reading is unreliable
    if (sensor.battery.doubleValue() < 10) {
      return "#000000";
    }
    
    // Sensor reading must now be a double
    var sensorReading = Double.parseDouble(sensor.reading);
    if (sensorReading < 0) {
      return "#000000";
    } else if (sensorReading < 32) {
      return "#00ff00";
    } else if (sensorReading < 64) {
      return "#40ff00";
    } else if (sensorReading < 96) {
      return "#80ff00";
    } else if (sensorReading < 128) {
      return "#c0ff00";
    } else if (sensorReading < 160) {
      return "#ffc000";
    } else if (sensorReading < 192) {
      return "#ff8000";
    } else if (sensorReading < 224) {
      return "#ff4000";
    } else if (sensorReading < 256) {
      return "#ff0000";
    }
    
    // Default to black to indicate faulty sensor
    return "#000000";
  }
  
  private String getMarkerSymbol(Sensor sensor, boolean visited) {
    if (!visited) {
      return "";
    }
   
    // Battery too low, hence reading is unreliable
    if (sensor.battery.doubleValue() < 10) {
      return "cross";
    } 
   
    // Sensor reading must now be a double
    var sensorReading = Double.parseDouble(sensor.reading);
    if (sensorReading < 0) {
      return "cross";
    } else if (sensorReading < 128) {
      return "lighthouse";
    } else if (sensorReading < 256) {
      return "danger";
    }
   
    // Default to the cross symbol to indicate faulty sensor
    return "cross";
  } 
}