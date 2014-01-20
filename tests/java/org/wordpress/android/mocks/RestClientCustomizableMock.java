package org.wordpress.android.mocks;

import android.content.Context;

import com.android.volley.Request.Method;
import com.google.gson.Gson;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.IOException;
import java.io.InputStream;

public class RestClientCustomizableMock extends RestClient {
    private Context mContext;
    private String mPrefix;

    public void setContextAndPrefix(Context context, String prefix) {
        mContext = context;
        mPrefix = prefix;
    }

    public void setPrefix(String prefix) {
        mPrefix = prefix;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public RestClientCustomizableMock(com.android.volley.RequestQueue queue) {
        super(queue);
    }

    public RestClientCustomizableMock(com.android.volley.RequestQueue queue, String token) {
        super(queue, token);
    }

    public static String getAbsoluteURL(String url) {
        return null;
    }

    public static String getAbsoluteURL(String path, java.util.Map<String, String> params) {
        return null;
    }

    public RestRequest get(String path, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": get(" + path + ")");
        return new RestRequest(Method.GET, path, null, listener, errorListener);
    }

    public RestRequest post(String path, java.util.Map<String, String> body, RestRequest.Listener listener,
                            RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": post(" + path + ")");
        return new RestRequest(Method.POST, path, body, listener, errorListener);
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public RestRequest makeRequest(int method, String url, java.util.Map<String, String> params,
                                   RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": makeRequest(" + url + ")");
        Gson gson = new Gson();
        // URL example: https://public-api.wordpress.com/rest/v1/me
        // Filename: default-public-api-wordpress-com-rest-v1-me.json
        String filename = mPrefix + "-" + url.replace("https://", "").replace("/", "-").replace(".", "-") + ".json";
        try {
            InputStream is = mContext.getAssets().open(filename);
            String data = convertStreamToString(is);
            AppLog.v(T.TESTS, "file read:" + filename);
            JSONObject jsonObj = new JSONObject(data);
            listener.onResponse(jsonObj);
        } catch (IOException e) {
            AppLog.e(T.TESTS, "can't read file: " + filename + " - " + e.toString());
        } catch (JSONException je) {
            AppLog.e(T.TESTS, "can't read file: " + filename + " - " + je.toString());
        }
        return new RestRequest(method, url, params, listener, errorListener);
    }

    public RestRequest send(RestRequest request) {
        return request;
    }

    public void setUserAgent(String userAgent) {
    }

    public void setAccessToken(String token) {
    }

    public boolean isAuthenticated() {
        return true;
    }
}
