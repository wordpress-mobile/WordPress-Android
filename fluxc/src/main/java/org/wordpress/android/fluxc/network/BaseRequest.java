package org.wordpress.android.fluxc.network;

import android.util.Base64;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.ErrorListener;
import com.android.volley.RetryPolicy;

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
    protected RetryPolicy mRetryPolicy;
    protected final Map<String, String> mHeaders = new HashMap<>(2);

    public BaseRequest(int method, String url, ErrorListener listener) {
        super(method, url, listener);
        // Make sure all our custom Requests are never cached.
        setShouldCache(false);
        mRetryPolicy = new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
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

    public void disableRetries() {
        mRetryPolicy = new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }
}
