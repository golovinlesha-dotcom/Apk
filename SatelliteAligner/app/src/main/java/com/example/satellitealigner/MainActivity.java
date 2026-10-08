package com.example.satellitealigner;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener, LocationListener {

    private View screenSelection, screenSun, screenGps, screenAiming;
    private SensorManager sensorManager;
    private LocationManager locationManager;

    // Gyro & Accel data
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float gyroOffsetAzimuth = 0f;
    private float currentAzimuth = 0f;
    private float currentElevation = 0f;

    // GPS Tracking
    private List<Location> gpsTrack = new ArrayList<>();
    private boolean isTrackingGps = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        screenSelection = findViewById(R.id.screen_selection);
        screenSun = findViewById(R.id.screen_sun);
        screenGps = findViewById(R.id.screen_gps);
        screenAiming = findViewById(R.id.screen_aiming);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        findViewById(R.id.btn_sun_calib).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchScreen(screenSun);
                // Инициализация камеры и логики совмещения кругов
                // При совпадении вызывается фиксация севера и переход на screenAiming
            }
        });

        findViewById(R.id.btn_gps_calib).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchScreen(screenGps);
            }
        });

        findViewById(R.id.btn_start_gps_track).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isTrackingGps = true;
                gpsTrack.clear();
                // Симуляция завершения трека через секунды или по нажатию
                calculateGpsNorth();
                switchScreen(screenAiming);
            }
        });
    }

    private void switchScreen(View targetScreen) {
        screenSelection.setVisibility(View.GONE);
        screenSun.setVisibility(View.GONE);
        screenGps.setVisibility(View.GONE);
        screenAiming.setVisibility(View.GONE);
        targetScreen.setVisibility(View.VISIBLE);
    }

    private void calculateGpsNorth() {
        // Логика вычисления истинного севера по круговой траектории (селфи палка)
        // Фиксация отклонений гироскопа и акселерометра
        gyroOffsetAzimuth = 0f; // Применяем вычисленный сдвиг
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) gravity = event.values.clone();
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) geomagnetic = event.values.clone();

        float[] R = new float[9];
        float[] I = new float[9];
        if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
            float[] orientation = new float[3];
            SensorManager.getOrientation(R, orientation);
            currentAzimuth = (float) Math.toDegrees(orientation[0]) + gyroOffsetAzimuth;
            currentElevation = (float) Math.toDegrees(orientation[1]);
            
            TextView tvAngles = findViewById(R.id.tv_current_angles);
            if (tvAngles != null && screenAiming.getVisibility() == View.VISIBLE) {
                tvAngles.setText(String.format("Азимут: %.1f° | Угол места: %.1f°", currentAzimuth, currentElevation));
            }
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    @Override public void onLocationChanged(Location location) {
        if (isTrackingGps) {
            gpsTrack.add(location);
        }
        TextView tvCoords = findViewById(R.id.tv_current_coords);
        if (tvCoords != null) {
            tvCoords.setText(String.format("Широта: %.5f | Долгота: %.5f", location.getLatitude(), location.getLongitude()));
        }
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}
