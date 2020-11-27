package uk.ac.ed.inf.aqmaps.drone;

public class Coordinates {
  public double lng;  // Longitude
  public double lat;  // Latitude
  
  @Override
  public String toString() {
    return Double.toString(this.lng) + "," + Double.toString(this.lat);
  }
}
