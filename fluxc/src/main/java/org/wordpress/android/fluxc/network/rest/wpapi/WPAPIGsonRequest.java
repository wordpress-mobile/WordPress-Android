package org.wordpress.android.fluxc.network.rest.wpapi;

import androidx.annotation.NonNull;

import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.network.rest.GsonRequest;
import org.wordpress.android.util.AppLog;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

public class WPAPIGsonRequest<T> extends GsonRequest<T> {
    public WPAPIGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                             Class<T> clazz, Listener<T> listener, BaseErrorListener errorListener) {
        super(method, params, body, url, clazz, null, listener, errorListener);
        // If it's a GET request, add the parameters to the URL
        if (method == Method.GET) {
            addQueryParameters(params);
        }
    }
    public WPAPIGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                            Type type, Listener<T> listener, BaseErrorListener errorListener) {
        super(method, params, body, url, null, type, listener, errorListener);
        // If it's a GET request, add the parameters to the URL
        if (method == Method.GET) {
            addQueryParameters(params);
        }
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        if (error.hasVolleyError() && error.volleyError.networkResponse != null) {
            String jsonString;
            try {
                jsonString = new String(error.volleyError.networkResponse.data,
                        HttpHeaderParser.parseCharset(error.volleyError.networkResponse.headers));
                JSONObject jsonObject = new JSONObject(jsonString);

                String errorMessage = jsonObject.optString("message", "");
                if (!errorMessage.isEmpty()) {
                    error.message = errorMessage;
                }
            } catch (UnsupportedEncodingException | JSONException e) {
                AppLog.w(AppLog.T.API, e.toString());
            }
        }

        return error;
    }}
