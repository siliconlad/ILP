package uk.ac.ed.inf.aqmaps.drone;

import uk.ac.ed.inf.aqmaps.webserver.Coordinates;

public class Sensor {
  // The What3Words location of the sensor
  public String location;
  // The battery level of the sensor
  public double battery;
  // The pollution reading from the sensor
  public String reading;
  // The coordinates of the sensor as an object
  public Coordinates coordinates;
  
  public Sensor(String loc, double battery, String reading, Coordinates coordinates) {
    this.location = loc;
    this.battery = battery;
    this.reading = reading;
    this.coordinates = coordinates;
  }
}
