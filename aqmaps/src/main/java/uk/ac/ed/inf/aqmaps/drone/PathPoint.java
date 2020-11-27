package uk.ac.ed.inf.aqmaps.drone;

public class PathPoint {
  public Coordinates startPos;
  public Coordinates endPos;
  public int direction;
  public Sensor sensor;
  public double distanceScore;
  
  public String toString() {
    var sensorString = (this.sensor == null) ? "null" : sensor.location;
    return this.startPos.toString() + "," + Integer.toString(this.direction) + "," + this.endPos.toString() + "," + sensorString;
  }
}
