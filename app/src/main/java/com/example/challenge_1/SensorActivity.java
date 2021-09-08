package com.example.challenge_1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class SensorActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback, LocationListener {

    // sending log output
    private static final String TAG = "MyActivity";

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private LocationManager locationManager;
    // Handle location changes
    private LocationListener locationListener;

    TextView xValue, yValue, zValue, position, movement_Check;

    // If app is running or if its on pause
    Boolean running = true;

    private MapView mapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    double lat = 0; // 52.213453
    double longi = 0; // 6.879420
    LatLng latLng = new LatLng(lat, longi);
    LatLng[] coordinates_total;

    GoogleMap googleMap;

    @Override
    public final void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);
        zValue = (TextView) findViewById(R.id.zValue);
        position = (TextView) findViewById(R.id.position);
        movement_Check = (TextView) findViewById(R.id.movement_Check);

        if (ActivityCompat.checkSelfPermission((Activity) this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }

        // OnCreate
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        if (ActivityCompat.checkSelfPermission((Activity) this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        lat = location.getLatitude();
        longi = location.getLongitude();

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);


        // Pause accelerometer if button pressed
        final Button button = findViewById(R.id.pauseButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (running == true) {
                    running = false;
                    button.setText("Continue");
                } else if (running == false) {
                    running = true;
                    button.setText("Pause");
                }
            }
        });

        // Send a DEBUG log message.
        Log.d(TAG, "OnCreate: Initializing the sensor services");
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(SensorActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "OnCreate: Registered accelerometer listener");
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        longi = location.getLongitude();
        googleMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
        Log.d(TAG, "onLocationChanged: has entered" + lat + " and " + longi);
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {

        float x = Math.round(event.values[0]);
        float y = Math.round(event.values[1]);
        float z = Math.round(event.values[2]);

        double pitch = Math.round(Math.atan2(-x,Math.sqrt(y*y+z*z)) * 57.3);
        double roll = Math.round(Math.atan2(y,z) * 57.3);

        if (running == true) {
            Log.d(TAG, "OnSensorChanged: X" + x + " Y:" + y + " Z:" + z);

            xValue.setText("xValue:" + x);
            yValue.setText("yValue:" + y);
            zValue.setText("zValue:" + z);

            /*
                Check the position based on the values, and changed the text to say either:
                Pitch: Y-axis: Tilted Up (positive), Tilted Down (negative)
                Roll: X-axis: Right (positive), Left (negative)
                Perfectly level: Pitch and Roll equal to zero
            */

            if (roll >= 0) {
                if (pitch >= 0){
                    if (pitch == 0 && roll == 0) {
                        position.setText("Perfectly level, pitch:" + pitch + " roll:" + roll);
                    } else {
                        position.setText("Tilted Up and to the right, pitch:" + pitch + " roll:" + roll);
                    }
                }  else {
                    position.setText("Tilted Down and to the right, pitch:" + pitch + " roll:" + roll);
                }
            } else {
                if (pitch >=0){
                    position.setText("Tilted Up and to the Left, pitch:" + pitch + " roll:" + roll);
                } else{
                    position.setText("Tilted Down and to the Left, pitch:" + pitch + " roll:" + roll);
                }
            }

            /*
                Check if the phone is in the same location as the one it had 3 seconds ago.
                Use a Handler
             */




        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sensor_Delay_Normal ~5hz
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // STORE THE DATA IN A PARTICULAR FILE IN OUR APP
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        // Editor String == key, Integer Value = Coordinate of the pothhole
        editor.putString("Coordinates", (new LatLng(1,2).toString()));
        // All the data will not get copied back to the original file until apply is called
        editor.apply();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // STORE THE DATA IN A PARTICULAR FILE IN OUR APP
        SharedPreferences sharedPreferences = getSharedPreferences("PothHoles", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        // Editor String == key, Integer Value = Coordinate of the pothhole
        editor.putString("Coordinates", (new LatLng(1,2).toString()));
        // All the data will not get copied back to the original file until apply is called
        editor.apply();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onMapReady(GoogleMap map) {
        // ONCE YOU HAVE THE COORDINATES, YOU CAN ADD MANY MARKERS
        googleMap = map;
        float zoomLevel = 5f;
        googleMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoomLevel));

        Log.d(TAG, "onMapReady: has entered");
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

};