package com.jli.marsrun.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jli.marsrun.MapUtil;
import com.jli.marsrun.R;
import com.jli.marsrun.manager.LocationTracker;

import java.util.List;
import java.util.TimerTask;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    public static final int IDLE_STATE = 0;
    public static final int PROGRESS_STATE = 1;
    public static final int PAUSED_STATE = 2;
    public static final int FINISHED_STATE = 3;

    public static final int FINE_LOCATION_REQUEST_CODE = 103;
    public static final int ZOOM_CONSTANT = 16;
    private GoogleMap mMap;

    ImageButton mLockUnlockBtn;
    Button mStartPauseBtn;
    Button mFinishBtn;

    TextView mDurationLabel;
    TextView mDistanceLabel;
    TextView mCaloriesLabel;
    TextView mPaceLabel;

    long mStartTime;
    long mDuration = 0;

    boolean isLocked = false;

    private int mState = IDLE_STATE;

    private LocationTracker mLocationTracker;

    String mDurationStat;
    String mDistanceStat;
    String mCaloriesStat;
    String mPaceStat;

    Circle mStartingCircle;
    Polyline mPolyline;

    Thread mUpdateTimerThread;
    Thread mUpdateMetricsThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        bindAndInitViews();

        mLocationTracker = LocationTracker.getInstance(this);
        mLocationTracker.setLocationListenerDelegate(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, FINE_LOCATION_REQUEST_CODE);
            return;
        }
        //Start location request if we have the permission to
        mLocationTracker.startLocationRequest();
    }

    void bindAndInitViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDurationLabel = (TextView) findViewById(R.id.duration);
        mDistanceLabel = (TextView) findViewById(R.id.distance);
        mCaloriesLabel = (TextView) findViewById(R.id.calories);
        mPaceLabel = (TextView) findViewById(R.id.pace);


        mStartPauseBtn = (Button) findViewById(R.id.start_pause_btn);
        mFinishBtn = (Button) findViewById(R.id.finish_btn);
        mLockUnlockBtn = (ImageButton) findViewById(R.id.lock_unlock_btn);
        mLockUnlockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLock();
            }
        });

        updateStartPauseButton();
        mFinishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishRun();
            }
        });

        TimerTask task = new TimerTask() {
            @Override
            public void run() {

            }
        };

        mUpdateTimerThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mState == PROGRESS_STATE)
                                    updateDuration();
                            }
                        });
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        mUpdateMetricsThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mState == PROGRESS_STATE)
                                    updateMetrics();
                            }
                        });
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
    }

    void toggleLock() {
        isLocked = !isLocked;
        mStartPauseBtn.setEnabled(!isLocked);
        mFinishBtn.setEnabled(!isLocked);
        if (isLocked) {
            mLockUnlockBtn.setImageResource(R.drawable.ic_lock_black_24dp);
        } else {
            mLockUnlockBtn.setImageResource(R.drawable.ic_lock_open_black_24dp);
        }
        updateStartPauseButton();
    }

    void updateStartPauseButton() {
        switch (mState) {
            case IDLE_STATE:
                mStartPauseBtn.setText("Start");
                mStartPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startRun();
                    }
                });
                break;
            case PROGRESS_STATE:
                mStartPauseBtn.setText("Pause");
                mStartPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pauseRun();
                    }
                });
                break;
            case PAUSED_STATE:
                mStartPauseBtn.setText("Resume");
                mStartPauseBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resumeRun();
                    }
                });
                break;
        }
    }

    void startRun() {
        mState = PROGRESS_STATE;
        mLocationTracker.startTracking();
        mStartTime = System.currentTimeMillis();
        mUpdateMetricsThread.start();
        mUpdateTimerThread.start();
        toggleLock();

        updateStartPauseButton();
    }

    void pauseRun() {
        mState = PAUSED_STATE;
        mLocationTracker.stopTracking();
        mDuration = System.currentTimeMillis() - mStartTime;
        mStartTime = System.currentTimeMillis();
        updateStartPauseButton();
    }

    void resumeRun() {
        mState = PROGRESS_STATE;
        mLocationTracker.startTracking();
        mStartTime = System.currentTimeMillis();
        toggleLock();
        updateStartPauseButton();
    }

    void finishRun() {
        mState = FINISHED_STATE;
        mUpdateMetricsThread.interrupt();
        mUpdateMetricsThread.interrupt();
        mLocationTracker.stopLocationRequest();
        mLocationTracker.stopTracking();

        Bundle args = new Bundle();
        args.putString("duration", mDurationStat);
        args.putString("distance", mDistanceStat);
        args.putString("calories", mCaloriesStat);
        args.putString("pace", mPaceStat);
        Intent intent = new Intent(MapsActivity.this, ResultActivity.class);
        intent.putExtras(args);
        startActivity(intent);
        mMap.clear();
        finish();
    }

    void updateDuration() {
        long delta = System.currentTimeMillis() - mStartTime;
        delta += mDuration;
        int elapsedSeconds = (int) delta / 1000;
        int hours = elapsedSeconds / 3600;
        int minutes = (elapsedSeconds % 3600) / 60;
        int seconds = elapsedSeconds % 60;

        mDurationStat = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        mDurationLabel.setText(mDurationStat);
    }

    void updateMetrics() {
        List<Location> locationList = mLocationTracker.getLocationList();
        double distance = MapUtil.calculateDistanceInMiles(locationList);
        mDistanceStat = String.format("%.2f", MapUtil.calculateDistanceInMiles(locationList));
        mDistanceLabel.setText(mDistanceStat);

        int caloriesBurned = (int) (distance * 105);
        mCaloriesStat = String.valueOf(caloriesBurned);
        mCaloriesLabel.setText(mCaloriesStat);

        long delta = System.currentTimeMillis() - mStartTime;
        delta += mDuration;
        int elapsedSeconds = (int) delta / 1000;
        //Don't divide by zero
        if (elapsedSeconds == 0)
            return;
        double pace = distance / (elapsedSeconds / 3600.0f);
        mPaceStat = String.format("%.2f", pace);
        mPaceLabel.setText(mPaceStat);
    }

    void updateMap() {
        List<Location> locationList = mLocationTracker.getLocationList();
        //OPTIMIZE
        //Currently removes old lines. Update with most current lines.
        if (mPolyline != null) {
            mPolyline.remove();
        }
        PolylineOptions mLineOptions = MapUtil.calculateLinesBasedOnLocations(locationList);
        mPolyline = mMap.addPolyline(mLineOptions);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    boolean getIsVisibleInCurrentRegion(Location loc) {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        return bounds.contains(latLng);
    }

    @TargetApi(Build.VERSION_CODES.M)
    void addCurrentLocationCircleShape() {
        List<Location> locationList = mLocationTracker.getLocationList();

        Location lastLocation = locationList.get(0);
        LatLng currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

        if (MapUtil.isLocationVisibleInBounds(mMap.getProjection().getVisibleRegion().latLngBounds,
                lastLocation)) {
            return;
        }

        if (mStartingCircle != null) {
            mStartingCircle.setCenter(currentLocation);
        } else {
            int strokeColor = getColor(R.color.colorPrimaryDark);
            int fillColor = getColor(R.color.colorPrimary);
            CircleOptions circleOptions = new CircleOptions()
                    .center(currentLocation)
                    .fillColor(fillColor)
                    .strokeColor(strokeColor)
                    .radius(15); // In meters
            mStartingCircle = mMap.addCircle(circleOptions);
        }
    }

    private void centerMapToUser() {
        Location lastLocation = mLocationTracker.getMostRecentLocation();
        LatLng currentLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, ZOOM_CONSTANT));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationTracker.startLocationRequest();
                } else {
                    // permission denied, boo! Disable the
                }
                return;
            }
        }
    }

    //Location Listener Impl

    @Override
    public void onLocationChanged(Location location) {
        addCurrentLocationCircleShape();
        updateMap();
        if (mMap != null) {
            centerMapToUser();
        }
    }

}
