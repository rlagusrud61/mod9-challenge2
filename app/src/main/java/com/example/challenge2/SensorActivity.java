package com.example.challenge2;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.IndoorBuilding;
import com.google.android.gms.maps.model.IndoorLevel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;


import android.content.BroadcastReceiver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.view.View.GONE;

// View.OnClickListener, BeaconConsumer, RangeNotifier
public class SensorActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback, LocationListener, View.OnClickListener, RangeNotifier, BeaconConsumer {

    // sending log output TAG
    private static final String TAG = "MyActivity";

    // Interact with the different beacons
    private BeaconManager beaconManager;

    // Search for beacons
    private static Region region;

    // Scan time for beacons (6 seconds)
    private static final long SCAN_TIME = 1000l;
    private static final String BECONS_CLOSE_BY = "BeaconsCloseBy";

    // For bluetooth devices recognition
    private BroadcastReceiver broadcastReceiver;
    private BluetoothAdapter bluetoothAdapter;

    HashMap<String,ArrayList> data = new HashMap<String,ArrayList>();

    // Step Counter
    private SensorManager sensorManager;
    private Sensor stepcounter;
    private boolean counter_present;
    int stepcount = 0;

    // ----------------------------------------

    private LocationManager locationManager;
    private ScreenReceiver screenReceiver;
    private FusedLocationProviderClient fusedLocationClient;

    Marker marker;

    Location location;

    // Front-End components
    TextView introText1, introText2, activity;
    ImageButton startButton;
    Button again;
    LinearLayout linearLayout;
    ListView bluetooth;

    Map<String,ArrayList> beacon_info = new HashMap<>();

    Boolean requested = true;
    Boolean reset = false;

    private MapView mapView;
    private boolean cameraset = false;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    double lat = 52.2392530; // 52.213453
    double longi = 6.8554879; // 6.879420

    double N = 4;
    double C = 0;

    double overlap_amount = 0.1;

    double l_distance = 20;

    double x_value1,x_value2,x_value3,y_value1,y_value2,y_value3,l_distance1, l_distance2,l_distance3;
//    double loc1[] = {43.77122467,46.30300847, l_distance};
//    double loc2[] = {40.2213248, 25.34740025, l_distance};
//    double loc3[] = {42.83513801, 51.42698583, l_distance};


    double loc1[] = {x_value1, y_value1, l_distance1};
    double loc2[] = {x_value2, y_value2, l_distance2};
    double loc3[] = {x_value3, y_value3, l_distance3};

    double[][] lfinal = {loc1,loc2,loc3};

    //HashMap<String, Double> beaconHashMap = new HashMap<>();

    GoogleMap googleMap;

    public double[] triangulate_common_chords(double[][] beacons) {
        double pos[] = {0,0};
        double x12 = beacons[0][0] - beacons[1][0];
        double x23 = beacons[1][0] - beacons[2][0];
        double y12 = beacons[0][1] - beacons[1][1];
        double y23 = beacons[1][1] - beacons[2][1];

        double c12 = beacons[0][0]*beacons[0][0] - beacons[1][0]*beacons[1][0] + beacons[0][1]*beacons[0][1] - beacons[1][1]*beacons[1][1] - (beacons[0][2]*beacons[0][2] - beacons[1][2]*beacons[1][2]);
        double c23 = (beacons[1][0]*beacons[1][0] - beacons[2][0]*beacons[2][0]) + (beacons[1][1]*beacons[1][1] - beacons[2][1]*beacons[2][1]) - (beacons[1][2]*beacons[1][2] - beacons[2][2]*beacons[2][2]);

        pos[0] = (((x23/x12)*c12)-c23)/((2*(x23/x12)*y12)-(2*y23));
        pos[1] = 1/x12 * (-y12*pos[0]+c12/2);
        return pos;
    }

    public double[][] prepare_common_chords(double[][] beacons) {

        double [][] fixed_beacons = beacons;
        boolean [] overlap_check = {false,false,false};

        double r12 = beacons[0][2] + beacons[1][2];
        double r23 = beacons[1][2] + beacons[2][2];
        double r13 = beacons[0][2] + beacons[2][2];

        double length_12 = Math.sqrt((beacons[1][0]-beacons[0][0])*(beacons[1][0]-beacons[0][0])+(beacons[1][1]-beacons[0][1])*(beacons[1][1]-beacons[0][1]));

        if(length_12 > r12){
            double dist12 = length_12-r12;
            fixed_beacons[0][2] += (dist12 + overlap_amount)/2;
            fixed_beacons[1][2] += (dist12 + overlap_amount)/2;
        }

        double length_23 = Math.sqrt((beacons[2][0]-beacons[1][0])*(beacons[2][0]-beacons[1][0])+(beacons[2][1]-beacons[1][1])*(beacons[2][1]-beacons[1][1]));

        if(length_23 > r23){
            double dist23 = length_23-r23;
            fixed_beacons[1][2] += (dist23 + overlap_amount)/2;
            fixed_beacons[2][2] += (dist23 + overlap_amount)/2;
        }

        double length_13 = Math.sqrt((beacons[2][0]-beacons[0][0])*(beacons[2][0]-beacons[0][0])+(beacons[2][1]-beacons[0][1])*(beacons[2][1]-beacons[0][1]));

        if(length_13 > r13){
            double dist13 = length_13-r13;
            fixed_beacons[0][2] += (dist13 + overlap_amount)/2;
            fixed_beacons[2][2] += (dist13 + overlap_amount)/2;
        }

        return fixed_beacons;
    }

    public double[] xy_to_coords(double[] xy){
        double[] coords = {0,0};
        double c_lat = 1.111206896555222e+05;
        double c_long = 6.799157303370348e+04;
        double long_0 = 6.854982000000000;
        double lat_0 = 52.238976000000000;

        coords[0] = xy[1]/c_lat + lat_0;
        coords[1] = xy[0]/c_long + long_0;

        return coords;
    }

    public void requestCurrentLocation(){
        Log.d(TAG, "r x_value " +x_value1 + "yvalue " + y_value1 + "distance " + l_distance1);
        Log.d(TAG, "loc1" + loc1[0]);
        double[][] preparing = prepare_common_chords(lfinal);
        Log.d(TAG,"preparing: " + preparing[0][0] + " " + preparing[0][1] + " " + preparing[0][2]
                + " " + preparing[1][0] + " " + preparing[1][1] + " " + preparing[1][2]
                + " " + preparing[2][0] + " " + preparing[2][1] + " " + preparing[2][2]);

        double[] xy = triangulate_common_chords(preparing);
        Log.d(TAG,"triangulation: " + xy[0] + " " + xy[1]);
        double[] coord = xy_to_coords(xy);
        Log.d(TAG,"xy to coords: " + coord[0] + " " + coord[1]);
        lat  = coord[0];
        longi = coord[1];
        requested = true;

        Log.d(TAG,"requestCurrentLocation: lat " + lat + "longi " + longi + "coords: " + coord);
    }

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
        activity.setText("0");
        bluetooth = findViewById(R.id.bluetooth);

        startButton.setOnClickListener(this);
        again.setOnClickListener(this);

        data = new HashMap<>();

        try {
            data = this.getData();
            //Log.d(TAG, "beacon info: " + data.get("529").get(5));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "beacon info: " + beacon_info.get(0));

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

        // initialize ScreenReceiver for tracking screen state changes
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiver = new ScreenReceiver();
        registerReceiver(screenReceiver, filter);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 10);
        }

        // OnCreate
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.map);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Step counter

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!= null) {
            stepcounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            counter_present = true;
        } else{
            counter_present = false;
        }

    }

    // Start button
    @Override
    public void onClick(View view) {
        // Press button to start application
        Log.d(TAG, "has entered onClick");
        if (view.equals(startButton) && !reset){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                }, 10);
            }
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter.isEnabled()){
                StartDetectingBeacons();
                ListAllBluetoothDevices();
            } else {
                // Ask the user to enable bluetooth
                Intent ask_bluetooth_enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(ask_bluetooth_enable, 1);
            }

        } else if (view.equals(again) && reset){
            mapView.setVisibility(View.GONE);
            again.setVisibility(View.GONE);
            introText1.setVisibility(View.VISIBLE);
            introText2.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
            activity.setVisibility(View.VISIBLE);
            reset = false;

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
    }

    private void ListAllBluetoothDevices() {

        bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();

        bluetoothAdapter.startDiscovery();

        ArrayList<String> arrayList = new ArrayList<>();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Todo: Might be an idea to only display the ones you have not found before. check if it already does that
                    Log.d(TAG, "BLUETOOTH DEVICE FOUND: NAME " + bluetoothDevice.getName() + "WITH MAC ADDRESS: " + bluetoothDevice.getAddress());
                    arrayList.add(bluetoothDevice.getName());
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);
    }


    private void StartDetectingBeacons() {
        Log.d(TAG, "has entered StartDetectingBeacons");

        // Time to scan for beacons (6 seconds)
        beaconManager.setForegroundBetweenScanPeriod(SCAN_TIME);

        // Pair with beacon service
        beaconManager.bind(this);

        mapView.setVisibility(View.VISIBLE);
//        activity.setVisibility(View.GONE);
        again.setVisibility(View.VISIBLE);
        introText1.setVisibility(View.GONE);
        introText2.setVisibility(View.GONE);
        startButton.setVisibility(View.GONE);
        reset = true;

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

        HashMap<String, Double> beaconHashMap = new HashMap<>();

        if (beacons.size() != 0) {
            double tx,rx,distance;

            for (Beacon beacon : beacons) {
                tx = beacon.getTxPower();
                rx = beacon.getRssi();
                distance = Math.pow(10, ((tx - rx + C) / (10 * N))); // distance between phone and the beacons (estimate)
                Log.d("Beacon", "Beacon detected: " + beacon + "With RSSI: " + beacon.getRssi() + "With transmission power : " + beacon.getTxPower() + "Distance suggested from library: " + beacon.getDistance());
                beaconHashMap.put(beacon.getBluetoothAddress(), distance);

            }
            activity.setText("Number of beacons detected: " + beaconHashMap.size());

            if (beaconHashMap.size() > 3) {
                int iterations = beaconHashMap.size() - 3; // we want to remove this much
                Log.d(TAG, "I have " + beaconHashMap.size() + ", so I'm gonna remove " + iterations + " beacons");
                int removalCounter = 0;
                while (removalCounter < iterations){
                    double maxDistanceValue = (Collections.max(beaconHashMap.values())); // get the max distance value
                    for (Map.Entry<String,Double> entry1 : beaconHashMap.entrySet()){
                        if (entry1.getValue().equals(maxDistanceValue)){
                            beaconHashMap.remove(entry1);
                            Log.d("Removed : " , "I removed this beacon : "  + entry1);
                            removalCounter++;
                        }
                    }
                }

            }
            if (beaconHashMap.size() == 3) {
                Log.d(TAG, "entering hashmap size == 3");
                Log.d(TAG, "BeaconHashMap toString(): " + beaconHashMap.toString());
                activity.setText(beaconHashMap.toString());
                ArrayList<String> macAddresses = new ArrayList<>();

                for (Map.Entry<String, Double> becaconEntry : beaconHashMap.entrySet()) {
                    macAddresses.add(becaconEntry.getKey());
                }
                Log.d(TAG, "macaddresses: " + macAddresses);

                try{

                    x_value1 = new Double(getData().get(macAddresses.get(0)).get(5).toString());
                    y_value1 = new Double(getData().get(macAddresses.get(0)).get(6).toString());
                    x_value2 = new Double(getData().get(macAddresses.get(1)).get(5).toString());
                    y_value2 = new Double(getData().get(macAddresses.get(1)).get(6).toString());
                    x_value3 = new Double(getData().get(macAddresses.get(2)).get(5).toString());
                    y_value3 = new Double(getData().get(macAddresses.get(2)).get(6).toString());

                    l_distance1 = beaconHashMap.get(macAddresses.get(0));
                    l_distance2 = beaconHashMap.get(macAddresses.get(1));
                    l_distance3 = beaconHashMap.get(macAddresses.get(2));


                    lfinal[0][0] = x_value1;
                    lfinal[0][1] = y_value1;
                    lfinal[0][2] = l_distance1;

                    lfinal[1][0] = x_value2;
                    lfinal[1][1] = y_value2;
                    lfinal[1][2] = l_distance2;

                    lfinal[2][0] = x_value3;
                    lfinal[2][1] = y_value3;
                    lfinal[2][2] = l_distance3;


                    requestCurrentLocation();

                    Log.d(TAG, "x_value " +x_value1 + "yvalue " + y_value1 + "distance " + l_distance1);

                    //Log.d(TAG, "x value of MAC e2:83:81:47:2c:be :" + getData().get("e2:83:81:47:2c:be").get(5));
                    //Log.d(TAG, "y value of MAC e2:83:81:47:2c:be :" + getData().get("e2:83:81:47:2c:be").get(6));

                } catch(IOException e){
                    e.printStackTrace();
                }

            }


            /*for (Map.Entry<String,Double> entry : beaconHashMap.entrySet()){
                Log.d(TAG, "entering for loop");
                //try {
                    //if (getData().containsKey(entry.getKey())){
                        if (beaconHashMap.size() > 3) {
                            Log.d(TAG, "entering hashmap size > 3");
                            int iterations = beaconHashMap.size() - 3; // we want to remove this much
                            Log.d(TAG, "I have " + beaconHashMap.size() + ", so I'm gonna remove " + iterations + " beacons");
                            int removalCounter = 0;
                            // de los detected beacons, remover los dos que tienen most distance
                            while (removalCounter < iterations){
                                //find the maximum one
                                double maxDistanceValue = (Collections.max(beaconHashMap.values())); // get the max distance value
                                for (Map.Entry<String,Double> entry1 : beaconHashMap.entrySet()){
                                    if (entry1.getValue().equals(maxDistanceValue)){
                                        beaconHashMap.remove(entry1);
                                        Log.d("Removed : " , "I removed this beacon : "  + entry1);
                                        removalCounter++;
                                    }
                                }
                            }
                        }
                        if (beaconHashMap.size() == 3) {
                            Log.d(TAG, "entering hashmap size == 3");
                            Log.d(TAG, "BeaconHashMap toString(): " + beaconHashMap.toString());
                            activity.setText(beaconHashMap.toString());
                            ArrayList<String> macAddresses = new ArrayList<>();

                            for (Map.Entry<String, Double> becaconEntry : beaconHashMap.entrySet()) {
                                macAddresses.add(becaconEntry.getKey());
                            }
                            Log.d(TAG, "macaddresses: " + macAddresses);

                            try{

                                x_value1 = new Double(getData().get(macAddresses.get(0)).get(5).toString());
                                y_value1 = new Double(getData().get(macAddresses.get(0)).get(6).toString());
                                x_value2 = new Double(getData().get(macAddresses.get(1)).get(5).toString());
                                y_value2 = new Double(getData().get(macAddresses.get(1)).get(6).toString());
                                x_value3 = new Double(getData().get(macAddresses.get(2)).get(5).toString());
                                y_value3 = new Double(getData().get(macAddresses.get(2)).get(6).toString());

                                l_distance1 = beaconHashMap.get(macAddresses.get(0));
                                l_distance2 = beaconHashMap.get(macAddresses.get(1));
                                l_distance3 = beaconHashMap.get(macAddresses.get(2));


                                lfinal[0][0] = x_value1;
                                lfinal[0][1] = y_value1;
                                lfinal[0][2] = l_distance1;

                                lfinal[1][0] = x_value2;
                                lfinal[1][1] = y_value2;
                                lfinal[1][2] = l_distance2;

                                lfinal[2][0] = x_value3;
                                lfinal[2][1] = y_value3;
                                lfinal[2][2] = l_distance3;


                                requestCurrentLocation();

                                Log.d(TAG, "x_value " +x_value1 + "yvalue " + y_value1 + "distance " + l_distance1);

                                //Log.d(TAG, "x value of MAC e2:83:81:47:2c:be :" + getData().get("e2:83:81:47:2c:be").get(5));
                                //Log.d(TAG, "y value of MAC e2:83:81:47:2c:be :" + getData().get("e2:83:81:47:2c:be").get(6));

                            } catch(IOException e){
                                e.printStackTrace();
                            }

                        }

                    //}
                //} catch (IOException e) {
                    //e.printStackTrace();
                //}
            }*/

        } else {
            Log.d(TAG, "No beacons were detected");
            activity.setText("You need at least 3 beacons to locate yourself");
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        if (requested) {
            if (marker != null){
                marker.remove();
            }
            Log.d(TAG, "location :" + String.valueOf(new LatLng(lat, longi)));
            float zoomLevel = 19f;
            marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, longi)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, longi), zoomLevel));
            requested = false;
        }
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
        Log.d(TAG, "Has entered 3");
        this.googleMap = map;
        System.out.println("OnMapReady" + googleMap);
        mapView.onResume();
        mapView.onEnterAmbient(null);
        map.getUiSettings().setIndoorLevelPickerEnabled(true);
        // This view is invisible, but it still takes up space for layout purposes. Otherwise use GONE

        googleMap.setOnIndoorStateChangeListener(new GoogleMap.OnIndoorStateChangeListener() {
            @Override
            public void onIndoorBuildingFocused() {

            }

            @Override
            public void onIndoorLevelActivated(IndoorBuilding indoorBuilding) {
                // From highest to lowest. So 1 = 4, 2 = 3...
                List<IndoorLevel> levels = indoorBuilding.getLevels();
                int level = indoorBuilding.getActiveLevelIndex();
                Log.d("Tag21", "Level Index: " + String.valueOf(level));
                String currentFloor = levels.get(level).getName();
                Log.d("Tag21", "Level: " + currentFloor);
            }

        });

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

    public HashMap<String,ArrayList> getData() throws IOException{

        try {

            InputStream myInput;
            // initialize asset manager
            AssetManager assetManager = getAssets();
            //  open excel sheet
            myInput = assetManager.open("beacons-info1.xls");
            // Create a POI File System object
            POIFSFileSystem myFileSystem = new POIFSFileSystem(myInput);
            // Create a workbook using the File System
            HSSFWorkbook myWorkBook = new HSSFWorkbook(myFileSystem);
            // Get the first sheet from workbook
            HSSFSheet sheet = myWorkBook.getSheetAt(0);

            DataFormatter formatter = new DataFormatter(Locale.US);

            int lastRow = sheet.getLastRowNum();

            for (int i = 0; i <= lastRow; i++) {
                ArrayList rest = new ArrayList();
                Row row = sheet.getRow(i);
                Cell beacon_id_Cell = row.getCell(0);
                String beacon_id = formatter.formatCellValue(beacon_id_Cell);
                Cell device_name_Cell = row.getCell(1);
                String device_name = formatter.formatCellValue(device_name_Cell);
                Cell mac_address_Cell = row.getCell(2);
                String mac_address = formatter.formatCellValue(mac_address_Cell);
                Cell longitude_Cell = row.getCell(3);
                String longitude = formatter.formatCellValue(longitude_Cell);
                Cell latitude_Cell = row.getCell(4);
                String longitudlatitudee = formatter.formatCellValue(latitude_Cell);
                Cell floor_Cell = row.getCell(5);
                String floor = formatter.formatCellValue(floor_Cell);
                Cell x_Cell = row.getCell(6);
                String x = formatter.formatCellValue(x_Cell);
                Cell y_Cell = row.getCell(7);
                String y = formatter.formatCellValue(y_Cell);

                rest.add(beacon_id);
                rest.add(device_name);
                rest.add(longitude);
                rest.add(longitudlatitudee);
                rest.add(floor);
                rest.add(x);
                rest.add(y);

                data.put(mac_address, rest);
            }
        } catch (Exception e){
            Log.e(TAG, "error in getData: " + e.toString());
        }
        return data;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.d(TAG,"OnSensorChanged");
        if (sensorEvent.sensor == stepcounter){
            stepcount = (int) sensorEvent.values[0];
            activity.setText(String.valueOf(stepcount));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
};
