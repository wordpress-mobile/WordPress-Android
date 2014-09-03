package com.wordpress.rest;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class RestClient {
    public static final String TAG = "WordPressREST";
    public static final String REST_API_ENDPOINT_URL = "https://public-api.wordpress.com/rest/v1/";
    public static final String PARAMS_ENCODING = "UTF-8";

    private RequestQueue mQueue;
    private String mAccessToken;
    private String mUserAgent;

    public RestClient(RequestQueue queue) {
        mQueue = queue;
    }

    public RestClient(RequestQueue queue, String token) {
        this(queue);
        mAccessToken = token;
    }

    public RestRequest get(String path, Listener<JSONObject> listener, ErrorListener errorListener) {
        return makeRequest(Method.GET, getAbsoluteURL(path), null, listener, errorListener);
    }

    public RestRequest post(String path, Map<String, String> body, Listener<JSONObject> listener,
                            ErrorListener errorListener) {
        return makeRequest(Method.POST, getAbsoluteURL(path), body, listener, errorListener);
    }

    public RestRequest makeRequest(int method, String url, Map<String, String> params, Listener<JSONObject> listener,
                                   ErrorListener errorListener) {
        RestRequest request = new RestRequest(method, url, params, listener, errorListener);
        request.setUserAgent(mUserAgent);
        request.setAccessToken(mAccessToken);
        return request;
    }

    public RestRequest send(RestRequest request) {
        // Volley send the request
        mQueue.add(request);
        return request;
    }

    public static String getAbsoluteURL(String url) {
        // if it already starts with our endpoint, let it pass through
        if (url.indexOf(REST_API_ENDPOINT_URL) == 0) {
            return url;
        }
        // if it has a leading slash, remove it
        if (url.indexOf("/") == 0) {
            url = url.substring(1);
        }
        // prepend the endpoint
        return String.format("%s%s", REST_API_ENDPOINT_URL, url);
    }

    public static String getAbsoluteURL(String path, Map<String, String> params) {
        String url = getAbsoluteURL(path);
        if (params != null) {
            // build a query string
            StringBuilder query = new StringBuilder();
            try {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    query.append(URLEncoder.encode(entry.getKey(), PARAMS_ENCODING));
                    query.append("=");
                    query.append(URLEncoder.encode(entry.getValue(), PARAMS_ENCODING));
                    query.append("&");
                }
            } catch (UnsupportedEncodingException uee) {
                throw new RuntimeException("Encoding not supported: " + PARAMS_ENCODING, uee);
            }
            url = String.format("%s?%s", url, query);
        }
        return url;
    }

    //Sets the User-Agent header to be sent with each future request.
    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }

    // Sets the auth token to be used in the request header
    public void setAccessToken(String token) {
        mAccessToken = token;
    }

    public boolean isAuthenticated() {
        return mAccessToken != null;
    }
}
