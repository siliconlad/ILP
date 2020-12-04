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
      
      try {
        var routeToSensor = this.getRouteToSensor(this.currentPos, nextSensor);
        
        // Update state (note: routeToSensor.size() > 0 due to specification)
        this.route.addAll(routeToSensor);
        this.battery -= routeToSensor.size();
        this.notVisited.remove(nextSensor);
        this.currentPos = routeToSensor.get(routeToSensor.size() - 1).endPos;
      } catch (BatteryLimitException e) {
        // Return to start because no other sensor is going to be closer
        System.out.println("Route exceeds battery limit.");
        break;
      }
    }
    
    System.out.println("Returning to start...");
    var routeToStart = this.getRouteToStart(this.currentPos);
   
    // Update state
    if (routeToStart.size() > 0) {
      this.route.addAll(routeToStart);
      this.battery -= routeToStart.size();
      this.currentPos = routeToStart.get(routeToStart.size() - 1).endPos;
    }
    
    System.out.println("Battery: " + this.battery);
    System.out.println("Visited " + (33 - this.notVisited.size()) + "/33 sensors.");
    System.out.println("Finished.");
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
  
    // The last move in the route is when we connect to the sensor
    routeToSensor.get(routeToSensor.size() - 1).sensor = sensor;
    
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
      System.out.println("Battery too low to return to start.");
      routeToStart.subList(0, this.battery);
    }
  
    return routeToStart;
  }
  
  private ArrayList<PathPoint> getRoute(Coordinates currentPos, Coordinates target, double maxOffset) {
    // Stores all visited path points
    var closedPathPoints = new ArrayList<PathPoint>();
    
    // Stores all path points to be visited
    var openPathPoints = new ArrayList<PathPoint>();
    
    // Create initial path point
    var currentPathPoint = new PathPoint();
    currentPathPoint.endPos = currentPos;
    currentPathPoint.distanceTravelled = 0.0;
    
    do {
      var newPathPoints = this.generatePathPoints(currentPathPoint, target, LIMIT_BRANCHING_FACTOR);
      for (var pathPoint : newPathPoints) {
        var pointVisited = false;

        // If we have not yet expanded the pathPoint, the current one may be shorter
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
        
        // If we've already expanded the pathPoint the current one must have larger distanceScore due to constant step sizes
        for (var closedPathPoint : closedPathPoints) {            
          if (pathPoint.endPos.equals(closedPathPoint.endPos)) {
            pointVisited = true;
            break;
          }
        }
        
        if (pointVisited) {
          continue;
        }
        
        // Otherwise, we've never seen this pathPoint before so add it to the list
        openPathPoints.add(pathPoint);
      }
      
      // Add current pathPoint to the list of expanded pathPoints
      closedPathPoints.add(currentPathPoint);
      
      // Select new pathPoint
      currentPathPoint = this.getNextPathPoint(openPathPoints);
      openPathPoints.remove(currentPathPoint);
    } while (getDistance(currentPathPoint.endPos, target) >= maxOffset);
    
    // Reconstruct path
    var routeToTarget = new ArrayList<PathPoint>();
    while (currentPathPoint.prev != null) {
      routeToTarget.add(0, currentPathPoint);
      currentPathPoint = currentPathPoint.prev;
    }
    routeToTarget.get(0).prev = null;
    
    return routeToTarget;
  }

  private PathPoint getNextPathPoint(ArrayList<PathPoint> nextPathPoints) {
    var minDistance = Double.POSITIVE_INFINITY;
    PathPoint minPathPoint = null;
    
    // Find pathPoint with lowest distanceScore
    for (var pathPoint : nextPathPoints) {
      if (pathPoint.distanceScore.doubleValue() < minDistance) {
        minDistance = pathPoint.distanceScore.doubleValue();
        minPathPoint = pathPoint;
      }
    }

    return minPathPoint;
  }
  
  private ArrayList<PathPoint> generatePathPoints(PathPoint currentPathPoint, Coordinates target, int limit) {
    var nextPathPoints = new ArrayList<PathPoint>();
    var currentPos = currentPathPoint.endPos;
    
    // Generate all valid pathPoints
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
    
    // Select the closest `limit` number of path points to reduce time complexity
    var limitedPathPoints = new ArrayList<PathPoint>();
    for (var i=0; i < limit && i < nextPathPoints.size(); i++) {
      var pathPoint = this.getNextPathPoint(nextPathPoints);
      limitedPathPoints.add(pathPoint);
      nextPathPoints.remove(pathPoint);
    }
    
    return limitedPathPoints;
  }
}
