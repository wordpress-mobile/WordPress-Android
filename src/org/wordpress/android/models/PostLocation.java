package org.wordpress.android.models;

import java.io.Serializable;

public class PostLocation implements Serializable {
    static final double INVALID_LATITUDE = 9999;
    static final double INVALID_LONGITUDE = 9999;
    static final double MIN_LATITUDE = -90;
    static final double MAX_LATITUDE = 90;
    static final double MIN_LONGITUDE = -180;
    static final double MAX_LONGITUDE = 180;

    private double mLatitude = INVALID_LATITUDE;
    private double mLongitude = INVALID_LONGITUDE;

    public PostLocation() { }

    public PostLocation(double latitude, double longitude) {
        setLatitude(latitude);
        setLongitude(longitude);
    }

    public boolean isValid() {
        return isValidLatitude(mLatitude) && isValidLongitude(mLongitude);
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        if (!isValidLatitude(latitude)) {
            throw new IllegalArgumentException(
                    "Invalid latitude; must be between the range " + MIN_LATITUDE + " and " + MAX_LATITUDE
            );
        }

        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        if (!isValidLongitude(longitude)) {
            throw new IllegalArgumentException(
                    "Invalid longitude; must be between the range " + MIN_LONGITUDE + " and " + MAX_LONGITUDE
            );
        }

        mLongitude = longitude;
    }

    private boolean isValidLatitude(double latitude) {
        return latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE;
    }

    private boolean isValidLongitude(double longitude) {
        return longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE;
    }

    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + (Double.valueOf(mLatitude).hashCode());
        hashCode = prime * hashCode + (Double.valueOf(mLongitude).hashCode());

        return hashCode;
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof PostLocation) {
            PostLocation otherLocation = (PostLocation) other;
            return this.mLatitude == otherLocation.mLatitude
                    && this.mLongitude == otherLocation.mLongitude;
        }
        return false;
    }

    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }
}
