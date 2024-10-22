package org.wordpress.android.fluxc.network.rest.wpcom;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.network.rest.GsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTimeoutRequestHandler;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

public class WPComGsonRequest<T> extends GsonRequest<T> {
    public interface WPComErrorListener {
        void onErrorResponse(@NonNull WPComGsonNetworkError error);
    }

    public interface OnJetpackTunnelTimeoutListener {
        void onJetpackTunnelTimeout(OnJetpackTimeoutError event);
    }

    public static class OnJetpackTimeoutError {
        public String apiPath;
        public int timesRetried;

        public OnJetpackTimeoutError(String apiPath, int timesRetried) {
            this.apiPath = apiPath;
            this.timesRetried = timesRetried;
        }
    }

    private OnJetpackTunnelTimeoutListener mOnJetpackTunnelTimeoutListener;

    private int mNumManualRetries = 0;

    public static final String REST_AUTHORIZATION_HEADER = "Authorization";
    public static final String REST_AUTHORIZATION_FORMAT = "Bearer %s";

    public static class WPComGsonNetworkError extends BaseNetworkError {
        @NonNull public String apiError;
        public WPComGsonNetworkError(@NonNull BaseNetworkError error) {
            super(error);
            this.apiError = "";
        }
    }

    private WPComGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                             Class<T> clazz, Type type, Listener<T> listener, BaseErrorListener errorListener) {
        super(method, params, body, url, clazz, type, listener, errorListener);
        addQueryParameters(params);
    }

    private WPComGsonRequest(int method, String url, Map<String, String> params, Map<String, Object> body,
                             Class<T> clazz, Type type, Listener<T> listener, BaseErrorListener errorListener,
                             GsonBuilder customGsonBuilder) {
        super(method, params, body, url, clazz, type, listener, errorListener, customGsonBuilder);
        addQueryParameters(params);
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

    // Overloaded method to include custom GsonBuilder
    public static <T> WPComGsonRequest<T> buildGetRequest(String url, Map<String, String> params, Class<T> clazz,
                                                          Listener<T> listener, WPComErrorListener errorListener,
                                                          GsonBuilder customGsonBuilder) {
        return new WPComGsonRequest<>(Method.GET, url, params, null, clazz, null, listener,
                wrapInBaseListener(errorListener), customGsonBuilder);
    }

    // Overloaded method to include custom GsonBuilder
    public static <T> WPComGsonRequest<T> buildGetRequest(String url, Map<String, String> params, Type type,
                                                          Listener<T> listener, WPComErrorListener errorListener,
                                                          GsonBuilder customGsonBuilder) {
        return new WPComGsonRequest<>(Method.GET, url, params, null, null, type, listener,
                wrapInBaseListener(errorListener), customGsonBuilder);
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

    /**
     * Creates a new JSON-formatted POST request.
     * @param url the request URL
     * @param params the parameters to append to the request URL
     * @param body the content body, which will be converted to JSON using {@link com.google.gson.Gson Gson}
     * @param clazz the class defining the expected response
     * @param listener the success listener
     * @param errorListener the error listener
     */
    public static <T> WPComGsonRequest<T> buildPostRequest(String url, Map<String, String> params,
                                                           Map<String, Object> body, Class<T> clazz,
                                                           Listener<T> listener, WPComErrorListener errorListener) {
        return new WPComGsonRequest<>(Method.POST, url, params, body, clazz, null, listener,
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

    void setOnJetpackTunnelTimeoutListener(OnJetpackTunnelTimeoutListener onJetpackTunnelTimeoutListener) {
        mOnJetpackTunnelTimeoutListener = onJetpackTunnelTimeoutListener;
    }

    /**
     * Mark that this request has been retried manually (by duplicating and re-enqueuing it).
     */
    public void increaseManualRetryCount() {
        mNumManualRetries++;
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

            if (JetpackTimeoutRequestHandler.isJetpackTimeoutError(returnedError)) {
                OnJetpackTimeoutError onJetpackTimeoutError = null;
                if (getMethod() == Method.GET && getParams() != null) {
                    onJetpackTimeoutError = new OnJetpackTimeoutError(getParams().get("path"), mNumManualRetries);
                } else if (getMethod() == Method.POST && getBodyAsMap() != null) {
                    Object pathValue = getBodyAsMap().get("path");
                    if (pathValue != null) {
                        onJetpackTimeoutError = new OnJetpackTimeoutError(pathValue.toString(), mNumManualRetries);
                    }
                }
                if (onJetpackTimeoutError != null) {
                    mOnJetpackTunnelTimeoutListener.onJetpackTunnelTimeout(onJetpackTimeoutError);
                }
            }
        }

        return returnedError;
    }
}
