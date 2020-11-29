package uk.ac.ed.inf.aqmaps.webserver;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.drone.Sensor;

class WebServerTest {
  
  // Initialise web server
  WebServer webServer = new WebServer("http", "localhost", "9898");
  
  @Test
  void testNoFlyZones() {
    assertAll("noFlyZones",
        () -> {
          FeatureCollection res = webServer.getNoFlyZones();
          assertNotNull(res);
          
          assertEquals(4, res.features().size());
          
          assertAll("Appleton Tower",
              () -> {
                var appletonFeature = res.features().get(0);
                var appletonGeometry = appletonFeature.geometry();
                var appletonPolygon = (Polygon) appletonGeometry;
                var appletonCoordinates = appletonPolygon.coordinates().get(0);
                
                assertEquals(15, appletonCoordinates.size());
              }
          );
          
          assertAll("David Hume Tower",
              () -> {
                var davidHumeFeature = res.features().get(1);
                var davidHumeGeometry = davidHumeFeature.geometry();
                var davidHumePolygon = (Polygon) davidHumeGeometry;
                var davidHumeCoordinates = davidHumePolygon.coordinates().get(0);
                
                assertEquals(5, davidHumeCoordinates.size());
              }
          );
          
          assertAll("Main Library",
              () -> {
                var libraryFeature = res.features().get(2);
                var libraryGeometry = libraryFeature.geometry();
                var libraryPolygon = (Polygon) libraryGeometry;
                var libraryCoordinates = libraryPolygon.coordinates().get(0);
                
                assertEquals(5, libraryCoordinates.size());
                
                assertEquals(-3.189708441495895, libraryCoordinates.get(0).longitude());
                assertEquals(55.94280820210462, libraryCoordinates.get(0).latitude());
                
                assertEquals(-3.1894750893115997, libraryCoordinates.get(1).longitude());
                assertEquals(55.94233128523903, libraryCoordinates.get(1).latitude());
                
                assertEquals(-3.1882721185684204, libraryCoordinates.get(2).longitude());
                assertEquals(55.942514542178216, libraryCoordinates.get(2).latitude());
                
                assertEquals(-3.1885054707527156, libraryCoordinates.get(3).longitude());
                assertEquals(55.94299295887116, libraryCoordinates.get(3).latitude());
                
                assertEquals(-3.189708441495895, libraryCoordinates.get(4).longitude());
                assertEquals(55.94280820210462, libraryCoordinates.get(4).latitude());
              }
          );
          
          assertAll("Informatics Forum",
              () -> {
                var forumFeature = res.features().get(3);
                var forumGeometry = forumFeature.geometry();
                var forumPolygon = (Polygon) forumGeometry;
                var forumCoordinates = forumPolygon.coordinates().get(0);
                
                assertEquals(11, forumCoordinates.size());
              }
          );
        }
    );
  }

  @Test
  void testGetSensors() {
    assertAll("sensors",
        () -> {
          ArrayList<Sensor> res = webServer.getSensors("2020", "01", "02");
          assertNotNull(res);

          assertEquals(33, res.size());

          assertAll("sensor 1",
              () -> assertEquals("coherent.saints.stuck", res.get(0).location),
              () -> assertEquals(5.505353, res.get(0).battery),
              () -> assertEquals("null", res.get(0).reading)
          );
          
          assertAll("sensor 6",
              () -> assertEquals("hooked.shine.third", res.get(7).location),
              () -> assertEquals(46.385944, res.get(7).battery),
              () -> assertEquals("43.79", res.get(7).reading)
          );
          
          assertAll("sensor 14",
              () -> assertEquals("along.spill.limp", res.get(14).location),
              () -> assertEquals(25.60094, res.get(14).battery),
              () -> assertEquals("0.0", res.get(14).reading)
          );
          
          assertAll("sensor 23",
              () -> assertEquals("rated.fired.crowds", res.get(28).location),
              () -> assertEquals(7.7690783, res.get(28).battery),
              () -> assertEquals("253.7372315682059", res.get(28).reading)
          );
          
          assertAll("sensor 33",
              () -> assertEquals("hello.love.keys", res.get(32).location),
              () -> assertEquals(79.6998, res.get(32).battery),
              () -> assertEquals("196.4", res.get(32).reading)
          );
        },
        () -> {
          ArrayList<Sensor> res = webServer.getSensors("2020", "12", "25");
          assertNotNull(res);
          
          assertEquals(33, res.size());

          assertAll("sensor 1",
              () -> assertEquals("thank.salsa.brain", res.get(0).location),
              () -> assertEquals(20.282084, res.get(0).battery),
              () -> assertEquals("51.05", res.get(0).reading)
          );
          
          assertAll("sensor 3",
              () -> assertEquals("panic.squad.danger", res.get(2).location),
              () -> assertEquals(7.814354, res.get(2).battery),
              () -> assertEquals("NaN", res.get(2).reading)
          );
          
          assertAll("sensor 14",
              () -> assertEquals("renew.ears.tend", res.get(13).location),
              () -> assertEquals(89.95044, res.get(13).battery),
              () -> assertEquals("110.86", res.get(13).reading)
          );
          
          assertAll("sensor 23",
              () -> assertEquals("bound.ends.matter", res.get(22).location),
              () -> assertEquals(27.960676, res.get(22).battery),
              () -> assertEquals("78.19", res.get(22).reading)
          );
          
          assertAll("sensor 33",
              () -> assertEquals("groups.ideas.script", res.get(32).location),
              () -> assertEquals(31.75398, res.get(32).battery),
              () -> assertEquals("70.64", res.get(32).reading)
          ); 
        }
    );
  }
  
  @Test
  void testGetWhat3WordsDetails() {
    assertAll("What3Words",
        () -> {
          What3Words res = webServer.getWhat3WordsDetails("acid.chair.butter");
          assertNotNull(res);
          
          assertAll("word",
              () -> assertEquals("GB", res.country),
              () -> assertEquals(-3.18526, res.square.southwest.lng),
              () -> assertEquals(55.944561, res.square.southwest.lat),
              () -> assertEquals(-3.185212, res.square.northeast.lng),
              () -> assertEquals(55.944588, res.square.northeast.lat),
              () -> assertEquals("Edinburgh", res.nearestPlace),
              () -> assertEquals(-3.185236, res.coordinates.lng),
              () -> assertEquals(55.944575, res.coordinates.lat),
              () -> assertEquals("acid.chair.butter", res.words),
              () -> assertEquals("en", res.language),              
              () -> assertEquals("https://w3w.co/acid.chair.butter", res.map)
          );
        },
        () -> {
          What3Words res = webServer.getWhat3WordsDetails("panic.squad.danger");
          assertNotNull(res);
          
          assertAll("word",
              () -> assertEquals("GB", res.country),
              () -> assertEquals(-3.187717, res.square.southwest.lng),
              () -> assertEquals(55.942755, res.square.southwest.lat),
              () -> assertEquals(-3.187669, res.square.northeast.lng),
              () -> assertEquals(55.942782, res.square.northeast.lat),
              () -> assertEquals("Edinburgh", res.nearestPlace),
              () -> assertEquals(-3.187693, res.coordinates.lng),
              () -> assertEquals(55.942769, res.coordinates.lat),
              () -> assertEquals("panic.squad.danger", res.words),
              () -> assertEquals("en", res.language),              
              () -> assertEquals("https://w3w.co/panic.squad.danger", res.map)
          );
        }
    );
  }
}
