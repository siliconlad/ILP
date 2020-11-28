package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

public class GreedyDrone {
  // Constants as defined in the specification
  private static final double MAX_SENSOR_RANGE = 0.0002;
  private static final double MAX_FINISH_RANGE = 0.0003;
  private static final double TRAVEL_DISTANCE = 0.0003;
  private static final int MAX_MOVES = 150;
  
  // Confinement area description
  private static final Point2D NORTH_WEST = new Point2D.Double(-3.192473, 55.946233);
  private static final Point2D SOUTH_WEST = new Point2D.Double(-3.192473, 55.942617);
  private static final Point2D SOUTH_EAST = new Point2D.Double(-3.184319, 55.942617);
  private static final Point2D NORTH_EAST = new Point2D.Double(-3.184319, 55.946233);
  
  private static final Line2D NORTH_BOUNDARY = new Line2D.Double(NORTH_WEST, NORTH_EAST);
  private static final Line2D WEST_BOUNDARY = new Line2D.Double(NORTH_WEST, SOUTH_WEST);
  private static final Line2D SOUTH_BOUNDARY = new Line2D.Double(SOUTH_WEST, SOUTH_EAST);
  private static final Line2D EAST_BOUNDARY = new Line2D.Double(NORTH_EAST, SOUTH_EAST);
  
  private ArrayList<Sensor> notVisited;
  private ArrayList<PathPoint> route;
  private FeatureCollection noFlyZones;
  private Coordinates startPos;
  private Coordinates currentPos;
  private int battery;
  private ArrayList<Line2D> boundaryLines;
  
  public GreedyDrone(ArrayList<Sensor> sensors, Coordinates startPos, FeatureCollection noFlyZones) {
    this.notVisited = sensors;
    this.route = new ArrayList<PathPoint>();
    this.noFlyZones = noFlyZones;
    this.startPos = startPos;
    this.currentPos = startPos;
    this.battery = MAX_MOVES;
    
    this.boundaryLines = new ArrayList<Line2D>();
    this.boundaryLines.add(NORTH_BOUNDARY);
    this.boundaryLines.add(WEST_BOUNDARY);
    this.boundaryLines.add(SOUTH_BOUNDARY);
    this.boundaryLines.add(EAST_BOUNDARY);
    this.boundaryLines.addAll(getBoundaryLines(noFlyZones));
    
    this.calculateRoute();
  }
  
  public Route getRoute() {
    return new Route(this.route, this.notVisited, this.noFlyZones);
  }
  
  private void calculateRoute() {
    while (this.notVisited.size() > 0) {
      var nextSensor = this.getClosestSensor(this.currentPos, this.notVisited);
      
      System.out.println();
      System.out.println("Next target: " + nextSensor.location + " " + nextSensor.coordinates.toString());
      System.out.println("Current pos: " + currentPos.toString());
      
      var routeToSensor = this.getRouteToSensor(this.currentPos, nextSensor);

      // TODO: Check for battery
      
      // Update state
      this.route.addAll(routeToSensor);
      this.battery -= routeToSensor.size();
      this.notVisited.remove(nextSensor);
      this.currentPos = routeToSensor.get(routeToSensor.size() - 1).endPos;
    }
    
    // Once we visit each node return to the beginning
    route.addAll(this.getRouteToTarget(this.currentPos, this.startPos));
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
      var nextPoint = this.getNextPathPoint(currentPos, sensor.coordinates);
      currentPos = nextPoint.endPos;
      routeToSensor.add(nextPoint);
      System.out.println("Current pos: " + currentPos.toString());
    } while (getDistance(currentPos, sensor.coordinates) >= MAX_SENSOR_RANGE);
    
    routeToSensor.get(routeToSensor.size() - 1).sensor = sensor;
    System.out.println("Found path to " + sensor.location);
    
    return routeToSensor;
  }
  
  private ArrayList<PathPoint> getRouteToTarget(Coordinates currentPos, Coordinates target) {
    var routeToStart = new ArrayList<PathPoint>();
    
    while (getDistance(currentPos, target) >= MAX_FINISH_RANGE) {
      // Construct route one move at a time
      var nextPoint = this.getNextPathPoint(currentPos, target);
      routeToStart.add(nextPoint);
      currentPos = nextPoint.endPos;
      System.out.println("Current pos: " + currentPos.toString());
    }
    
    return routeToStart;
  }
  
  private PathPoint getNextPathPoint(Coordinates currentPos, Coordinates target) {
    double minDistance = Double.POSITIVE_INFINITY;
    int direction = 0;
    
    for (var i = 0; i < 360; i += 10) {
      var nextPos = move(currentPos, i);
      var distance = getDistance(nextPos, target);
      
      if (distance < minDistance) {
        var moveIsValid = isMoveValid(currentPos, nextPos, this.boundaryLines);
        if (moveIsValid) {
          minDistance = distance;
          direction = i;
        }
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
  
  private static boolean isMoveValid(Coordinates start, Coordinates end, ArrayList<Line2D> boundaryLines) {
    var startPoint = new Point2D.Double(start.lng, start.lat);
    var endPoint = new Point2D.Double(end.lng, end.lat);
    var moveLine = new Line2D.Double(startPoint, endPoint);
    
    for (var boundary : boundaryLines) {
      if (moveLine.intersectsLine(boundary)) {
        return false;
      }
    }
    
    return true;
  }
    
  
  private static ArrayList<Line2D> getBoundaryLines(FeatureCollection noFlyZones) {
    var boundaryLines = new ArrayList<Line2D>();
    
    var noFlyZonesFeatures = noFlyZones.features();
    for (var noFlyZoneFeature : noFlyZonesFeatures) {
      var noFlyZoneGeometry = noFlyZoneFeature.geometry();
      var noFlyZonePolygon = (Polygon)noFlyZoneGeometry;
      var noFlyZoneCoordinates = noFlyZonePolygon.coordinates().get(0);
      
      for (var i=0; i < noFlyZoneCoordinates.size() - 1; i++) {
        var start = noFlyZoneCoordinates.get(i);
        var end = noFlyZoneCoordinates.get(i+1);
        
        var startPoint = new Point2D.Double(start.longitude(), start.latitude());
        var endPoint = new Point2D.Double(end.longitude(), end.latitude());
        
        boundaryLines.add(new Line2D.Double(startPoint, endPoint));
      }
    }

    return boundaryLines;
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
