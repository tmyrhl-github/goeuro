import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * GoEuro Test application
 */
public class GoEuroTest {
	// GoEuro API end point URL
	final static String ENDPOINT_URL = "http://api.goeuro.com/api/v2/position/suggest/en/";
	final static String CHARSET = "UTF-8";
    final static String FILE_EXT = ".csv";

	/**
	 * Application entry point. The first method argument should contain the location name.
	 * @param args
	 */
	public static void main(String[] args) {
		if(args != null && args.length > 0) {
			getLocationData(args[0]);
		}
		else {
			System.out.println("Please supply a location name.");
		}
	}

    /*
     * Read the location data from the GoEuro end point URL
     */
    private static void getLocationData(String location) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        // remove any non letters, special characters and spaces from the input
        location = location.replaceAll("[^a-zA-Z ]", "");
        try {
            URL url = new URL(ENDPOINT_URL + location);
            // Create the request to GoEuro API, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept-Charset", CHARSET);
            urlConnection.setConnectTimeout(10000);
            urlConnection.connect();
            urlConnection.setReadTimeout(10000);
            // Open the input stream
            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
            	System.err.println("Unable to read data from endpoint");
                return;
            }
            // Create the input stream reader
            reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET));
            // Read the input stream
            String line;
            StringBuffer buffer = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            if (buffer.length() == 0) {
            	System.out.println("No data found for location \"" + location + "\"");
                return;
            }
 
            getLocationDataFromJson(buffer.toString(), location);
        } 
        catch (IOException e) {
        	System.err.println("An error was encountered when fetching data for \"" + location + "\"");
            return;
        } 
        finally {
        	// close the URL connection
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            // close the input reader
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                	System.err.println("Unable to close input reader " + e);
                }
            }
        }
    }
    
   /*
    * Parse the JSON input and create the CSV file.
    */
    private static void getLocationDataFromJson(String locationJsonStr, String location) {
        final String JSON_POSITION = "geo_position";
        final String JSON_LATITUDE = "latitude";
        final String JSON_LONGITUDE = "longitude";
        final String JSON_ID = "_id";
        final String JSON_NAME = "name";
        final String JSON_TYPE = "type";
        
        ArrayList<String> csvArray = new ArrayList<String>();

        try {
         	JSONArray jsonArray = new JSONArray(locationJsonStr);
         	
         	if(jsonArray == null || jsonArray.length() == 0) {
            	System.out.println("No data found for location \"" + location + "\"");
                return;
         	}
        	
	        int count = jsonArray.length(); 
			for(int i=0 ; i<count; i++){ 
				try {
					JSONObject jsonObject = jsonArray.getJSONObject(i);  
					long id = jsonObject.getLong(JSON_ID);
					String name = jsonObject.getString(JSON_NAME);
					String type = jsonObject.getString(JSON_TYPE);
			        JSONObject pos = jsonObject.getJSONObject(JSON_POSITION);
			        double latitude = pos.getDouble(JSON_LATITUDE);
			        double longitude = pos.getDouble(JSON_LONGITUDE);
			        String csvStr = new String(id + ",\"" + name + "\",\"" + type +  
			        	"\"," + latitude +  "," + longitude);
			        csvArray.add(csvStr);
				}
				catch (JSONException e) {
					System.err.println("Error parsing JSON array " + e);        
				}
			}
			// write the CSV data to file
			if (csvArray.size() > 0) {
				writeToFile(csvArray, location);
			}
        } 
        catch (JSONException e) {
        	System.err.println("Error parsing JSON " + e);        
        }
    }
    
    /*
     * Write the CSV formatted data out to a file that has the same name as the chosen 
     * location e.g. berlin.csv. If the file already exists it will be overwritten.
     */
    private static void writeToFile(ArrayList<String> entries, String location) {
      	FileOutputStream fos = null;
     	OutputStreamWriter osw = null;
    	BufferedWriter writer = null;
        try {
	        fos = new FileOutputStream(location + FILE_EXT);
	        osw = new OutputStreamWriter(fos, CHARSET);
	        writer = new BufferedWriter(osw);
	        for(String entry : entries) {
	        	writer.write(entry, 0, entry.length());
	        	writer.newLine();
	        }
        }
		catch(IOException e) {
        	System.err.println("Error wrting csv: " + e);    
		}
        finally {
        	try {
	        	if(writer != null) {
	        		writer.close();
	         	}
	        	if(osw != null) {
	        		osw.close();
	         	}
	        	if(fos != null) {
	        		fos.close();
	         	}
        	}
        	catch(IOException e) {
            		System.err.println("Error closing output stream: " + e);    
        	}
        }
    }
}
