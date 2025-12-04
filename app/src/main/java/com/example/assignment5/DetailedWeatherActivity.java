package com.example.assignment5;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity that displays detailed hourly weather information for a specific day.
 * 
 * This activity is launched when the user clicks on a day card in MainActivity.
 * It displays:
 * - Four interactive line charts showing hourly trends for:
 *   * Temperature (red line)
 *   * Humidity (blue line)
 *   * Wind Speed (green line)
 *   * Precipitation/Rain (purple line)
 * - Summary statistics showing daily averages and totals
 * 
 * The charts are created using the MPAndroidChart library, which provides
 * professional-looking, interactive line charts with zoom and scroll capabilities.
 * 
 * Data is passed from MainActivity via Intent extras, containing the hourly
 * weather data for the selected day.
 */
public class DetailedWeatherActivity extends AppCompatActivity {

    // ========== CHART VIEWS ==========
    // LineChart objects from MPAndroidChart library for displaying hourly data trends
    
    // Chart displaying hourly temperature throughout the day (in Fahrenheit)
    private LineChart tempChart;
    
    // Chart displaying hourly humidity percentage throughout the day
    private LineChart humidityChart;
    
    // Chart displaying hourly wind speed throughout the day (in mph)
    private LineChart windChart;
    
    // Chart displaying hourly precipitation/rain amount throughout the day (in mm)
    private LineChart rainChart;
    
    // ========== TEXT VIEWS FOR SUMMARY STATISTICS ==========
    // These display calculated averages and totals for the day
    
    // Label showing which day this is (e.g., "Today", "Tomorrow", "Wed 11 19")
    private TextView dayLabel;
    
    // Text view displaying the average temperature for the day
    private TextView avgTempText;
    
    // Text view displaying the average humidity percentage for the day
    private TextView avgHumidityText;
    
    // Text view displaying the average wind speed for the day
    private TextView avgWindText;
    
    // Text view displaying the total rain/precipitation for the day
    private TextView totalRainText;
    
    // ========== NAVIGATION ==========
    // Button to return to the main forecast screen
    private ImageButton backButton;

    /**
     * Called when the activity is first created.
     * 
     * This method:
     * 1. Sets up the layout
     * 2. Retrieves hourly weather data passed from MainActivity via Intent
     * 3. Initializes all UI components
     * 4. Sets up the line charts with hourly data
     * 5. Calculates and displays daily averages
     * 
     * @param savedInstanceState Bundle containing saved state (null for first creation)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Call parent implementation for proper activity initialization
        super.onCreate(savedInstanceState);
        
        // Inflate and set the layout XML file (activity_detailed_weather.xml)
        // This connects the XML layout to this Java activity
        setContentView(R.layout.activity_detailed_weather);

        // ========== RETRIEVE DATA FROM INTENT ==========
        // MainActivity passes data via Intent extras when launching this activity
        
        // Get the index of the day that was clicked (0 = today, 1 = tomorrow, etc.)
        // Default to 0 if not provided
        int dayIndex = getIntent().getIntExtra("dayIndex", 0);
        
        // Get the day label string (e.g., "Today", "Tomorrow", "Wed 11 19")
        // This will be displayed at the top of the screen
        String dayLabelStr = getIntent().getStringExtra("dayLabel");
        
        // Get the list of hourly weather data points for this day
        // HourlyWeatherData implements Serializable, so it can be passed via Intent
        // This list contains 24 HourlyWeatherData objects (one for each hour of the day)
        ArrayList<HourlyWeatherData> hourlyData = (ArrayList<HourlyWeatherData>) 
            getIntent().getSerializableExtra("hourlyData");

        // Initialize all view references by finding them by ID from the layout
        initializeViews();
        
        // Set up the back button to close this activity and return to MainActivity
        // finish() closes the current activity and returns to the previous one
        backButton.setOnClickListener(v -> finish());
        
        // Only set up charts and display data if we have valid hourly data
        // This prevents crashes if data wasn't passed correctly
        if (hourlyData != null && !hourlyData.isEmpty()) {
            // Set the day label at the top of the screen
            // Use provided label or default to "Day Details" if not provided
            dayLabel.setText(dayLabelStr != null ? dayLabelStr : "Day Details");
            
            // Set up all four line charts with the hourly data
            // This creates the visual graphs showing trends throughout the day
            setupCharts(hourlyData);
            
            // Calculate daily averages and totals, then display them in summary TextViews
            // This shows the user quick statistics without having to read the charts
            calculateAndDisplayAverages(hourlyData);
        }
    }

    /**
     * Initialize all view references by finding them by their IDs from the layout XML.
     * 
     * This method must be called after setContentView() so that the views exist.
     * It connects the Java variables to the actual UI elements in the layout.
     */
    private void initializeViews() {
        // Find and store reference to the back button
        backButton = findViewById(R.id.backButton);
        
        // Find and store reference to the day label TextView
        dayLabel = findViewById(R.id.detailedDayLabel);
        
        // Find and store references to summary statistic TextViews
        avgTempText = findViewById(R.id.avgTempText);
        avgHumidityText = findViewById(R.id.avgHumidityText);
        avgWindText = findViewById(R.id.avgWindText);
        totalRainText = findViewById(R.id.totalRainText);
        
        // Find and store references to all four line chart views
        // These are LineChart objects from the MPAndroidChart library
        tempChart = findViewById(R.id.tempChart);
        humidityChart = findViewById(R.id.humidityChart);
        windChart = findViewById(R.id.windChart);
        rainChart = findViewById(R.id.rainChart);
    }

    /**
     * Set up all four line charts with hourly weather data.
     * 
     * This method coordinates the setup of all charts. It checks if data is available
     * for each weather variable and either sets up the chart or hides it if data
     * is missing.
     * 
     * Charts are only displayed if the corresponding data is available in the
     * hourly data. If data is missing (null), the chart is hidden to avoid
     * showing empty or misleading graphs.
     * 
     * @param hourlyData List of HourlyWeatherData objects containing hourly weather
     *                  measurements for the selected day
     */
    private void setupCharts(List<HourlyWeatherData> hourlyData) {
        // ========== TEMPERATURE CHART ==========
        // Temperature is always required, so always set up this chart
        setupTemperatureChart(hourlyData);
        
        // ========== HUMIDITY CHART ==========
        // Check if humidity data is available (it's optional in the API response)
        // We check the first data point as a quick way to see if any humidity data exists
        if (hourlyData.get(0).humidity != null) {
            // Humidity data is available - set up and display the chart
            setupHumidityChart(hourlyData);
        } else {
            // No humidity data - hide the chart so it doesn't take up space
            humidityChart.setVisibility(android.view.View.GONE);
        }
        
        // ========== WIND SPEED CHART ==========
        // Check if wind speed data is available (it's optional in the API response)
        if (hourlyData.get(0).windSpeed != null) {
            // Wind speed data is available - set up and display the chart
            setupWindChart(hourlyData);
        } else {
            // No wind speed data - hide the chart
            windChart.setVisibility(android.view.View.GONE);
        }
        
        // ========== RAIN/PRECIPITATION CHART ==========
        // Check if rain/precipitation data is available (it's optional in the API response)
        if (hourlyData.get(0).rain != null) {
            // Rain data is available - set up and display the chart
            setupRainChart(hourlyData);
        } else {
            // No rain data - hide the chart
            rainChart.setVisibility(android.view.View.GONE);
        }
    }

    /**
     * Set up the temperature line chart with hourly temperature data.
     * 
     * This method:
     * 1. Converts hourly temperature data into chart Entry objects
     * 2. Extracts hour labels from time strings for the X-axis
     * 3. Configures the chart appearance (color, line width, etc.)
     * 4. Sets up the X-axis to display hour labels
     * 5. Displays the chart
     * 
     * The chart uses a red color scheme to represent temperature.
     * 
     * @param hourlyData List of HourlyWeatherData objects containing hourly temperature readings
     */
    private void setupTemperatureChart(List<HourlyWeatherData> hourlyData) {
        // Create lists to hold chart data points and X-axis labels
        // Entry objects represent (x, y) coordinates on the chart
        ArrayList<Entry> entries = new ArrayList<>();
        // Labels for the X-axis (hour labels like "00:00", "01:00", etc.)
        ArrayList<String> labels = new ArrayList<>();
        
        // Iterate through all hourly data points and create chart entries
        for (int i = 0; i < hourlyData.size(); i++) {
            // Get the hourly data for this hour
            HourlyWeatherData data = hourlyData.get(i);
            
            // Create a chart entry: x = index (hour number), y = temperature value
            // Entry constructor: Entry(float x, float y)
            // We cast temperature to float because Entry requires float values
            entries.add(new Entry(i, (float) data.temperature));
            
            // Extract the hour from the time string to create a readable label
            // Time format is ISO 8601: "2024-01-15T14:00" (year-month-dayThour:minute)
            String timeStr = data.time;
            if (timeStr != null && timeStr.length() > 13) {
                // Extract hour portion: substring(11, 13) gets characters at positions 11-12
                // For "2024-01-15T14:00", positions 11-12 are "14"
                String hour = timeStr.substring(11, 13);
                // Format as "14:00" for display
                labels.add(hour + ":00");
            } else {
                // Fallback: if time string is invalid, just use the index number
                labels.add(String.valueOf(i));
            }
        }
        
        // ========== CONFIGURE CHART DATA SET ==========
        // Create a LineDataSet which holds the data points and styling information
        // The second parameter is the label that appears in the legend
        LineDataSet dataSet = new LineDataSet(entries, "Temperature (°F)");
        
        // Set chart color to red (0xFFE74C3C is ARGB format: Alpha, Red, Green, Blue)
        // 0xFF = fully opaque, E7 = red component, 4C = green component, 3C = blue component
        dataSet.setColor(0xFFE74C3C);
        
        // Set the line width to 2 pixels for good visibility
        dataSet.setLineWidth(2f);
        
        // Set the color of the data point circles (same red as the line)
        dataSet.setCircleColor(0xFFE74C3C);
        
        // Set the radius of the circles to 4 pixels
        dataSet.setCircleRadius(4f);
        
        // Set the text size for values displayed on the chart
        dataSet.setValueTextSize(10f);
        
        // ========== CONFIGURE CHART ==========
        // Create LineData object which contains the data set
        // This is what gets passed to the chart
        LineData lineData = new LineData(dataSet);
        
        // Set the data on the chart
        tempChart.setData(lineData);
        
        // Set a description for the chart (appears below the chart)
        tempChart.getDescription().setText("Hourly Temperature");
        
        // Set up a custom formatter for X-axis labels
        // This allows us to display hour labels instead of just numbers
        tempChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Convert the float value to an integer index
                int index = (int) value;
                // Check if index is within bounds of our labels array
                if (index >= 0 && index < labels.size()) {
                    // Return the hour label (e.g., "14:00")
                    return labels.get(index);
                }
                // Return empty string if index is out of bounds
                return "";
            }
        });
        
        // Position the X-axis at the bottom of the chart
        tempChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        
        // Rotate X-axis labels -45 degrees to prevent overlap
        // This makes hour labels more readable when there are many data points
        tempChart.getXAxis().setLabelRotationAngle(-45f);
        
        // Force the chart to redraw with the new data
        // This is necessary after making changes to the chart
        tempChart.invalidate();
    }

    /**
     * Set up the humidity line chart with hourly humidity percentage data.
     * 
     * Similar to setupTemperatureChart() but for humidity data.
     * Uses a blue color scheme to represent humidity.
     * 
     * @param hourlyData List of HourlyWeatherData objects containing hourly humidity readings
     */
    private void setupHumidityChart(List<HourlyWeatherData> hourlyData) {
        // Create lists for chart data points and X-axis labels
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        
        // Iterate through hourly data and create chart entries
        for (int i = 0; i < hourlyData.size(); i++) {
            HourlyWeatherData data = hourlyData.get(i);
            
            // Only add entry if humidity data exists (it's nullable)
            if (data.humidity != null) {
                // Create chart entry: x = hour index, y = humidity percentage
                // floatValue() converts Double to float
                entries.add(new Entry(i, data.humidity.floatValue()));
                
                // Extract hour label from time string
                String timeStr = data.time;
                if (timeStr != null && timeStr.length() > 13) {
                    // Extract hour (positions 11-12) and format as "HH:00"
                    labels.add(timeStr.substring(11, 13) + ":00");
                } else {
                    // Fallback to index if time string is invalid
                    labels.add(String.valueOf(i));
                }
            }
        }
        
        // Configure chart with blue color scheme
        LineDataSet dataSet = new LineDataSet(entries, "Humidity (%)");
        dataSet.setColor(0xFF3498DB);  // Blue color
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(0xFF3498DB);
        dataSet.setCircleRadius(4f);
        
        // Set up and display the chart
        LineData lineData = new LineData(dataSet);
        humidityChart.setData(lineData);
        humidityChart.getDescription().setText("Hourly Humidity");
        humidityChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });
        humidityChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        humidityChart.invalidate();
    }

    /**
     * Set up the wind speed line chart with hourly wind speed data.
     * 
     * Similar to other chart setup methods but for wind speed.
     * Uses a green color scheme to represent wind speed.
     * 
     * @param hourlyData List of HourlyWeatherData objects containing hourly wind speed readings
     */
    private void setupWindChart(List<HourlyWeatherData> hourlyData) {
        // Create list for chart data points
        ArrayList<Entry> entries = new ArrayList<>();
        
        // Iterate through hourly data and create chart entries
        for (int i = 0; i < hourlyData.size(); i++) {
            HourlyWeatherData data = hourlyData.get(i);
            
            // Only add entry if wind speed data exists (it's nullable)
            if (data.windSpeed != null) {
                // Create chart entry: x = hour index, y = wind speed in mph
                entries.add(new Entry(i, data.windSpeed.floatValue()));
            }
        }
        
        // Configure chart with green color scheme
        LineDataSet dataSet = new LineDataSet(entries, "Wind Speed (mph)");
        dataSet.setColor(0xFF2ECC71);  // Green color
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(0xFF2ECC71);
        dataSet.setCircleRadius(4f);
        
        // Set up and display the chart
        LineData lineData = new LineData(dataSet);
        windChart.setData(lineData);
        windChart.getDescription().setText("Hourly Wind Speed");
        windChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        windChart.invalidate();
    }

    /**
     * Set up the rain/precipitation line chart with hourly rain data.
     * 
     * Similar to other chart setup methods but for precipitation.
     * Uses a purple color scheme to represent rain.
     * 
     * @param hourlyData List of HourlyWeatherData objects containing hourly rain/precipitation readings
     */
    private void setupRainChart(List<HourlyWeatherData> hourlyData) {
        // Create list for chart data points
        ArrayList<Entry> entries = new ArrayList<>();
        
        // Iterate through hourly data and create chart entries
        for (int i = 0; i < hourlyData.size(); i++) {
            HourlyWeatherData data = hourlyData.get(i);
            
            // Only add entry if rain data exists (it's nullable)
            if (data.rain != null) {
                // Create chart entry: x = hour index, y = rain amount in mm
                entries.add(new Entry(i, data.rain.floatValue()));
            }
        }
        
        // Configure chart with purple color scheme
        LineDataSet dataSet = new LineDataSet(entries, "Rain (mm)");
        dataSet.setColor(0xFF9B59B6);  // Purple color
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(0xFF9B59B6);
        dataSet.setCircleRadius(4f);
        
        // Set up and display the chart
        LineData lineData = new LineData(dataSet);
        rainChart.setData(lineData);
        rainChart.getDescription().setText("Hourly Precipitation");
        rainChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        rainChart.invalidate();
    }

    /**
     * Calculate daily averages and totals from hourly data, then display them in TextViews.
     * 
     * This method:
     * 1. Sums all hourly values for each weather variable
     * 2. Calculates averages by dividing sums by count
     * 3. For rain, displays total (sum) rather than average (more meaningful)
     * 4. Formats and displays results in the summary TextViews
     * 
     * Note: Some variables (humidity, wind, rain) are optional and may be null.
     * We track counts separately to calculate averages correctly when some hours
     * have missing data.
     * 
     * @param hourlyData List of HourlyWeatherData objects containing hourly measurements
     */
    private void calculateAndDisplayAverages(List<HourlyWeatherData> hourlyData) {
        // Initialize accumulators for summing values
        // These will hold the sum of all hourly values for each variable
        double tempSum = 0;        // Sum of all hourly temperatures
        double humiditySum = 0;    // Sum of all hourly humidity percentages
        double windSum = 0;        // Sum of all hourly wind speeds
        double rainSum = 0;        // Sum of all hourly rain amounts
        
        // Initialize counters for optional variables
        // We need separate counts because some hours may have null values
        int humidityCount = 0;  // Number of hours with valid humidity data
        int windCount = 0;      // Number of hours with valid wind speed data
        int rainCount = 0;      // Number of hours with valid rain data
        
        // Iterate through all hourly data points and accumulate sums
        for (HourlyWeatherData data : hourlyData) {
            // Temperature is always present, so always add it
            tempSum += data.temperature;
            
            // Humidity is optional - only add if not null
            if (data.humidity != null) {
                humiditySum += data.humidity;
                humidityCount++;  // Increment count for average calculation
            }
            
            // Wind speed is optional - only add if not null
            if (data.windSpeed != null) {
                windSum += data.windSpeed;
                windCount++;  // Increment count for average calculation
            }
            
            // Rain is optional - only add if not null
            if (data.rain != null) {
                rainSum += data.rain;
                rainCount++;  // Increment count (though we'll use sum, not average)
            }
        }
        
        // ========== CALCULATE AND DISPLAY TEMPERATURE AVERAGE ==========
        // Temperature is always available, so divide by total number of hours
        double avgTemp = tempSum / hourlyData.size();
        // Format with 1 decimal place and display
        avgTempText.setText(String.format(Locale.US, "Average: %.1f°F", avgTemp));
        
        // ========== CALCULATE AND DISPLAY HUMIDITY AVERAGE ==========
        // Check if we have any humidity data
        if (humidityCount > 0) {
            // Calculate average by dividing sum by count of valid values
            double avgHumidity = humiditySum / humidityCount;
            // Format as percentage with 1 decimal place
            avgHumidityText.setText(String.format(Locale.US, "Average: %.1f%%", avgHumidity));
        } else {
            // No humidity data available - show "N/A"
            avgHumidityText.setText("Average: N/A");
        }
        
        // ========== CALCULATE AND DISPLAY WIND SPEED AVERAGE ==========
        // Check if we have any wind speed data
        if (windCount > 0) {
            // Calculate average by dividing sum by count of valid values
            double avgWind = windSum / windCount;
            // Format with 1 decimal place and unit
            avgWindText.setText(String.format(Locale.US, "Average: %.1f mph", avgWind));
        } else {
            // No wind speed data available - show "N/A"
            avgWindText.setText("Average: N/A");
        }
        
        // ========== DISPLAY RAIN TOTAL ==========
        // For rain, we display the total (sum) rather than average
        // This is more meaningful - users want to know total precipitation for the day
        if (rainCount > 0) {
            // Display total rain with 2 decimal places (more precision for small amounts)
            totalRainText.setText(String.format(Locale.US, "Total: %.2f mm", rainSum));
        } else {
            // No rain data - show 0 mm
            totalRainText.setText("Total: 0 mm");
        }
    }
}


