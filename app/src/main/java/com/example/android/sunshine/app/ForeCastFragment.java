package com.example.android.sunshine.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    private final String TAG = FeftchWeatherTask.class.getSimpleName();

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

        try {
            JSONObject weatherJson = new JSONObject(getWeatherData());
            JSONArray weatherJsonArray = weatherJson.getJSONArray("list");
            for (int i = 0; i < weatherJsonArray.length(); i++) {
                JSONObject oneObject = weatherJsonArray.getJSONObject(i);
                String oneObjectsItem = oneObject.getString("STRINGNAMEinTHEarray");
                String oneObjectsItem2 = oneObject.getString("anotherSTRINGNAMEINtheARRAY");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error when parsing JSON", e);
            e.printStackTrace();
        }

        List<String> weekForecast = new ArrayList<String>(Arrays.asList(forecastArray));

        ArrayAdapter<String> forecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);

        ListView forecastListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        forecastListView.setAdapter(forecastAdapter);

        return rootView;
    }

    public class FeftchWeatherTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void doInBackground(Void... params) {
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
                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=5310,no&cnt=7&unit=metric&mode=json");

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
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
            return forecastJsonStr;
        }
    }
}