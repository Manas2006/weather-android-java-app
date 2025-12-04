package com.example.assignment5;

import java.util.List;

/**
 * Data model class representing a daily weather forecast summary.
 * 
 * This class stores aggregated weather data for a single day, including:
 * - Daily averages calculated from hourly data (temperature, humidity, wind speed, rain)
 * - A user-friendly date label ("Today", "Tomorrow", or formatted date)
 * - Complete hourly data for detailed views
 * 
 * DailyForecast objects are created in MainActivity after parsing JSON from the API
 * and calculating daily averages. They are used to:
 * - Display forecast cards in the main screen
 * - Pass data to DetailedWeatherActivity when user clicks a day card
 * - Show summary information without needing to process hourly data each time
 * 
 * The class has two constructors:
 * - Simple constructor: Only requires date label and temperature (for basic use)
 * - Full constructor: Includes all weather variables and hourly data (for complete forecasts)
 */
public class DailyForecast {
    /**
     * User-friendly label for this day.
     * 
     * Examples:
     * - "Today" for the current day
     * - "Tomorrow" for the next day
     * - "Wed 11 19" for future days (abbreviated day name, month number, day number)
     * 
     * This is displayed in the forecast cards to help users quickly identify which day
     * the forecast represents.
     */
    public final String dateLabel;
    
    /**
     * Daily average temperature in Fahrenheit.
     * 
     * This is calculated by averaging all hourly temperature readings for the day
     * (typically 24 hours worth of data). It's always present (not nullable) because
     * temperature is a core weather variable that the API always provides.
     */
    public final double averageTempF;
    
    /**
     * Daily average relative humidity percentage.
     * 
     * Range: 0-100 (0% = completely dry, 100% = saturated air).
     * Calculated by averaging all hourly humidity readings for the day.
     * Nullable because some API responses may not include humidity data.
     */
    public final Double averageHumidity;
    
    /**
     * Daily average wind speed in miles per hour (mph).
     * 
     * Calculated by averaging all hourly wind speed readings for the day.
     * Nullable because some API responses may not include wind speed data.
     */
    public final Double averageWindSpeed;
    
    /**
     * Daily average precipitation/rain amount in millimeters (mm).
     * 
     * This represents the average hourly rain amount. Note that for display purposes,
     * the total rain (sum) is often more meaningful than the average, but this field
     * stores the average for consistency with other averages.
     * 
     * Nullable because there may be no rain during the day, or data may be unavailable.
     */
    public final Double averageRain;
    
    /**
     * Complete list of hourly weather data points for this day.
     * 
     * This list typically contains 24 HourlyWeatherData objects (one for each hour).
     * It's used to:
     * - Display detailed hourly charts in DetailedWeatherActivity
     * - Calculate averages if needed
     * - Provide granular weather information beyond daily summaries
     * 
     * Nullable because some forecast objects may be created without hourly data
     * (e.g., using the simple constructor).
     */
    public final List<HourlyWeatherData> hourlyData;

    /**
     * Simple constructor for creating a DailyForecast with minimal data.
     * 
     * This constructor is used when only basic information (date and temperature)
     * is available. All other fields are set to null.
     * 
     * @param dateLabel User-friendly label for the day (e.g., "Today", "Tomorrow")
     * @param averageTempF Daily average temperature in Fahrenheit
     */
    public DailyForecast(String dateLabel, double averageTempF) {
        // Store the provided values
        this.dateLabel = dateLabel;
        this.averageTempF = averageTempF;
        
        // Set all optional fields to null since they're not provided
        this.averageHumidity = null;
        this.averageWindSpeed = null;
        this.averageRain = null;
        this.hourlyData = null;
    }

    /**
     * Full constructor for creating a DailyForecast with complete data.
     * 
     * This constructor is used when all weather variables and hourly data are available.
     * It's the primary constructor used in MainActivity after parsing API responses
     * and calculating daily averages.
     * 
     * @param dateLabel User-friendly label for the day (e.g., "Today", "Tomorrow", "Wed 11 19")
     * @param averageTempF Daily average temperature in Fahrenheit
     * @param averageHumidity Daily average humidity percentage, or null if unavailable
     * @param averageWindSpeed Daily average wind speed in mph, or null if unavailable
     * @param averageRain Daily average rain amount in mm, or null if unavailable
     * @param hourlyData Complete list of hourly weather data for this day (typically 24 hours)
     */
    public DailyForecast(String dateLabel, double averageTempF, Double averageHumidity, 
                        Double averageWindSpeed, Double averageRain, List<HourlyWeatherData> hourlyData) {
        // Store all provided values
        // All fields are final, so they cannot be modified after construction
        this.dateLabel = dateLabel;
        this.averageTempF = averageTempF;
        this.averageHumidity = averageHumidity;
        this.averageWindSpeed = averageWindSpeed;
        this.averageRain = averageRain;
        this.hourlyData = hourlyData;
    }
}

