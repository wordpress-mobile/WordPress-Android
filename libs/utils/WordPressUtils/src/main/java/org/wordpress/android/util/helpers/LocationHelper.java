//This Handy-Dandy class acquired and tweaked from http://stackoverflow.com/a/3145655/309558
package org.wordpress.android.util.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

public class LocationHelper {
    Timer mTimer;
    LocationManager mLocationManager;
    LocationResult mLocationResult;
    boolean mGpsEnabled = false;
    boolean mNetworkEnabled = false;

    @SuppressLint("MissingPermission")
    public boolean getLocation(Activity activity, LocationResult result) {
        mLocationResult = result;
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        }

        // exceptions will be thrown if provider is not permitted.
        try {
            mGpsEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            mNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        // don't start listeners if no provider is enabled
        if (!mGpsEnabled && !mNetworkEnabled) {
            return false;
        }

        if (mGpsEnabled) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        }

        if (mNetworkEnabled) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        }

        mTimer = new Timer();
        mTimer.schedule(new GetLastLocation(), 30000);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        @SuppressLint("MissingPermission")
        public void onLocationChanged(Location location) {
            mTimer.cancel();
            mLocationResult.gotLocation(location);
            mLocationManager.removeUpdates(this);
            mLocationManager.removeUpdates(locationListenerNetwork);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        @SuppressLint("MissingPermission")
        public void onLocationChanged(Location location) {
            mTimer.cancel();
            mLocationResult.gotLocation(location);
            mLocationManager.removeUpdates(this);
            mLocationManager.removeUpdates(locationListenerGps);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    class GetLastLocation extends TimerTask {
        @Override
        @SuppressLint("MissingPermission")
        public void run() {
            mLocationManager.removeUpdates(locationListenerGps);
            mLocationManager.removeUpdates(locationListenerNetwork);

            Location net_loc = null, gps_loc = null;
            if (mGpsEnabled) {
                gps_loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (mNetworkEnabled) {
                net_loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // if there are both values use the latest one
            if (gps_loc != null && net_loc != null) {
                if (gps_loc.getTime() > net_loc.getTime()) {
                    mLocationResult.gotLocation(gps_loc);
                } else {
                    mLocationResult.gotLocation(net_loc);
                }
                return;
            }

            if (gps_loc != null) {
                mLocationResult.gotLocation(gps_loc);
                return;
            }
            if (net_loc != null) {
                mLocationResult.gotLocation(net_loc);
                return;
            }
            mLocationResult.gotLocation(null);
        }
    }

    public static abstract class LocationResult {
        public abstract void gotLocation(Location location);
    }

    @SuppressLint("MissingPermission")
    public void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mLocationManager.removeUpdates(locationListenerGps);
            mLocationManager.removeUpdates(locationListenerNetwork);
        }
    }
}
