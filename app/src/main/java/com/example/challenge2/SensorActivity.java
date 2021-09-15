package com.example.challenge2;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import android.content.BroadcastReceiver;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.view.View.GONE;

// View.OnClickListener, BeaconConsumer, RangeNotifier
public class SensorActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, View.OnClickListener, RangeNotifier, BeaconConsumer {

    // sending log output TAG
    private static final String TAG = "MyActivity";

    // Interact with the different beacons
    private BeaconManager beaconManager;

    // Search for beacons
    private static Region region;

    // Scan time for beacons (6 seconds)
    private static final long SCAN_TIME = 6000l;
    private static final String BECONS_CLOSE_BY = "BeaconsCloseBy";

    // Program is running or is on pause
    private boolean running = false;

    // Create list of ActivityTransition objects (walking, still)
    private List<ActivityTransition> activityTransitionList;

    private ActivityRecognitionClient activityRecognitionClient;

    private PendingIntent pendingIntent;

    // ----------------------------------------


    private LocationManager locationManager;
    private ScreenReceiver screenReceiver;

    // Front-End components
    TextView introText1, introText2, activity;
    ImageButton startButton;
    Button again;
    LinearLayout linearLayout;

    Boolean requested = true;

    private MapView mapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    double lat = 0; // 52.213453
    double longi = 0; // 6.879420

    double N = 2;
    double C = 0;

    GoogleMap googleMap;

    @Override
    public final void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize front-end (buttons, text, map...)
        introText1 = findViewById(R.id.introText1);
        introText2 = findViewById(R.id.introText2);
        startButton = findViewById(R.id.startButton);
        again = findViewById(R.id.again);
        linearLayout = findViewById(R.id.layout);
        activity = findViewById(R.id.activity);

        startButton.setOnClickListener(this);

        // Initialize Beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this);

        // Set a beacon protocol
        // Another example would be Kontakt.io. https://beaconlayout.wordpress.com/
        // Eddystone UID and Eddystone TLM
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT));
        // iBeacon
        beaconManager.getBeaconParsers().add(
                new BeaconParser().
                        setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // Beacon identifiers. Beacons with all possible identifiers (empty arrayList)
        ArrayList<Identifier> identifiers = new ArrayList<>();

        region = new Region(BECONS_CLOSE_BY, identifiers);

            // List of activity transitions to track (still or walking)
        activityTransitionList = new ArrayList<>();

        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        Intent intent = new Intent(this,PermissionRationalActivity.class);
        pendingIntent = PendingIntent.getBroadcast(SensorActivity.this, 0, intent, 0);

        // listen for activity changes.
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);

        // transitions Updates
        Task<Void> task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, pendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "onSuccess");
                    }
                });

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Transitions Api could NOT be registered: " + e);

                    }
                });

        if (ActivityTransitionResult.hasResult(intent)){
            Log.d(TAG, "has entered here");
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                activity.setText(event.getActivityType() + " " + event.getTransitionType());
            }
        } else {
            Log.d(TAG, "has not entered here");
        }
        // ----------------------------------------

        // initialize ScreenReceiver for tracking screen state changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver,filter);

        // Asking for location permission
        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACTIVITY_RECOGNITION
            },45);
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
        Log.d(TAG, "has entered onClick");
        if (view.equals(startButton) && !running){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, 10);
            }
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter.isEnabled()){
                StartDetectingBeacons();
            } else {
                // Ask the user to enable bluetooth
                Intent ask_bluetooth_enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(ask_bluetooth_enable, 1);
            }

        // Press button to pause application
        } else if (view.equals(startButton) && running){
            StopDetectingBeacons();
        }
    }


    private void StartDetectingBeacons() {
        Log.d(TAG, "has entered StartDetectingBeacons");

        // App is now running (detecting beacons)
        running = true;

        // Change the icon to the pause symbol
        startButton.setImageResource(R.drawable.stop);

        // Change text
        introText1.setText("Searching for beacons ...");
        introText2.setText(" ");

        // Time to scan for beacons (6 seconds)
        beaconManager.setForegroundBetweenScanPeriod(SCAN_TIME);

        // Pair with beacon service
        beaconManager.bind(this);

    }

    @Override
    public void onBeaconServiceConnect() {

        try {
            // Start to search for beacons
            beaconManager.startRangingBeaconsInRegion(region);
            Log.d(TAG, "Searching for beacons");
        } catch (Exception e){
            Log.d(TAG, "An error occured while trying to search for beacons" + e.getMessage());
        }

        // Display beacons detected after the time given to scan (6 seconds in our case)
        beaconManager.addRangeNotifier(this);

    }

    /**
     * Called after SCAN_TIME seconds with the detected beacons in that time period
     */
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        double tx = 0;
        double rx = 0;
        double d = 0;

        if (beacons.size() != 0){
            for (Beacon beacon : beacons) {
                tx = beacon.getTxPower();
                rx = beacon.getRssi();
                d = Math.pow(10,((tx - rx + C)/(10*N)));
                Log.d(TAG, "Beacon detected: " + beacon + " signal strength: " + rx + " tx power: " + tx + " distance?: " + d);
            }
        } else {
            Log.d(TAG, "No beacons were detected");
        }

    }

    private void StopDetectingBeacons() {

        // App is not running (stop detecting beacons)
        running = false;

        // Change the icon to the start symbol
        startButton.setImageResource(R.drawable.play);

        // Hide icon and text
        startButton.setVisibility(View.GONE);
        introText1.setVisibility(View.GONE);
        introText2.setVisibility(View.GONE);

        // Show map and Again Button
        mapView.setVisibility(View.VISIBLE);
        again.setVisibility(View.VISIBLE);

        // Stop the process of detection
        try {
            beaconManager.stopMonitoringBeaconsInRegion(region);
            Log.d(TAG, "Stop detecting beacons...");
        } catch (Exception e){
            Log.d(TAG, "An error occured while trying to stop detecting beacons" + e.getMessage());
        }

        beaconManager.removeAllRangeNotifiers();
        beaconManager.unbind(this);

        // TODO: Buttons and text gone, Map appears with current location

    }


    @Override
    public void onLocationChanged(Location location) {
        if (requested) {
            lat = location.getLatitude();
            longi = location.getLongitude();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
            requested = false;
        }
        if (mapView.getVisibility() == View.VISIBLE) {
            float zoomLevel = 15f;
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, longi), zoomLevel));
        }
    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {

    }

    @Override
    public void onFlushComplete(int requestCode) {

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
        mapView.setVisibility(GONE);
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
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

};