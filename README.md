# Weather Forecast Android App

A comprehensive Android weather forecast application built with Java that displays 7-day weather forecasts, detailed hourly charts, and temperature predictions using machine learning.

## Features

### 🌤️ Weather Forecast
- **7-Day Forecast**: View daily weather summaries for the next week
- **Daily Averages**: Automatically calculated averages for:
  - Temperature (Fahrenheit)
  - Humidity (percentage)
  - Wind Speed (mph)
  - Precipitation (mm)
- **Large Today Display**: Prominent temperature display for current day

### 📊 Detailed Hourly View
- **Interactive Charts**: Four beautiful line charts showing hourly trends:
  - Temperature (red)
  - Humidity (blue)
  - Wind Speed (green)
  - Precipitation (purple)
- **Summary Statistics**: Daily averages and totals displayed at a glance
- **Tap to View**: Click any day card to see detailed hourly breakdown

### 🤖 Machine Learning Temperature Prediction
- **Smart Predictions**: Predict tomorrow's temperature using linear regression
- **Historical Data Training**: Model trained on 120+ days of historical weather data
- **Model Caching**: Trained models are cached per city for fast predictions
- **Automatic Retraining**: Models automatically retrain when they become stale (>7 days old)

### 🏙️ Multi-City Support
- **Predefined Cities**: Austin, New York, Los Angeles, Chicago, Houston
- **Add Custom Cities**: Add any city by providing name, state, and coordinates
- **City Switching**: Easily switch between cities with a tap
- **Per-City Models**: Each city has its own cached prediction model

## Screenshots

*Add screenshots of your app here*

## Technical Details

### Architecture
- **Language**: Java
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Architecture Pattern**: MVC (Model-View-Controller)
- **Async Operations**: AsyncTask (deprecated but acceptable for min SDK 24)

### API Integration
- **Weather API**: [Open Meteo](https://open-meteo.com/)
  - Forecast API: `https://api.open-meteo.com/v1/forecast`
  - Archive API: `https://archive-api.open-meteo.com/v1/archive`
- **No API Key Required**: Open Meteo is free and open-source
- **Data Format**: JSON responses parsed using Android's built-in JSONObject

### Key Components

#### Activities
- **MainActivity**: Main screen displaying 7-day forecast and prediction controls
- **DetailedWeatherActivity**: Detailed hourly view with interactive charts

#### Data Models
- **City**: Represents a city with name, state, and coordinates
- **DailyForecast**: Daily weather summary with averages
- **HourlyWeatherData**: Individual hourly weather measurements
- **TemperatureModel**: ML model storing linear regression parameters

#### Core Functionality
1. **Network Requests**: HTTP GET requests to Open Meteo API
2. **JSON Parsing**: Extracts hourly data from API responses
3. **Average Calculation**: Computes daily averages from hourly data
4. **UI Binding**: Updates TextViews and charts with calculated data
5. **ML Training**: Linear regression on historical temperature data
6. **Model Persistence**: Saves trained models to SharedPreferences

### Dependencies

```kotlin
// Core Android libraries
implementation(libs.appcompat)
implementation(libs.material)
implementation(libs.activity)
implementation(libs.constraintlayout)
implementation(libs.cardview)

// Charting library
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
```

## Installation

### Prerequisites
- Android Studio (latest version recommended)
- Android SDK with API level 24 or higher
- Java 11 or higher

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/Manas2006/weather-android-java-app.git
   cd weather-android-java-app
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository folder
   - Click "OK"

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle dependencies
   - Wait for the sync to complete

4. **Run the App**
   - Connect an Android device or start an emulator
   - Click the "Run" button (green play icon) or press `Shift + F10`
   - The app will build and install on your device

## Usage

### Viewing Forecast
1. Open the app - it automatically fetches weather for the default city (Austin, TX)
2. View the 7-day forecast cards showing daily averages
3. Tap the refresh button (top right) to update the forecast

### Viewing Detailed Hourly Data
1. Tap any day card in the forecast list
2. View four interactive line charts showing hourly trends
3. Scroll to see all charts and summary statistics
4. Tap the back button to return to the main screen

### Predicting Temperature
1. Scroll to the "Temperature Prediction" section at the bottom
2. Tap "Predict Tomorrow" button
3. Wait for the model to train (first time) or use cached model
4. View the predicted temperature for tomorrow

### Managing Cities
1. Tap the city name at the top of the screen
2. Select a city from the list to switch
3. Or tap "+ Add New City" to add a custom city
4. Enter city name, state, latitude, and longitude
5. Tap "Add" to save and switch to the new city

## Code Structure

```
app/src/main/
├── java/com/example/assignment5/
│   ├── MainActivity.java              # Main screen with forecast & prediction
│   ├── DetailedWeatherActivity.java   # Detailed hourly view with charts
│   ├── City.java                      # City data model
│   ├── DailyForecast.java             # Daily forecast data model
│   ├── HourlyWeatherData.java         # Hourly data point model
│   └── TemperatureModel.java          # ML model for predictions
└── res/
    ├── layout/
    │   ├── activity_main.xml          # Main screen layout
    │   └── activity_detailed_weather.xml # Detailed view layout
    └── ... (other resources)
```

## How It Works

### Weather Data Flow
1. User opens app → `MainActivity.onCreate()`
2. `startForecastFetch()` → `FetchForecastTask` (AsyncTask)
3. Builds Open Meteo API URL with city coordinates
4. Fetches hourly data for 7 days via HTTP GET
5. `parseForecastJson()` groups hourly data by date
6. Calculates daily averages (temperature, humidity, wind, rain)
7. `bindForecastData()` updates UI with forecast cards

### Temperature Prediction Flow
1. User taps "Predict Tomorrow"
2. Checks for cached model (valid if < 7 days old)
3. If no valid cache:
   - Fetches 120 days of historical data from Archive API
   - Trains linear regression model: `y = mx + b`
   - Saves model to SharedPreferences
4. Uses model to predict tomorrow's temperature (based on day of year)
5. Displays prediction result

### Linear Regression Model
- **Input**: Day of year (1-365)
- **Output**: Predicted temperature in Fahrenheit
- **Training**: Uses least squares method on historical data
- **Formula**: `temperature = slope × dayOfYear + intercept`
- **Minimum Data**: Requires 100+ data points for reliable training

## API Details

### Forecast API Request
```
GET https://api.open-meteo.com/v1/forecast
  ?latitude={lat}
  &longitude={lon}
  &hourly=temperature_2m,relative_humidity_2m,wind_speed_10m,rain,surface_pressure,visibility
  &temperature_unit=fahrenheit
  &windspeed_unit=mph
  &forecast_days=7
  &timezone=auto
```

### Archive API Request
```
GET https://archive-api.open-meteo.com/v1/archive
  ?latitude={lat}
  &longitude={lon}
  &hourly=temperature_2m
  &start_date={start_date}
  &end_date={end_date}
  &timezone=UTC
```

## Permissions

The app requires the following permission:
- `INTERNET`: To fetch weather data from Open Meteo API

## Known Limitations

- Uses deprecated `AsyncTask` (acceptable for min SDK 24, but should be migrated to Coroutines/RxJava for production)
- Simple linear regression model doesn't account for current weather conditions or short-term patterns
- Predictions are experimental and should be used as rough estimates only

## Future Enhancements

- [ ] Migrate from AsyncTask to Kotlin Coroutines
- [ ] Add weather icons based on conditions
- [ ] Implement location services for automatic city detection
- [ ] Add more sophisticated ML models (neural networks, time series)
- [ ] Support for multiple units (Celsius, Kelvin)
- [ ] Weather alerts and notifications
- [ ] Offline caching of forecast data
- [ ] Widget support for home screen

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is open source and available for educational purposes.

## Author

**Manas Pathak**
- GitHub: [@Manas2006](https://github.com/Manas2006)

## Acknowledgments

- [Open Meteo](https://open-meteo.com/) for providing free weather API
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) for beautiful charting library
- Android community for excellent documentation and resources

---

**Note**: This app is for educational purposes and demonstrates Android development concepts including networking, JSON parsing, UI design, and basic machine learning integration.

