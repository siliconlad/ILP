package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;

import uk.ac.ed.inf.aqmaps.webserver.WebServer;
import uk.ac.ed.inf.aqmaps.webserver.SensorInfo;
import uk.ac.ed.inf.aqmaps.webserver.Coordinates;
import uk.ac.ed.inf.aqmaps.webserver.ResponseException;
import uk.ac.ed.inf.aqmaps.webserver.SendRequestException;

import uk.ac.ed.inf.aqmaps.drone.GreedyDrone;
import uk.ac.ed.inf.aqmaps.drone.Route;
import uk.ac.ed.inf.aqmaps.drone.Sensor;


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
        // var randomSeed = Integer.valueOf(args[5]);
        var port = args[6];
        
        var webServer = new WebServer("http", "localhost", port);
        
        try {
          var sensorList = webServer.getSensorInfo(year, month, day);
          var sensors = processSensors(sensorList, webServer);
          
          // Create object for starting position
          var startPos = new Coordinates();
          startPos.lng = longitude;
          startPos.lat = latitude;
          
          var noFlyZones = webServer.getNoFlyZones();
          var drone = new GreedyDrone(sensors, startPos, noFlyZones);
          
          System.out.println("Calculating route...");
          drone.calculateRoute();
          
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
    
    private static ArrayList<Sensor> processSensors(ArrayList<SensorInfo> sensors, WebServer webserver) {
      var sensorObjects = new ArrayList<Sensor>();
      
      for (var sensor : sensors) {
        try {
          System.out.println(sensor.location);
          var sensorLocationInfo = webserver.getWhat3WordsDetails(sensor.location);
          var sensorObject = new Sensor(sensor.location, sensor.battery, sensor.reading, sensorLocationInfo.coordinates);
          
          sensorObjects.add(sensorObject);
       
        } catch (SendRequestException e) {
          e.printStackTrace();
          System.exit(1);
        } catch (ResponseException e) {
          e.printStackTrace();
          System.exit(1);
        }
      }
      
      
      return sensorObjects;
    }
}
