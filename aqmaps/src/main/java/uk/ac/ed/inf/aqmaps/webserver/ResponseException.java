package uk.ac.ed.inf.aqmaps.webserver;

@SuppressWarnings("serial")
public class ResponseException extends Exception {
  public ResponseException(String message) {
    super(message);
  }
}
