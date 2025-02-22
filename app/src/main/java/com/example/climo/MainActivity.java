package com.example.climo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_TIMEOUT_MS = 15000;

    // UI Components
    private EditText cityInput;
    private TextView weatherDisplay;
    private TextView conditionDisplay;
    private TextView locationName;
    private ProgressBar progressBar;
    private LinearLayout forecastContainer;

    // Location Services
    private android.location.LocationManager locationManager;
    private Handler locationTimeoutHandler;
    private android.location.LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIComponents();
        setupWeatherFetchButton();
        checkLocationPermission();
    }

    /**
     * Initializes UI components from the layout.
     */
    private void initializeUIComponents() {
        cityInput = findViewById(R.id.editTextCity);
        weatherDisplay = findViewById(R.id.actualWeather);
        conditionDisplay = findViewById(R.id.condition);
        locationName = findViewById(R.id.locationName);
        progressBar = findViewById(R.id.progressBar);
        forecastContainer = findViewById(R.id.forecastContainer);
    }

    /**
     * Sets up the click listener for the weather fetch button.
     */
    private void setupWeatherFetchButton() {
        Button fetchButton = findViewById(R.id.buttonFetchWeather);
        fetchButton.setOnClickListener(v -> {
            String city = cityInput.getText().toString().trim();
            if (!city.isEmpty()) {
                hideKeyboard();
                forecastContainer.removeAllViews(); // Clear previous forecast cards
                fetchWeather(city, false);
                fetchForecast(city);
            } else {
                showToast("Please enter a city name");
            }
        });
    }

    /**
     * Checks for location permission and fetches current location weather if granted.
     */
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocationWeather();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * Hides the soft keyboard.
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Builds the API URL for current weather data.
     * @param encodedLocation URL-encoded location string.
     * @return The complete API URL.
     */
    private String buildCurrentWeatherApiUrl(String encodedLocation) {
        return "https://api.weatherapi.com/v1/current.json?key=" +
                getString(R.string.weather_api_key) +
                "&q=" + encodedLocation;
    }

    /**
     * Builds the API URL for forecast data (next 3 days).
     * @param encodedLocation URL-encoded location string.
     * @return The complete API URL.
     */
    private String buildForecastApiUrl(String encodedLocation) {
        return "https://api.weatherapi.com/v1/forecast.json?key=" +
                getString(R.string.weather_api_key) +
                "&q=" + encodedLocation + "&days=3";
    }

    /**
     * Fetches current weather data and updates the UI.
     * @param location City name or coordinates.
     * @param isCurrentLocation True if using device location.
     */
    private void fetchWeather(String location, boolean isCurrentLocation) {
        showProgress(true);
        try {
            String encodedLocation = URLEncoder.encode(location, "UTF-8");
            String url = buildCurrentWeatherApiUrl(encodedLocation);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        showProgress(false);
                        showToast("Network error: " + e.getMessage());
                    });
                    Log.e("WeatherAPI", "Network error", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    showProgress(false);
                    if (!response.isSuccessful()) {
                        assert response.body() != null;
                        String errorBody = response.body().string();
                        Log.e("WeatherAPI", "API Error: " + errorBody);
                        runOnUiThread(() -> showToast("API Error: " + errorBody));
                        return;
                    }
                    try (response) {
                        assert response.body() != null;
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        processCurrentWeather(jsonResponse);
                    } catch (JSONException e) {
                        runOnUiThread(() -> showToast("Parsing error"));
                        Log.e("WeatherAPI", "JSON Parsing error", e);
                    }
                }
            });
        } catch (IOException e) {
            showProgress(false);
            Log.e("WeatherAPI", "Encoding Error", e);
        }
    }

    /**
     * Processes the JSON response for current weather and updates the UI.
     * @param jsonResponse JSON response from the API.
     */
    private void processCurrentWeather(JSONObject jsonResponse) throws JSONException {
        JSONObject locationData = jsonResponse.getJSONObject("location");
        JSONObject currentData = jsonResponse.getJSONObject("current");

        String city = locationData.getString("name");
        String temp = currentData.getString("temp_c") + "°C";
        String condition = "Condition: " + currentData.getJSONObject("condition").getString("text");

        runOnUiThread(() -> {
            locationName.setText(city);
            weatherDisplay.setText(temp);
            conditionDisplay.setText(condition);
        });
    }

    /**
     * Fetches forecast data for the next 3 days and populates the forecast container.
     * @param location City name or coordinates.
     */
    private void fetchForecast(String location) {
        try {
            String encodedLocation = URLEncoder.encode(location, "UTF-8");
            String url = buildForecastApiUrl(encodedLocation);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> showToast("Forecast network error: " + e.getMessage()));
                    Log.e("ForecastAPI", "Network error", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        assert response.body() != null;
                        String errorBody = response.body().string();
                        Log.e("ForecastAPI", "API Error: " + errorBody);
                        runOnUiThread(() -> showToast("Forecast API Error: " + errorBody));
                        return;
                    }
                    try (response) {
                        assert response.body() != null;
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        processForecastData(jsonResponse);
                    } catch (JSONException e) {
                        runOnUiThread(() -> showToast("Forecast parsing error"));
                        Log.e("ForecastAPI", "JSON Parsing error", e);
                    }
                }
            });
        } catch (IOException e) {
            Log.e("ForecastAPI", "Encoding Error", e);
        }
    }

    /**
     * Processes forecast JSON data and populates the forecast container with modern forecast cards.
     * Each card shows the date (formatted as '27 Jan') and min/max temperatures.
     * @param jsonResponse JSON response from the forecast API.
     */
    private void processForecastData(JSONObject jsonResponse) throws JSONException {
        if (!jsonResponse.has("forecast")) {
            throw new JSONException("No forecast data available");
        }
        JSONObject forecastObj = jsonResponse.getJSONObject("forecast");
        JSONArray forecastDays = forecastObj.getJSONArray("forecastday");

        // Clear previous forecast views
        runOnUiThread(() -> forecastContainer.removeAllViews());

        // Formatter to convert "YYYY-MM-DD" to "dd MMM"
        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

        // Iterate over forecast days (expecting 3 days)
        for (int i = 0; i < forecastDays.length(); i++) {
            JSONObject dayForecast = forecastDays.getJSONObject(i);
            String dateStr = dayForecast.getString("date");
            final String displayDate;
            String displayDate1;
            try {
                Date date = apiDateFormat.parse(dateStr);
                assert date != null;
                displayDate1 = displayDateFormat.format(date);
            } catch (ParseException e) {
                displayDate1 = dateStr;
            }
            displayDate = displayDate1;
            JSONObject dayData = dayForecast.getJSONObject("day");
            String maxTemp = dayData.getString("maxtemp_c") + "°C";
            String minTemp = dayData.getString("mintemp_c") + "°C";

            // Build forecast text for temperatures
            final String tempText = String.format(Locale.getDefault(), "Min: %s | Max: %s", minTemp, maxTemp);

            // Create a vertical LinearLayout for the forecast card
            runOnUiThread(() -> {
                LinearLayout cardLayout = new LinearLayout(MainActivity.this);
                cardLayout.setOrientation(LinearLayout.VERTICAL);
                // Each card will have equal weight to occupy full width without scrolling
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                cardParams.setMargins(16, 0, 16, 0);
                cardLayout.setLayoutParams(cardParams);
                cardLayout.setPadding(16, 16, 16, 16);
                // Set the background with border (created in forecast_card_bg.xml)
                cardLayout.setBackgroundResource(R.drawable.forecast_card_bg);

                // Create and style the date TextView
                TextView dateTextView = new TextView(MainActivity.this);
                dateTextView.setText(displayDate);
                dateTextView.setTextSize(16);
                dateTextView.setTextColor(getResources().getColor(R.color.text_900));
                dateTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                dateTextView.setTypeface(null, android.graphics.Typeface.BOLD);

                // Create and style the temperatures TextView
                TextView tempTextView = new TextView(MainActivity.this);
                tempTextView.setText(tempText);
                tempTextView.setTextSize(16);
                tempTextView.setTextColor(getResources().getColor(R.color.text_900));
                tempTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                // Add the date and temperature TextViews to the card layout
                cardLayout.addView(dateTextView);
                cardLayout.addView(tempTextView);

                // Add the card layout to the forecast container
                forecastContainer.addView(cardLayout, cardParams);
            });
        }
    }


    /**
     * Fetches weather data for the user's current location.
     */
    private void fetchCurrentLocationWeather() {
        showProgress(true);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationTimeoutHandler = new Handler();

        try {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    locationTimeoutHandler.removeCallbacksAndMessages(null);
                    locationManager.removeUpdates(this);
                    String coordinates = location.getLatitude() + "," + location.getLongitude();
                    fetchWeather(coordinates, true);
                    fetchForecast(coordinates);
                }

                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(@NonNull String provider) {}
                @Override public void onProviderDisabled(@NonNull String provider) {
                    showProgress(false);
                    showToast("Enable GPS for location services");
                }
            };

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener
            );

            locationTimeoutHandler.postDelayed(() -> {
                locationManager.removeUpdates(locationListener);
                showProgress(false);
                showToast("Location request timed out");
            }, LOCATION_TIMEOUT_MS);

        } catch (SecurityException e) {
            showProgress(false);
            showToast("Location permissions are not granted");
        }
    }

    /**
     * Handles the result of the location permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocationWeather();
            } else {
                showProgress(false);
                showPermissionDeniedDialog();
            }
        }
    }

    /**
     * Displays an alert dialog when location permission is denied.
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Location access denied. You can still search cities manually.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Controls the visibility of the progress bar.
     * @param show True to show, false to hide.
     */
    private void showProgress(boolean show) {
        runOnUiThread(() -> progressBar.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    /**
     * Displays a short toast message.
     * @param message Message to display.
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationTimeoutHandler != null) {
            locationTimeoutHandler.removeCallbacksAndMessages(null);
        }
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }
}