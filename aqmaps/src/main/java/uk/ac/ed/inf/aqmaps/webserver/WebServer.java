package uk.ac.ed.inf.aqmaps.webserver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.util.ArrayList;

import java.net.ConnectException;
import java.io.IOException;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.mapbox.geojson.FeatureCollection;


public class WebServer {
  HttpClient client = HttpClient.newHttpClient();
  String protocol;
  String host;
  String port;
  
  
  public WebServer(String protocol, String host, String port) {
    this.protocol = protocol;
    this.host = host;
    this.port = port;
  }
  
  
  public ArrayList<SensorInfo> getSensorInfo(String year, String month, String day) throws SendRequestException, ResponseException {
    var filePath = "maps/" + year + "/" + month + "/" + day + "/" + "air-quality-data.json";
    var response = sendRequest(filePath);
    
    // Process JSON into an array of objects
    Type listType =  new TypeToken<ArrayList<SensorInfo>>() {}.getType();
    ArrayList<SensorInfo> sensorInfoList = new Gson().fromJson(response.body(), listType);
    return sensorInfoList;
  }
  
  
  public ArrayList<What3Words> getWhat3Words(String first, String second, String third) throws SendRequestException, ResponseException {
    var filePath = "words/" + first + "/" + second + "/" + third + "/" + "details.json";
    var response = sendRequest(filePath);
    
    // Process JSON into an array of objects
    Type listType =  new TypeToken<ArrayList<What3Words>>() {}.getType();
    ArrayList<What3Words> what3WordsList = new Gson().fromJson(response.body(), listType);
    return what3WordsList;
  }
  
  
  public FeatureCollection getNoFlyZones() throws SendRequestException, ResponseException {
    var filePath = "buildings/no-fly-zones.geojson";
    var response = sendRequest(filePath);
    
    // Convert from JSON
    var noFlyZones = FeatureCollection.fromJson(response.body());
    return noFlyZones;
  }
  

  private HttpResponse<String> sendRequest(String filePath) throws SendRequestException, ResponseException {
    // Initialise response object to be returned
    HttpResponse<String> response = null;
    
    // Create root of the URL (URL excluding file path) to use in exception messages
    var urlRoot = this.protocol + "://" + this.host + ":" + this.port;
    var urlString = urlRoot + "/" + filePath;
    
    try {
      // Send GET request to server
      var request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();
      response = this.client.send(request, BodyHandlers.ofString());
    } catch (ConnectException e) {
      throw new SendRequestException("Unable to connect to " + urlRoot);
    } catch (IllegalArgumentException e) {
      throw new SendRequestException("Invalid URI " + urlString);
    } catch (InterruptedException e) {
      throw new SendRequestException("Operation was interrupted");
    } catch (IOException e) {
      throw new SendRequestException("IO Exception");
    }
    
    // Make sure there has been some response from the web server
    if (response == null) {
      throw new ResponseException("HttpResponse is null");
    }
    
    // Ensure resource was found
    if (response.statusCode() == 404) {
      throw new ResponseException("404 Not Found: " + filePath);
    }
    
    return response; 
  }
  
}
