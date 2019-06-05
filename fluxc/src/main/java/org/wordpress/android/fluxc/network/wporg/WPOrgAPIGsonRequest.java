package org.wordpress.android.fluxc.network.wporg;

import androidx.annotation.NonNull;

import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.network.rest.GsonRequest;

import java.util.Map;

public class WPOrgAPIGsonRequest<T> extends GsonRequest<T> {
    public WPOrgAPIGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                               Class<T> clazz, Listener<T> listener, BaseErrorListener errorListener) {
        super(method, params, body, url, clazz, null, listener, errorListener);
        // If it's a GET request, add the parameters to the URL
        if (method == Method.GET) {
            addQueryParameters(params);
        }
    }

    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        return error;
    }
}
