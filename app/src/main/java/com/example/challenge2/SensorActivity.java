package com.example.challenge2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import com.google.android.gms.maps.model.MarkerOptions;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.RegionViewModel;

import java.util.ArrayList;
import java.util.Collection;



public class SensorActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback, LocationListener, View.OnClickListener, RangeNotifier, MonitorNotifier {

    // sending log output TAG
    private static final String TAG = "MyActivity";

    // Interact with the different beacons
    private BeaconManager beaconManager;

    // Search for beacons
    private static Region region = new Region("rid", null, null, null);

    private static final String BECONS_CLOSE_BY = "BeaconsCloseBy";

    // Program is running or is on pause
    private boolean running = false;
    // ----------------------------------------


    private LocationManager locationManager;
    private ScreenReceiver screenReceiver;

    // Front-End components
    TextView xValue, yValue, introText1, introText2, introText3;
    ImageButton startButton;
    Button again;
    LinearLayout linearLayout;

    Boolean requested = true;

    private MapView mapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    double lat = 0; // 52.213453
    double longi = 0; // 6.879420

    GoogleMap googleMap;

    @Override
    public final void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize front-end (buttons, text, map...)
        introText1 = findViewById(R.id.introText1);
        introText2 = findViewById(R.id.introText2);
        introText3 = findViewById(R.id.introText3);

        startButton = findViewById(R.id.startButton);
        again = findViewById(R.id.again);
        linearLayout = findViewById(R.id.layout);

        startButton.setOnClickListener(this);

        // Initialize Beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // Set a beacon protocol; Eddystone. We can choose a diferent one later if we want
        // Another example would be iBeacon or Kontakt.io. Search on google as it is not included in this library
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        // In case we would like iBeacon

        /*beaconManager.getBeaconParsers().add(
                new BeaconParser().
                        setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));*/

        // Beacon identifiers
        ArrayList<Identifier> identifiers = new ArrayList<>();

        region = new Region("regionId", null, null, null);

        // ----------------------------------------

        // initialize ScreenReceiver for tracking screen state changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver,filter);

        if (ActivityCompat.checkSelfPermission((Activity) this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
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
        mapView = findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

    }

    // Start button
    @Override
    public void onClick(View view){
        // Press button to start application
        if (view.equals(startButton) && !running){
            if (ActivityCompat.checkSelfPermission((Activity) this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, 10);
                StartDetectingBeacons();
            }
        // Press button to pause application
        } else if (view.equals(startButton) && running){
            StopDetectingBeacons();
        }
    }

    private void StartDetectingBeacons() {

        // App is now running (detecting beacons)
        running = true;

        // Time to scan for beacons (6 seconds)
        beaconManager.setForegroundBetweenScanPeriod(6000l);

        // Start to search for beacons
        try {
            // Monitoring (look for beacons that match the passed Region object)
            // Ranging ( monitor and provide updates on the est. mDistance every seconds while beacons are visible)
            beaconManager.startMonitoring(region);
            beaconManager.startRangingBeacons(region);
            beaconManager.addMonitorNotifier(this);

            // Set up a Live Data observer
            RegionViewModel regionViewModel = BeaconManager.getInstanceForApplication(this).getRegionViewModel(region);


        Log.d(TAG, "Searching for beacons");

        } catch (Exception e){
            Log.d(TAG, "An error occured while trying to search for beacons" + e.getMessage());
        }

        // Display beacons detected after the time given to scan (6 seconds in our case)
            // setMonitoringListener instead of setRangingListener


        //if (beacons.size() != 0){

       // }

    }

    private void StopDetectingBeacons() {
        running = false;
        beaconManager.stopRangingBeacons(region);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (requested) {
            lat = location.getLatitude();
            longi = location.getLongitude();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
            // lat_total.add(lat);
            // longi_total.add(longi);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
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


    @Override
    public void didEnterRegion(Region region) {

    }

    @Override
    public void didExitRegion(Region region) {

    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {

    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

    }
};