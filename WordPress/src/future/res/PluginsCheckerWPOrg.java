package org.wordpress.android.ui.accounts.helpers;

import android.text.TextUtils;
import android.webkit.URLUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * This class can test a WordPress installation for plugins/themes that cause problems with WordPress for Android connecting correctly.
 *
 * The tool is a black box scanner, it allows remote testing of a WordPress installation.
 * Find problematic plugins and themes, configuration issues and other glitches that can cause problems with our apps.
 *
 */
public class PluginsCheckerWPOrg {
    private final static String BB_PLUGINS_LIST_URL = "https://raw.githubusercontent.com/wordpress-mobile/app-blocking-plugins/master/xmlrpc-plugins.json";

    // Do not use the WP-APP user agent. Requests could be blocked if made from our app UA.
    private final static String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.116 Safari/537.36";

    /** Socket timeout in milliseconds for the requests */
    private static final int REQUEST_TIMEOUT_MS = 30000;

    String mOriginalURL;

    public PluginsCheckerWPOrg(String url) {
        mOriginalURL = url;
    }

    /**
     * This routine does a black box scanning on the plugis folder of the remote host, and tries to find plugins that cause
     * problems connecting to the host from one of our mobile apps.
     */
    public List<Plugin> checkForPlugins() {
        String responseHTML = downloadPluginsList();
        if (TextUtils.isEmpty(responseHTML)) {
            AppLog.w(AppLog.T.NUX, "Without the list we can't check if the host has some BB plugins installed on it.");
            return null;
        }

        JSONArray listOfPlugins;
        try {
            listOfPlugins = new JSONArray(responseHTML);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.NUX, "Error while parsing the list of plugins returned from the server.", e);
            return null;
        }

        // we have the list. Start the process of checking for plugins
        String baseURL = getBaseURL(mOriginalURL);

        if (!baseURL.contains("/plugins")) {
            baseURL = baseURL + "/wp-content/plugins/";
        }

        AppLog.i(AppLog.T.NUX, "The calculated plugins URL is the following: " + baseURL);

        if (!URLUtil.isValidUrl(baseURL)) {
            AppLog.w(AppLog.T.NUX, "The calculated plugins URL isn't a valid URL. Returning now.");
            return null;
        }

        int respCode = openConnection(baseURL);
        if (respCode != HttpURLConnection.HTTP_OK && respCode != 401 && respCode != 403) {
            AppLog.w(AppLog.T.NUX, "The request to plugins URL returned with an unexpected HTTP error code. Returning now.");
            return null;
        }

        AppLog.i(AppLog.T.NUX, "Start checking the plugins list..");
        ArrayList<Plugin> listOfBBPlugins = new ArrayList<>();
        for (int i=0; i<listOfPlugins.length(); i++) {
            try {
                JSONObject currentObject = listOfPlugins.getJSONObject(i);
                String currentPluginURL = baseURL + currentObject.getString("name") + "/";
                AppLog.i(AppLog.T.NUX, "Testing the following plugin " + currentPluginURL);
                respCode = openConnection(currentPluginURL);
                if (respCode != HttpURLConnection.HTTP_NOT_FOUND) {
                    Plugin currentBBPLugin = new Plugin();
                    currentBBPLugin.name = currentObject.getString("name");
                    currentBBPLugin.url = currentObject.getString("url");
                    listOfBBPlugins.add(currentBBPLugin);
                    AppLog.i(AppLog.T.NUX, "Plugin found on the server: " + currentObject.getString("name"));
                } else {
                    AppLog.i(AppLog.T.NUX, "Plugin NOT found on the server: " + currentObject.getString("name"));
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NUX, "Error while parsing the " + i + " in the list of plugins returned from the server.", e);
            }
        }

        return listOfBBPlugins;
    }

    private String downloadPluginsList() {
        HttpURLConnection conn = null;
        String response = "";
        try {
            URL url = new URL(BB_PLUGINS_LIST_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(REQUEST_TIMEOUT_MS);
            conn.setConnectTimeout(REQUEST_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "close");
            conn.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String inLine;
            while ((inLine = in.readLine()) != null) {
                response += inLine;
            }
        } catch (Exception e) {
            AppLog.e(AppLog.T.NUX, "Error while downloading the plugins list from the server...", e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
            }
        }
        return response;
    }

    private int openConnection(String url) {
        HttpURLConnection conn = null;
        try {
            URL requestURL = new URL(url);
            conn = (HttpURLConnection) requestURL.openConnection();
            conn.setReadTimeout(REQUEST_TIMEOUT_MS);
            conn.setConnectTimeout(REQUEST_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setUseCaches(false);
            conn.setRequestProperty("Connection", "close");
            conn.setRequestMethod("GET");
            conn.connect();
            int respCode = conn.getResponseCode();
            return respCode;
        } catch (Exception e) {
            AppLog.e(AppLog.T.NUX, "Error while checking for the plugins folder on the server.", e);
        } finally {
            try {
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (Exception e) {
            }

        }
        return 0;
    }

    private String getBaseURL(String url) {
        String sanitizedURL = url;
        try {
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "wp-login.php" );
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-admin" );
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/wp-content" );
            sanitizedURL = truncateURLAtPrefix(sanitizedURL, "/xmlrpc.php" );
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.NUX, "Can't clean the original url: " + sanitizedURL, e);
        }
        while (sanitizedURL.endsWith("/")) {
            sanitizedURL = sanitizedURL.substring(0, sanitizedURL.length() - 1);
        }
        return sanitizedURL;
    }

    private String truncateURLAtPrefix(String url, String prefix) throws IllegalArgumentException {
        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("Input URL " + url + " is not valid!");
        }
        if (TextUtils.isEmpty(prefix)) {
            throw new IllegalArgumentException("Input prefix is empty or null");
        }

        if (url.indexOf(prefix) > 0) {
            url = url.substring(0, url.indexOf(prefix));
        }

        if (!URLUtil.isValidUrl(url)) {
            throw new IllegalArgumentException("The new URL " + url + " is not valid!");
        }

        return url;
    }

    public class Plugin {
        String name;
        String url;
    }
}
