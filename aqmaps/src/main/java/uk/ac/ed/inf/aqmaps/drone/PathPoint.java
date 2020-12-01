package uk.ac.ed.inf.aqmaps.drone;

class PathPoint {
  public Coordinates startPos;
  public Coordinates endPos;
  public Integer direction;
  public Sensor sensor;
  
  // Used for the path planning algorithm only
  protected Double distanceTravelled;
  protected Double distanceScore;
  protected PathPoint prev;
  
  @Override
  public String toString() {
    // May not be initialised in the beginning
    var startPosString = (this.startPos == null) ? "null" : this.startPos.toString();
    var endPosString = (this.endPos == null) ? "null" : this.endPos.toString();
    var directionString = (this.direction == null) ? "null" : this.direction.toString();
    var sensorString = (this.sensor == null) ? "null" : this.sensor.location;

    return startPosString + "," + directionString + "," + endPosString + "," + sensorString;
  }
}
