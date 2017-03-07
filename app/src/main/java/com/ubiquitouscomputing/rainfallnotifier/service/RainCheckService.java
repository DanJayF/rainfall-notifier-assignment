package com.ubiquitouscomputing.rainfallnotifier.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ubiquitouscomputing.rainfallnotifier.BuildConfig;
import com.ubiquitouscomputing.rainfallnotifier.MainActivity;
import com.ubiquitouscomputing.rainfallnotifier.R;
import com.ubiquitouscomputing.rainfallnotifier.SettingsActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;

public class RainCheckService extends IntentService {

    private static final String TAG = RainCheckService.class.getSimpleName();

    //Preference Variables
    boolean currentLocToggle = true;
    SharedPreferences prefs;

    public RainCheckService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    //This method handles a single incoming request
    public void onHandleIntent(Intent intent) {

        //Retrieve notification preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentLocToggle = prefs.getBoolean(getString(R.string.current_location_toggle_key), true);
        String currentLocName = prefs.getString(getString(R.string.saved_location_name), "");

        //Determine location required
        String latLongArgs;
        if(currentLocToggle) {
            //Get the current location coordinates and build API arguments
            latLongArgs = getCurrentLatLong();
        }
        else {
            //Get saved location coordinates and build API arguments
            float locLat = prefs.getFloat(getString(R.string.saved_location_latitude), 0);
            float locLong = prefs.getFloat(getString(R.string.saved_location_longitude), 0);
            latLongArgs = "lat="+locLat+"&lon="+locLong;
        }

        //Download and parse the rainfall data
        String jsonWeather = getWeatherData(latLongArgs);
        String rainTotal = parseJSONRain(jsonWeather);
        String notificationMessage;

        //Build the notification message
        if (currentLocToggle) {
            if(rainTotal.equals("0")) {notificationMessage = "There is currently no rain forecast for your location.";}
            else                      {notificationMessage = "There is " +rainTotal+ "mm of rain forecast for your current location.";}
        }
        else {
            if(rainTotal.equals("0")) {notificationMessage = "There is currently no rain forecast for " +currentLocName;}
            else                      {notificationMessage = "There is " +rainTotal+ "mm of rain forecast for " +currentLocName;}
        }

        //Send the notification!
        createNotification(notificationMessage);
    }

    //Get current LatLong
    private String getCurrentLatLong() {

        String latLongArgs = null;

        //Use GPS to find current location
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location currentLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            double locLat = currentLoc.getLatitude();
            double locLong = currentLoc.getLongitude();

            //Construct coordinate arguments for API string
            latLongArgs = "lat="+locLat+"&lon="+locLong;
        }
        catch (SecurityException ex) {ex.printStackTrace();}

        return latLongArgs;
    }

    //Downloads the JSON weather information using the given location
    private String getWeatherData(String location) {

        String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?APPID=" + BuildConfig.RAINFALL_NOTIFIER_API_KEY + "&units=metric&cnt=8&";

        HttpURLConnection con = null;
        InputStream is = null;

        try {
            con = (HttpURLConnection) (new URL(BASE_URL + location)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();

            //Read in the response
            StringBuffer buffer = new StringBuffer();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null )
                buffer.append(line + "rn");

            //Close off the connection
            is.close();
            con.disconnect();

            //Return the buffer is String format
            return buffer.toString();
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
        //In case the try fails, make sure the connection is closed
        finally {
            try {is.close();}       catch(Throwable t) {}
            try {con.disconnect();} catch(Throwable t) {}
        }

        return null;
    }

    //Parse the JSON file, return the sum of all rainfall values
    private String parseJSONRain(String jsonForecast) {

        double dayRainTotal = 0;
        DecimalFormat decimalFormat = new DecimalFormat("#.#");

        try {
            //Create our JSONObject from the data and retrieve "list" (array of forecasts)
            JSONObject jObj = new JSONObject(jsonForecast);
            JSONArray jArr = jObj.getJSONArray("list");

            //Loop through all forecasts and add rain values to total
            for (int i=0; i < jArr.length(); i++) {
                JSONObject threeHourForecast = jArr.getJSONObject(i);

                //Rain may not be forecast during this 3hr window
                try {
                    JSONObject tempRainObj = threeHourForecast.getJSONObject("rain");
                    dayRainTotal += tempRainObj.getDouble("3h");
                }
                catch(JSONException ignored) {}
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        //Return the total rainfall for the next 24 hours to one decimal place
        return decimalFormat.format(dayRainTotal);
    }

    //Creates a basic heads-up notification with the passed String
    private void createNotification(String message) {

        //Prepare an intent which is triggered if notification tapped
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //Build notification itself
        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(R.drawable.notification_cloud)
            .setContentTitle("24hr Rainfall Forecast")
            .setContentText(message)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH) //Enables heads-up
            .setVibrate(new long[0]); //Required for heads-up
        Notification notification = builder.build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; //Disabling app binding
    }
}
