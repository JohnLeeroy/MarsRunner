package com.jli.marsrun.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by john on 4/24/16.
 */
public class LocationTracker implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "LocationTracker";
    private static LocationTracker sInstance;

    private Context mContext;
    boolean mShouldStartLocationRequest = false;
    boolean mIsTracking = false;    //Used to figure out if we should save location data

    private ArrayList<Location> mLocations;
    private GoogleApiClient mGoogleApiClient;

    LocationListener mLocationListenerDelegate;

    public static LocationTracker getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LocationTracker(context);
        }
        return sInstance;
    }

    private LocationTracker(Context context) {
        mLocations = new ArrayList<>();
        mContext = context;
        initMapsAPI(context);
    }

    private void initMapsAPI(Context context) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
        Log.d(TAG, "Google API initialized");
    }

    //REFACTOR, move into a config container that can be injected into LocationTracker
    //  LocationTracker will take the config and create the location request
    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    public void startLocationRequest() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION} , FINE_LOCATION_REQUEST_CODE);
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            mShouldStartLocationRequest = true;
            return;
        }

        LocationRequest locationRequest = createLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        Log.d(TAG, "Start Connection Request");
    }

    public void stopLocationRequest() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void startTracking() {
        mIsTracking = true;
    }

    public void stopTracking() {
        mIsTracking = false;
    }

    public void trackLocation(Location loc) {
        //TODO fix this logic
        //hacked to set initial location or last location to the latest data received from
        //location services
        if (!mIsTracking) {
            int size = mLocations.size();
            if (size > 0)
                mLocations.set(size - 1, loc);
            else
                mLocations.add(loc);
        } else {
            mLocations.add(loc);
        }
        mLocationListenerDelegate.onLocationChanged(loc);
        Log.d(TAG, String.format("%f, %f", loc.getLatitude(), loc.getLongitude()));
        Log.d(TAG, "Total points tracked: " + mLocations.size());
    }

    public Location getMostRecentLocation() {
        int size = mLocations.size();
        if (size == 0) {
            return null;
        }
        return mLocations.get(mLocations.size() - 1);
    }

    public void setLocationListenerDelegate(LocationListener listener) {
        mLocationListenerDelegate = listener;
    }

    public List<Location> getLocationList() {
        return mLocations;
    }

    //GoogleApiClient Callback Impl

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connection Success");
        if (mShouldStartLocationRequest) {
            startLocationRequest();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed");
    }

    //LocationListener Impl

    @Override
    public void onLocationChanged(Location location) {
        //DEBUG
//        if (mLocations.size() > 0) {
//            location.setLatitude(location.getLatitude() + Math.random() * .005);
//            location.setLongitude(location.getLongitude() + Math.random() * .005);
//        }
        trackLocation(location);
    }
}
