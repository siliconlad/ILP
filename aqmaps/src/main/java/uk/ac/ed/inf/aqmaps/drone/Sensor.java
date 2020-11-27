package uk.ac.ed.inf.aqmaps.drone;

public class Sensor {
  public String location;
  public Double battery;
  public String reading;
  public Coordinates coordinates;
  
  @Override
  public String toString()  {
    // Coordinates may not be straight away
    var coordinatesString = (this.coordinates == null) ? "null" : this.coordinates.toString();
    
    return "Sensor Object" + "\n" +
           "Location: " + this.location + "\n" + 
           "Battery: " + this.battery + "\n" +
           "Reading: " + this.reading + "\n" +
           "Coordinates: " + coordinatesString;
  }
}
