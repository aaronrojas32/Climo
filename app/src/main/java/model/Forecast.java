package model;

public class Forecast {
    private String time;
    private double temperature;
    private String icon;

    public Forecast(String time, double temperature, String icon) {
        this.time = time;
        this.temperature = temperature;
        this.icon = icon;
    }

    public String getTime() {
        return time;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getIcon() {
        return icon;
    }
}
