package com.example.climo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.climo.R;

import java.util.ArrayList;
import java.util.List;

import adapter.ForecastAdapter;
import model.ForecastItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rvForecast = findViewById(R.id.rvForecast);
        rvForecast.setLayoutManager(new LinearLayoutManager(this));

        // Lista de ejemplo
        List<ForecastItem> forecastList = new ArrayList<>();
        forecastList.add(new ForecastItem("08:00 AM", "20°C", R.drawable.ic_launcher_background));
        forecastList.add(new ForecastItem("12:00 PM", "25°C", R.drawable.ic_launcher_background));
        forecastList.add(new ForecastItem("04:00 PM", "22°C", R.drawable.ic_launcher_background));

        ForecastAdapter adapter = new ForecastAdapter(forecastList);
        rvForecast.setAdapter(adapter);
    }
}
