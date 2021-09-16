package com.example.challenge2;

import android.Manifest;
import android.app.Activity;
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
import android.text.TextUtils;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import org.altbeacon.beacon.BuildConfig;
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


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private static final String BEACONS_CLOSE_BY = "BeaconsCloseBy";

    // Program is running or is on pause
    private boolean running = false;

    // Create list of ActivityTransition objects (walking, still)

    // Action fired when transitions are triggered.
    private boolean activityTrackingEnabled;

    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.LIBRARY_PACKAGE_NAME + "TRANSITIONS_RECEIVER_ACTION";
    private List<ActivityTransition> activityTransitionList;
//    private TransitionsReceiver transitionsReceiver;
    private ActivityRecognitionClient activityRecognitionClient;

    private PendingIntent pendingIntent;

    // ----------------------------------------


    private LocationManager locationManager;

    // Front-End components
    TextView introText1, introText2, activity;
    ImageButton startButton;
    Button again;
    LinearLayout linearLayout;

    Boolean requested = true;

    private MapView mapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    static final double lat = 52.2393907; // Ravelijn coordinates
    static final double longi = 6.8555544; //

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

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

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

        region = new Region(BEACONS_CLOSE_BY, identifiers);


        activityTrackingEnabled=false;
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

//        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
//        pendingIntent = PendingIntent.getBroadcast(SensorActivity.this, 0, intent, 0);
//
//        // Register a BroadcastReceiver to listen for activity transitions.
//        registerReceiver(transitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));
//
//        transitionsReceiver = new TransitionsReceiver();

        // ----------------------------------------

        // Asking for location permission
        if (ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);



    }

    public void onClickEnableOrDisableActivityRecognition(View view) {

        // TODO: Enable/Disable activity tracking and ask for permissions if needed.
        if (activityRecognitionPermissionApproved()) {

            if (activityTrackingEnabled) {
                disableActivityTransitions();

            } else {
                enableActivityTransitions();
            }

        } else {
            // Request permission and start activity for result. If the permission is approved, we
            // want to make sure we start activity recognition tracking.
//            Intent startIntent = new Intent(this, PermissionRationalActivity.class);
//            startActivityForResult(startIntent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Start activity recognition if the permission was approved.
        Log.d(TAG, "Activitytrackingenabled: " + activityTrackingEnabled );
        if (activityRecognitionPermissionApproved() && !activityTrackingEnabled) {
            Log.d(TAG,"Gonna enable activity transitions");
            enableActivityTransitions();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onClick(View view){

    }
    // Start button
    public void onClickButton(View view){
        // Press button to start application
        Log.d(TAG, "has entered onClick");
//        onClickEnableOrDisableActivityRecognition(startButton);
        Log.d(TAG, "mapvisibility : " + mapView.getVisibility());
            if (mapView.getVisibility() == View.VISIBLE) {
                Log.d(TAG,"si esta visible");
                float zoomLevel = 20f;
                googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, longi), zoomLevel));
            }

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

            mapView.setVisibility(View.VISIBLE);
            Log.d(TAG,"set visibility to visible again");
            StopDetectingBeacons();
    }

    private boolean activityRecognitionPermissionApproved(){
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
        );
    }
    private void enableActivityTransitions(){
        Log.d(TAG,"enableActivityTransitions");

        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, pendingIntent);


        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        activityTrackingEnabled = true;
                        Log.d(TAG,"Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Transitions Api could NOT be registered: " + e);

                    }
                });

    }

    private void disableActivityTransitions(){
        Log.d(TAG, "disableActivityTransitions()");

        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        activityTrackingEnabled = false;
                        Log.d(TAG, "Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG,"Transitions could not be unregistered: " + e);
                    }
                });

    }

    private void StartDetectingBeacons() {
        Log.d(TAG, "has entered StartDetectingBeacons");

        // App is now running (detecting beacons)
        running = true;

        // Change the icon to the pause symbol
        startButton.setImageResource(R.drawable.stop);


        // Time to scan for beacons (6 seconds)
        beaconManager.setForegroundBetweenScanPeriod(SCAN_TIME);

        // Pair with beacon service
        beaconManager.bind(this);

        // App is not running (stop detecting beacons)
        running = false;

        // Change the icon to the start symbol
        startButton.setImageResource(R.drawable.locationbutton);

        // Hide icon and text
        startButton.setVisibility(View.GONE);
        introText1.setVisibility(View.GONE);
        introText2.setVisibility(View.GONE);

        // Show map and Again Button

        mapView.setVisibility(View.VISIBLE);
        again.setVisibility(View.VISIBLE);

        Log.d(TAG,"mapview set to : " + mapView.getVisibility());

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

        if (beacons.size() != 0){
            for (Beacon beacon : beacons){
                Log.d(TAG, "Beacon detected: " + beacon);
            }
        } else {
            Log.d(TAG, "No beacons were detected");
        }

    }

    private void StopDetectingBeacons() {

        // Stop the process of detection
        try {
            beaconManager.stopMonitoringBeaconsInRegion(region);
            Log.d(TAG, "Stop detecting beacons...");
        } catch (Exception e){
            Log.d(TAG, "An error occured while trying to stop detecting beacons" + e.getMessage());
        }

        beaconManager.removeAllRangeNotifiers();
        beaconManager.unbind(this);


    }


    @Override
    public void onLocationChanged(Location location) {
        if (requested) {
//            lat = location.getLatitude();
//            longi = location.getLongitude();
            googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
            requested = false;
        }

    }

    @Override
    public void onFlushComplete(int requestCode) {

    }

    @Override
    protected void onStart(){
        super.onStart();

        //Register BroadcastReceiver to listen for activity transitions
//        registerReceiver(transitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Disable activity transitions when user leaves the app
        if (activityTrackingEnabled) {
            disableActivityTransitions();
        }
        super.onPause();
        StopDetectingBeacons();

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister activity transition receiver when user leaves the app.
//        unregisterReceiver(transitionsReceiver);

        StopDetectingBeacons();
    }


    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        System.out.println("OnMapReady" + googleMap);
        mapView.onResume();
        mapView.onEnterAmbient(null);
        // This view is invisible, but it still takes up space for layout purposes. Otherwise use GONE
        mapView.setVisibility(View.VISIBLE);
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

    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "UNKNOWN";
        }
    }

    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

//    public class TransitionsReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            Log.d(TAG, "onReceive(): " + intent);
//
//            if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
//
//                Log.d(TAG, "Unsupported action received by TransitionsReceiver : " + intent.getAction());
//            }
//
//            // Extract activity transition information from listener.
//            if (ActivityTransitionResult.hasResult(intent)) {
//
//                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
//
//                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
//
//                    String info = "Transition: " + toActivityString(event.getActivityType()) +
//                            " (" + toTransitionType(event.getTransitionType()) + ")" + "   " +
//                            new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
//
//                    Log.d(TAG, info);
//                }
//            }
//
//        }
//    }

};