package uk.ac.ed.inf.aqmaps.webserver;

public class Coordinates {
  public double lng;  // Longitude
  public double lat;  // Latitude
  
  public String toString() {
    return Double.toString(this.lng) + "," + Double.toString(this.lat);
  }
}
