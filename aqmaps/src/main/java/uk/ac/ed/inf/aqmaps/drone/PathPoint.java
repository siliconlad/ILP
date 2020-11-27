package uk.ac.ed.inf.aqmaps.drone;

public class PathPoint {
  public Coordinates startPos;
  public Coordinates endPos;
  public Integer direction;
  public Sensor sensor;
  public Double distanceScore;
  
  @Override
  public String toString() {
    // May not be initialized in the beginning
    var endPosString = (this.endPos == null) ? "null" : this.endPos.toString();
    var directionString = (this.direction == null) ? "null" : this.direction.toString();
    var sensorString = (this.sensor == null) ? "null" : this.sensor.location;

    return this.startPos.toString() + "," + directionString + "," + endPosString + "," + sensorString;
  }
}
