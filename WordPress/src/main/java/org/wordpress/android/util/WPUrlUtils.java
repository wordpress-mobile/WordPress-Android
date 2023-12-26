package org.wordpress.android.util;

import android.content.Context;

import org.wordpress.android.Constants;

import java.net.URL;

public class WPUrlUtils {
    public static boolean safeToAddWordPressComAuthToken(String url) {
        return UrlUtils.isHttps(url) && isWordPressCom(url);
    }

    public static boolean safeToAddWordPressComAuthToken(URL url) {
        return UrlUtils.isHttps(url) && isWordPressCom(url);
    }

    public static boolean safeToAddPrivateAtCookie(String url, String cookieHost) {
        return UrlUtils.getHost(url).equals(cookieHost);
    }

    public static boolean isWordPressCom(String url) {
        return UrlUtils.getHost(url).endsWith(".wordpress.com") || UrlUtils.getHost(url).equals("wordpress.com");
    }

    public static boolean isWordPressCom(URL url) {
        if (url == null) {
            return false;
        }
        return url.getHost().endsWith(".wordpress.com") || url.getHost().equals("wordpress.com");
    }

    public static String buildTermsOfServiceUrl(Context context) {
        return Constants.URL_TOS + "?locale=" + LanguageUtils.getPatchedCurrentDeviceLanguage(context);
    }
}
