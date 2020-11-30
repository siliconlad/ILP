package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.ed.inf.aqmaps.drone.Coordinates;
import uk.ac.ed.inf.aqmaps.drone.AStarDrone;
import uk.ac.ed.inf.aqmaps.drone.Sensor;
import uk.ac.ed.inf.aqmaps.webserver.ResponseException;
import uk.ac.ed.inf.aqmaps.webserver.SendRequestException;
import uk.ac.ed.inf.aqmaps.webserver.WebServer;

public class App 
{
  public static void main( String[] args )
  {
    // Parse command line input
    var day = args[0];
    var month = args[1];
    var year = args[2];
    var latitude = Double.valueOf(args[3]);
    var longitude = Double.valueOf(args[4]);
    var port = args[6];
    
    var webServer = new WebServer("http", "localhost", port);
    
    try {
      var sensors = webServer.getSensorsWithCoordinates(year, month, day);
      
      var startPos = new Coordinates();
      startPos.lng = longitude;
      startPos.lat = latitude;
      
      var noFlyZones = webServer.getNoFlyZones();
      
      var drone = new AStarDrone(sensors, startPos, noFlyZones);
      var droneRoute = drone.getRoute();
      droneRoute.buildMap(false);
      droneRoute.saveMap("readings-" + day + "-" + month + "-" + year + ".geojson");
      droneRoute.saveRoute("flightpath-" + day + "-" + month + "-" + year + ".txt");
      
    } catch (SendRequestException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (ResponseException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
