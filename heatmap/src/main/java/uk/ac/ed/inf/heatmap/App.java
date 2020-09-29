package uk.ac.ed.inf.heatmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class App {
    public static void main( String[] args ) {    	        
		try {
	    	// File name is passed as a command-line argument
	        String predictionsFileName = args[0];
	        File predictionsFile = new File(predictionsFileName);
			HeatMap heatMap = new HeatMap(predictionsFile);
			
			heatMap.save("testfile.json");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    

}
