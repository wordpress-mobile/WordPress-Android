package org.wordpress.android.mocks;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request.Method;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.TestUtils;
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
        super(queue, token, REST_API_ENDPOINT_URL_V1);
    }

    public String getAbsoluteURL(String url) {
        return null;
    }

    public String getAbsoluteURL(String path, java.util.Map<String, String> params) {
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

    private VolleyError forgeVolleyErrorFromFilename(String filename) {
        String strData = fileToString(filename);
        byte[] data = new byte[0];
        if (strData != null) {
            data = strData.getBytes();
        }
        NetworkResponse networkResponse = new NetworkResponse(400, data, null, false);
        VolleyError ve = new VolleyError(networkResponse);
        return ve;
    }

    private TimeoutError forgeVolleyTimeoutError() {
        TimeoutError te = new TimeoutError();
        return te;
    }

    private String fileToString(String filename) {
        try {
            InputStream is = mContext.getAssets().open(filename);
            String data = TestUtils.convertStreamToString(is);
            AppLog.v(T.TESTS, "file read:" + filename);
            return data;
        } catch (IOException e) {
            AppLog.e(T.TESTS, "can't read file: " + filename + " - " + e.toString());
        }
        return null;
    }

    public RestRequest makeRequest(int method, String url, java.util.Map<String, String> params,
                                   RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": makeRequest(" + url + ")");
        RestRequest dummyReturnValue = new RestRequest(method, url, params, listener, errorListener);
        // URL example: https://public-api.wordpress.com/rest/v1/me
        // Filename: default-public-api-wordpress-com-rest-v1-me.json
        String filename = mPrefix + "-" + url.replace("https://", "").replace("/", "-").replace(".", "-").replace("?",
                "-") + ".json";

        if ("password-invalid".equals(mPrefix) && errorListener != null) {
            errorListener.onErrorResponse(forgeVolleyErrorFromFilename(filename));
            return dummyReturnValue;
        }

        if ("username-exists".equals(mPrefix) && errorListener != null) {
            errorListener.onErrorResponse(forgeVolleyErrorFromFilename(filename));
            return dummyReturnValue;
        }

        if ("timeout".equals(mPrefix) && errorListener != null) {
            errorListener.onErrorResponse(forgeVolleyTimeoutError());
            return dummyReturnValue;
        }

        if ("site-reserved".equals(mPrefix) && errorListener != null) {
            errorListener.onErrorResponse(forgeVolleyErrorFromFilename(filename));
            return dummyReturnValue;
        }

        String data = fileToString(filename);
        if (data == null) {
            AppLog.e(T.TESTS, "Can't read file: " + filename);
            throw new RuntimeException("Can't read file: " + filename);
        }

        try {
            JSONObject jsonObj = new JSONObject(data);
            listener.onResponse(jsonObj);
        } catch (JSONException je) {
            AppLog.e(T.TESTS, je.toString());
        }
        return dummyReturnValue;
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
