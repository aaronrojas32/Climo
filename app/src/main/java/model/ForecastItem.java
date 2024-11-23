package model;

public class ForecastItem {
    private String time;
    private String temperature;
    private int icon;

    public ForecastItem(String time, String temperature, int icon) {
        this.time = time;
        this.temperature = temperature;
        this.icon = icon;
    }

    public String getTime() {
        return time;
    }

    public String getTemperature() {
        return temperature;
    }

    public int getIcon() {
        return icon;
    }
}
