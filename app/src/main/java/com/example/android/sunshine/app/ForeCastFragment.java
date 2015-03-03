package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final String TAG = ForecastFragment.class.getSimpleName();
    private GoogleApiClient googleApiClient;
    private SharedPreferences sharedPreferences;
    private Uri weatherUri;

    ArrayAdapter<String> forecastAdapter;

    public ForecastFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String zip = sharedPreferences.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        weatherUri = Uri.parse(getString(R.string.weather_base_uri))
                .buildUpon()
                .appendQueryParameter("q", zip)
                .appendQueryParameter("cnt", "7")
                .appendQueryParameter("units", "metric")
                .appendQueryParameter("mode", "json")
                .build();
        buildGoogleApiClient();
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forecastArray = {
                "Today - Sunny - 88 / 63",
                "Tomorrow - Sunny - 88 / 63",
                "Wed - Sunny - 88 / 63",
                "Thur - Sunny - 88 / 63",
                "Fri - Sunny - 88 / 63",
                "Sat - Sunny - 88 / 63",
                "Sun - Sunny - 88 / 63",
                "Today - Sunny - 88 / 63",
                "Tomorrow - Sunny - 88 / 63",
                "Wed - Sunny - 88 / 63",
                "Thur - Sunny - 88 / 63",
                "Fri - Sunny - 88 / 63",
                "Sat - Sunny - 88 / 63",
                "Sun - Sunny - 88 / 63"
        };

//        try {
////            JSONObject weatherJson = new JSONObject(getWeatherData());
////            JSONArray weatherJsonArray = weatherJson.getJSONArray("list");
////            for (int i = 0; i < weatherJsonArray.length(); i++) {
////                JSONObject oneObject = weatherJsonArray.getJSONObject(i);
////                String oneObjectsItem = oneObject.getString("STRINGNAMEinTHEarray");
////                String oneObjectsItem2 = oneObject.getString("anotherSTRINGNAMEINtheARRAY");
////            }
//        } catch (JSONException e) {
//            Log.e(TAG, "Error when parsing JSON", e);
//            e.printStackTrace();
//        }

        List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));

        forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        ListView forecastListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastListView.setAdapter(forecastAdapter);

        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Toast.makeText(getActivity(), forecastAdapter.getItem(position), Toast.LENGTH_LONG).show();
                Intent detailsIntent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, forecastAdapter.getItem(position));
                startActivity(detailsIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        fetchWeather();
        super.onStart();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                fetchWeather();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected " + bundle);
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {
            weatherUri = Uri.parse(getString(R.string.weather_base_uri))
                    .buildUpon()
                    .appendQueryParameter("lat", Double.toString(location.getLatitude()))
                    .appendQueryParameter("lon", Double.toString(location.getLongitude()))
                    .appendQueryParameter("cnt", "7")
                    .appendQueryParameter("units", "metric")
                    .appendQueryParameter("mode", "json")
                    .build();

            fetchWeather();
        } else {
            Log.d(TAG, "Location unknown " + location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection failed " + connectionResult);
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "Building Google API Client");
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void fetchWeather() {
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        fetchWeatherTask.execute();
    }

    public class FetchWeatherTask extends AsyncTask<Void, Void, String[]> {
        private final String TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                URL weatherUrl = new URL(weatherUri.toString());

                Log.d(TAG, weatherUrl.toString());
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) weatherUrl.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if (inputStream != null) {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() != 0) {
                        forecastJsonStr = buffer.toString();
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(forecastJsonStr, 7);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing json string", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                forecastAdapter.setNotifyOnChange(false);
                forecastAdapter.clear();
                for (int i = 0; i < result.length; i++) {
                    forecastAdapter.add(result[i]);
                }
                forecastAdapter.setNotifyOnChange(true);
                forecastAdapter.notifyDataSetChanged();
            }
        }
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        String unitType = sharedPreferences.getString(getString(R.string.pref_display_units_key), getString(R.string.pref_display_unit_metric));

        if (unitType.equals(getString(R.string.pref_display_unit_metric))) {
            return Math.round(high) + "/" + Math.round(low);
        } else if (unitType.equals(getString(R.string.pref_display_unit_imperial))) {
            return Math.round((high * 1.8) + 32) + "/" + Math.round((low * 1.8) + 32);
        } else {
            Log.d(TAG, "Unit type not found " + unitType);
            return "";
        }
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
        Log.d(TAG, "Weather data: " + forecastJsonStr);
        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        if (weatherArray != null) {
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);
                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);
                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }
        }

//        for (String s : resultStrs) {
//            Log.v(TAG, "Forecast entry: " + s);
//        }
        return resultStrs;

    }
}