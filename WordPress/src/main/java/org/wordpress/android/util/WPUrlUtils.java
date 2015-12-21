package org.wordpress.android.util;

import java.net.URI;
import java.net.URL;

public class WPUrlUtils {

    public static synchronized boolean safeToAddWordPressComAuthToken(String url) {
        return UrlUtils.isHttps(url) && isWordPressCom(url);
    }

    public static synchronized boolean safeToAddWordPressComAuthToken(URL url) {
        return UrlUtils.isHttps(url) && isWordPressCom(url);
    }

    public static synchronized boolean safeToAddWordPressComAuthToken(URI uri) {
        return UrlUtils.isHttps(uri) && isWordPressCom(uri);
    }

    public static synchronized boolean isWordPressCom(String url) {
        return UrlUtils.getHost(url).endsWith(".wordpress.com") || UrlUtils.getHost(url).equals("wordpress.com");
    }

    public static synchronized boolean isWordPressCom(URL url) {
        if (url == null) {
            return false;
        }
        return url.getHost().endsWith(".wordpress.com") || url.getHost().equals("wordpress.com");
    }

    public static synchronized boolean isWordPressCom(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return false;
        }
        return uri.getHost().endsWith(".wordpress.com") || uri.getHost().equals("wordpress.com");
    }

    public static synchronized boolean isGravatar(URL url) {
        if (url == null) {
            return false;
        }
        return url.getHost().endsWith("gravatar.com");
    }


}