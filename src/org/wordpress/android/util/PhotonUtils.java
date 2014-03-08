package org.wordpress.android.util;

import android.text.TextUtils;

/**
 * Created by nbradbury on 7/11/13.
 * routines related to the Photon API
 * http://developer.wordpress.com/docs/photon/
 */
public class PhotonUtils {

    private PhotonUtils() {
        throw new AssertionError();
    }

    /*
     * gravatars often contain the ?s= parameter which determines their size - detect this and
     * replace it with a new ?s= parameter which requests the avatar at the exact size needed
     */
    public static String fixAvatar(final String imageUrl, int avatarSz) {
        if (TextUtils.isEmpty(imageUrl))
            return "";

        // if this isn't a gravatar image, return as resized photon image url
        if (!imageUrl.contains("gravatar.com"))
            return getPhotonImageUrl(imageUrl, avatarSz, avatarSz);

        // remove all other params, then add query string for size and "mystery man" default
        return UrlUtils.removeQuery(imageUrl) + String.format("?s=%d&d=mm", avatarSz);
    }

    /*
    * returns true if the passed url is an obvious "mshots" url
    */
    public static boolean isMshotsUrl(final String imageUrl) {
        return (imageUrl != null && imageUrl.contains("/mshots/"));
    }

    /*
     * returns a photon url for the passed image with the resize query set to the passed dimensions
     */
    public static String getPhotonImageUrl(String imageUrl, int width, int height) {
        if (TextUtils.isEmpty(imageUrl))
            return "";

        // make sure it's valid
        int schemePos = imageUrl.indexOf("://");
        if (schemePos==-1)
            return imageUrl;

        // remove existing query string since it may contain params that conflict with the passed ones
        imageUrl = UrlUtils.removeQuery(imageUrl);

        // don't use with GIFs - photon breaks animated GIFs, and sometimes returns a GIF that
        // can't be read by BitmapFactory.decodeByteArray (used by Volley in ImageRequest.java
        // to decode the downloaded image)
        // ex: http://i0.wp.com/lusianne.files.wordpress.com/2013/08/193.gif?resize=768,320
        if (imageUrl.endsWith(".gif"))
            return imageUrl;

        // if this is an "mshots" url, skip photon and return it with a query that sets the width/height
        // (these are screenshots of the blog that often appear in freshly pressed posts)
        // see http://wp.tutsplus.com/tutorials/how-to-generate-website-screenshots-for-your-wordpress-site/
        if (isMshotsUrl(imageUrl))
            return imageUrl + String.format("?w=%d&h=%d", width, height);

        // if both width & height are passed use the "resize" param, use only "w" or "h" if just
        // one of them is set, otherwise no query string
        final String query;
        if (width > 0 && height > 0) {
            query = String.format("?resize=%d,%d", width, height);
        } else if (width > 0) {
            query = String.format("?w=%d", width);
        } else if (height > 0) {
            query = String.format("?h=%d", height);
        } else {
            query = "";
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
