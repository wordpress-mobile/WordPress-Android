package org.wordpress.android.fluxc.network.rest.wpcom;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.network.rest.GsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

public class WPComGsonRequest<T> extends GsonRequest<T> {
    public interface WPComErrorListener {
        void onErrorResponse(@NonNull WPComGsonNetworkError error);
    }

    public static final String REST_AUTHORIZATION_HEADER = "Authorization";
    public static final String REST_AUTHORIZATION_FORMAT = "Bearer %s";

    public static class WPComGsonNetworkError extends BaseNetworkError {
        public String apiError;
        public WPComGsonNetworkError(BaseNetworkError error) {
            super(error);
            this.apiError = "";
        }
    }

    private WPComGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                             Class<T> clazz, Type type, Listener<T> listener, BaseErrorListener errorListener) {
        super(method, params, body, url, clazz, type, listener, errorListener);
        // If it's a GET request, add the parameters to the URL
        if (method == Method.GET) {
            addQueryParameters(params);
        }
    }

    /**
     * Creates a new GET request.
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    public static <T> WPComGsonRequest<T> buildGetRequest(String url, Map<String, String> params, Class<T> clazz,
                                                          Listener<T> listener, WPComErrorListener errorListener) {
        return new WPComGsonRequest<>(Method.GET, url, params, null, clazz, null, listener,
                wrapInBaseListener(errorListener));
    }

    public static <T> WPComGsonRequest<T> buildGetRequest(String url, Map<String, String> params, Type type,
                                                          Listener<T> listener, WPComErrorListener errorListener) {
        return new WPComGsonRequest<>(Method.GET, url, params, null, null, type, listener,
                wrapInBaseListener(errorListener));
    }

    /**
     * Creates a new JSON-formatted POST request.
     * @param url the request URL
     * @param body the content body, which will be converted to JSON using {@link com.google.gson.Gson Gson}
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    public static <T> WPComGsonRequest<T> buildPostRequest(String url, Map<String, Object> body, Class<T> clazz,
                                                           Listener<T> listener, WPComErrorListener errorListener) {
        return new WPComGsonRequest<>(Method.POST, url, null, body, clazz, null, listener,
                wrapInBaseListener(errorListener));
    }

    public static <T> WPComGsonRequest<T> buildPostRequest(String url, Map<String, Object> body, Type type,
                                                           Listener<T> listener, WPComErrorListener errorListener) {
        return new WPComGsonRequest<>(Method.POST, url, null, body, null, type, listener,
                wrapInBaseListener(errorListener));
    }

    public static <T> WPComGsonRequest<T> buildFormPostRequest(String url, Map<String, String> params, Type type,
                                                           Listener<T> listener, WPComErrorListener errorListener) {
        return new WPComGsonRequest<>(Method.POST, url, params, null, null, type, listener,
                wrapInBaseListener(errorListener));
    }

    private static BaseErrorListener wrapInBaseListener(final WPComErrorListener wpComErrorListener) {
        return new BaseErrorListener() {
            @Override
            public void onErrorResponse(@NonNull BaseNetworkError error) {
                if (wpComErrorListener != null) {
                    wpComErrorListener.onErrorResponse((WPComGsonNetworkError) error);
                }
            }
        };
    }

    private String addDefaultParameters(String url) {
        return url;
    }

    public void setAccessToken(String token) {
        if (token == null) {
            mHeaders.remove(REST_AUTHORIZATION_HEADER);
        } else {
            mHeaders.put(REST_AUTHORIZATION_HEADER, String.format(REST_AUTHORIZATION_FORMAT, token));
        }
    }
    @Override
    public BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error) {
        WPComGsonNetworkError returnedError = new WPComGsonNetworkError(error);
        if (error.hasVolleyError() && error.volleyError.networkResponse != null
                && error.volleyError.networkResponse.statusCode >= 400) {
            String jsonString;
            try {
                jsonString = new String(error.volleyError.networkResponse.data,
                        HttpHeaderParser.parseCharset(error.volleyError.networkResponse.headers));
            } catch (UnsupportedEncodingException e) {
                jsonString = "";
            }

            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(jsonString);
            } catch (JSONException e) {
                jsonObject = new JSONObject();
            }
            String apiError = jsonObject.optString("error", "");
            if (TextUtils.isEmpty(apiError)) {
                // WP V2 endpoints use "code" instead of "error"
                apiError = jsonObject.optString("code", "");
            }
            String apiMessage = jsonObject.optString("message", "");
            if (TextUtils.isEmpty(apiMessage)) {
                // Auth endpoints use "error_description" instead of "message"
                apiMessage = jsonObject.optString("error_description", "");
            }

            // Augment BaseNetworkError by what we can parse from the response
            returnedError.apiError = apiError;
            returnedError.message = apiMessage;

            // Check if we know this error
            if (apiError.equals("authorization_required") || apiError.equals("invalid_token")
                    || apiError.equals("access_denied") || apiError.equals("needs_2fa")) {
                AuthenticationError authError = new AuthenticationError(
                        Authenticator.wpComApiErrorToAuthenticationError(apiError, returnedError.message),
                        returnedError.message);
                AuthenticateErrorPayload payload = new AuthenticateErrorPayload(authError);
                mOnAuthFailedListener.onAuthFailed(payload);
            }
        }

        return returnedError;
    }
}
