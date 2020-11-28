package uk.ac.ed.inf.aqmaps.drone;

@SuppressWarnings("serial")
public class RouteNotFoundException extends Exception {
  public RouteNotFoundException(String message) {
    super(message);
  }
}
