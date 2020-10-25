package uk.ac.ed.inf.aqmaps.webserver;

public class What3Words {
  public String country;
  public Square square;
  public String nearestPlace;
  public Point coordinates;
  public String words;
  public String language;
  public String map;
  
  public static class Square {
    Point southwest;
    Point northeast;
  }
  
  public static class Point {
    double lng;
    double lat;
  }
  
}
