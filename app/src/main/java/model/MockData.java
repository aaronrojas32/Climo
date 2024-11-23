package model;

import java.util.ArrayList;
import java.util.List;

public class MockData {

    public static WeatherData getWeatherData() {
        return new WeatherData("Madrid", 25.3, "Soleado", "ic_sunny");
    }

    public static List<ForecastItem> getForecastData() {
        List<ForecastItem> forecastList = new ArrayList<>();

        forecastList.add(new ForecastItem("10:00 AM", "24.0", 23));
        forecastList.add(new ForecastItem("1:00 PM", "27.0", 23));
        forecastList.add(new ForecastItem("4:00 PM", "28.5", 23));
        forecastList.add(new ForecastItem("7:00 PM", "23.0", 23));

        return forecastList;
    }
}
