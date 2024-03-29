package uk.ac.ed.inf.aqmaps.webserver;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.FeatureCollection;

import uk.ac.ed.inf.aqmaps.drone.Sensor;

public class WebServer {
  // Prevents the creation of many clients
  private static final HttpClient client = HttpClient.newHttpClient();
  private final String noFlyZonePath = "buildings/no-fly-zones.geojson";

  private String protocol;
  private String host;
  private String port;
  
  public WebServer(String protocol, String host, String port) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
  }
  
  public ArrayList<Sensor> getSensorsWithCoordinates(String year, String month, String day) throws SendRequestException, ResponseException {
    var sensors = this.getSensors(year, month, day);
    
    for (var sensor : sensors) {
      var sensorLocationInfo = this.getWhat3WordsDetails(sensor.location);
      sensor.coordinates = sensorLocationInfo.coordinates;
    }

    return sensors;
  }
  
  public ArrayList<Sensor> getSensors(String year, String month, String day) throws SendRequestException, ResponseException {
    var filePath = "maps/" + year + "/" + month + "/" + day + "/" + "air-quality-data.json";
    var response = this.sendRequest(filePath);
    
    // Process JSON into an array of objects
    Type listType =  new TypeToken<ArrayList<Sensor>>() {}.getType();
    ArrayList<Sensor> sensorInfoList = new Gson().fromJson(response.body(), listType);
    return sensorInfoList;
  }

  public What3Words getWhat3WordsDetails(String location) throws SendRequestException, ResponseException {
    var words = location.split("\\.");
    var filePath = "words/" + words[0] + "/" + words[1] + "/" + words[2] + "/" + "details.json";
    var response = this.sendRequest(filePath);
    
    var what3WordsDetails = new Gson().fromJson(response.body(), What3Words.class);
    return what3WordsDetails;
  }
  
  public FeatureCollection getNoFlyZones() throws SendRequestException, ResponseException {
    var response = this.sendRequest(this.noFlyZonePath);
    var noFlyZones = FeatureCollection.fromJson(response.body());
    return noFlyZones;
  }

  private HttpResponse<String> sendRequest(String filePath) throws SendRequestException, ResponseException {
    HttpResponse<String> response = null;
    
    // Create root of the URL (URL excluding file path) to use in exception messages
    var urlRoot = this.protocol + "://" + this.host + ":" + this.port;
    var urlString = urlRoot + "/" + filePath;
    
    try {
      var request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();
      response = client.send(request, BodyHandlers.ofString());
    } catch (ConnectException e) {
      throw new SendRequestException("Unable to connect to " + urlRoot);
    } catch (IllegalArgumentException e) {
      throw new SendRequestException("Invalid URI " + urlString);
    } catch (InterruptedException e) {
      throw new SendRequestException("Operation was interrupted");
    } catch (IOException e) {
      throw new SendRequestException("IO Exception");
    }
    
    if (response == null) {
      throw new ResponseException("HttpResponse is null");
    }
    
    if (response.statusCode() != 200) {
      throw new ResponseException("Status code not 200 (returned " + response.statusCode() + " instead)");
    }
    
    return response; 
  }
}
