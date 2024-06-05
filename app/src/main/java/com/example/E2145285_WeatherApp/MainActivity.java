package com.example.E2145285_WeatherApp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "726957c65547eae0c025f5f0b6f0b6e7";

    LocationManager locationManager;
    private TextView locationTextView;
    private TextView addressTextView;
    private TextView timeTextView;
    private TextView tempTextView;
    private TextView humidityTextView;
    private TextView descriptionTextView;
    private TextView latest_update;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        addressTextView = findViewById(R.id.addressTextView);
        timeTextView = findViewById(R.id.timeTextView);
        tempTextView = findViewById(R.id.tempTextView);
        humidityTextView = findViewById(R.id.humidityTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        latest_update = findViewById(R.id.latest_update);

        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(v -> refreshLocationAndWeather());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            } catch (SecurityException e) {
                Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
                Log.e("Location Error", Objects.requireNonNull(e.getMessage()));
            }
        } else {
            Toast.makeText(this, "Please enable GPS.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate();
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        locationTextView.setText("Location: " + latitude + ", " + longitude);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                // Get the address components
                String streetNumber = address.getSubThoroughfare() != null ? address.getSubThoroughfare() : "";
                String streetName = address.getThoroughfare() != null ? address.getThoroughfare() : "";
                String locality = address.getLocality() != null ? address.getLocality() : "";
                String country = address.getCountryName() != null ? address.getCountryName() : "";

                String formattedAddress = String.format("%s %s, %s, %s", streetNumber, streetName, locality, country);
                addressTextView.setText(formattedAddress);
            } else {
                addressTextView.setText("Address not found.");
            }
        } catch (IOException e) {
            Toast.makeText(this, "Geocoder service not available.", Toast.LENGTH_SHORT).show();
            Log.e("Geocoder Error", Objects.requireNonNull(e.getMessage()));
        }

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        latest_update.setText("Latest Update: " + String.format("%02d:%02d:%02d", hour, minute, second));

        fetchWeatherData(latitude, longitude);
        updateTime();
    }

    private void updateTime() {
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                timeTextView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
                handler.postDelayed(this, 1000);
            }
        });
    }

    private void fetchWeatherData(double latitude, double longitude) {
        Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);

        Gson gson = new GsonBuilder().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        WeatherService weatherService = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = weatherService.getCurrentWeather(latitude, longitude, API_KEY, "metric");

        call.enqueue(new Callback<WeatherResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weatherResponse = response.body();
                    if (weatherResponse != null) {
                        Log.d("Weather API", "Response: " + new Gson().toJson(weatherResponse));
                        String description = weatherResponse.weather.get(0).description;
                        float temp = weatherResponse.main.temp;
                        int humidity = weatherResponse.main.humidity;

                        tempTextView.setText("Temp: \n" + temp + "°C");
                        humidityTextView.setText("Humidity: \n" + humidity + "%");
                        descriptionTextView.setText("Description: \n" + description);
                    } else {
                        tempTextView.setText("No weather data available.");
                    }
                } else {
                    try {
                        assert response.errorBody() != null;
                        String errorResponse = response.errorBody().string();
                        Log.e("Weather API", "Error: " + errorResponse);
                        tempTextView.setText("Failed to get weather data! (cod: " + response.code() + ", message: " + errorResponse + ")");
                    } catch (IOException e) {
                        tempTextView.setText("Failed to get weather data! (IO Exception)");
                        Log.e("Weather API Error", Objects.requireNonNull(e.getMessage()));
                    }
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                Log.e("Weather API", "Network Error: " + t.getMessage());
                tempTextView.setText("Network Error!");
            }
        });
    }

    private void refreshLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
            } catch (SecurityException e) {
                Toast.makeText(this, "Location permission not granted.", Toast.LENGTH_SHORT).show();
                Log.e("Location Error", Objects.requireNonNull(e.getMessage()));
            }
        } else {
            Toast.makeText(this, "Please enable GPS.", Toast.LENGTH_SHORT).show();
        }

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
            double latitude = lastKnownLocation.getLatitude();
            double longitude = lastKnownLocation.getLongitude();
            fetchWeatherData(latitude, longitude);
        } else {
            Toast.makeText(this, "Failed to get location.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(@NonNull String provider) {}

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, "Please enable GPS.", Toast.LENGTH_SHORT).show();
    }
}
