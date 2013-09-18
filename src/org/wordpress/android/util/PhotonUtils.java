package org.wordpress.android.util;

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
        if (imageUrl==null)
            throw new IllegalArgumentException("avatar imageUrl cannot be null");

        // if this isn't a gravatar image, return as resized photon image url
        if (!imageUrl.contains("gravatar.com"))
            return getPhotonImageUrl(imageUrl, avatarSz, avatarSz);

        // remove all other params
        int queryPos = imageUrl.indexOf("?");
        if (queryPos==-1)
            return imageUrl + String.format("?s=%d", avatarSz);

        return imageUrl.substring(0, queryPos) + String.format("?s=%d", avatarSz);
    }

    /*
     * returns a photon url for the passed image with the resize query set to the passed dimensions
     */
    public static String getPhotonImageUrl(String imageUrl, int width, int height) {
        if (imageUrl==null)
            throw new IllegalArgumentException("photon imageUrl cannot be null");

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
        if (imageUrl.contains("/mshots/"))
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
        if (imageUrl.contains("i0.wp.com"))
            return imageUrl + query;

        // must use https for https image urls
        if (UrlUtils.isHttps(imageUrl)) {
            return "https://i0.wp.com/" + imageUrl.substring(schemePos+3, imageUrl.length()) + query;
        } else {
            return "http://i0.wp.com/" + imageUrl.substring(schemePos+3, imageUrl.length()) + query;
        }
    }
}
