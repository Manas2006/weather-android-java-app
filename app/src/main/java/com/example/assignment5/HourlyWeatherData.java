package com.example.assignment5;

import java.io.Serializable;

/**
 * Data model class representing a single hour's weather data within a day.
 * 
 * This class stores all weather measurements for one specific hour (e.g., 2:00 PM).
 * It's used to:
 * - Store hourly data fetched from the weather API
 * - Pass hourly data between activities (via Intent, requires Serializable)
 * - Display detailed hourly charts in DetailedWeatherActivity
 * - Calculate daily averages in MainActivity
 * 
 * The class implements Serializable so it can be passed between activities
 * via Intent extras. This allows MainActivity to send hourly data to
 * DetailedWeatherActivity when the user clicks on a day card.
 * 
 * Note: Some fields are nullable (Double instead of double) because the API
 * may not always provide all weather variables for every hour.
 */
public class HourlyWeatherData implements Serializable {
    /**
     * ISO 8601 formatted time string for this hour.
     * Format: "yyyy-MM-ddTHH:mm" (e.g., "2024-01-15T14:00" for 2:00 PM on Jan 15, 2024)
     * This uniquely identifies which hour this data represents.
     */
    public final String time;
    
    /**
     * Air temperature at 2 meters above ground level.
     * Measured in Fahrenheit (converted from API response if needed).
     * This is always present (not nullable) as temperature is a core weather variable.
     */
    public final double temperature;
    
    /**
     * Relative humidity percentage at 2 meters above ground level.
     * Range: 0-100 (0% = completely dry, 100% = saturated air).
     * Nullable because some API responses may not include humidity data.
     */
    public final Double humidity;
    
    /**
     * Wind speed at 10 meters above ground level.
     * Measured in miles per hour (mph).
     * Nullable because some API responses may not include wind speed data.
     */
    public final Double windSpeed;
    
    /**
     * Precipitation/rain amount for this hour.
     * Measured in millimeters (mm).
     * Nullable because there may be no rain during this hour, or data may be unavailable.
     */
    public final Double rain;
    
    /**
     * Atmospheric pressure at surface level.
     * Measured in hectopascals (hPa).
     * Nullable because some API responses may not include pressure data.
     */
    public final Double pressure;
    
    /**
     * Horizontal visibility distance.
     * Measured in kilometers (km).
     * Nullable because some API responses may not include visibility data.
     */
    public final Double visibility;

    /**
     * Constructor to create an HourlyWeatherData object with all weather measurements.
     * 
     * @param time ISO 8601 time string (e.g., "2024-01-15T14:00")
     * @param temperature Air temperature in Fahrenheit (required, always present)
     * @param humidity Relative humidity percentage (0-100), or null if unavailable
     * @param windSpeed Wind speed in mph, or null if unavailable
     * @param rain Precipitation amount in mm, or null if unavailable
     * @param pressure Atmospheric pressure in hPa, or null if unavailable
     * @param visibility Visibility distance in km, or null if unavailable
     */
    public HourlyWeatherData(String time, double temperature, Double humidity, 
                             Double windSpeed, Double rain, Double pressure, Double visibility) {
        // Store all parameters as final fields (immutable after construction)
        // This ensures data integrity - once created, the object cannot be modified
        this.time = time;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.rain = rain;
        this.pressure = pressure;
        this.visibility = visibility;
    }
}

