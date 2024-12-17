package com.example.climo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // UI elements
    private EditText cityInput; // Input field for city name
    private TextView weatherDisplay; // Displays temperature
    private TextView conditionDisplay; // Displays weather condition
    private TextView locationName; // Displays location name (current or searched)
    private LocationManager locationManager; // For accessing GPS location

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        cityInput = findViewById(R.id.editTextCity);
        Button fetchWeatherButton = findViewById(R.id.buttonFetchWeather);
        weatherDisplay = findViewById(R.id.actualWeather);
        conditionDisplay = findViewById(R.id.condition);
        locationName = findViewById(R.id.locationName);

        // Set up button to fetch weather based on entered city
        fetchWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityInput.getText().toString().trim();
                if (!city.isEmpty()) {
                    hideKeyboard(); // Hide keyboard when button is clicked
                    fetchWeather(city, false); // Fetch weather for entered city
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a city", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Check for location permission and fetch current location weather if granted
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocationWeather();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Hides the soft keyboard from the screen.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Fetches weather data for the specified location.
     * @param location The city name or GPS coordinates (latitude,longitude).
     * @param isCurrentLocation True if the location is obtained from GPS, false if it's user input.
     */
    private void fetchWeather(String location, boolean isCurrentLocation) {
        String API_KEY = getString(R.string.weather_api_key); // API key for WeatherAPI
        String url = "https://api.weatherapi.com/v1/current.json?key=" + API_KEY + "&q=" + location;

        Log.d("WeatherAPI", "Generated URL: " + url);

        OkHttpClient client = new OkHttpClient();

        // Build the HTTP request
        Request request = new Request.Builder()
                .url(url)
                .build();

        // Execute the HTTP request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle network error
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e("WeatherAPI", "Network error", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Handle API error
                    String errorBody = response.body().string();
                    Log.e("WeatherAPI", "API Error: " + errorBody);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + errorBody, Toast.LENGTH_LONG).show());
                    return;
                }

                try {
                    // Parse the JSON response
                    String jsonResponse = response.body().string();
                    Log.d("WeatherAPI", "Response: " + jsonResponse);
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONObject location = jsonObject.getJSONObject("location");
                    JSONObject current = jsonObject.getJSONObject("current");

                    // Extract relevant data
                    String cityName = location.getString("name");
                    String temperature = current.getString("temp_c");
                    String condition = current.getJSONObject("condition").getString("text");

                    String temp = temperature + "°C";
                    String cond = "Condition: " + condition;

                    // Update the UI with the fetched data
                    runOnUiThread(() -> {
                        locationName.setText(cityName); // Display location name
                        weatherDisplay.setText(temp); // Display temperature
                        conditionDisplay.setText(cond); // Display condition
                    });

                } catch (JSONException e) {
                    // Handle JSON parsing error
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Parsing error", Toast.LENGTH_SHORT).show());
                    Log.e("WeatherAPI", "JSON Parsing error", e);
                }
            }
        });
    }

    /**
     * Fetches the weather data for the user's current location.
     */
    private void fetchCurrentLocationWeather() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            // Request GPS location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String coordinates = latitude + "," + longitude; // Format as "latitude,longitude"
                    fetchWeather(coordinates, true); // Fetch weather for current location
                    locationManager.removeUpdates(this); // Stop receiving location updates
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permissions are not granted", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the result of the location permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch current location weather
                fetchCurrentLocationWeather();
            } else {
                // Permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}