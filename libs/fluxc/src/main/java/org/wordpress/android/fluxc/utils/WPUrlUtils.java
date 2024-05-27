package org.wordpress.android.fluxc.utils;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.UrlUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;

/**
 * From wpandroid
 * TODO: move to wputils
 */
public class WPUrlUtils {
    public static boolean safeToAddWordPressComAuthToken(String url) {
        return UrlUtils.isHttps(url) && isWordPressCom(url);
    }

    public static boolean safeToAddWordPressComAuthToken(URL url) {
        return UrlUtils.isHttps(url) && isWordPressCom(url);
    }

    public static boolean safeToAddWordPressComAuthToken(URI uri) {
        return UrlUtils.isHttps(uri) && isWordPressCom(uri);
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

    public static boolean isWordPressCom(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return false;
        }
        return uri.getHost().endsWith(".wordpress.com") || uri.getHost().equals("wordpress.com");
    }

    public static boolean isGravatar(URL url) {
        if (url == null) {
            return false;
        }
        return url.getHost().equals("gravatar.com") || url.getHost().endsWith(".gravatar.com");
    }

    /**
     * Not strictly working with URLs, but this method exists already in WPUtils and will be removed
     * when that library is imported.
     */
    public static JSONObject volleyErrorToJSON(VolleyError volleyError) {
        if (volleyError == null || volleyError.networkResponse == null || volleyError.networkResponse.data == null
                || volleyError.networkResponse.headers == null) {
            return null;
        }

        String contentType = volleyError.networkResponse.headers.get("Content-Type");
        if (contentType == null || !contentType.equals("application/json")) {
            return null;
        }

        try {
            String response = new String(volleyError.networkResponse.data, "UTF-8");
            return new JSONObject(response);
        } catch (UnsupportedEncodingException e) {
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
}
