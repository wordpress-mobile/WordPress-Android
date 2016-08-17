package org.wordpress.android.fluxc.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.ErrorListener;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import org.wordpress.android.fluxc.network.rest.wpcom.auth.Authenticator.AuthenticateErrorPayload;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseRequest<T> extends com.android.volley.Request<T> {
    public interface OnAuthFailedListener {
        void onAuthFailed(AuthenticateErrorPayload errorType);
    }

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final int DEFAULT_REQUEST_TIMEOUT = 30000;

    protected OnAuthFailedListener mOnAuthFailedListener;
    protected final Map<String, String> mHeaders = new HashMap<>(2);

    public static class BaseNetworkError {
        public GenericErrorType error;
        public String message;
        public VolleyError volleyError;

        public BaseNetworkError(GenericErrorType error, @Nullable String message, @NonNull VolleyError volleyError) {
            this.message = message;
            this.error = error;
            this.volleyError = volleyError;
        }

        public BaseNetworkError(@NonNull VolleyError volleyError) {
            this.volleyError = volleyError;
        }
        public boolean isGeneric() {
            return error != null;
        }
    }

    public enum GenericErrorType {
        NOT_FOUND,
        CENSORED,
        SERVER_ERROR,
        TIMEOUT
    }

    public static abstract class BaseErrorListener implements ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            this.onErrorResponse(getGenericError(error));
        }

        private @NonNull BaseNetworkError getGenericError(VolleyError volleyError) {
            if (volleyError.networkResponse == null) {
                return new BaseNetworkError(volleyError);
            }
            if (volleyError instanceof TimeoutError) {
                return new BaseNetworkError(GenericErrorType.TIMEOUT, "", volleyError);
            }
            switch (volleyError.networkResponse.statusCode) {
                case 404:
                    return new BaseNetworkError(GenericErrorType.NOT_FOUND, volleyError.getMessage(), volleyError);
                case 451:
                    return new BaseNetworkError(GenericErrorType.CENSORED, volleyError.getMessage(), volleyError);
                case 500:
                    return new BaseNetworkError(GenericErrorType.SERVER_ERROR, volleyError.getMessage(), volleyError);
            }
            return new BaseNetworkError(volleyError);
        }

        public abstract void onErrorResponse(@NonNull BaseNetworkError error);
    }

    public BaseRequest(int method, String url, BaseErrorListener listener) {
        super(method, url, listener);
        // Make sure all our custom Requests are never cached.
        setShouldCache(false);
        setRetryPolicy(new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    @Override
    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public void setHTTPAuthHeaderOnMatchingURL(HTTPAuthManager httpAuthManager) {
        HTTPAuthModel httpAuthModel = httpAuthManager.getHTTPAuthModel(getUrl());
        if (httpAuthModel != null) {
            String creds = String.format("%s:%s", httpAuthModel.getUsername(), httpAuthModel.getPassword());
            String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
            mHeaders.put("Authorization", auth);
        }
    }

    public void setOnAuthFailedListener(OnAuthFailedListener onAuthFailedListener) {
        mOnAuthFailedListener = onAuthFailedListener;
    }

    public void setUserAgent(String userAgent) {
        mHeaders.put(USER_AGENT_HEADER, userAgent);
    }

    /**
     * Convenience method for setting a {@link com.android.volley.RetryPolicy} with no retries.
     */
    public void disableRetries() {
        setRetryPolicy(new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }


}
