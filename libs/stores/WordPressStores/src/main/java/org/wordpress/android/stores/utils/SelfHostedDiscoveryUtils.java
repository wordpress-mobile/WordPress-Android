package org.wordpress.android.stores.utils;

import android.support.annotation.NonNull;
import android.webkit.URLUtil;

import org.wordpress.android.stores.utils.SelfHostedDiscoveryUtils.DiscoveryCallback.Error;
import org.wordpress.android.util.UrlUtils;

public class SelfHostedDiscoveryUtils {
    public interface DiscoveryCallback {
        enum Error {INVALID_SOURCE_URL, WORDPRESS_COM_SITE}

        void onError(Error error);

        void onSuccess(String xmlrpcEndpoint, String restEndpoint);
    }

    /**
     * Try to find the WP REST API and XMLRPC endpoints
     *
     * @param url      source xmlrpcEndpoint
     * @param callback will be called with results or error
     */
    public static void discoverSelfHostedEndPoint(@NonNull final String url,
                                                  @NonNull final DiscoveryCallback callback) {
        if (WPUrlUtils.isWordPressCom(url)) {
            callback.onError(Error.WORDPRESS_COM_SITE);
            return;
        }

        // Add http to the beginning of the URL if needed and convert IDN names to punycode if necessary
        final String cleanUrl = UrlUtils.addUrlSchemeIfNeeded(UrlUtils.convertUrlToPunycodeIfNeeded(url), false);

        if (!URLUtil.isValidUrl(cleanUrl)) {
            callback.onError(Error.INVALID_SOURCE_URL);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String xmlrpcEndpoint = discoverXMLRPCEndpoint(cleanUrl);
                String wprestEndpoint = discoverWPRESTEndpoint(cleanUrl);
                callback.onSuccess(xmlrpcEndpoint, wprestEndpoint);
            }
        }).start();
    }

    public static String discoverXMLRPCEndpoint(@NonNull String url) {
        // TODO: reuse existing discover
        return url + "/xmlrpc.php";
    }

    public static String discoverWPRESTEndpoint(@NonNull String url) {
        // TODO: See http://v2.wp-api.org/guide/discovery/
        return url + "/wp-json/wp/v2/";
    }
}
