package com.example.climo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActionBar;
import android.content.Context;
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

    private EditText cityInput;
    private TextView weatherDisplay;
    private TextView conditionDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        hideSystemUI();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityInput = findViewById(R.id.editTextCity);
        Button fetchWeatherButton = findViewById(R.id.buttonFetchWeather);
        weatherDisplay = findViewById(R.id.actualWeather);
        conditionDisplay = findViewById(R.id.condition);

        fetchWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityInput.getText().toString().trim();
                if (!city.isEmpty()) {
                    hideKeyboard();
                    fetchWeather(city);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a city", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        //Setups permantly focus mode.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void fetchWeather(String city) {
        String API_KEY = getString(R.string.weather_api_key); // Obtén la clave desde apikeys.xml
        String url = "https://api.weatherapi.com/v1/current.json?key=" + API_KEY + "&q=" + city;

        Log.d("WeatherAPI", "Generated URL: " + url); // Log para verificar la URL generada

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e("WeatherAPI", "Network error", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body().string();
                    Log.e("WeatherAPI", "API Error: " + errorBody); // Muestra el error completo
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Error: " + errorBody, Toast.LENGTH_LONG).show());
                    return;
                }

                try {
                    String jsonResponse = response.body().string();
                    Log.d("WeatherAPI", "Response: " + jsonResponse); // Log para la respuesta completa
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    JSONObject current = jsonObject.getJSONObject("current");
                    String temperature = current.getString("temp_c");
                    String condition = current.getJSONObject("condition").getString("text");

                    String temp = temperature + "°C";
                    String cond = "Condition: " + condition;

                    runOnUiThread(() -> weatherDisplay.setText(temp));
                    runOnUiThread(() -> conditionDisplay.setText(cond));


                } catch (JSONException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Parsing error", Toast.LENGTH_SHORT).show());
                    Log.e("WeatherAPI", "JSON Parsing error", e);
                }
            }
        });
    }
}
