package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;

public class GreedyDrone extends Drone {
  private ArrayList<Sensor> notVisited;
  private ArrayList<PathPoint> route;
  private FeatureCollection noFlyZones;
  private Coordinates startPos;
  private Coordinates currentPos;
  
  public GreedyDrone(ArrayList<Sensor> sensors, Coordinates startPos, FeatureCollection noFlyZones) {
    super(noFlyZones);
    
    this.notVisited = sensors;
    this.route = new ArrayList<PathPoint>();
    this.noFlyZones = noFlyZones;
    this.startPos = startPos;
    this.currentPos = startPos;

    this.calculateRoute();
  }
  
  public Route getRoute() {
    return new Route(this.route, this.notVisited, this.noFlyZones);
  }
  
  private void calculateRoute() {
    var skippedSensors = new ArrayList<Sensor>();
    
    while (this.notVisited.size() > 0) {
      var nextSensor = this.getClosestSensor(this.currentPos, this.notVisited);
      
      System.out.println();
      System.out.println("Next target: " + nextSensor.location);
      System.out.println("Target pos: " + nextSensor.coordinates.toString());
      System.out.println("Current pos: " + currentPos.toString());
      
      try {
        var routeToSensor = this.getRouteToSensor(this.currentPos, nextSensor);

        // Ensure we can always return to the start
        try {
          this.getRouteToTarget(routeToSensor.get(routeToSensor.size() - 1).endPos, this.startPos);
        } catch (BatteryLimitException e) {
          System.out.println("Would not be able to return to start. Skipping sensor...");
          this.notVisited.remove(nextSensor);
          skippedSensors.add(nextSensor);
          continue;
        } catch (RouteNotFoundException e) {
          System.out.println("Cannot find path back to start. Skipping sensor...");
          this.notVisited.remove(nextSensor);
          skippedSensors.add(nextSensor);
          continue;
        }
        
        // Update state
        this.route.addAll(routeToSensor);
        this.battery -= routeToSensor.size();
        this.notVisited.remove(nextSensor);
        this.currentPos = routeToSensor.get(routeToSensor.size() - 1).endPos;
        System.out.println("Battery: " + this.battery);
        
        // Skipped sensors may be reachable after moving to nextSensor so add them back
        this.notVisited.addAll(skippedSensors);
        skippedSensors.clear();
      } catch (BatteryLimitException e) {
        // Return to start because no other accessible sensor is going to be closer
        System.out.println("Route exceeds battery limit. Returning to start...");
        break;
      } catch (RouteNotFoundException e) {
        System.out.println("Path planning got stuck. Skipping sensor...");
        this.notVisited.remove(nextSensor);
        skippedSensors.add(nextSensor);
      }
    }
    
    System.out.println();
    System.out.println("Returning to start...");

    try {
      var routeToStart = this.getRouteToTarget(this.currentPos, this.startPos);
      route.addAll(routeToStart);
      this.battery -= routeToStart.size();
      this.currentPos = routeToStart.get(routeToStart.size() - 1).endPos;
    } catch (BatteryLimitException | RouteNotFoundException e) {
      System.out.println();
      System.out.println("FAILED to return to start.");
      System.out.println();
    }

    System.out.println("Battery: " + this.battery);
    
    // notVisited should have all the sensors that were not visited at the end of execution
    this.notVisited.addAll(skippedSensors);
    if (this.notVisited.size() > 0) {
      System.out.println();
      System.out.println(this.notVisited.size() + " sensors were not were visited.");
      System.out.println();
    }
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
 
  private ArrayList<PathPoint> getRouteToSensor(Coordinates currentPos, Sensor sensor) throws BatteryLimitException, RouteNotFoundException {
    var routeToSensor = new ArrayList<PathPoint>();
    
    // Specification requires movement before reaching sensor
    do {
      var nextPoint = this.getNextPathPoint(currentPos, sensor.coordinates);
      
      // Check to see if algorithm is stuck in a loop
      var routeLength = routeToSensor.size();
      if (routeLength > 0) {
        var currDirection = routeToSensor.get(routeLength - 1).direction;
        if (currDirection == (nextPoint.direction + 180) % 360) {
          throw new RouteNotFoundException("Greedy algorithm got stuck in loop.");
        }
      }
      
      currentPos = nextPoint.endPos;
      routeToSensor.add(nextPoint);
      System.out.println("Move to: " + currentPos.toString());
      
      if (routeToSensor.size() > this.battery) {
        throw new BatteryLimitException("Route exceeds battery limit");
      }
      
    } while (getDistance(currentPos, sensor.coordinates) >= MAX_SENSOR_RANGE);
    
    routeToSensor.get(routeToSensor.size() - 1).sensor = sensor;
    System.out.println("Found path to " + sensor.location);
    
    return routeToSensor;
  }
  
  private ArrayList<PathPoint> getRouteToTarget(Coordinates currentPos, Coordinates target) throws BatteryLimitException, RouteNotFoundException {
    var routeToStart = new ArrayList<PathPoint>();
    
    System.out.println("Current pos: " + currentPos.toString());
    
    while (getDistance(currentPos, target) >= MAX_FINISH_RANGE) {
      var nextPoint = this.getNextPathPoint(currentPos, target);
      
      // Check to see if algorithm is stuck in a loop
      var routeLength = routeToStart.size();
      if (routeLength > 0) {
        var currDirection = routeToStart.get(routeLength - 1).direction;
        if (currDirection == (nextPoint.direction + 180) % 360) {
          throw new RouteNotFoundException("Greedy algorithm got stuck in loop.");
          }
      }
      
      currentPos = nextPoint.endPos;
      routeToStart.add(nextPoint);
      System.out.println("Move to: " + currentPos.toString());
      
      if (routeToStart.size() > this.battery) {
        throw new BatteryLimitException("Route exceeds battery limit");
      }
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
        if (isMoveValid(currentPos, nextPos)) {
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
}
