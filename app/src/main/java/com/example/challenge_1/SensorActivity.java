package com.example.challenge_1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


public class SensorActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback, LocationListener {

    // sending log output
    private static final String TAG = "MyActivity";

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private LocationManager locationManager;
    // Handle location changes
    private LocationListener locationListener;
    private ScreenReceiver screenReceiver;

    // Front-End components
    TextView xValue, yValue, zValue, introText1, introText2, introText3;
    ImageButton startButton;
    Button again;
    LinearLayout linearLayout;

    // If app is running or if its on pause
    Boolean running = false;
    Boolean reset = false;
    Boolean requested = true;

    private MapView mapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    double lat = 0; // 52.213453
    double longi = 0; // 6.879420

    Marker myMarker;

    GoogleMap googleMap;

    ArrayList<Float> accel_x = new ArrayList<Float>();
    ArrayList<Float> accel_y = new ArrayList<Float>();
    ArrayList<Float> accel_z = new ArrayList<Float>();
    ArrayList<Double> accel_mag = new ArrayList<Double>();

    boolean firstDetection = false;
    boolean speedbump = false;
    boolean anomaly = false;

    final int dt = 200000;
    final float sensitivity = 6;
    long time = 0;
    boolean startTime = true;

    ArrayList<Double> lat_total = new ArrayList<Double>();
    ArrayList<Double> longi_total = new ArrayList<Double>();

    //FirebaseStorage storage = FirebaseStorage.getInstance();
    //StorageReference storageRef = storage.getReference();
    // Create a child reference
    // imagesRef now points to "images"
    //StorageReference coordinates = storageRef.getParent().child("SensorActivity");


    private float average(ArrayList<Double> input) {
        float temp = 0;
        for (int i=0; i<input.size();i++) {
            temp += input.get(i);
        }
        return temp/input.size();
    }


    @Override
    public final void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        introText1 = (TextView) findViewById(R.id.introText1);
        introText2 = (TextView) findViewById(R.id.introText2);
        introText3 = (TextView) findViewById(R.id.introText3);
        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);

        startButton = (ImageButton) findViewById(R.id.startButton);
        again = (Button) findViewById(R.id.again);
        linearLayout = findViewById(R.id.layout);

        // initialize ScreenReceiver for tracking screen state changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver,filter);

        if (ActivityCompat.checkSelfPermission((Activity) this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }

        // OnCreate
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        //Image Button
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!running && !reset) {
                    running = true;
                    linearLayout.setBackgroundResource(R.drawable.app_challenge1);
                    introText1.setText("Press the button to start ");
                    introText2.setText("detecting the anomalies ");
                    introText3.setText("on your journey");
                    xValue.setVisibility(View.VISIBLE);
                    yValue.setVisibility(View.VISIBLE);

                    startButton.setImageResource(R.drawable.stop);
                    mapView.setVisibility(View.GONE);
                } else if (running && !reset) {
                    linearLayout.setBackgroundResource(R.drawable.app_challenge1);
                    running = false;
                    reset = true;
                    introText1.setText("All done! ");
                    introText2.setText("These were the anomalies you ");
                    introText3.setText("encountered during your journey:");
                    xValue.setVisibility(View.GONE);
                    yValue.setVisibility(View.GONE);
                    linearLayout.setBackgroundColor(Color.rgb(5,129,146));
                    // Make map visible here
                    mapView.setVisibility(View.VISIBLE);
                    again.setVisibility(View.VISIBLE);
                    startButton.setVisibility(View.GONE);
                } else if (!running && reset) {
                    linearLayout.setBackgroundResource(R.drawable.app_challenge1);
                    reset = false;
                    introText1.setText("Press the button to start ");
                    introText2.setText("detecting the anomalies ");
                    introText3.setText("on your journey");
                    startButton.setVisibility(View.VISIBLE);
                    startButton.setImageResource(R.drawable.play);
                    mapView.setVisibility(View.GONE);
                    again.setVisibility(View.GONE);
                }
            }
        });

        // Do it again button to restart the app
        again.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!running && reset) {
                    linearLayout.setBackgroundResource(R.drawable.app_challenge1);
                    reset = false;
                    introText1.setText("Press the button to start ");
                    introText2.setText("detecting the anomalies ");
                    introText3.setText("on your journey");
                    startButton.setVisibility(View.VISIBLE);
                    startButton.setImageResource(R.drawable.play);
                    mapView.setVisibility(View.GONE);
                    again.setVisibility(View.GONE);
                }
            }
        });


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(SensorActivity.this, accelerometer, dt);
        Log.d(TAG, "OnCreate: Registered accelerometer listener");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (requested) {
            lat = location.getLatitude();
            longi = location.getLongitude();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
            lat_total.add(lat);
            longi_total.add(longi);
            requested = false;
        }
        if (mapView.getVisibility() == View.VISIBLE) {
            float zoomLevel = 15f;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, longi), zoomLevel));
            // GET THE ONES FROM THE CLOUD AND CHANGE THEIR COLOR TO BLUE?


            // SEND ALL THE LAT AND LONGI TO THE CLOUD

        }
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float x;
        float y;
        float z;
        double mag;

        x = event.values[0]; //getting the accelerometer values
        y = event.values[1];
        z = event.values[2];
        accel_mag.add(Math.sqrt(x*x + y*y + z*z) - 9.81); // calculating the magnitude of the acceleration

        if(accel_mag.size() > 5) { //limiting the window for the averaging
            accel_mag.remove(0);
        }

        if (running == true) {
            mag = average(accel_mag); //calculating average of last couple of samples

            Log.d(TAG, "OnSensorChanged: mag:" + mag);

            if (firstDetection) {
                if (startTime) {
                    time = System.nanoTime();
                    startTime = false;
                }
                if (System.nanoTime() > time + 600000000) {
                    firstDetection = false;
                    startTime = true;
                    speedbump = true;
                }
                else {
                    if (System.nanoTime() > time + 140000000) {
                        if (mag>sensitivity) {
                            firstDetection = false;
                            startTime = true;
                            anomaly = true;
                            requested = true;
                        }
                    }
                }
            }
            else {
                if (speedbump || anomaly) {
                    if (startTime) {
                        time = System.nanoTime();
                        startTime = false;
                    }
                    if (time + 2000000000 < System.nanoTime()) {
                        anomaly = false;
                        speedbump = false;
                        startTime = true;
                    }
                    else {
                        if (speedbump) yValue.setText("speedbump");
                        if (anomaly) yValue.setText("anomaly");
                    }
                }
                else {
                    firstDetection = mag>sensitivity;
                    yValue.setText("");
                }
            }


            /*if (detected) {
                if (startTime) {
                    time = System.nanoTime();
                    startTime = false;
                }
                if (time + 2000000000 > System.nanoTime()) {
                    yValue.setText("yValue:" + y + " Anomaly detected!");
                }
                else {
                    yValue.setText("yValue:" + y);
                    detected = false;
                    startTime = true;
                }

            }
            else {
                detected = mag > 2;
                yValue.setText("yValue:" + y);
            }*/

            xValue.setText("magnitude - gravity: " + mag);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, dt);
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
        googleMap = map;
        googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
        System.out.println("OnMapReady" + googleMap);
        mapView.onResume();
        mapView.onEnterAmbient(null);
        // This view is invisible, but it still takes up space for layout purposes. Otherwise use GONE
        mapView.setVisibility(View.GONE);
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