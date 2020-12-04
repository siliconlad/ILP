package uk.ac.ed.inf.aqmaps.drone;

public class Coordinates {
  public double lng;  // Longitude
  public double lat;  // Latitude
  
  @Override
  public String toString() {
    return Double.toString(this.lng) + "," + Double.toString(this.lat);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Coordinates)) {
      return false;
    }
    
    Coordinates otherObj = (Coordinates)obj;
    var lngsEqual = Double.compare(this.lng, otherObj.lng);
    var latsEqual = Double.compare(this.lat, otherObj.lat);
    
    return (lngsEqual == 0) && (latsEqual == 0);
  }
}
