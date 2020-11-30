package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;

public class AStarDrone extends Drone {
  private static final int LIMIT_BRANCHING_FACTOR = 5;
  
  private ArrayList<Sensor> notVisited;
  private ArrayList<PathPoint> route;
  private Coordinates startPos;
  private Coordinates currentPos;

  public AStarDrone(ArrayList<Sensor> sensors, Coordinates startPos, FeatureCollection noFlyZones) {
    super(noFlyZones);
    
    this.notVisited = sensors;
    this.route = new ArrayList<PathPoint>();
    this.startPos = startPos;
    this.currentPos = startPos;
    
    this.calculateRoute();
  }
  
  public Route getRoute() {
    return new Route(this.route, this.notVisited, this.noFlyZones);
  }
  
  private void calculateRoute() {    
    while (this.notVisited.size() > 0) {
      var nextSensor = this.getClosestSensor(this.currentPos, this.notVisited);
      
      System.out.println("Next target: " + nextSensor.location);
      System.out.println("Target pos: " + nextSensor.coordinates.toString());
      System.out.println("Current pos: " + currentPos.toString());
      
      try {
        var routeToSensor = this.getRouteToSensor(this.currentPos, nextSensor);
        
        // Update state (routeToSensor.size() > 0 due to specification)
        this.route.addAll(routeToSensor);
        this.battery -= routeToSensor.size();
        this.notVisited.remove(nextSensor);
        this.currentPos = routeToSensor.get(routeToSensor.size() - 1).endPos;
        System.out.println("Battery: " + this.battery);
      } catch (BatteryLimitException e) {
        // Return to start because no other accessible sensor is going to be closer
        System.out.println("Route exceeds battery limit. Returning to start...");
        break;
      }
    }
    
    System.out.println("Returning to start...");
    var routeToStart = this.getRouteToStart(this.currentPos);
   
    // Update state
    if (routeToStart.size() > 0) {
      route.addAll(routeToStart);
      this.battery -= routeToStart.size();
      this.currentPos = routeToStart.get(routeToStart.size() - 1).endPos;
    }
    System.out.println("Battery: " + this.battery);
    
    if (this.notVisited.size() > 0) {
      System.out.println(this.notVisited.size() + " sensors were not were visited.");
    }
  }
  
  private Sensor getClosestSensor(Coordinates currentPos, ArrayList<Sensor> sensors) {
    Sensor closestSensor = null;
    var smallestDistance = Double.POSITIVE_INFINITY;
    
    for (var sensor : sensors) {
      var distance = getDistance(currentPos, sensor.coordinates);
      if (distance < smallestDistance) {
          closestSensor = sensor;
          smallestDistance = distance;
      }
    }
    return closestSensor;
  }
  
  private ArrayList<PathPoint> getRouteToSensor(Coordinates currentPos, Sensor sensor) throws BatteryLimitException {
    var routeToSensor = this.getRoute(currentPos, sensor.coordinates, MAX_SENSOR_RANGE);
    
    if (routeToSensor.size() > this.battery) {
      throw new BatteryLimitException("Route exceeds battery limit.");
    }
  
    routeToSensor.get(routeToSensor.size() - 1).sensor = sensor;
    System.out.println("Found path to " + sensor.location);
    
    return routeToSensor;
  }
  
  private ArrayList<PathPoint> getRouteToStart(Coordinates currentPos) {
    var routeToStart = new ArrayList<PathPoint>();
    
    // Specification does not require us to move before we finish
    if (getDistance(currentPos, this.startPos) >= MAX_FINISH_RANGE) {
      routeToStart = this.getRoute(currentPos, this.startPos, MAX_FINISH_RANGE); 
    }
    
    // Trim to satisfy battery amount
    if (routeToStart.size() > this.battery) {
      routeToStart.subList(0, this.battery);
    }
  
    return routeToStart;
  }
  
  private ArrayList<PathPoint> getRoute(Coordinates currentPos, Coordinates target, double max_offset) {
    var route = new ArrayList<PathPoint>();
    var closedPathPoints = new ArrayList<PathPoint>();
    var openPathPoints = new ArrayList<PathPoint>();
    
    // Create initial path point
    var currentPathPoint = new PathPoint();
    currentPathPoint.endPos = currentPos;
    currentPathPoint.distanceTravelled = 0.0;
    
    do {
      var newPathPoints = this.generatePathPoints(currentPathPoint, target, LIMIT_BRANCHING_FACTOR);
      for (var pathPoint : newPathPoints) {
        var pointVisited = false;
        
        // If we have not yet expanded the path point, the current one may be shorter
        for (var openPathPoint : openPathPoints) {            
          if (pathPoint.endPos.equals(openPathPoint.endPos)) {
            if (pathPoint.distanceScore.doubleValue() < openPathPoint.distanceScore.doubleValue()) {
              openPathPoints.remove(openPathPoint);
              openPathPoints.add(pathPoint);
            }
            pointVisited = true;
            break;
          }
        }
        
        if (pointVisited) {
          continue;
        }
        
        // If we've already expanded the node the current node must be longer due to constant step sizes
        for (var closedPathPoint : closedPathPoints) {            
          if (pathPoint.endPos.equals(closedPathPoint.endPos)) {
            pointVisited = true;
            break;
          }
        }
        
        if (pointVisited) {
          continue;
        }
        
        // Otherwise, we've never seen this point before so add it to the list
        openPathPoints.add(pathPoint);
      }
      
      // Add current node to the list of expanded nodes
      closedPathPoints.add(currentPathPoint);
      
      // Select new node from frontier
      currentPathPoint = this.getNextPathPoint(openPathPoints, target);
      openPathPoints.remove(currentPathPoint);
    } while (getDistance(currentPathPoint.endPos, target) >= max_offset);
    
    // Reconstruct path
    while (currentPathPoint.prev != null) {
      route.add(0, currentPathPoint);
      currentPathPoint = currentPathPoint.prev;
    }
    route.get(0).prev = null;
    
    return route;
  }

  private PathPoint getNextPathPoint(ArrayList<PathPoint> nextPathPoints, Coordinates target) {
    var minDistance = Double.POSITIVE_INFINITY;
    PathPoint minPathPoint = null;
    
    if (nextPathPoints.size() == 0) {
      throw new IllegalArgumentException("nextPathPoints is an empty ArrayList.");
    }

    for (var pathPoint : nextPathPoints) {
      if (pathPoint.distanceScore.doubleValue() < minDistance) {
        minDistance = pathPoint.distanceScore;
        minPathPoint = pathPoint;
      }
    }

    return minPathPoint;
  }
  
  private ArrayList<PathPoint> generatePathPoints(PathPoint currentPathPoint, Coordinates target, int limit) {
    var nextPathPoints = new ArrayList<PathPoint>();
    var currentPos = currentPathPoint.endPos;
    
    for (var i=0; i < 360; i += 10) {
      var nextPos = move(currentPos, i);
      var distance = currentPathPoint.distanceTravelled + TRAVEL_DISTANCE;

      if (isMoveValid(currentPos, nextPos)) {
        var newPoint = new PathPoint();
        newPoint.startPos = currentPos;
        newPoint.endPos = nextPos;
        newPoint.direction = i;
        newPoint.sensor = null;
        newPoint.distanceTravelled = distance;
        newPoint.distanceScore = distance + getDistance(nextPos, target);
        newPoint.prev = currentPathPoint;
        nextPathPoints.add(newPoint);
      }
    }
    
    // Select the closest `limit` number of path points
    var limitedPathPoints = new ArrayList<PathPoint>();
    for (var i=0; i < limit && i < nextPathPoints.size(); i++) {
      var pathPoint = this.getNextPathPoint(nextPathPoints, target);
      limitedPathPoints.add(pathPoint);
      nextPathPoints.remove(pathPoint);
    }
    
    return limitedPathPoints;
  }
}
