package org.wordpress.android.fluxc.network.discovery;

import android.text.TextUtils;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.util.AppLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscoveryUtils {
    /**
     * Strip known unnecessary paths from XML-RPC URL and remove trailing slashes
     */
    @NonNull
    public static String stripKnownPaths(@NonNull String url) {
        // Remove 'wp-login.php' if available in the URL
        String sanitizedURL = truncateUrl(url, "wp-login.php");

        // Remove '/wp-admin' if available in the URL
        sanitizedURL = truncateUrl(sanitizedURL, "/wp-admin");

        // Remove '/wp-content' if available in the URL
        sanitizedURL = truncateUrl(sanitizedURL, "/wp-content");

        sanitizedURL = truncateUrl(sanitizedURL, "/xmlrpc.php?rsd");

        // remove any trailing slashes
        while (sanitizedURL.endsWith("/")) {
            sanitizedURL = sanitizedURL.substring(0, sanitizedURL.length() - 1);
        }

        return sanitizedURL;
    }

    /**
     * Truncate a string beginning at the marker
     * @param url input string
     * @param marker the marker to begin the truncation from
     * @return new string truncated to the beginning of the marker or the input string if marker is not found
     */
    @NonNull
    public static String truncateUrl(@NonNull String url, @NonNull String marker) {
        if (TextUtils.isEmpty(marker) || !url.contains(marker)) {
            return url;
        }

        final String newUrl = url.substring(0, url.indexOf(marker));

        return URLUtil.isValidUrl(newUrl) ? newUrl : url;
    }

    /**
     * Append 'xmlrpc.php' if missing in the URL
     */
    @NonNull
    public static String appendXMLRPCPath(@NonNull String url) {
        // Don't use 'ends' here! Some hosting wants parameters passed to baseURL/xmlrpc-php?my-authcode=XXX
        if (url.contains("xmlrpc.php")) {
            return url;
        } else {
            return url + "/xmlrpc.php";
        }
    }

    /**
     * Verify that the response of system.listMethods matches the expected list of available XML-RPC methods
     */
    public static boolean validateListMethodsResponse(@Nullable Object[] availableMethods) {
        if (availableMethods == null) {
            AppLog.e(AppLog.T.NUX, "The response of system.listMethods was empty!");
            return false;
        }
        // validate xmlrpc methods
        String[] requiredMethods = {"wp.getProfile", "wp.getUsersBlogs", "wp.getPage", "wp.getCommentStatusList",
                "wp.newComment", "wp.editComment", "wp.deleteComment", "wp.getComments", "wp.getComment",
                "wp.getOptions", "wp.uploadFile", "wp.newCategory",
                "wp.getTags", "wp.getCategories", "wp.editPage", "wp.deletePage",
                "wp.newPage", "wp.getPages"};

        for (String currentRequiredMethod : requiredMethods) {
            boolean match = false;
            for (Object currentAvailableMethod : availableMethods) {
                if ((currentAvailableMethod).equals(currentRequiredMethod)) {
                    match = true;
                    break;
                }
            }

            if (!match) {
                AppLog.e(AppLog.T.NUX, "The following XML-RPC method: " + currentRequiredMethod + " is missing on the"
                                       + " server.");
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether given network error is a 401 Unauthorized HTTP error
     */
    public static boolean isHTTPAuthErrorMessage(@Nullable Exception e) {
        return e != null && e.getMessage() != null && e.getMessage().contains("401");
    }

    /**
     * Find the XML-RPC endpoint for the WordPress API.
     *
     * @return XML-RPC endpoint for the specified site, or null if unable to discover endpoint.
     */
    @Nullable
    public static String getXMLRPCApiLink(@Nullable String html) {
        Pattern xmlrpcLink = Pattern.compile("<api\\s*?name=\"WordPress\".*?apiLink=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        if (html != null) {
            Matcher matcher = xmlrpcLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null; // never found the api link tag
    }

    /**
     * Find the XML-RPC endpoint by using the pingback tag
     *
     * @return String XML-RPC url
     */
    @Nullable
    public static String getXMLRPCPingback(@Nullable String html) {
        Pattern pingbackLink = Pattern.compile(
                "<link\\s*?rel=\"pingback\"\\s*?href=\"(.*?)\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        if (html != null) {
            Matcher matcher = pingbackLink.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
