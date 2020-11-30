package uk.ac.ed.inf.aqmaps.drone;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

public abstract class Drone {
  // Constants as defined in the specification
  protected static final double MAX_SENSOR_RANGE = 0.0002;
  protected static final double MAX_FINISH_RANGE = 0.0003;
  protected static final double TRAVEL_DISTANCE = 0.0003;
  protected static final int MAX_MOVES = 150;
  
  // Confinement area description as defined in the specification
  protected static final Point2D NORTH_WEST = new Point2D.Double(-3.192473, 55.946233);
  protected static final Point2D SOUTH_WEST = new Point2D.Double(-3.192473, 55.942617);
  protected static final Point2D SOUTH_EAST = new Point2D.Double(-3.184319, 55.942617);
  protected static final Point2D NORTH_EAST = new Point2D.Double(-3.184319, 55.946233);
  
  protected static final Line2D NORTH_BOUNDARY = new Line2D.Double(NORTH_WEST, NORTH_EAST);
  protected static final Line2D WEST_BOUNDARY = new Line2D.Double(NORTH_WEST, SOUTH_WEST);
  protected static final Line2D SOUTH_BOUNDARY = new Line2D.Double(SOUTH_WEST, SOUTH_EAST);
  protected static final Line2D EAST_BOUNDARY = new Line2D.Double(NORTH_EAST, SOUTH_EAST);
  
  // Object attributes used by all drones
  protected int battery = MAX_MOVES;
  protected ArrayList<Line2D> boundaryLines;
  protected FeatureCollection noFlyZones;
  
  public Drone(FeatureCollection noFlyZones) {
    this.noFlyZones = noFlyZones;
    
    // Construct boundary lines from noFlyZones
    this.boundaryLines = new ArrayList<Line2D>();
    this.boundaryLines.add(NORTH_BOUNDARY);
    this.boundaryLines.add(WEST_BOUNDARY);
    this.boundaryLines.add(SOUTH_BOUNDARY);
    this.boundaryLines.add(EAST_BOUNDARY);
    this.boundaryLines.addAll(getBoundaryLines(noFlyZones));
  }
  
  // Define methods that must be implemented by Drone subclasses
  public abstract Route getRoute();
  
  protected boolean isMoveValid(Coordinates start, Coordinates end) {
    var startPoint = new Point2D.Double(start.lng, start.lat);
    var endPoint = new Point2D.Double(end.lng, end.lat);
    var moveLine = new Line2D.Double(startPoint, endPoint);
    
    for (var boundary : this.boundaryLines) {
      if (moveLine.intersectsLine(boundary)) {
        return false;
      }
    }
    
    return true;
  }
  
  // Static methods common to all drones  
  protected static ArrayList<Line2D> getBoundaryLines(FeatureCollection noFlyZones) {
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
  
  protected static Coordinates move(Coordinates currentPos, int direction) {
    var newCoordinates = new Coordinates();
    newCoordinates.lat = currentPos.lat + (TRAVEL_DISTANCE * Math.sin(Math.toRadians(direction)));
    newCoordinates.lng = currentPos.lng + (TRAVEL_DISTANCE * Math.cos(Math.toRadians(direction)));
    
    return newCoordinates;
  }
  
  protected static double getDistance(Coordinates start, Coordinates end) {
    double squaredLng = Math.pow(start.lng - end.lng, 2);
    double squaredLat = Math.pow(start.lat - end.lat, 2);
    return Math.sqrt(squaredLng + squaredLat);
  }
}
