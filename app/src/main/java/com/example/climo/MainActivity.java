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
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormatSymbols;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.bumptech.glide.Glide;

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
    private ImageView weatherIcon;

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
        weatherIcon = findViewById(R.id.weatherIcon);
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
                forecastContainer.removeAllViews();
                fetchWeatherData(city); // Single call
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
     * Processes the JSON response for current weather and updates the UI.
     * @param jsonResponse JSON response from the API.
     */
    private void processCurrentWeather(JSONObject jsonResponse) throws JSONException {
        JSONObject locationData = jsonResponse.getJSONObject("location");
        JSONObject currentData = jsonResponse.getJSONObject("current");

        String city = locationData.getString("name");
        String temp = currentData.getString("temp_c") + "°C";
        String condition = "Condition: " + currentData.getJSONObject("condition").getString("text");

        String iconUrl = "https:" + currentData.getJSONObject("condition").getString("icon");
        runOnUiThread(() -> {
            Glide.with(MainActivity.this)
                    .load(iconUrl)
                    .into(weatherIcon);
        });

        runOnUiThread(() -> {
            locationName.setText(city);
            weatherDisplay.setText(temp);
            conditionDisplay.setText(condition);
        });
    }

    /**
     * Fetches both current weather and forecast data in a single API call
     * @param location City name or coordinates (format: "lat,lon")
     */
    private void fetchWeatherData(String location) {
        showProgress(true);
        try {
            String encodedLocation = URLEncoder.encode(location, "UTF-8");
            String url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                    getString(R.string.weather_api_key) +
                    "&q=" + encodedLocation +
                    "&days=3&aqi=no&alerts=no";

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        showProgress(false);
                        showToast("Network error: " + e.getMessage());
                        Log.e("WeatherAPI", "Network error", e);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    showProgress(false);
                    try (response) {
                        if (response.isSuccessful()) {
                            assert response.body() != null;
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);

                            // Process both current and forecast data
                            runOnUiThread(() -> {
                                try {
                                    processCurrentWeather(jsonResponse);
                                    processForecastData(jsonResponse);
                                } catch (JSONException e) {
                                    showToast("Error processing data");
                                    Log.e("DataProcessing", "JSON error", e);
                                }
                            });
                        } else {
                            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                            runOnUiThread(() -> {
                                showToast("API Error: " + errorBody);
                                Log.e("WeatherAPI", "API Error: " + errorBody);
                            });
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            showToast("Data parsing error");
                            Log.e("JSON Parsing", "Error", e);
                        });
                    }
                }
            });
        } catch (IOException e) {
            showProgress(false);
            Log.e("Encoding", "Location encoding error", e);
        }
    }

    /**
     * Processes forecast JSON data and populates the forecast container with modern forecast cards.
     * Each card shows the date (formatted as '27 Jan') and min/max temperatures.
     * @param jsonResponse JSON response from the forecast API.
     */
    private void processForecastData(JSONObject jsonResponse) throws JSONException {
        JSONObject forecast = jsonResponse.getJSONObject("forecast");
        JSONArray forecastDays = forecast.getJSONArray("forecastday");

        runOnUiThread(() -> {
            forecastContainer.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

            for (int i = 0; i < forecastDays.length(); i++) {
                try {
                    JSONObject day = forecastDays.getJSONObject(i);
                    JSONObject dayData = day.getJSONObject("day");
                    JSONObject condition = dayData.getJSONObject("condition");

                    View forecastItem = inflater.inflate(R.layout.item_forecast, forecastContainer, false);

                    TextView date = forecastItem.findViewById(R.id.forecastDate);
                    ImageView icon = forecastItem.findViewById(R.id.forecastIcon);
                    TextView maxTemp = forecastItem.findViewById(R.id.forecastMaxTemp);
                    TextView minTemp = forecastItem.findViewById(R.id.forecastMinTemp);

                    // Formatear fecha simple
                    String[] dateParts = day.getString("date").split("-");
                    String formattedDate = dateParts[2] + " " + getMonthName(Integer.parseInt(dateParts[1]));

                    date.setText(formattedDate);
                    maxTemp.setText("Max: " + dayData.getString("maxtemp_c") + "°C");
                    minTemp.setText("Min: " + dayData.getString("mintemp_c") + "°C");

                    Glide.with(MainActivity.this)
                            .load("https:" + condition.getString("icon"))
                            .into(icon);

                    forecastContainer.addView(forecastItem);

                } catch (JSONException e) {
                    Log.e("Forecast", "Error en día " + i, e);
                }
            }
        });
    }

    private String getMonthName(int month) {
        return new DateFormatSymbols().getMonths()[month-1].substring(0, 3);
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
                    fetchWeatherData(coordinates);
                    fetchWeatherData(coordinates);
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