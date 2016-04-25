package com.jli.marsrun;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Created by john on 4/24/16.
 */
public class MapUtil {
    static final double METERS_TO_MILES_CONVERSION = 0.000621371;

    //Calculates total distance between each location point
    public static double calculateDistanceInMiles(List<Location> locations) {
        double totalDistance = 0;
        for(int i = 0; i < locations.size()-1; i++) {
            Location a = locations.get(i);
            Location b = locations.get(i+1);
            double distance = Math.abs(a.distanceTo(b));
            totalDistance += distance;
        }
        return metersToMiles(totalDistance);
    }

    public static double metersToMiles(double meters) {
        return  meters * METERS_TO_MILES_CONVERSION;
    }

    //Generate a list of PolyLines inside of the PolylineOptions based on received Location data
    public static PolylineOptions calculateLinesBasedOnLocations(List<Location> locations) {
        PolylineOptions rectOptions = new PolylineOptions();
        if(locations.size() <= 1) {
            return  rectOptions;
        }

        double distance = 0;
        final double minLineDistanceThreshold = .01;
        int index = 0;
        int upperBound = locations.size() - 1;

        Location a, b = null;
        rectOptions.add(new LatLng(locations.get(0).getLatitude(), locations.get(0).getLongitude()));
        while(index < upperBound) {
            while(distance < minLineDistanceThreshold && index < upperBound) {
                a = locations.get(index);
                b = locations.get(index + 1);
                distance += Math.abs(a.distanceTo(b));
                index++;
            }
            distance = 0;
            rectOptions.add(new LatLng(b.getLatitude(), b.getLongitude()));
        }
        return rectOptions;
    }

    public static boolean isLocationVisibleInBounds(LatLngBounds bounds, Location loc) {
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        return bounds.contains(latLng);
    }
}
