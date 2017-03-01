package org.wordpress.android.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class GeocoderUtils {
    private GeocoderUtils() {
        throw new AssertionError();
    }

    public static Geocoder getGeocoder(Context context) {
        // first make sure a Geocoder service exists on this device (requires API 9)
        if (!Geocoder.isPresent()) {
            return null;
        }

        Geocoder gcd;

        try {
            gcd = new Geocoder(context, LanguageUtils.getCurrentDeviceLanguage(context));
        } catch (NullPointerException cannotIstantiateEx) {
            AppLog.e(AppLog.T.UTILS, "Cannot instantiate Geocoder", cannotIstantiateEx);
            return null;
        }

        return gcd;
    }

    public static Address getAddressFromCoords(Context context, double latitude, double longitude) {
        Address address = null;
        List<Address> addresses = null;

        Geocoder gcd = getGeocoder(context);

        if (gcd == null) {
            return null;
        }

        try {
            addresses = gcd.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            // may get "Unable to parse response from server" IOException here if Geocoder
            // service is hit too frequently
            AppLog.e(AppLog.T.UTILS,
                    "Unable to parse response from server. Is Geocoder service hitting the server too frequently?",
                    e
            );
        }

        // addresses may be null or empty if network isn't connected
        if (addresses != null && addresses.size() > 0) {
            address = addresses.get(0);
        }

        return address;
    }

    public static Address getAddressFromLocationName(Context context, String locationName) {
        int maxResults = 1;
        Address address = null;
        List<Address> addresses = null;

        Geocoder gcd = getGeocoder(context);

        if (gcd == null) {
            return null;
        }

        try {
            addresses = gcd.getFromLocationName(locationName, maxResults);
        } catch (IOException e) {
            AppLog.e(AppLog.T.UTILS, "Failed to get coordinates from location", e);
        }

        // addresses may be null or empty if network isn't connected
        if (addresses != null && addresses.size() > 0) {
            address = addresses.get(0);
        }

        return address;
    }

    public static String getLocationNameFromAddress(Address address) {
        String locality = "", adminArea = "", country = "";
        if (address.getLocality() != null) {
            locality = address.getLocality();
        }

        if (address.getAdminArea() != null) {
            adminArea = address.getAdminArea();
        }

        if (address.getCountryName() != null) {
            country = address.getCountryName();
        }

        return ((locality.equals("")) ? locality : locality + ", ")
                + ((adminArea.equals("")) ? adminArea : adminArea + " ") + country;
    }

    public static double[] getCoordsFromAddress(Address address) {
        double[] coordinates = new double[2];

        if (address.hasLatitude() && address.hasLongitude()) {
            coordinates[0] = address.getLatitude();
            coordinates[1] = address.getLongitude();
        }

        return coordinates;
    }
}
