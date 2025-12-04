package com.example.assignment5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Main activity for the weather forecast app.
 * 
 * This activity serves as the main screen of the weather application. It displays:
 * - A 7-day weather forecast with daily averages (temperature, humidity, wind speed)
 * - A large temperature display for today
 * - City management functionality (switch/add cities)
 * - Temperature prediction feature using machine learning
 * 
 * The app fetches weather data from the Open Meteo API, parses JSON responses,
 * calculates daily averages from hourly data, and displays them in a user-friendly UI.
 * 
 * Note: Uses AsyncTask which is deprecated in API 30+, but acceptable for this assignment (min SDK 24).
 * In production, you would use modern alternatives like Coroutines, RxJava, or Retrofit.
 */
@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    // ========== UI COMPONENT REFERENCES ==========
    // These variables hold references to views in the layout XML file
    
    // Progress bar shown while fetching forecast data from the API
    private ProgressBar progressBar;
    
    // TextView to display error messages if API call fails
    private TextView errorTextView;
    
    // Label showing the current city name (clickable to manage cities)
    private TextView cityLabel;
    
    // Large text view displaying today's average temperature prominently
    private TextView todayBigTemp;
    
    // Subtitle text below the large temperature (e.g., "Average today")
    private TextView subtitleText;
    
    // Button to manually refresh/refetch the weather forecast
    private ImageButton refreshButton;
    
    // ========== PREDICTION UI COMPONENTS ==========
    // UI elements for the temperature prediction feature
    
    // Button that triggers the temperature prediction process
    private Button predictButton;
    
    // Progress bar shown while training the prediction model
    private ProgressBar predictionProgressBar;
    
    // TextView displaying the prediction result or status messages
    private TextView predictionTextView;
    
    // ========== FORECAST DISPLAY ARRAYS ==========
    // Arrays to hold references to TextViews for each of the 7 forecast days
    // Index 0 = Day 1 (Today), Index 1 = Day 2 (Tomorrow), etc.
    
    // Array of TextViews for day labels ("Today", "Tomorrow", "Wed 11 19", etc.)
    private TextView[] dayLabels;
    
    // Array of TextViews displaying average temperature for each day
    private TextView[] dayTemps;
    
    // Array of TextViews displaying average humidity percentage for each day
    private TextView[] dayHumidities;
    
    // Array of TextViews displaying average wind speed for each day
    private TextView[] dayWinds;
    
    // Array of CardView containers for each day - used to handle click events
    // When user clicks a card, it opens detailed hourly view for that day
    private View[] dayCards;
    
    // ========== CITY MANAGEMENT ==========
    // Variables for managing multiple cities and switching between them
    
    // List of all available cities (predefined + user-added)
    private List<City> cities;
    
    // The currently selected city for which weather is being displayed
    private City currentCity;
    
    // SharedPreferences object for persistent storage of app settings
    // Used to save current city selection and trained ML models
    private SharedPreferences prefs;
    
    // Key for the SharedPreferences file name
    private static final String PREFS_NAME = "WeatherPrefs";
    
    // Key for storing the currently selected city name in SharedPreferences
    private static final String KEY_CURRENT_CITY = "currentCity";
    
    // Key for storing the list of cities (if we were to persist the full list)
    private static final String KEY_CITIES = "cities";
    
    // ========== ML MODEL PERSISTENCE KEYS ==========
    // Keys for storing trained temperature prediction models in SharedPreferences
    // Each key is prefixed with a city-specific identifier to support per-city models
    
    // Key prefix for storing the slope (m) parameter of the linear regression model
    // Format: "model_slope_" + cityKey
    private static final String KEY_MODEL_SLOPE = "model_slope_";
    
    // Key prefix for storing the intercept (b) parameter of the linear regression model
    // Format: "model_intercept_" + cityKey
    private static final String KEY_MODEL_INTERCEPT = "model_intercept_";
    
    // Key prefix for storing when the model was trained (timestamp in milliseconds)
    // Format: "model_training_date_" + cityKey
    private static final String KEY_MODEL_TRAINING_DATE = "model_training_date_";
    
    // Key prefix for storing how many data points were used to train the model
    // Format: "model_data_count_" + cityKey
    private static final String KEY_MODEL_DATA_COUNT = "model_data_count_";
    
    // Key prefix for storing which city the model was trained for (for validation)
    // Format: "model_city_" + cityKey
    private static final String KEY_MODEL_CITY = "model_city_";
    
    // Number of days after which a cached model is considered stale and needs retraining
    // Models older than 7 days are automatically retrained for better accuracy
    private static final int MODEL_RETRAIN_DAYS = 7;
    
    // ========== DATA STORAGE ==========
    // Variables to hold data fetched from the API
    
    // List of daily forecasts currently displayed (stored here so we can pass to detailed view)
    // This is populated after a successful API call and contains 7 DailyForecast objects
    private List<DailyForecast> currentForecasts;
    
    // Cached temperature prediction model for the current city
    // If this is not null and not stale, we can make predictions without retraining
    // This improves performance by avoiding unnecessary API calls and model training
    private TemperatureModel cachedModel;

    /**
     * Called when the activity is first created.
     * This is the entry point of the activity lifecycle.
     * 
     * @param savedInstanceState Bundle containing saved state if activity was previously destroyed
     *                          (e.g., during screen rotation). Null for first-time creation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent class implementation - required for proper activity initialization
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display (content extends behind system bars)
        // This makes the app use the full screen, including areas behind status bar and navigation bar
        EdgeToEdge.enable(this);
        
        // Inflate and set the layout XML file (activity_main.xml) as the content view
        // This connects the XML layout to this Java activity
        setContentView(R.layout.activity_main);
        
        // Handle window insets (system bars like status bar and navigation bar)
        // This ensures content doesn't get hidden behind system UI elements
        // The lambda function receives the main view and window insets, then applies padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Get the insets (padding needed) for system bars (status bar, navigation bar, etc.)
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Apply padding to the main view so content isn't hidden behind system bars
            // left, top, right, bottom padding matches the system bar dimensions
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            
            // Return the insets to indicate we've consumed them
            return insets;
        });

        // Initialize all view references by finding views by their IDs from the layout
        // This must be called after setContentView() so the views exist
        initializeViews();
        
        // Initialize SharedPreferences for persistent storage
        // MODE_PRIVATE means only this app can access these preferences
        // This is where we'll save city selection and ML model data
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Set up the list of cities (predefined + load saved selection)
        // This populates the cities list and sets the currentCity variable
        initializeCities();
        
        // Try to load a previously trained and saved temperature prediction model
        // If a valid model exists for the current city, we can use it for fast predictions
        // without needing to retrain from scratch
        boolean modelLoaded = loadCachedModel();
        if (!modelLoaded) {
            // Log that no cached model was found - this is normal for first-time use
            // The model will be trained when the user first clicks "Predict Tomorrow"
            Log.d("TemperaturePrediction", "No valid cached model found, will train on first prediction");
        }
        
        // Set up click listener for the refresh button
        // When clicked, it will fetch fresh weather data from the API
        refreshButton.setOnClickListener(v -> startForecastFetch());
        
        // Set up click listener for the prediction button
        // When clicked, it will either use cached model or train a new one, then predict tomorrow's temp
        predictButton.setOnClickListener(v -> startPrediction());
        
        // Make the city label clickable to open city management dialog
        // This allows users to switch cities or add new ones
        cityLabel.setOnClickListener(v -> showCityManagementDialog());
        
        // Automatically start fetching forecast data when activity is created
        // This ensures the user sees weather data immediately upon opening the app
        startForecastFetch();
    }
    
    /**
     * Generate a unique identifier key for the current city.
     * 
     * This key is used to store and retrieve ML models in SharedPreferences.
     * Each city gets its own model because weather patterns vary by location.
     * 
     * The key combines city name, latitude, and longitude to ensure uniqueness.
     * For example: "Austin_30.28_-97.76"
     * 
     * @return A unique string identifier for the current city, or "default" if no city is set
     */
    private String getCityKey() {
        // Safety check: if no city is currently selected, return a default key
        if (currentCity == null) {
            return "default";
        }
        
        // Create unique key by combining city name, latitude, and longitude
        // This ensures that even if two cities have the same name, they'll have different keys
        // The format is: "CityName_latitude_longitude"
        // Example: "Austin_30.28_-97.76" or "New York_40.71_-74.01"
        return currentCity.name + "_" + currentCity.latitude + "_" + currentCity.longitude;
    }
    
    /**
     * Load a previously saved temperature prediction model from SharedPreferences.
     * 
     * This method attempts to retrieve a trained ML model that was saved for the current city.
     * The model consists of slope and intercept values from linear regression, along with
     * metadata about when it was trained and how much data was used.
     * 
     * The model is only loaded if:
     * 1. A model exists in SharedPreferences for this city
     * 2. The saved model is actually for the current city (validation check)
     * 3. The model is not stale (less than MODEL_RETRAIN_DAYS old)
     * 
     * @return true if a valid model was successfully loaded, false otherwise
     */
    private boolean loadCachedModel() {
        try {
            // Safety check: can't load a model if no city is selected
            if (currentCity == null) {
                Log.d("TemperaturePrediction", "No current city, cannot load model");
                return false;
            }
            
            // Generate unique key for this city to look up its saved model
            String cityKey = getCityKey();
            
            // Build all the SharedPreferences keys for this city's model data
            // Each piece of model data is stored with a unique key
            String slopeKey = KEY_MODEL_SLOPE + cityKey;           // e.g., "model_slope_Austin_30.28_-97.76"
            String interceptKey = KEY_MODEL_INTERCEPT + cityKey;   // e.g., "model_intercept_Austin_30.28_-97.76"
            String dateKey = KEY_MODEL_TRAINING_DATE + cityKey;    // e.g., "model_training_date_Austin_30.28_-97.76"
            String countKey = KEY_MODEL_DATA_COUNT + cityKey;      // e.g., "model_data_count_Austin_30.28_-97.76"
            String cityNameKey = KEY_MODEL_CITY + cityKey;        // e.g., "model_city_Austin_30.28_-97.76"
            
            // Check if any model data exists for this city
            // We check for slopeKey as a quick way to see if a model was ever saved
            if (!prefs.contains(slopeKey)) {
                // No model found - this is normal for first-time use or new cities
                Log.d("TemperaturePrediction", "No cached model found for city: " + currentCity.getDisplayName());
                return false;
            }
            
            // Verify the saved model is actually for the current city
            // This prevents loading a model that was saved for a different city
            // (in case city coordinates changed or there was a data corruption)
            String savedCityName = prefs.getString(cityNameKey, "");
            if (!savedCityName.equals(currentCity.getDisplayName())) {
                // Model exists but is for a different city - don't use it
                Log.d("TemperaturePrediction", "Cached model is for different city: " + savedCityName + " vs " + currentCity.getDisplayName());
                return false;
            }
            
            // Retrieve all model parameters from SharedPreferences
            // Note: doubles are stored as longs using Double.doubleToLongBits() for precision
            // We must convert them back using Double.longBitsToDouble()
            double slope = Double.longBitsToDouble(prefs.getLong(slopeKey, 0));
            double intercept = Double.longBitsToDouble(prefs.getLong(interceptKey, 0));
            long trainingDate = prefs.getLong(dateKey, 0);  // Timestamp when model was trained
            int dataCount = prefs.getInt(countKey, 0);      // Number of data points used for training
            
            // Reconstruct the TemperatureModel object from saved parameters
            cachedModel = new TemperatureModel(slope, intercept, trainingDate, dataCount);
            
            // Log successful load for debugging
            Log.d("TemperaturePrediction", "Loaded cached model for " + currentCity.getDisplayName() + 
                  ": slope=" + slope + ", intercept=" + intercept + ", trainingDate=" + trainingDate + ", dataCount=" + dataCount);
            
            // Check if the model is stale (too old)
            // Models older than MODEL_RETRAIN_DAYS should be retrained for better accuracy
            // Weather patterns can change over time, so fresh models are more accurate
            if (cachedModel.isStale(MODEL_RETRAIN_DAYS)) {
                Log.d("TemperaturePrediction", "Cached model is stale (older than " + MODEL_RETRAIN_DAYS + " days), will retrain");
                // Clear the stale model so a new one will be trained
                cachedModel = null;
                return false;
            }
            
            // Model is valid and ready to use!
            Log.d("TemperaturePrediction", "Cached model is valid and ready to use for " + currentCity.getDisplayName());
            return true;
            
        } catch (Exception e) {
            // If anything goes wrong during loading, log the error and return false
            // This ensures the app doesn't crash and will just train a new model instead
            Log.e("TemperaturePrediction", "Error loading cached model", e);
            cachedModel = null;
            return false;
        }
    }
    
    /**
     * Save a trained temperature prediction model to SharedPreferences for persistent storage.
     * 
     * This method saves all the parameters of a trained ML model so it can be reused later
     * without needing to retrain. The model is saved per-city, so each city has its own model.
     * 
     * The saved data includes:
     * - Slope (m) and intercept (b) from linear regression equation: y = mx + b
     * - Training date timestamp (to check if model is stale)
     * - Number of data points used (for validation)
     * - City name (for validation to ensure model matches city)
     * 
     * Note: Double values are converted to Long using doubleToLongBits() because
     * SharedPreferences doesn't support double directly, and this preserves precision.
     * 
     * @param model The TemperatureModel object containing the trained model parameters
     */
    private void saveModel(TemperatureModel model) {
        try {
            // Safety check: can't save a model if no city is selected
            if (currentCity == null) {
                Log.e("TemperaturePrediction", "Cannot save model: no current city");
                return;
            }
            
            // Generate unique key for this city to store its model
            String cityKey = getCityKey();
            
            // Build all the SharedPreferences keys for storing model data
            String slopeKey = KEY_MODEL_SLOPE + cityKey;
            String interceptKey = KEY_MODEL_INTERCEPT + cityKey;
            String dateKey = KEY_MODEL_TRAINING_DATE + cityKey;
            String countKey = KEY_MODEL_DATA_COUNT + cityKey;
            String cityNameKey = KEY_MODEL_CITY + cityKey;
            
            // Get an Editor object to modify SharedPreferences
            // All changes must be made through the editor, then committed/applied
            SharedPreferences.Editor editor = prefs.edit();
            
            // Save model parameters
            // Convert doubles to longs for storage (SharedPreferences doesn't support double)
            // Double.doubleToLongBits() preserves the exact bit pattern of the double
            editor.putLong(slopeKey, Double.doubleToLongBits(model.slope));
            editor.putLong(interceptKey, Double.doubleToLongBits(model.intercept));
            
            // Save metadata about the model
            editor.putLong(dateKey, model.trainingDate);        // When model was trained
            editor.putInt(countKey, model.dataPointCount);     // How many data points were used
            editor.putString(cityNameKey, currentCity.getDisplayName());  // Which city this model is for
            
            // Apply changes asynchronously (non-blocking)
            // Use apply() instead of commit() for better performance
            editor.apply();
            
            // Log successful save for debugging
            Log.d("TemperaturePrediction", "Saved model for " + currentCity.getDisplayName() + 
                  ": slope=" + model.slope + ", intercept=" + model.intercept + 
                  ", trainingDate=" + model.trainingDate + ", dataCount=" + model.dataPointCount);
            
            // Update the cached model reference so we can use it immediately
            // without needing to reload from SharedPreferences
            cachedModel = model;
            
        } catch (Exception e) {
            // If saving fails, log the error but don't crash the app
            // The model will just be retrained next time
            Log.e("TemperaturePrediction", "Error saving model", e);
        }
    }
    
    /**
     * Train a linear regression model from historical temperature data.
     * 
     * This method performs simple linear regression to find the best-fit line through
     * historical temperature data. The model predicts temperature based on day of year.
     * 
     * Linear regression finds the equation: y = mx + b
     * Where:
     *   - x = day of year (1-365)
     *   - y = average daily temperature in Fahrenheit
     *   - m = slope (how much temperature changes per day of year)
     *   - b = intercept (base temperature)
     * 
     * The algorithm uses the least squares method to minimize the sum of squared errors
     * between predicted and actual temperatures.
     * 
     * Formula for slope (m): m = (n*Σ(xy) - Σ(x)*Σ(y)) / (n*Σ(x²) - (Σ(x))²)
     * Formula for intercept (b): b = (Σ(y) - m*Σ(x)) / n
     * 
     * @param historicalData List of HistoricalDataPoint objects containing day-of-year
     *                       and temperature pairs from past weather data
     * @return A trained TemperatureModel object, or null if training failed
     */
    private TemperatureModel trainModel(List<HistoricalDataPoint> historicalData) {
        try {
            // Log how many data points we're training with
            Log.d("TemperaturePrediction", "Training model with " + historicalData.size() + " data points");
            
            // Validate we have sufficient data for reliable training
            // Need at least 100 data points to get a meaningful regression
            // Too few points can lead to overfitting or inaccurate models
            if (historicalData.size() < 100) {
                Log.e("TemperaturePrediction", "Insufficient data for training: " + historicalData.size() + " points");
                return null;
            }
            
            // Initialize accumulators for linear regression calculation
            // These will sum up values needed for the regression formulas
            double sumX = 0;      // Sum of all x values (day of year)
            double sumY = 0;      // Sum of all y values (temperatures)
            double sumXY = 0;    // Sum of x*y products
            double sumX2 = 0;    // Sum of x² values
            int n = historicalData.size();  // Number of data points
            
            // Iterate through all historical data points and accumulate sums
            // This is the first pass through the data to calculate statistics
            for (HistoricalDataPoint point : historicalData) {
                double x = point.dayOfYear;        // Day of year (1-365)
                double y = point.temperature;      // Average temperature for that day
                
                // Accumulate values needed for regression formulas
                sumX += x;           // Add day of year to sum
                sumY += y;           // Add temperature to sum
                sumXY += x * y;      // Add product of x and y
                sumX2 += x * x;      // Add square of x
            }
            
            // Log the accumulated statistics for debugging
            Log.d("TemperaturePrediction", "Training stats - SumX: " + sumX + ", SumY: " + sumY + 
                  ", SumXY: " + sumXY + ", SumX2: " + sumX2 + ", n: " + n);
            
            // Calculate the denominator for the slope formula
            // Formula: denominator = n*Σ(x²) - (Σ(x))²
            // This is used in the slope calculation
            double denominator = (n * sumX2 - sumX * sumX);
            
            // Check if denominator is too small (close to zero)
            // This would cause division by zero or very large errors
            // Can happen if all data points have the same x value (same day of year)
            if (Math.abs(denominator) < 0.0001) {
                Log.e("TemperaturePrediction", "Cannot calculate regression: denominator too small: " + denominator);
                return null;
            }
            
            // Calculate slope (m) using the least squares formula
            // Formula: m = (n*Σ(xy) - Σ(x)*Σ(y)) / (n*Σ(x²) - (Σ(x))²)
            // This tells us how much temperature changes per unit change in day of year
            double m = (n * sumXY - sumX * sumY) / denominator;
            
            // Calculate intercept (b) using the slope we just calculated
            // Formula: b = (Σ(y) - m*Σ(x)) / n
            // This is the base temperature when day of year is 0 (theoretical)
            double b = (sumY - m * sumX) / n;
            
            // Log the trained model equation for debugging
            Log.d("TemperaturePrediction", "Trained model: y = " + m + "x + " + b);
            Log.d("TemperaturePrediction", "Slope (m): " + m + ", Intercept (b): " + b);
            
            // Validate that the model parameters are reasonable
            // NaN (Not a Number) or Infinite values indicate calculation errors
            // This can happen with invalid input data or numerical instability
            if (Double.isNaN(m) || Double.isNaN(b) || Double.isInfinite(m) || Double.isInfinite(b)) {
                Log.e("TemperaturePrediction", "Invalid model parameters: m=" + m + ", b=" + b);
                return null;
            }
            
            // Create a TemperatureModel object with the trained parameters
            // System.currentTimeMillis() records when the model was trained
            // n is the number of data points used for training
            TemperatureModel model = new TemperatureModel(m, b, System.currentTimeMillis(), n);
            
            // Save the trained model to SharedPreferences for future use
            // This allows us to reuse the model without retraining every time
            saveModel(model);
            
            // Log successful completion
            Log.d("TemperaturePrediction", "Model training completed and saved successfully");
            return model;
            
        } catch (Exception e) {
            // If anything goes wrong during training, log the error and return null
            // The app will handle this gracefully by showing an error message
            Log.e("TemperaturePrediction", "Error training model", e);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Initialize cities list with default cities.
     */
    private void initializeCities() {
        cities = new ArrayList<>();
        cities.add(new City("Austin", "TX", 30.28, -97.76));
        cities.add(new City("New York", "NY", 40.71, -74.01));
        cities.add(new City("Los Angeles", "CA", 34.05, -118.24));
        cities.add(new City("Chicago", "IL", 41.88, -87.63));
        cities.add(new City("Houston", "TX", 29.76, -95.37));
        
        // Load saved current city or use first city
        String savedCity = prefs.getString(KEY_CURRENT_CITY, "Austin, TX");
        currentCity = cities.get(0);  // Default to first city
        for (City city : cities) {
            if (city.getDisplayName().equals(savedCity)) {
                currentCity = city;
                break;
            }
        }
        
        updateCityLabel();
    }
    
    /**
     * Show dialog to add or switch cities.
     */
    private void showCityManagementDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Manage Cities");
        
        // Create list of city names
        String[] cityNames = new String[cities.size() + 1];
        for (int i = 0; i < cities.size(); i++) {
            cityNames[i] = cities.get(i).getDisplayName();
        }
        cityNames[cities.size()] = "+ Add New City";
        
        builder.setItems(cityNames, (dialog, which) -> {
            if (which == cities.size()) {
                // Add new city
                showAddCityDialog();
            } else {
                // Switch to selected city
                City newCity = cities.get(which);
                // Clear cached model when switching cities
                if (currentCity != null && !newCity.getDisplayName().equals(currentCity.getDisplayName())) {
                    Log.d("TemperaturePrediction", "City changed from " + currentCity.getDisplayName() + 
                          " to " + newCity.getDisplayName() + ", clearing cached model");
                    cachedModel = null;
                }
                currentCity = newCity;
                prefs.edit().putString(KEY_CURRENT_CITY, currentCity.getDisplayName()).apply();
                updateCityLabel();
                // Load model for new city
                loadCachedModel();
                startForecastFetch();
            }
        });
        
        builder.show();
    }
    
    /**
     * Show dialog to add a new city.
     */
    private void showAddCityDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Add New City");
        
        final android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint("City Name");
        final android.widget.EditText stateInput = new android.widget.EditText(this);
        stateInput.setHint("State (e.g., TX)");
        final android.widget.EditText latInput = new android.widget.EditText(this);
        latInput.setHint("Latitude");
        final android.widget.EditText lonInput = new android.widget.EditText(this);
        lonInput.setHint("Longitude");
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(nameInput);
        layout.addView(stateInput);
        layout.addView(latInput);
        layout.addView(lonInput);
        
        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            try {
                String name = nameInput.getText().toString().trim();
                String state = stateInput.getText().toString().trim();
                double lat = Double.parseDouble(latInput.getText().toString());
                double lon = Double.parseDouble(lonInput.getText().toString());
                
                if (!name.isEmpty() && !state.isEmpty()) {
                    City newCity = new City(name, state, lat, lon);
                    cities.add(newCity);
                    // Clear cached model when adding/switching to new city
                    if (currentCity != null && !newCity.getDisplayName().equals(currentCity.getDisplayName())) {
                        Log.d("TemperaturePrediction", "City changed to new city, clearing cached model");
                        cachedModel = null;
                    }
                    currentCity = newCity;
                    prefs.edit().putString(KEY_CURRENT_CITY, currentCity.getDisplayName()).apply();
                    updateCityLabel();
                    // Load model for new city
                    loadCachedModel();
                    startForecastFetch();
                    Toast.makeText(this, "City added!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    /**
     * Update the city label display.
     */
    private void updateCityLabel() {
        if (currentCity != null) {
            cityLabel.setText(currentCity.getDisplayName().toUpperCase(Locale.US));
        }
    }

    /**
     * Initialize all view references from the layout.
     */
    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);
        cityLabel = findViewById(R.id.cityLabel);
        todayBigTemp = findViewById(R.id.todayBigTemp);
        subtitleText = findViewById(R.id.subtitleText);
        refreshButton = findViewById(R.id.refreshButton);

        // Initialize arrays for day labels and temperatures
        dayLabels = new TextView[]{
            findViewById(R.id.day1Label),
            findViewById(R.id.day2Label),
            findViewById(R.id.day3Label),
            findViewById(R.id.day4Label),
            findViewById(R.id.day5Label),
            findViewById(R.id.day6Label),
            findViewById(R.id.day7Label)
        };

        dayTemps = new TextView[]{
            findViewById(R.id.day1Temp),
            findViewById(R.id.day2Temp),
            findViewById(R.id.day3Temp),
            findViewById(R.id.day4Temp),
            findViewById(R.id.day5Temp),
            findViewById(R.id.day6Temp),
            findViewById(R.id.day7Temp)
        };

        dayHumidities = new TextView[]{
            findViewById(R.id.day1Humidity),
            findViewById(R.id.day2Humidity),
            findViewById(R.id.day3Humidity),
            findViewById(R.id.day4Humidity),
            findViewById(R.id.day5Humidity),
            findViewById(R.id.day6Humidity),
            findViewById(R.id.day7Humidity)
        };

        dayWinds = new TextView[]{
            findViewById(R.id.day1Wind),
            findViewById(R.id.day2Wind),
            findViewById(R.id.day3Wind),
            findViewById(R.id.day4Wind),
            findViewById(R.id.day5Wind),
            findViewById(R.id.day6Wind),
            findViewById(R.id.day7Wind)
        };

        // Day cards for click handling
        dayCards = new View[]{
            findViewById(R.id.card1),
            findViewById(R.id.card2),
            findViewById(R.id.card3),
            findViewById(R.id.card4),
            findViewById(R.id.card5),
            findViewById(R.id.card6),
            findViewById(R.id.card7)
        };
        
        // Set click listeners on cards
        for (int i = 0; i < dayCards.length; i++) {
            final int dayIndex = i;
            dayCards[i].setOnClickListener(v -> openDetailedView(dayIndex));
        }

        // Prediction UI
        predictButton = findViewById(R.id.predictButton);
        predictionProgressBar = findViewById(R.id.predictionProgressBar);
        predictionTextView = findViewById(R.id.predictionTextView);
    }
    
    /**
     * Open detailed weather view for a specific day.
     */
    private void openDetailedView(int dayIndex) {
        if (currentForecasts == null || dayIndex >= currentForecasts.size()) {
            Toast.makeText(this, "Weather data not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DailyForecast forecast = currentForecasts.get(dayIndex);
        if (forecast.hourlyData == null || forecast.hourlyData.isEmpty()) {
            Toast.makeText(this, "Hourly data not available for this day", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, DetailedWeatherActivity.class);
        intent.putExtra("dayIndex", dayIndex);
        intent.putExtra("dayLabel", forecast.dateLabel);
        intent.putExtra("hourlyData", new ArrayList<>(forecast.hourlyData));
        startActivity(intent);
    }

    /**
     * Initiate the process of fetching weather forecast data from the API.
     * 
     * This method prepares the UI for loading (shows progress bar, disables refresh button)
     * and then starts an asynchronous task to fetch data from the Open Meteo API.
     * 
     * The actual network request happens in a background thread via AsyncTask,
     * so it doesn't block the main UI thread and cause the app to freeze.
     */
    private void startForecastFetch() {
        // Show the progress bar to indicate data is being loaded
        // This gives visual feedback to the user that something is happening
        progressBar.setVisibility(View.VISIBLE);
        
        // Hide any previous error messages since we're starting a fresh fetch
        errorTextView.setVisibility(View.GONE);
        
        // Disable the refresh button to prevent multiple simultaneous requests
        // This prevents users from spamming the API and causing conflicts
        refreshButton.setEnabled(false);
        
        // Dim the temperature text views to indicate data is being refreshed
        // Alpha of 0.5 makes them semi-transparent (50% opacity)
        // This provides visual feedback that old data is being replaced
        for (TextView temp : dayTemps) {
            temp.setAlpha(0.5f);
        }
        
        // Create and execute the AsyncTask to fetch forecast data
        // AsyncTask runs in a background thread, so it won't block the UI
        // The task will handle the HTTP request, JSON parsing, and UI updates
        new FetchForecastTask().execute();
    }

    /**
     * Build the complete URL for the Open Meteo weather forecast API.
     * 
     * This method constructs the HTTP GET request URL with all necessary parameters
     * to fetch 7 days of hourly weather data for the current city.
     * 
     * The Open Meteo API is a free, open-source weather API that doesn't require
     * an API key. It provides weather forecasts and historical data.
     * 
     * API Endpoint: https://api.open-meteo.com/v1/forecast
     * 
     * Parameters included:
     * - latitude/longitude: Geographic coordinates of the city
     * - hourly: Comma-separated list of weather variables to retrieve
     * - temperature_unit: fahrenheit (for US users)
     * - windspeed_unit: mph (miles per hour, for US users)
     * - forecast_days: 7 (number of days to forecast)
     * - timezone: auto (automatically detect timezone from coordinates)
     * 
     * @return The complete API URL string ready to be used in an HTTP request
     */
    private String buildForecastUrl() {
        // Safety check: if no city is selected, default to Austin, TX
        // This prevents crashes if city management hasn't been initialized yet
        if (currentCity == null) {
            currentCity = new City("Austin", "TX", 30.28, -97.76);
        }
        
        // Build the URL by concatenating base URL with query parameters
        // Each parameter is separated by & and uses URL encoding
        return "https://api.open-meteo.com/v1/forecast" +
                // Add latitude parameter (decimal degrees, e.g., 30.28 for Austin)
                "?latitude=" + currentCity.latitude +
                // Add longitude parameter (decimal degrees, e.g., -97.76 for Austin)
                "&longitude=" + currentCity.longitude +
                // Request multiple hourly weather variables:
                // - temperature_2m: Air temperature at 2 meters above ground
                // - relative_humidity_2m: Humidity percentage at 2 meters
                // - wind_speed_10m: Wind speed at 10 meters above ground
                // - rain: Precipitation amount
                // - surface_pressure: Atmospheric pressure at surface level
                // - visibility: Horizontal visibility distance
                "&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,rain,surface_pressure,visibility" +
                // Request temperature in Fahrenheit (US standard)
                "&temperature_unit=fahrenheit" +
                // Request wind speed in miles per hour (US standard)
                "&windspeed_unit=mph" +
                // Request forecast for 7 days ahead
                "&forecast_days=7" +
                // Automatically detect timezone based on coordinates
                "&timezone=auto";
    }

    /**
     * Generate a human-readable label for a day based on its offset from today.
     * 
     * This method creates user-friendly day labels for the forecast cards:
     * - Offset 0 (today) → "Today"
     * - Offset 1 (tomorrow) → "Tomorrow"
     * - Offset 2+ → Formatted date like "Wed 11 19" (day name, month number, day number)
     * 
     * The formatted date uses a compact format that's easy to read at a glance.
     * 
     * @param baseDate The base date to calculate from (typically today's date)
     * @param offset Number of days from the base date (0 = today, 1 = tomorrow, etc.)
     * @return A user-friendly label string like "Today", "Tomorrow", or "Wed 11 19"
     */
    private String labelForOffset(Calendar baseDate, int offset) {
        // Clone the base date so we don't modify the original
        // Calendar objects are mutable, so cloning prevents side effects
        Calendar targetDate = (Calendar) baseDate.clone();
        
        // Add the offset to get the target date
        // If offset is 0, targetDate stays the same (today)
        // If offset is 1, targetDate becomes tomorrow, etc.
        targetDate.add(Calendar.DAY_OF_MONTH, offset);
        
        // Check the offset to determine which label format to use
        if (offset == 0) {
            // Today - use simple "Today" label
            return "Today";
        } else if (offset == 1) {
            // Tomorrow - use simple "Tomorrow" label
            return "Tomorrow";
        } else {
            // Future days (2+ days away) - use formatted date
            // Format: "EEE M d" means:
            //   EEE = Abbreviated day name (Mon, Tue, Wed, etc.)
            //   M = Month number (1-12, no leading zero)
            //   d = Day of month (1-31, no leading zero)
            // Example output: "Wed 11 19" (Wednesday, November 19th)
            SimpleDateFormat formatter = new SimpleDateFormat("EEE M d", Locale.US);
            return formatter.format(targetDate.getTime());
        }
    }

    /**
     * AsyncTask to fetch weather forecast data from the Open Meteo API.
     * 
     * AsyncTask allows network operations to run in a background thread,
     * preventing the UI from freezing during the HTTP request.
     * 
     * Generic parameters:
     * - Void: No input parameters needed
     * - Void: No progress updates published
     * - List<DailyForecast>: Returns a list of daily forecast objects
     * 
     * Note: AsyncTask is deprecated in API 30+ but acceptable for min SDK 24.
     * Modern apps should use Coroutines, RxJava, or Retrofit instead.
     */
    @SuppressWarnings("deprecation")
    private class FetchForecastTask extends AsyncTask<Void, Void, List<DailyForecast>> {
        // Store error message if the API call fails
        // This will be displayed to the user in onPostExecute()
        private String errorMessage = null;

        /**
         * This method runs in a background thread and performs the actual HTTP request.
         * 
         * It:
         * 1. Builds the API URL
         * 2. Opens an HTTP connection
         * 3. Sends a GET request
         * 4. Reads the JSON response
         * 5. Parses the JSON into DailyForecast objects
         * 
         * @param voids No parameters needed (Void... means variable number of Void arguments)
         * @return List of DailyForecast objects, or null if an error occurred
         */
        @Override
        protected List<DailyForecast> doInBackground(Void... voids) {
            try {
                // Step 1: Build the complete API URL with all parameters
                // buildForecastUrl() constructs the URL with city coordinates and request parameters
                URL url = new URL(buildForecastUrl());
                
                // Step 2: Open an HTTP connection to the API endpoint
                // HttpURLConnection is Java's built-in HTTP client
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                // Step 3: Set the HTTP method to GET (we're requesting data, not sending)
                connection.setRequestMethod("GET");
                
                // Step 4: Set timeouts to prevent the app from hanging indefinitely
                // If connection takes longer than 10 seconds, it will throw an exception
                connection.setConnectTimeout(10000);  // 10 seconds to establish connection
                connection.setReadTimeout(10000);      // 10 seconds to read response data

                // Step 5: Check the HTTP response code
                // HTTP_OK (200) means the request was successful
                // Other codes (404, 500, etc.) indicate errors
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Request failed - store error message for display to user
                    errorMessage = "Server error: " + responseCode;
                    return null;
                }

                // Step 6: Read the response body (JSON data)
                // The API returns JSON text that we need to read line by line
                // BufferedReader wraps InputStreamReader for efficient reading
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                
                // StringBuilder is used to efficiently concatenate all lines of the response
                // It's more efficient than using String concatenation in a loop
                StringBuilder response = new StringBuilder();
                String line;
                
                // Read the response line by line until there are no more lines
                // Each line is appended to the StringBuilder
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                // Step 7: Clean up resources
                // Close the reader to free memory
                reader.close();
                // Disconnect the connection to free network resources
                connection.disconnect();

                // Step 8: Parse the JSON response string into DailyForecast objects
                // parseForecastJson() extracts data from JSON and calculates daily averages
                return parseForecastJson(response.toString());

            } catch (Exception e) {
                // If anything goes wrong (network error, parsing error, etc.),
                // catch the exception and store an error message
                // This prevents the app from crashing
                errorMessage = "Error fetching data: " + e.getMessage();
                e.printStackTrace();  // Print stack trace for debugging
                return null;  // Return null to indicate failure
            }
        }

        /**
         * Parse the JSON response from Open Meteo API and compute daily averages.
         * 
         * This is a critical method that:
         * 1. Parses the JSON structure returned by the API
         * 2. Groups hourly data points by date
         * 3. Calculates daily averages for temperature, humidity, wind speed, and rain
         * 4. Creates DailyForecast objects for each of the next 7 days
         * 
         * The API returns hourly data in parallel arrays (one array for time, one for temperature, etc.)
         * All arrays have the same length, and index i in each array corresponds to the same hour.
         * 
         * Example JSON structure:
         * {
         *   "hourly": {
         *     "time": ["2024-01-15T00:00", "2024-01-15T01:00", ...],
         *     "temperature_2m": [45.2, 44.8, ...],
         *     "relative_humidity_2m": [65, 67, ...],
         *     ...
         *   }
         * }
         * 
         * @param jsonString The complete JSON response string from the API
         * @return List of DailyForecast objects, one for each of the next 7 days
         * @throws Exception If JSON parsing fails or data structure is invalid
         */
        private List<DailyForecast> parseForecastJson(String jsonString) throws Exception {
            // Step 1: Parse the JSON string into a JSONObject
            // This is the root object containing all the API response data
            JSONObject root = new JSONObject(jsonString);
            
            // Step 2: Extract the "hourly" object which contains all hourly weather data
            // The API nests hourly data under a "hourly" key
            JSONObject hourly = root.getJSONObject("hourly");
            
            // Step 3: Extract arrays for each weather variable
            // The API returns parallel arrays - index i in each array corresponds to the same hour
            // getJSONArray() throws exception if key doesn't exist
            // optJSONArray() returns null if key doesn't exist (for optional fields)
            
            JSONArray timeArray = hourly.getJSONArray("time");  // Array of ISO time strings like "2024-01-15T14:00"
            JSONArray tempArray = hourly.getJSONArray("temperature_2m");  // Array of temperatures in Fahrenheit
            
            // Optional arrays (may not always be present in API response)
            JSONArray humidityArray = hourly.optJSONArray("relative_humidity_2m");  // Humidity percentages
            JSONArray windArray = hourly.optJSONArray("wind_speed_10m");  // Wind speeds in mph
            JSONArray rainArray = hourly.optJSONArray("rain");  // Precipitation amounts in mm
            JSONArray pressureArray = hourly.optJSONArray("surface_pressure");  // Atmospheric pressure in hPa
            JSONArray visibilityArray = hourly.optJSONArray("visibility");  // Visibility in km

            // Step 4: Create maps to group hourly values by date
            // Key: date string in format "yyyy-MM-dd" (e.g., "2024-01-15")
            // Value: List of all hourly values for that date
            // We need to group by date so we can calculate daily averages later
            
            Map<String, List<Double>> dateToTemps = new HashMap<>();      // Maps date -> list of hourly temperatures
            Map<String, List<Double>> dateToHumidities = new HashMap<>(); // Maps date -> list of hourly humidities
            Map<String, List<Double>> dateToWinds = new HashMap<>();      // Maps date -> list of hourly wind speeds
            Map<String, List<Double>> dateToRains = new HashMap<>();     // Maps date -> list of hourly rain amounts
            Map<String, List<HourlyWeatherData>> dateToHourlyData = new HashMap<>();  // Maps date -> complete hourly data objects
            
            // Step 5: Iterate through all hourly data points
            // Loop through the time array (or temperature array - they should be same length)
            // For each hour, extract the date and group the values by date
            for (int i = 0; i < timeArray.length() && i < tempArray.length(); i++) {
                // Get the time string for this hour (format: "2024-01-15T14:00")
                String timeString = timeArray.getString(i);
                
                // Get the temperature for this hour (already in Fahrenheit from API)
                double temp = tempArray.getDouble(i);
                
                // Extract just the date part from the time string
                // Time format is "yyyy-MM-ddTHH:mm", so first 10 characters are the date
                // Example: "2024-01-15T14:00" -> "2024-01-15"
                String dateKey = timeString.substring(0, 10);
                
                // Step 6: Get optional weather values (may be null if not available)
                // Check if array exists and if index is within bounds before accessing
                // Use ternary operator: condition ? value_if_true : value_if_false
                Double humidity = (humidityArray != null && i < humidityArray.length()) 
                    ? humidityArray.getDouble(i) : null;
                Double wind = (windArray != null && i < windArray.length()) 
                    ? windArray.getDouble(i) : null;
                Double rain = (rainArray != null && i < rainArray.length()) 
                    ? rainArray.getDouble(i) : null;
                Double pressure = (pressureArray != null && i < pressureArray.length()) 
                    ? pressureArray.getDouble(i) : null;
                Double visibility = (visibilityArray != null && i < visibilityArray.length()) 
                    ? visibilityArray.getDouble(i) : null;
                
                // Step 7: Store complete hourly data object for detailed view
                // This preserves all hourly data so we can show detailed charts later
                // Check if we've seen this date before - if not, create a new list
                if (!dateToHourlyData.containsKey(dateKey)) {
                    dateToHourlyData.put(dateKey, new ArrayList<>());
                }
                // Add this hour's complete data to the list for this date
                dateToHourlyData.get(dateKey).add(
                    new HourlyWeatherData(timeString, temp, humidity, wind, rain, pressure, visibility));
                
                // Step 8: Group temperature values by date for averaging
                // Check if this is the first hour we've seen for this date
                if (!dateToTemps.containsKey(dateKey)) {
                    // Create a new list to store temperatures for this date
                    dateToTemps.put(dateKey, new ArrayList<>());
                }
                // Add this hour's temperature to the list for this date
                dateToTemps.get(dateKey).add(temp);
                
                // Step 9: Group humidity values by date (if available)
                // Only add if humidity value is not null (some hours may not have humidity data)
                if (humidity != null) {
                    if (!dateToHumidities.containsKey(dateKey)) {
                        dateToHumidities.put(dateKey, new ArrayList<>());
                    }
                    dateToHumidities.get(dateKey).add(humidity);
                }
                
                // Step 10: Group wind speed values by date (if available)
                if (wind != null) {
                    if (!dateToWinds.containsKey(dateKey)) {
                        dateToWinds.put(dateKey, new ArrayList<>());
                    }
                    dateToWinds.get(dateKey).add(wind);
                }
                
                // Step 11: Group rain values by date (if available)
                if (rain != null) {
                    if (!dateToRains.containsKey(dateKey)) {
                        dateToRains.put(dateKey, new ArrayList<>());
                    }
                    dateToRains.get(dateKey).add(rain);
                }
            }

            // ========== STEP 12: CALCULATE DAILY AVERAGES ==========
            // Now that we've grouped all hourly data by date, we need to:
            // 1. Calculate the average for each weather variable per day
            // 2. Create DailyForecast objects with these averages
            // 3. Generate user-friendly labels ("Today", "Tomorrow", etc.)
            
            // Get today's date to calculate day offsets
            Calendar today = Calendar.getInstance();
            
            // Create list to hold the final forecast objects (one per day)
            List<DailyForecast> forecasts = new ArrayList<>();
            
            // Get all unique dates we have data for and sort them chronologically
            // The dates are strings like "2024-01-15", so sorting alphabetically also sorts chronologically
            List<String> sortedDates = new ArrayList<>(dateToTemps.keySet());
            sortedDates.sort(String::compareTo);  // Sort in ascending order (earliest to latest)
            
            // Limit processing to 7 days (the API may return more, but we only display 7)
            // Math.min() ensures we don't go out of bounds if API returns fewer than 7 days
            int daysToProcess = Math.min(7, sortedDates.size());
            
            // Process each day to calculate averages
            for (int i = 0; i < daysToProcess; i++) {
                // Get the date string for this day (e.g., "2024-01-15")
                String dateKey = sortedDates.get(i);
                
                // Get the list of all hourly temperatures for this date
                // This list contains 24 values (one for each hour of the day)
                List<Double> temps = dateToTemps.get(dateKey);
                
                // ========== CALCULATE TEMPERATURE AVERAGE ==========
                // Sum all hourly temperatures for this day
                double tempSum = 0.0;
                for (Double temp : temps) {
                    tempSum += temp;  // Add each hourly temperature to the sum
                }
                // Calculate average by dividing sum by number of hours
                // This gives us the mean temperature for the day
                double avgTemp = tempSum / temps.size();
                
                // ========== CALCULATE HUMIDITY AVERAGE ==========
                // Humidity is optional, so check if we have data for this date
                Double avgHumidity = null;
                if (dateToHumidities.containsKey(dateKey)) {
                    // Get list of all hourly humidity values for this date
                    List<Double> humidities = dateToHumidities.get(dateKey);
                    // Sum all humidity values
                    double humiditySum = 0.0;
                    for (Double h : humidities) {
                        humiditySum += h;
                    }
                    // Calculate average humidity percentage
                    avgHumidity = humiditySum / humidities.size();
                }
                // If no humidity data, avgHumidity remains null
                
                // ========== CALCULATE WIND SPEED AVERAGE ==========
                // Wind speed is optional, so check if we have data
                Double avgWind = null;
                if (dateToWinds.containsKey(dateKey)) {
                    // Get list of all hourly wind speeds for this date
                    List<Double> winds = dateToWinds.get(dateKey);
                    // Sum all wind speed values
                    double windSum = 0.0;
                    for (Double w : winds) {
                        windSum += w;
                    }
                    // Calculate average wind speed in mph
                    avgWind = windSum / winds.size();
                }
                
                // ========== CALCULATE RAIN AVERAGE ==========
                // Rain is optional, so check if we have data
                Double avgRain = null;
                if (dateToRains.containsKey(dateKey)) {
                    // Get list of all hourly rain amounts for this date
                    List<Double> rains = dateToRains.get(dateKey);
                    // Sum all rain values (this gives total rain for the day)
                    double rainSum = 0.0;
                    for (Double r : rains) {
                        rainSum += r;
                    }
                    // Calculate average rain amount in mm
                    // Note: For rain, we might want total instead of average, but API provides hourly amounts
                    avgRain = rainSum / rains.size();
                }
                
                // ========== GENERATE USER-FRIENDLY DAY LABEL ==========
                // Calculate how many days from today this date is
                // This allows us to show "Today", "Tomorrow", or a formatted date
                
                // Format today's date as "yyyy-MM-dd" to compare with dateKey
                String todayDateStr = String.format(Locale.US, "%04d-%02d-%02d",
                    today.get(Calendar.YEAR),                    // 4-digit year
                    today.get(Calendar.MONTH) + 1,                // Month (Calendar uses 0-11, so add 1)
                    today.get(Calendar.DAY_OF_MONTH));            // Day of month
                
                // Calculate the offset (number of days from today)
                int offset = 0;  // Default to 0 (today)
                if (!dateKey.equals(todayDateStr)) {
                    // This date is not today, so calculate the difference
                    // Parse the dateKey string into a Calendar object
                    Calendar targetDate = Calendar.getInstance();
                    String[] parts = dateKey.split("-");  // Split "2024-01-15" into ["2024", "01", "15"]
                    targetDate.set(
                        Integer.parseInt(parts[0]),      // Year
                        Integer.parseInt(parts[1]) - 1,  // Month (Calendar uses 0-11)
                        Integer.parseInt(parts[2]));     // Day
                    
                    // Calculate difference in milliseconds
                    long diffMillis = targetDate.getTimeInMillis() - today.getTimeInMillis();
                    // Convert milliseconds to days
                    // 24 hours * 60 minutes * 60 seconds * 1000 milliseconds = milliseconds per day
                    offset = (int) (diffMillis / (24 * 60 * 60 * 1000));
                }
                
                // Generate a human-readable label based on the offset
                // labelForOffset() returns "Today", "Tomorrow", or "Wed 11 19"
                String label = labelForOffset(today, offset);
                
                // Get the complete hourly data for this day (for detailed view)
                List<HourlyWeatherData> hourlyDataForDay = dateToHourlyData.get(dateKey);
                
                // Create a DailyForecast object with all the calculated averages and hourly data
                // This object will be displayed in the UI
                forecasts.add(new DailyForecast(label, avgTemp, avgHumidity, avgWind, avgRain, hourlyDataForDay));
            }

            // Return the list of daily forecasts (one for each of the next 7 days)
            return forecasts;
        }

        /**
         * Called on the main UI thread after doInBackground() completes.
         * 
         * This method runs on the main thread, so it's safe to update UI elements here.
         * It receives the result from doInBackground() and updates the UI accordingly.
         * 
         * @param forecasts The list of DailyForecast objects returned from doInBackground(),
         *                  or null if an error occurred
         */
        @Override
        protected void onPostExecute(List<DailyForecast> forecasts) {
            // Hide the progress bar since data loading is complete
            progressBar.setVisibility(View.GONE);
            
            // Re-enable the refresh button so user can fetch data again
            refreshButton.setEnabled(true);

            // Check if the API call failed or returned no data
            if (forecasts == null || forecasts.isEmpty()) {
                // Display error message to the user
                // Use the error message from doInBackground() if available,
                // otherwise show a generic error message
                errorTextView.setText(errorMessage != null ? errorMessage : "Failed to load forecast data");
                errorTextView.setVisibility(View.VISIBLE);
                return;  // Exit early - don't try to update UI with invalid data
            }

            // API call succeeded - hide any previous error messages
            errorTextView.setVisibility(View.GONE);
            
            // Store forecasts in instance variable so we can pass them to detailed view
            // when user clicks on a day card
            currentForecasts = forecasts;
            
            // Update all UI elements with the forecast data
            // This method populates all the TextViews with calculated averages
            bindForecastData(forecasts);
        }
    }

    /**
     * Update the UI with forecast data by binding DailyForecast objects to TextViews.
     * 
     * This method takes the list of DailyForecast objects (which contain calculated averages)
     * and displays them in the corresponding UI elements. It:
     * 1. Displays today's temperature in the large text view
     * 2. Updates each of the 7 day cards with their respective forecast data
     * 3. Formats numbers appropriately (temperature with 1 decimal, humidity as percentage, etc.)
     * 4. Handles missing data gracefully (shows "--" if data unavailable)
     * 
     * @param forecasts List of DailyForecast objects, one for each day (typically 7 days)
     */
    private void bindForecastData(List<DailyForecast> forecasts) {
        // Safety check: don't try to update UI if we have no data
        if (forecasts == null || forecasts.isEmpty()) {
            return;
        }

        // ========== DISPLAY TODAY'S LARGE TEMPERATURE ==========
        // Get the first forecast (index 0) which represents today
        DailyForecast todayForecast = forecasts.get(0);
        
        // Format temperature with no decimal places and display in large text view
        // Format: "%.0f°" means 0 decimal places, adds degree symbol
        // Example: 75.6°F becomes "76°"
        todayBigTemp.setText(String.format(Locale.US, "%.0f°", todayForecast.averageTempF));

        // ========== UPDATE EACH DAY'S FORECAST CARD ==========
        // Calculate how many days we have data for (may be less than 7 if API returns fewer)
        // dayLabels.length is 7 (we have 7 day cards in the UI)
        int count = Math.min(forecasts.size(), dayLabels.length);
        
        // Loop through each day and update its corresponding UI card
        for (int i = 0; i < count; i++) {
            // Get the forecast object for this day
            DailyForecast forecast = forecasts.get(i);
            
            // Restore full opacity to temperature text (was dimmed during loading)
            // Alpha of 1.0f means fully opaque (100% visible)
            dayTemps[i].setAlpha(1.0f);
            
            // ========== UPDATE DAY LABEL ==========
            // Set the day label (e.g., "Today", "Tomorrow", "Wed 11 19")
            // Check for null to prevent NullPointerException
            if (dayLabels[i] != null) {
                dayLabels[i].setText(forecast.dateLabel);
            }
            
            // ========== UPDATE TEMPERATURE ==========
            // Display the average temperature for this day
            // Format: "%.1f°F" means 1 decimal place, adds °F
            // Example: 75.6 becomes "75.6°F"
            if (dayTemps[i] != null) {
                dayTemps[i].setText(String.format(Locale.US, "%.1f°F", forecast.averageTempF));
            }
            
            // ========== UPDATE HUMIDITY ==========
            // Display average humidity percentage if available
            if (dayHumidities[i] != null) {
                if (forecast.averageHumidity != null) {
                    // Format humidity as percentage with no decimals
                    // Example: 65.3 becomes "Humidity 65%"
                    dayHumidities[i].setText(String.format(Locale.US, "Humidity %.0f%%", forecast.averageHumidity));
                } else {
                    // No humidity data available - show placeholder
                    dayHumidities[i].setText("Humidity --");
                }
            }
            
            // ========== UPDATE WIND SPEED ==========
            // Display average wind speed if available
            if (dayWinds[i] != null) {
                if (forecast.averageWindSpeed != null) {
                    // Format wind speed with 1 decimal place, add "mph" unit
                    // Example: 12.5 becomes "Wind 12.5 mph"
                    dayWinds[i].setText(String.format(Locale.US, "Wind %.1f mph", forecast.averageWindSpeed));
                } else {
                    // No wind data available - show placeholder
                    dayWinds[i].setText("Wind --");
                }
            }
        }

        // ========== CLEAR REMAINING DAY CARDS ==========
        // If we have fewer than 7 forecasts (e.g., API only returned 5 days),
        // clear the remaining day cards so they don't show stale data
        for (int i = count; i < dayLabels.length; i++) {
            // Clear day label
            if (dayLabels[i] != null) {
                dayLabels[i].setText("--");
            }
            
            // Clear temperature and make it semi-transparent to indicate no data
            if (dayTemps[i] != null) {
                dayTemps[i].setText("--°F");
                dayTemps[i].setAlpha(0.5f);  // 50% opacity indicates missing/incomplete data
            }
            
            // Clear humidity
            if (dayHumidities[i] != null) {
                dayHumidities[i].setText("Humidity --");
            }
            
            // Clear wind speed
            if (dayWinds[i] != null) {
                dayWinds[i].setText("Wind --");
            }
        }
    }

    /**
     * Start the temperature prediction task.
     */
    private void startPrediction() {
        Log.d("TemperaturePrediction", "startPrediction() called");
        
        // Check if we have a valid cached model
        if (cachedModel != null && !cachedModel.isStale(MODEL_RETRAIN_DAYS)) {
            Log.d("TemperaturePrediction", "Using cached model for prediction");
            makePredictionWithModel(cachedModel);
            return;
        }
        
        // Need to train or retrain model
        Log.d("TemperaturePrediction", "No valid cached model, training new model");
        predictionProgressBar.setVisibility(View.VISIBLE);
        predictionTextView.setText("Training model...");
        predictButton.setEnabled(false);
        
        new PredictTemperatureTask().execute();
    }
    
    /**
     * Make prediction using an existing model (fast path).
     */
    private void makePredictionWithModel(TemperatureModel model) {
        try {
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_YEAR, 1);
            int tomorrowDayOfYear = tomorrow.get(Calendar.DAY_OF_YEAR);
            
            double prediction = model.predict(tomorrowDayOfYear);
            
            Log.d("TemperaturePrediction", "Prediction using cached model: " + prediction + "°F for dayOfYear " + tomorrowDayOfYear);
            
            String resultText = String.format(Locale.US, 
                "Predicted tomorrow average: %.1f°F (experimental)", prediction);
            predictionTextView.setText(resultText);
            
        } catch (Exception e) {
            Log.e("TemperaturePrediction", "Error making prediction with cached model", e);
            predictionTextView.setText("Error: " + e.getMessage());
        }
    }

    /**
     * Build the Open Meteo API URL for historical weather data.
     * Uses the archive API endpoint for past data.
     * Fetches at least 120 days of past data to ensure we have 100+ data points.
     * @return The complete API URL string for historical data
     */
    private String buildHistoricalDataUrl() {
        if (currentCity == null) {
            currentCity = new City("Austin", "TX", 30.28, -97.76);
        }
        
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
        Calendar startDate = (Calendar) endDate.clone();
        startDate.add(Calendar.DAY_OF_YEAR, -120); // 120 days ago
        
        String startDateStr = String.format(Locale.US, "%04d-%02d-%02d", 
            startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH) + 1, startDate.get(Calendar.DAY_OF_MONTH));
        String endDateStr = String.format(Locale.US, "%04d-%02d-%02d",
            endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH) + 1, endDate.get(Calendar.DAY_OF_MONTH));
        
        // Use the archive API endpoint for historical data
        // According to Open Meteo docs: https://open-meteo.com/en/docs/historical-weather-api
        // Archive API format: archive-api.open-meteo.com/v1/archive
        // Required parameters: latitude, longitude, start_date, end_date, hourly
        // Note: Archive API returns temperature in Celsius (no temperature_unit parameter)
        // We'll convert to Fahrenheit after receiving the data
        // Timezone: Use UTC for reliability (always supported by API)
        String url = "https://archive-api.open-meteo.com/v1/archive" +
                "?latitude=" + currentCity.latitude +
                "&longitude=" + currentCity.longitude +
                "&hourly=temperature_2m" +
                "&start_date=" + startDateStr +
                "&end_date=" + endDateStr +
                "&timezone=UTC";
        
        Log.d("TemperaturePrediction", "Historical data URL: " + url);
        Log.d("TemperaturePrediction", "Date range: " + startDateStr + " to " + endDateStr + 
              " (" + (120) + " days)");
        return url;
    }
    
    /**
     * Test method to verify API call and model training.
     * This can be called manually for debugging.
     */
    private void testApiAndModel() {
        Log.d("TemperaturePrediction", "=== TEST: Starting API and Model Test ===");
        
        new Thread(() -> {
            try {
                // Test 1: Build URL
                String url = buildHistoricalDataUrl();
                Log.d("TemperaturePrediction", "TEST: URL built successfully: " + url);
                
                // Test 2: Fetch data
                FetchHistoricalDataTask fetchTask = new FetchHistoricalDataTask();
                List<HistoricalDataPoint> data = fetchTask.doInBackground();
                
                if (data == null || data.isEmpty()) {
                    Log.e("TemperaturePrediction", "TEST FAILED: No data returned. Error: " + 
                          (fetchTask.errorMessage != null ? fetchTask.errorMessage : "Unknown"));
                    return;
                }
                
                Log.d("TemperaturePrediction", "TEST: API call successful, received " + data.size() + " data points");
                
                // Test 3: Train model
                TemperatureModel model = trainModel(data);
                
                if (model == null) {
                    Log.e("TemperaturePrediction", "TEST FAILED: Model training failed");
                    return;
                }
                
                Log.d("TemperaturePrediction", "TEST: Model training successful");
                Log.d("TemperaturePrediction", "TEST: Model parameters - slope=" + model.slope + 
                      ", intercept=" + model.intercept);
                
                // Test 4: Make prediction
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                int tomorrowDayOfYear = tomorrow.get(Calendar.DAY_OF_YEAR);
                double prediction = model.predict(tomorrowDayOfYear);
                
                Log.d("TemperaturePrediction", "TEST: Prediction successful - " + prediction + "°F for dayOfYear " + tomorrowDayOfYear);
                Log.d("TemperaturePrediction", "=== TEST: All tests passed ===");
                
            } catch (Exception e) {
                Log.e("TemperaturePrediction", "TEST FAILED: Exception occurred", e);
            }
        }).start();
    }

    /**
     * Data class to hold historical daily temperature data.
     */
    private static class HistoricalDataPoint {
        final int dayOfYear;
        final double temperature;
        final String date;

        HistoricalDataPoint(int dayOfYear, double temperature, String date) {
            this.dayOfYear = dayOfYear;
            this.temperature = temperature;
            this.date = date;
        }
    }

    /**
     * AsyncTask to fetch historical weather data from Open Meteo API.
     * Note: AsyncTask is deprecated in API 30+, but acceptable for this assignment (min SDK 24).
     */
    @SuppressWarnings("deprecation")
    private class FetchHistoricalDataTask extends AsyncTask<Void, Void, List<HistoricalDataPoint>> {
        String errorMessage = null;  // Package-private for access from PredictTemperatureTask

        @Override
        protected List<HistoricalDataPoint> doInBackground(Void... voids) {
            Log.d("TemperaturePrediction", "FetchHistoricalDataTask.doInBackground() started");
            try {
                // Build URL and open connection
                URL url = new URL(buildHistoricalDataUrl());
                Log.d("TemperaturePrediction", "Opening connection to: " + url.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setReadTimeout(15000);

                // Check response code
                int responseCode = connection.getResponseCode();
                Log.d("TemperaturePrediction", "Response code: " + responseCode);
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Read error response body for more details
                    String errorBody = "";
                    try {
                        BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorReader.close();
                        errorBody = errorResponse.toString();
                        Log.e("TemperaturePrediction", "Error response body: " + errorBody);
                    } catch (Exception e) {
                        Log.e("TemperaturePrediction", "Could not read error stream", e);
                    }
                    
                    errorMessage = "Server error: " + responseCode;
                    if (!errorBody.isEmpty()) {
                        errorMessage += " - " + errorBody;
                    }
                    Log.e("TemperaturePrediction", "HTTP error: " + responseCode + ", message: " + errorMessage);
                    return null;
                }

                // Read response
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                connection.disconnect();

                Log.d("TemperaturePrediction", "Response received, length: " + response.length());
                
                // Parse JSON
                return parseHistoricalJson(response.toString());

            } catch (Exception e) {
                errorMessage = "Error fetching historical data: " + e.getMessage();
                Log.e("TemperaturePrediction", "Exception in FetchHistoricalDataTask", e);
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Parse the JSON response and compute daily averages.
         * @param jsonString The JSON response string
         * @return List of HistoricalDataPoint objects with dayOfYear and temperature
         */
        private List<HistoricalDataPoint> parseHistoricalJson(String jsonString) throws Exception {
            Log.d("TemperaturePrediction", "parseHistoricalJson() started");
            JSONObject root = new JSONObject(jsonString);
            JSONObject hourly = root.getJSONObject("hourly");
            
            JSONArray timeArray = hourly.getJSONArray("time");
            JSONArray tempArray = hourly.getJSONArray("temperature_2m");

            Log.d("TemperaturePrediction", "Found " + timeArray.length() + " hourly data points");
            
            // Validate arrays have data
            if (timeArray.length() == 0 || tempArray.length() == 0) {
                throw new Exception("Empty data arrays from API");
            }
            
            if (timeArray.length() != tempArray.length()) {
                Log.w("TemperaturePrediction", "Array length mismatch: time=" + timeArray.length() + 
                      ", temp=" + tempArray.length());
            }

            // Group hourly values by date (yyyy-MM-dd)
            Map<String, List<Double>> dateToTemps = new HashMap<>();
            int validPoints = 0;
            int invalidPoints = 0;
            
            for (int i = 0; i < timeArray.length() && i < tempArray.length(); i++) {
                try {
                    String timeString = timeArray.getString(i);
                    
                    // Validate time string format
                    if (timeString == null || timeString.length() < 10) {
                        Log.w("TemperaturePrediction", "Invalid time string at index " + i + ": " + timeString);
                        invalidPoints++;
                        continue;
                    }
                    
                    double tempCelsius = tempArray.getDouble(i);
                    
                    // Validate temperature is reasonable (not NaN or infinite)
                    if (Double.isNaN(tempCelsius) || Double.isInfinite(tempCelsius)) {
                        Log.w("TemperaturePrediction", "Invalid temperature at index " + i + ": " + tempCelsius);
                        invalidPoints++;
                        continue;
                    }
                    
                    // Archive API returns temperature in Celsius, convert to Fahrenheit
                    double tempFahrenheit = (tempCelsius * 9.0 / 5.0) + 32.0;
                    
                    // Extract date part (first 10 characters: "yyyy-MM-dd")
                    String dateKey = timeString.substring(0, 10);
                    
                    if (!dateToTemps.containsKey(dateKey)) {
                        dateToTemps.put(dateKey, new ArrayList<>());
                    }
                    dateToTemps.get(dateKey).add(tempFahrenheit);
                    validPoints++;
                    
                } catch (Exception e) {
                    Log.w("TemperaturePrediction", "Error processing data point " + i + ": " + e.getMessage());
                    invalidPoints++;
                }
            }
            
            Log.d("TemperaturePrediction", "Processed " + validPoints + " valid points, " + invalidPoints + " invalid points");

            Log.d("TemperaturePrediction", "Grouped into " + dateToTemps.size() + " unique dates");
            
            if (dateToTemps.isEmpty()) {
                throw new Exception("No valid dates found in API response");
            }

            // Compute daily averages and create data points
            List<HistoricalDataPoint> dataPoints = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            
            // Get sorted dates
            List<String> sortedDates = new ArrayList<>(dateToTemps.keySet());
            sortedDates.sort(String::compareTo);
            
            Log.d("TemperaturePrediction", "Date range: " + sortedDates.get(0) + " to " + 
                  sortedDates.get(sortedDates.size() - 1));
            
            for (String dateKey : sortedDates) {
                try {
                    List<Double> temps = dateToTemps.get(dateKey);
                    
                    if (temps == null || temps.isEmpty()) {
                        Log.w("TemperaturePrediction", "No temperatures for date: " + dateKey);
                        continue;
                    }
                    
                    // Compute average
                    double sum = 0.0;
                    for (Double temp : temps) {
                        if (temp != null && !Double.isNaN(temp) && !Double.isInfinite(temp)) {
                            sum += temp;
                        }
                    }
                    double avgTemp = sum / temps.size();
                    
                    // Validate average is reasonable
                    if (Double.isNaN(avgTemp) || Double.isInfinite(avgTemp)) {
                        Log.w("TemperaturePrediction", "Invalid average for date: " + dateKey);
                        continue;
                    }
                    
                    // Calculate day of year
                    String[] parts = dateKey.split("-");
                    if (parts.length != 3) {
                        Log.w("TemperaturePrediction", "Invalid date format: " + dateKey);
                        continue;
                    }
                    
                    cal.set(Integer.parseInt(parts[0]),
                           Integer.parseInt(parts[1]) - 1,
                           Integer.parseInt(parts[2]));
                    int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
                    
                    dataPoints.add(new HistoricalDataPoint(dayOfYear, avgTemp, dateKey));
                    
                } catch (Exception e) {
                    Log.w("TemperaturePrediction", "Error processing date " + dateKey + ": " + e.getMessage());
                }
            }

            Log.d("TemperaturePrediction", "Created " + dataPoints.size() + " historical data points");
            
            // Log statistics
            if (!dataPoints.isEmpty()) {
                double minTemp = dataPoints.get(0).temperature;
                double maxTemp = dataPoints.get(0).temperature;
                double sumTemp = 0;
                for (HistoricalDataPoint point : dataPoints) {
                    minTemp = Math.min(minTemp, point.temperature);
                    maxTemp = Math.max(maxTemp, point.temperature);
                    sumTemp += point.temperature;
                }
                double avgTemp = sumTemp / dataPoints.size();
                Log.d("TemperaturePrediction", "Temperature stats - Min: " + minTemp + "°F, Max: " + 
                      maxTemp + "°F, Avg: " + avgTemp + "°F");
            }
            
            return dataPoints;
        }
    }

    /**
     * AsyncTask to train model and predict tomorrow's temperature.
     * Note: AsyncTask is deprecated in API 30+, but acceptable for this assignment (min SDK 24).
     */
    @SuppressWarnings("deprecation")
    private class PredictTemperatureTask extends AsyncTask<Void, Void, TemperatureModel> {
        private String errorMessage = null;

        @Override
        protected TemperatureModel doInBackground(Void... voids) {
            Log.d("TemperaturePrediction", "PredictTemperatureTask.doInBackground() started");
            try {
                // Step 1: Fetch historical data
                Log.d("TemperaturePrediction", "Step 1: Fetching historical data from API");
                FetchHistoricalDataTask fetchTask = new FetchHistoricalDataTask();
                List<HistoricalDataPoint> historicalData = fetchTask.doInBackground();
                
                if (historicalData == null || historicalData.isEmpty()) {
                    errorMessage = fetchTask.errorMessage != null ? fetchTask.errorMessage : "No historical data available";
                    Log.e("TemperaturePrediction", "No historical data: " + errorMessage);
                    return null;
                }
                
                Log.d("TemperaturePrediction", "API returned " + historicalData.size() + " historical data points");
                
                // Validate data quality
                if (historicalData.size() < 100) {
                    errorMessage = "Insufficient data: only " + historicalData.size() + " points (need 100+)";
                    Log.e("TemperaturePrediction", errorMessage);
                    return null;
                }
                
                // Log sample data points for verification
                Log.d("TemperaturePrediction", "Sample data points (first 5):");
                for (int i = 0; i < Math.min(5, historicalData.size()); i++) {
                    HistoricalDataPoint point = historicalData.get(i);
                    Log.d("TemperaturePrediction", "  [" + i + "] dayOfYear=" + point.dayOfYear + 
                          ", temp=" + point.temperature + "°F, date=" + point.date);
                }
                
                // Step 2: Train model
                Log.d("TemperaturePrediction", "Step 2: Training model");
                TemperatureModel model = trainModel(historicalData);
                
                if (model == null) {
                    errorMessage = "Model training failed";
                    Log.e("TemperaturePrediction", errorMessage);
                    return null;
                }
                
                Log.d("TemperaturePrediction", "Model trained successfully: slope=" + model.slope + 
                      ", intercept=" + model.intercept);
                
                return model;
                
            } catch (Exception e) {
                errorMessage = "Prediction error: " + e.getMessage();
                Log.e("TemperaturePrediction", "Exception in PredictTemperatureTask", e);
                e.printStackTrace();
                if (e.getCause() != null) {
                    Log.e("TemperaturePrediction", "Cause: " + e.getCause().getMessage(), e.getCause());
                    errorMessage += " (Cause: " + e.getCause().getMessage() + ")";
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(TemperatureModel model) {
            Log.d("TemperaturePrediction", "onPostExecute() called with model: " + (model != null ? "valid" : "null"));
            predictionProgressBar.setVisibility(View.GONE);
            predictButton.setEnabled(true);
            
            if (model == null) {
                String errorMsg = errorMessage != null ? errorMessage : "Model training failed";
                Log.e("TemperaturePrediction", "Prediction failed: " + errorMsg);
                predictionTextView.setText("Error: " + errorMsg);
                return;
            }
            
            // Make prediction using the trained model
            makePredictionWithModel(model);
        }
    }
}
