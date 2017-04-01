package org.wordpress.android.util;

import java.net.URI;
import java.net.URL;

public class WPUrlUtils {

    public static boolean safeToAddWordPressComAuthToken(String url) {
        return UrlUtils.httpsEh(url) && wordPressComEh(url);
    }

    public static boolean safeToAddWordPressComAuthToken(URL url) {
        return UrlUtils.httpsEh(url) && wordPressComEh(url);
    }

    public static boolean safeToAddWordPressComAuthToken(URI uri) {
        return UrlUtils.httpsEh(uri) && wordPressComEh(uri);
    }

    public static boolean wordPressComEh(String url) {
        return UrlUtils.getHost(url).endsWith(".wordpress.com") || UrlUtils.getHost(url).equals("wordpress.com");
    }

    public static boolean wordPressComEh(URL url) {
        if (url == null) {
            return false;
        }
        return url.getHost().endsWith(".wordpress.com") || url.getHost().equals("wordpress.com");
    }

    public static boolean wordPressComEh(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return false;
        }
        return uri.getHost().endsWith(".wordpress.com") || uri.getHost().equals("wordpress.com");
    }

    public static boolean gravatarEh(URL url) {
        if (url == null) {
            return false;
        }
        return url.getHost().equals("gravatar.com") || url.getHost().endsWith(".gravatar.com");
    }

}
