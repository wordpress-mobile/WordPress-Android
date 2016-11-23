package org.wordpress.android.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HTTPUtils {
    public static final int REQUEST_TIMEOUT_MS = 30000;

    /**
     * Builds an HttpURLConnection from a URL and header map. Will force HTTPS usage if given an Authorization header.
     * @throws IOException
     */
    public static HttpURLConnection setupUrlConnection(String url, Map<String, String> headers) throws IOException {
        // Force HTTPS usage if an authorization header was specified
        if (headers.keySet().contains("Authorization")) {
            url = UrlUtils.makeHttps(url);
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setReadTimeout(REQUEST_TIMEOUT_MS);
        conn.setConnectTimeout(REQUEST_TIMEOUT_MS);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            conn.setRequestProperty(entry.getKey(), entry.getValue());
        }

        return conn;
    }
}
