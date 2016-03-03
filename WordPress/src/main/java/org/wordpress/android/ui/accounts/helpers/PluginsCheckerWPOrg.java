package org.wordpress.android.ui.accounts.helpers;

import android.text.TextUtils;
import android.webkit.URLUtil;

import com.android.volley.TimeoutError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPRestClient;
import org.xmlrpc.android.ApiHelper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PluginsCheckerWPOrg {
    //TODO: update the address with a valid .org site
    private final static String BB_PLUGINS_LIST_URL = "http://www.eritreo.it/test.json";

    String mOriginalURL;

    public PluginsCheckerWPOrg(String url) {
        mOriginalURL = url;
    }

    public List<Plugin> checkForPlugins() {
        String responseHTML = null;
        try {
            responseHTML = ApiHelper.getResponse(BB_PLUGINS_LIST_URL);
        } catch (IOException | TimeoutError e) {
            AppLog.e(AppLog.T.NUX, "Can't download the plugins list from the server!", e);
        }
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
            baseURL = baseURL + "/plugins/";
        }

        if (!URLUtil.isValidUrl(baseURL)) {
            return null;
        }

        int respCode = openConnection(baseURL);
        if (respCode != HttpURLConnection.HTTP_OK && respCode != 401 && respCode != 403) {
            return null;
        }

        ArrayList<Plugin> listOfBBPlugins = new ArrayList<>();
        for (int i=0; i<listOfPlugins.length(); i++) {
            try {
                JSONObject currentObject = listOfPlugins.getJSONObject(i);
                respCode = openConnection(baseURL + currentObject.getString("name") + "/");
                if (respCode != HttpURLConnection.HTTP_NOT_FOUND) {
                    Plugin currentBBPLugin = new Plugin();
                    currentBBPLugin.name = currentObject.getString("name");
                    currentBBPLugin.url = currentObject.getString("url");
                    listOfBBPlugins.add(currentBBPLugin);
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NUX, "Error while parsing the " + i + " in the list of plugins returned from the server.", e);
            }
        }

        return listOfBBPlugins;
    }

    private int openConnection(String url) {
        HttpURLConnection conn = null;
        try {
            URL requestURL = new URL(url);
            conn = (HttpURLConnection) requestURL.openConnection();
            conn.setReadTimeout(WPRestClient.REST_TIMEOUT_MS);
            conn.setConnectTimeout(WPRestClient.REST_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", WordPress.getUserAgent());
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
