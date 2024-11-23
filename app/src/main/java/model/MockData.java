package model;

import java.util.ArrayList;
import java.util.List;

public class MockData {

    public static WeatherData getWeatherData() {
        return new WeatherData("Madrid", 25.3, "Soleado", "ic_sunny");
    }

    public static List<Forecast> getForecastData() {
        List<Forecast> forecastList = new ArrayList<>();

        forecastList.add(new Forecast("10:00 AM", 24.0, "ic_cloudy"));
        forecastList.add(new Forecast("1:00 PM", 27.0, "ic_sunny"));
        forecastList.add(new Forecast("4:00 PM", 28.5, "ic_sunny"));
        forecastList.add(new Forecast("7:00 PM", 23.0, "ic_cloudy"));

        return forecastList;
    }
}
