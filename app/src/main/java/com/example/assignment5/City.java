package com.example.assignment5;

import java.io.Serializable;

/**
 * Data model class representing a city with its geographic location.
 * 
 * This class stores information about a city needed to fetch weather data:
 * - City name and state (for display purposes)
 * - Latitude and longitude coordinates (for API requests)
 * 
 * The class implements Serializable so it can be passed between activities
 * via Intent extras if needed, though currently cities are managed within
 * MainActivity.
 * 
 * Cities are used to:
 * - Build API URLs with correct coordinates
 * - Display city name in the UI
 * - Store per-city ML models (each city has its own temperature prediction model)
 * - Allow users to switch between different cities
 */
public class City implements Serializable {
    /**
     * The name of the city (e.g., "Austin", "New York", "Los Angeles").
     * This is used for display purposes and to identify the city.
     */
    public final String name;
    
    /**
     * The state abbreviation (e.g., "TX", "NY", "CA").
     * Used along with the city name to create a display string like "Austin, TX".
     */
    public final String state;
    
    /**
     * The latitude coordinate of the city in decimal degrees.
     * Range: -90.0 to 90.0 (negative = south of equator, positive = north).
     * 
     * Examples:
     * - Austin, TX: approximately 30.28
     * - New York, NY: approximately 40.71
     * 
     * This is required by the weather API to fetch location-specific forecasts.
     */
    public final double latitude;
    
    /**
     * The longitude coordinate of the city in decimal degrees.
     * Range: -180.0 to 180.0 (negative = west of prime meridian, positive = east).
     * 
     * Examples:
     * - Austin, TX: approximately -97.76
     * - New York, NY: approximately -74.01
     * 
     * This is required by the weather API to fetch location-specific forecasts.
     */
    public final double longitude;

    /**
     * Constructor to create a City object with name, state, and coordinates.
     * 
     * @param name The city name (e.g., "Austin")
     * @param state The state abbreviation (e.g., "TX")
     * @param latitude The latitude coordinate in decimal degrees
     * @param longitude The longitude coordinate in decimal degrees
     */
    public City(String name, String state, double latitude, double longitude) {
        // Store all parameters as final fields (immutable after construction)
        // This ensures city data cannot be accidentally modified
        this.name = name;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Get a formatted display name for the city.
     * 
     * Returns a string in the format "CityName, State" (e.g., "Austin, TX").
     * This is used throughout the UI to display which city's weather is being shown.
     * 
     * @return A formatted string combining city name and state (e.g., "Austin, TX")
     */
    public String getDisplayName() {
        // Concatenate city name, comma, space, and state abbreviation
        return name + ", " + state;
    }
}

