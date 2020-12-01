package uk.ac.ed.inf.aqmaps.drone;

class PathPoint {
  public Coordinates startPos;
  public Coordinates endPos;
  public Integer direction;
  public Sensor sensor;
  public Double distanceTravelled;
  public Double distanceScore;
  public PathPoint prev;
  
  @Override
  public String toString() {
    // May not be initialised in the beginning
    var endPosString = (this.endPos == null) ? "null" : this.endPos.toString();
    var directionString = (this.direction == null) ? "null" : this.direction.toString();
    var sensorString = (this.sensor == null) ? "null" : this.sensor.location;

    return this.startPos.toString() + "," + directionString + "," + endPosString + "," + sensorString;
  }
}
