package uk.ac.ed.inf.aqmaps.webserver;

public class What3Words {
  public String country;
  public Square square;
  public String nearestPlace;
  public Coordinates coordinates;
  public String words;
  public String language;
  public String map;
  
  public static class Square {
    Coordinates southwest;
    Coordinates northeast;
  }
  
}
