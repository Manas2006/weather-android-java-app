package com.example.assignment5;

import java.io.Serializable;

/**
 * Data class to store trained temperature prediction model parameters.
 * 
 * This class represents a simple linear regression model used to predict
 * temperature based on the day of year. The model uses the equation:
 * 
 *   y = m*x + b
 * 
 * Where:
 *   - y = predicted temperature (in Fahrenheit)
 *   - x = day of year (1-365)
 *   - m = slope (how much temperature changes per day of year)
 *   - b = intercept (base temperature)
 * 
 * The model is trained using historical weather data (typically 120+ days)
 * via linear regression (least squares method). Once trained, it can quickly
 * predict temperatures without needing to retrain.
 * 
 * The class implements Serializable so it can be saved to SharedPreferences
 * for persistence across app sessions. This allows the app to cache models
 * and avoid retraining every time the user requests a prediction.
 * 
 * Models are considered "stale" after a certain number of days (typically 7)
 * and should be retrained for better accuracy as weather patterns change.
 */
public class TemperatureModel implements Serializable {
    /**
     * The slope (m) in the linear regression equation y = mx + b.
     * 
     * This value represents how much the temperature changes per unit change
     * in day of year. A positive slope means temperature generally increases
     * as the year progresses (summer is warmer), while a negative slope would
     * indicate decreasing temperatures (though this is unusual for annual patterns).
     * 
     * Example: If slope = 0.1, temperature increases by 0.1°F per day of year.
     */
    public final double slope;
    
    /**
     * The intercept (b) in the linear regression equation y = mx + b.
     * 
     * This value represents the theoretical temperature when day of year is 0
     * (though day of year ranges from 1-365, so this is just a mathematical
     * parameter, not an actual temperature reading).
     * 
     * The intercept helps position the regression line correctly on the graph.
     */
    public final double intercept;
    
    /**
     * Timestamp (in milliseconds since Unix epoch) when this model was trained.
     * 
     * This is used to determine if the model is stale and needs retraining.
     * Models older than a certain threshold (e.g., 7 days) are considered
     * less accurate because weather patterns can change over time.
     * 
     * Stored as long to match System.currentTimeMillis() format.
     */
    public final long trainingDate;
    
    /**
     * Number of historical data points used to train this model.
     * 
     * More data points generally lead to more accurate models (up to a point).
     * This value is stored for reference and validation purposes.
     * Typically, models are trained with 100+ data points for reliability.
     */
    public final int dataPointCount;

    /**
     * Constructor to create a TemperatureModel with trained parameters.
     * 
     * @param slope The slope (m) from linear regression
     * @param intercept The intercept (b) from linear regression
     * @param trainingDate Timestamp when model was trained (milliseconds since epoch)
     * @param dataPointCount Number of data points used for training
     */
    public TemperatureModel(double slope, double intercept, long trainingDate, int dataPointCount) {
        // Store all parameters as final fields (immutable after construction)
        // This ensures the model parameters cannot be accidentally modified
        this.slope = slope;
        this.intercept = intercept;
        this.trainingDate = trainingDate;
        this.dataPointCount = dataPointCount;
    }

    /**
     * Predict the average temperature for a given day of year.
     * 
     * Uses the linear regression equation: temperature = slope * dayOfYear + intercept
     * 
     * This is a simple prediction based on seasonal patterns. It doesn't account for:
     * - Current weather conditions
     * - Short-term weather patterns
     * - Extreme weather events
     * 
     * The prediction is experimental and should be used as a rough estimate only.
     * 
     * @param dayOfYear Day of year (1-365, where 1 = January 1st, 365 = December 31st)
     * @return Predicted average temperature in Fahrenheit
     */
    public double predict(int dayOfYear) {
        // Apply the linear regression formula: y = mx + b
        // Multiply slope by day of year, then add intercept
        return slope * dayOfYear + intercept;
    }

    /**
     * Check if this model is stale and should be retrained.
     * 
     * Models become less accurate over time as weather patterns change.
     * This method calculates the age of the model and compares it to a threshold.
     * 
     * @param maxAgeDays Maximum age in days before the model is considered stale.
     *                   Typically 7 days - models older than this should be retrained.
     * @return true if the model is older than maxAgeDays and should be retrained,
     *         false if the model is still fresh and can be used
     */
    public boolean isStale(int maxAgeDays) {
        // Get current time in milliseconds
        long currentTime = System.currentTimeMillis();
        
        // Calculate how old the model is (in milliseconds)
        long ageMillis = currentTime - trainingDate;
        
        // Convert age from milliseconds to days
        // 24 hours * 60 minutes * 60 seconds * 1000 milliseconds = milliseconds per day
        long ageDays = ageMillis / (24 * 60 * 60 * 1000);
        
        // Return true if model is older than the maximum allowed age
        return ageDays >= maxAgeDays;
    }
}


