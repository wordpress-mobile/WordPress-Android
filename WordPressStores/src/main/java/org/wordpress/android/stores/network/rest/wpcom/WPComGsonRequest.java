package org.wordpress.android.stores.network.rest.wpcom;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.rest.GsonRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class WPComGsonRequest<T> extends GsonRequest<T> {
    private static final String REST_AUTHORIZATION_HEADER = "Authorization";
    private static final String REST_AUTHORIZATION_FORMAT = "Bearer %s";

    private final Map<String, String> mParams;

    public WPComGsonRequest(int method, String url, Map<String, String> params, Class<T> clazz,
                            Listener<T> listener, ErrorListener errorListener) {
        super(method, url, clazz, listener, errorListener);
        mParams = params;
    }

    public void removeAccessToken() {
        setAccessToken(null);
    }

    public void setAccessToken(String token) {
        if (token == null) {
            mHeaders.remove(REST_AUTHORIZATION_HEADER);
        } else {
            mHeaders.put(REST_AUTHORIZATION_HEADER, String.format(REST_AUTHORIZATION_FORMAT, token));
        }
    }

    @Override
    protected Map<String, String> getParams() {
        return mParams;
    }

    @Override
    public void deliverError(VolleyError error) {
        super.deliverError(error);

        // Fire OnAuthFailedListener if we receive an invalid token error
        if (error.networkResponse != null && error.networkResponse.statusCode >= 400 && mOnAuthFailedListener != null) {
            String jsonString;
            try {
                jsonString = new String(error.networkResponse.data,
                        HttpHeaderParser.parseCharset(error.networkResponse.headers));
            } catch (UnsupportedEncodingException e) {
                jsonString = "";
            }

            // TODO: we could use GSON here
            JSONObject responseObject;
            try {
                responseObject = new JSONObject(jsonString);
            } catch (JSONException e) {
                responseObject = new JSONObject();
            }

            String restError = responseObject.optString("error", "");
            if (restError.equals("authorization_required") || restError.equals("invalid_token")) {
                mOnAuthFailedListener.onAuthFailed(AuthError.INVALID_TOKEN);
            }
        }
    }
}
