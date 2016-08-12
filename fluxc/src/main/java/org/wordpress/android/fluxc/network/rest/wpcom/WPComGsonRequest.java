package org.wordpress.android.fluxc.network.rest.wpcom;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.network.rest.GsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class WPComGsonRequest<T> extends GsonRequest<T> {
    public static final String REST_AUTHORIZATION_HEADER = "Authorization";
    public static final String REST_AUTHORIZATION_FORMAT = "Bearer %s";

    public WPComGsonRequest(int method, String url, Map<String, String> params, Class<T> clazz,
                            Listener<T> listener, ErrorListener errorListener) {
        super(method, params, url, clazz, listener, errorListener);
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
    public void deliverError(VolleyError volleyError) {
        super.deliverError(volleyError);

        // Fire OnAuthFailedListener if we receive it matches certain types of error
        if (volleyError.networkResponse != null && volleyError.networkResponse.statusCode >= 400 && mOnAuthFailedListener != null) {
            String jsonString;
            try {
                jsonString = new String(volleyError.networkResponse.data,
                        HttpHeaderParser.parseCharset(volleyError.networkResponse.headers));
            } catch (UnsupportedEncodingException e) {
                jsonString = "";
            }

            // TODO: we could use GSON here
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonString);
            } catch (JSONException e) {
                jsonObject = new JSONObject();
            }

            String apiResponseError = jsonObject.optString("error", "");
            if (apiResponseError.equals("authorization_required")
                    || apiResponseError.equals("invalid_token")
                    || apiResponseError.equals("access_denied")
                    || apiResponseError.equals("needs_2fa")) {
                AuthenticationError error = Authenticator.jsonErrorToAuthenticationError(jsonObject);
                AuthenticateErrorPayload payload = new AuthenticateErrorPayload(error, jsonObject.optString
                        ("error_description", ""));
                mOnAuthFailedListener.onAuthFailed(payload);
            }
        }
    }
}
