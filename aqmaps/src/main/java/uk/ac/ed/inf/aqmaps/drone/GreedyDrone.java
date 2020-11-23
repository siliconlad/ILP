package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import uk.ac.ed.inf.aqmaps.webserver.Coordinates;

public class GreedyDrone {
  // Constants as defined in the specification
  private static final double MAX_SENSOR_RANGE = 0.0002;
  private static final double MAX_FINISH_RANGE = 0.0003;
  private static final double TRAVEL_DISTANCE = 0.0003;
  private static final int MAX_MOVES = 150;
  
  private ArrayList<Sensor> notVisited;
  private ArrayList<PathPoint> route;
  private FeatureCollection noFlyZones;
  private Coordinates startPos;
  private Coordinates currentPos;
  private int battery;
  
  public GreedyDrone(ArrayList<Sensor> sensors, Coordinates startPos, FeatureCollection noFlyZones) {
    // Set drone state
    this.notVisited = sensors;
    this.route = new ArrayList<PathPoint>();
    this.noFlyZones = noFlyZones;
    this.startPos = startPos;
    this.currentPos = startPos;
    this.battery = MAX_MOVES;
  }
  
  
  public Route getRoute() {
    return new Route(this.route, this.notVisited, this.noFlyZones);
  }
  
  public void calculateRoute() {
    // Continue until we visit all sensors
    while (this.notVisited.size() > 0) {
      // Get the closest sensor from current position
      var nextSensor = getNextSensor(this.currentPos, this.notVisited);
      
      // Debugging
      System.out.println();
      System.out.println("Next target: " + nextSensor.location + " " + nextSensor.coordinates.toString());
      System.out.println("Current pos: " + currentPos.toString());
      
      // Find route to sensor
      var routeToSensor = getRouteToSensor(this.currentPos, nextSensor);
      
      // Add path to route
      this.route.addAll(routeToSensor);
      
      // Update battery level
      this.battery -= routeToSensor.size();
      
      // Remove sensor from list
      this.notVisited.remove(nextSensor);
      
      // Update position
      this.currentPos = routeToSensor.get(routeToSensor.size() - 1).endPos;
    }
    
    // Once we visit each node return to the beginning
    route.addAll(getRouteToStart(this.currentPos, this.startPos));
    
    System.out.println("Battery: " + this.battery);
    
  }
  
  static private Sensor getNextSensor(Coordinates currentPos, ArrayList<Sensor> sensors) {
    Sensor closestSensor = null;
    double smallestDistance = Double.POSITIVE_INFINITY;
    
    for (var sensor : sensors) {
      var distance = getDistance(currentPos, sensor.coordinates);
      
      // Store the first sensor
      if (closestSensor == null) {
        closestSensor = sensor;
        smallestDistance = distance;
        continue;
      }
      
      if (distance < smallestDistance) {
          closestSensor = sensor;
          smallestDistance = distance;
      }
    }
    
    return closestSensor;
  }
 
  static private ArrayList<PathPoint> getRouteToSensor(Coordinates currentPos, Sensor sensor) {
    var routeToSensor = new ArrayList<PathPoint>();
    
    // TODO: incorporate max number of moves
    // TODO: Check battery
    
    do {
      // Construct route one move at a time
      var nextPoint = getNextPathPoint(currentPos, sensor.coordinates);
      routeToSensor.add(nextPoint);
      
      // Update current position
      currentPos = nextPoint.endPos;

      // Debugging
      System.out.println("Current pos: " + currentPos.toString());
      
    } while (getDistance(currentPos, sensor.coordinates) >= MAX_SENSOR_RANGE);
    
    // Set the sensor element of the last point in the route
    routeToSensor.get(routeToSensor.size() - 1).sensor = sensor;
    System.out.println("Found path to " + sensor.location);
    
    return routeToSensor;
  }
  
  static private ArrayList<PathPoint> getRouteToStart(Coordinates currentPos, Coordinates target) {
    var routeToStart = new ArrayList<PathPoint>();

    // Debugging
    System.out.println();
    System.out.println("Returning to start...");
    
    while (getDistance(currentPos, target) >= MAX_FINISH_RANGE) {
      // Construct route one move at a time
      var nextPoint = getNextPathPoint(currentPos, target);
      routeToStart.add(nextPoint);
      
      // Update current position
      currentPos = nextPoint.endPos;
      
      // Debugging
      System.out.println("Current pos: " + currentPos.toString());
    }
    
    // Debugging
    System.out.println("Returned to start");
    
    return routeToStart;
  }
  
  static private PathPoint getNextPathPoint(Coordinates currentPos, Coordinates target) {
    double minDistance = getDistance(move(currentPos, 0), target);
    int direction = 0;
    
    for (var i = 10; i < 360; i += 10) {
      var distance = getDistance(move(currentPos, i), target);
      
      if (distance < minDistance) {
        minDistance = distance;
        direction = i;
      } 
    }
    
    // Create PathPoint
    var closestPoint = new PathPoint();
    closestPoint.startPos = currentPos;
    closestPoint.endPos = move(currentPos, direction);
    closestPoint.direction = direction;
    closestPoint.sensor = null;
    closestPoint.distanceScore = minDistance;
    
    return closestPoint;
  }
  
  static private Coordinates move(Coordinates currentPos, int direction) {
    var newCoordinates = new Coordinates();
    newCoordinates.lat = currentPos.lat + (TRAVEL_DISTANCE * Math.sin(Math.toRadians(direction)));
    newCoordinates.lng = currentPos.lng + (TRAVEL_DISTANCE * Math.cos(Math.toRadians(direction)));
    
    return newCoordinates;
  }
  
  static double getDistance(Coordinates start, Coordinates end) {
    double squaredLng = Math.pow(start.lng - end.lng, 2);
    double squaredLat = Math.pow(start.lat - end.lat, 2);
    return Math.sqrt(squaredLng + squaredLat);
  }
  
}
