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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Main activity handling weather data fetching and UI interactions
 */
public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_TIMEOUT_MS = 15000;

    // UI Components
    private EditText cityInput;
    private TextView weatherDisplay;
    private TextView conditionDisplay;
    private TextView locationName;
    private ProgressBar progressBar;

    // Location Services
    private LocationManager locationManager;
    private Handler locationTimeoutHandler;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUIComponents();
        setupWeatherFetchButton();
        checkLocationPermission();
    }

    /**
     * Initializes all UI components from layout
     */
    private void initializeUIComponents() {
        cityInput = findViewById(R.id.editTextCity);
        weatherDisplay = findViewById(R.id.actualWeather);
        conditionDisplay = findViewById(R.id.condition);
        locationName = findViewById(R.id.locationName);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Sets up click listener for weather fetch button
     */
    private void setupWeatherFetchButton() {
        Button fetchButton = findViewById(R.id.buttonFetchWeather);
        fetchButton.setOnClickListener(v -> {
            String city = cityInput.getText().toString().trim();
            if (!city.isEmpty()) {
                hideKeyboard();
                fetchWeather(city, false);
            } else {
                showToast("Please enter a city name");
            }
        });
    }

    /**
     * Checks location permission status
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
     * Hides the soft keyboard
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Fetches weather data from WeatherAPI
     * @param location City name or coordinates
     * @param isCurrentLocation True if using device location
     */
    private void fetchWeather(String location, boolean isCurrentLocation) {
        showProgress(true);
        try {
            String encodedLocation = URLEncoder.encode(location, "UTF-8");
            String url = buildApiUrl(encodedLocation);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    handleNetworkError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    handleApiResponse(response);
                }
            });
        } catch (IOException e) {
            handleEncodingError(e);
        }
    }

    /**
     * Builds API request URL
     * @param encodedLocation URL-safe location string
     * @return Complete API URL
     */
    private String buildApiUrl(String encodedLocation) {
        return "https://api.weatherapi.com/v1/current.json?key=" +
                getString(R.string.weather_api_key) +
                "&q=" + encodedLocation;
    }

    /**
     * Handles API response data
     * @param response API response object
     */
    private void handleApiResponse(Response response) throws IOException {
        showProgress(false);
        try {
            if (response.isSuccessful()) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                processWeatherData(jsonResponse);
            } else {
                handleApiError(response);
            }
        } catch (JSONException e) {
            handleJsonError(e);
        } finally {
            response.close();
        }
    }

    /**
     * Processes successful weather data
     * @param jsonResponse Parsed JSON response
     */
    private void processWeatherData(JSONObject jsonResponse) throws JSONException {
        if (!jsonResponse.has("location") || !jsonResponse.has("current")) {
            throw new JSONException("Invalid API response structure");
        }

        JSONObject locationData = jsonResponse.getJSONObject("location");
        JSONObject currentData = jsonResponse.getJSONObject("current");

        String city = locationData.getString("name");
        String temp = currentData.getString("temp_c") + "°C";
        String condition = "Condition: " + currentData.getJSONObject("condition").getString("text");

        updateWeatherUI(city, temp, condition);
    }

    /**
     * Updates UI with weather information
     */
    private void updateWeatherUI(String city, String temperature, String condition) {
        runOnUiThread(() -> {
            locationName.setText(city);
            weatherDisplay.setText(temperature);
            conditionDisplay.setText(condition);
        });
    }

    /**
     * Handles API error responses
     */
    private void handleApiError(Response response) throws IOException {
        try {
            String errorBody = response.body().string();
            JSONObject errorJson = new JSONObject(errorBody);
            String errorMessage = errorJson.getJSONObject("error").getString("message");
            showToast("Error: " + errorMessage);
        } catch (JSONException e) {
            showToast("Unknown API error occurred");
        }
    }

    /**
     * Fetches current location weather data
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
                }

                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(@NonNull String provider) {}

                @Override
                public void onProviderDisabled(@NonNull String provider) {
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
            showToast("Location permission required");
        }
    }

    /**
     * Handles permission request results
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
     * Shows permission denied explanation dialog
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Location access denied. You can still search cities manually.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Controls progress bar visibility
     * @param show True to show progress bar
     */
    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            Log.d("ProgressBar", "Visibility: " + (show ? "VISIBLE" : "GONE")); // Nuevo log
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }
    /**
     * Displays short toast messages
     * @param message Message to display
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    /**
     * Handles network errors
     */
    private void handleNetworkError() {
        showProgress(false);
        showToast("Network connection required");
    }

    /**
     * Handles JSON parsing errors
     */
    private void handleJsonError(JSONException e) {
        showToast("Data parsing error");
        Log.e("WeatherAPI", "JSON Error", e);
    }

    /**
     * Handles URL encoding errors
     */
    private void handleEncodingError(IOException e) {
        showProgress(false);
        Log.e("WeatherAPI", "Encoding Error", e);
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