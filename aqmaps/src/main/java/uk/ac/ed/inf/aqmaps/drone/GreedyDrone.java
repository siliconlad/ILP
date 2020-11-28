package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;

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
    this.notVisited = sensors;
    this.route = new ArrayList<PathPoint>();
    this.noFlyZones = noFlyZones;
    this.startPos = startPos;
    this.currentPos = startPos;
    this.battery = MAX_MOVES;
    
    this.calculateRoute();
  }
  
  public Route getRoute() {
    return new Route(this.route, this.notVisited, this.noFlyZones);
  }
  
  private void calculateRoute() {
    while (this.notVisited.size() > 0) {
      var nextSensor = getClosestSensor(this.currentPos, this.notVisited);
      
      System.out.println();
      System.out.println("Next target: " + nextSensor.location + " " + nextSensor.coordinates.toString());
      System.out.println("Current pos: " + currentPos.toString());
      
      var routeToSensor = getRouteToSensor(this.currentPos, nextSensor);
      
      // Update state
      this.route.addAll(routeToSensor);
      this.battery -= routeToSensor.size();
      this.notVisited.remove(nextSensor);
      this.currentPos = routeToSensor.get(routeToSensor.size() - 1).endPos;
    }
    
    // Once we visit each node return to the beginning
    route.addAll(getRouteToTarget(this.currentPos, this.startPos));
    System.out.println("Battery: " + this.battery);
    
  }
  
  private Sensor getClosestSensor(Coordinates currentPos, ArrayList<Sensor> sensors) {
    Sensor closestSensor = null;
    double smallestDistance = Double.POSITIVE_INFINITY;
    
    for (var sensor : sensors) {
      var distance = getDistance(currentPos, sensor.coordinates);
      if (distance < smallestDistance) {
          closestSensor = sensor;
          smallestDistance = distance;
      }
    }
    
    return closestSensor;
  }
 
  private ArrayList<PathPoint> getRouteToSensor(Coordinates currentPos, Sensor sensor) {
    var routeToSensor = new ArrayList<PathPoint>();
    
    // Specification requires movement before reaching sensor
    do {
      var nextPoint = getNextPathPoint(currentPos, sensor.coordinates);
      currentPos = nextPoint.endPos;
      routeToSensor.add(nextPoint);
    } while (getDistance(currentPos, sensor.coordinates) >= MAX_SENSOR_RANGE);
    
    routeToSensor.get(routeToSensor.size() - 1).sensor = sensor;
    System.out.println("Found path to " + sensor.location);
    
    return routeToSensor;
  }
  
  private static ArrayList<PathPoint> getRouteToTarget(Coordinates currentPos, Coordinates target) {
    var routeToStart = new ArrayList<PathPoint>();
    
    while (getDistance(currentPos, target) >= MAX_FINISH_RANGE) {
      // Construct route one move at a time
      var nextPoint = getNextPathPoint(currentPos, target);
      routeToStart.add(nextPoint);
      currentPos = nextPoint.endPos;
      System.out.println("Current pos: " + currentPos.toString());
    }
    
    return routeToStart;
  }
  
  private static PathPoint getNextPathPoint(Coordinates currentPos, Coordinates target) {
    double minDistance = Double.POSITIVE_INFINITY;
    int direction = 0;
    
    for (var i = 0; i < 360; i += 10) {
      var distance = getDistance(move(currentPos, i), target);
      
      if (distance < minDistance) {
        minDistance = distance;
        direction = i;
      } 
    }
    
    var closestPoint = new PathPoint();
    closestPoint.startPos = currentPos;
    closestPoint.endPos = move(currentPos, direction);
    closestPoint.direction = direction;
    closestPoint.sensor = null;
    closestPoint.distanceScore = minDistance;
    
    return closestPoint;
  }
  
  private static Coordinates move(Coordinates currentPos, int direction) {
    var newCoordinates = new Coordinates();
    newCoordinates.lat = currentPos.lat + (TRAVEL_DISTANCE * Math.sin(Math.toRadians(direction)));
    newCoordinates.lng = currentPos.lng + (TRAVEL_DISTANCE * Math.cos(Math.toRadians(direction)));
    
    return newCoordinates;
  }
  
  private static double getDistance(Coordinates start, Coordinates end) {
    double squaredLng = Math.pow(start.lng - end.lng, 2);
    double squaredLat = Math.pow(start.lat - end.lat, 2);
    return Math.sqrt(squaredLng + squaredLat);
  }
  
}
