package model;

public class WeatherData {

    private String cityName;
    private double temperature;
    private String weatherDescription;
    private String icon;

    public WeatherData(String cityName, double temperature, String weatherDescription, String icon) {
        this.cityName = cityName;
        this.temperature = temperature;
        this.weatherDescription = weatherDescription;
        this.icon = icon;

    }

    public String getCityName() {
        return cityName;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public String getIcon() {
        return icon;
    }
}
