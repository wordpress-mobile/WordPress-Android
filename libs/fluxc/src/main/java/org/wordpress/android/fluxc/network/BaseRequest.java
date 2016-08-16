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

    public static class GenericError {
        public GenericErrorType error;
        public String message;
        public GenericError(GenericErrorType error, @Nullable String message) {
            this.message = message;
            this.error = error;
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
            this.onErrorResponse(getGenericError(error), error);
        }

        private GenericError getGenericError(VolleyError volleyError) {
            if (volleyError.networkResponse == null) {
                return null;
            }
            if (volleyError instanceof TimeoutError) {
                return new GenericError(GenericErrorType.TIMEOUT, "");
            }
            switch (volleyError.networkResponse.statusCode) {
                case 404:
                    return new GenericError(GenericErrorType.NOT_FOUND, volleyError.getLocalizedMessage());
                case 451:
                    return new GenericError(GenericErrorType.CENSORED, volleyError.getLocalizedMessage());
                case 500:
                    return new GenericError(GenericErrorType.SERVER_ERROR, volleyError.getLocalizedMessage());
            }
            return null;
        }

        public abstract void onErrorResponse(@Nullable GenericError genericError, @NonNull VolleyError error);
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
