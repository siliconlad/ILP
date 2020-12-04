package uk.ac.ed.inf.aqmaps.drone;

@SuppressWarnings("serial")
public class BatteryLimitException extends Exception {
  public BatteryLimitException(String message) {
    super(message);
  }
}
