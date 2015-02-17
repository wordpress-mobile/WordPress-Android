package org.wordpress.android.util;

import android.text.TextUtils;

/**
 * routines related to the Photon API
 * http://developer.wordpress.com/docs/photon/
 */
public class PhotonUtils {

    private PhotonUtils() {
        throw new AssertionError();
    }

    /*
    * returns true if the passed url is an obvious "mshots" url
    */
    public static boolean isMshotsUrl(final String imageUrl) {
        return (imageUrl != null && imageUrl.contains("/mshots/"));
    }

    /*
     * returns a photon url for the passed image with the resize query set to the passed
     * dimensions - note that the passed quality parameter will only affect JPEGs
     */
    public static enum Quality {
        HIGH,
        MEDIUM,
        LOW
    }
    public static String getPhotonImageUrl(String imageUrl, int width, int height) {
        return getPhotonImageUrl(imageUrl, width, height, Quality.MEDIUM);
    }
    public static String getPhotonImageUrl(String imageUrl, int width, int height, Quality quality) {
        if (TextUtils.isEmpty(imageUrl)) {
            return "";
        }

        // make sure it's valid
        int schemePos = imageUrl.indexOf("://");
        if (schemePos == -1) {
            return imageUrl;
        }

        // remove existing query string since it may contain params that conflict with the passed ones
        imageUrl = UrlUtils.removeQuery(imageUrl);

        // don't use with GIFs - photon breaks animated GIFs, and sometimes returns a GIF that
        // can't be read by BitmapFactory.decodeByteArray (used by Volley in ImageRequest.java
        // to decode the downloaded image)
        if (imageUrl.endsWith(".gif")) {
            return imageUrl;
        }

        // if this is an "mshots" url, skip photon and return it with a query that sets the width/height
        if (isMshotsUrl(imageUrl)) {
            return imageUrl + "?w=" + width + "&h=" + height;
        }

        // strip=all removes EXIF and other non-visual data from JPEGs
        String query = "?strip=all";

        switch (quality) {
            case HIGH:
                query += "&quality=100";
                break;
            case LOW:
                query += "&quality=35";
                break;
            default: // medium
                query += "&quality=65";
                break;
        }

        // if both width & height are passed use the "resize" param, use only "w" or "h" if just
        // one of them is set
        if (width > 0 && height > 0) {
            query += "&resize=" + width + "," + height;
        } else if (width > 0) {
            query += "&w=" + width;
        } else if (height > 0) {
            query += "&h=" + height;
        }

        // return passed url+query if it's already a photon url
        if (imageUrl.contains(".wp.com")) {
            if (imageUrl.contains("i0.wp.com") || imageUrl.contains("i1.wp.com") || imageUrl.contains("i2.wp.com"))
                return imageUrl + query;
        }

        // must use https for https image urls
        if (UrlUtils.isHttps(imageUrl)) {
            return "https://i0.wp.com/" + imageUrl.substring(schemePos+3, imageUrl.length()) + query;
        } else {
            return "http://i0.wp.com/" + imageUrl.substring(schemePos+3, imageUrl.length()) + query;
        }
    }
}
