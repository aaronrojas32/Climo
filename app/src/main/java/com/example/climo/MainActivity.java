package com.example.climo;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvCity, tvTemperature, tvDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referencias a los elementos del layout
        tvCity = findViewById(R.id.tvCity);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);

        // Obtener el clima actual para una ciudad predeterminada
        fetchWeatherData("London"); // Cambia "London" por una ciudad predeterminada
    }

    private void fetchWeatherData(String city) {
        // Obtener la clave API desde los recursos
        String apiKey = getString(R.string.weather_api_key);
        String url = "https://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + city;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    runOnUiThread(() -> parseWeatherData(jsonResponse));
                }
            }
        });
    }

    private void parseWeatherData(String jsonResponse) {
        // Usar Gson para analizar la respuesta JSON
        JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
        JsonObject current = jsonObject.getAsJsonObject("current");
        JsonObject location = jsonObject.getAsJsonObject("location");

        // Obtener datos del JSON
        String city = location.get("name").getAsString();
        String temperature = current.get("temp_c").getAsString() + "°C";
        String description = current.getAsJsonObject("condition").get("text").getAsString();

        // Actualizar la UI
        tvCity.setText(city);
        tvTemperature.setText(temperature);
        tvDescription.setText(description);
    }
}
