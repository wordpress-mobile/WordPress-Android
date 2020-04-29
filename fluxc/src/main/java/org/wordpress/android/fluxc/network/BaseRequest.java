package org.wordpress.android.fluxc.network;

import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.wordpress.android.fluxc.FluxCError;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

public abstract class BaseRequest<T> extends Request<T> {
    public static final int DEFAULT_REQUEST_TIMEOUT = 30000;
    public static final int UPLOAD_REQUEST_READ_TIMEOUT = 60000;

    // Only used when enabledCaching is called - caching is off by default for all requests
    public static final int DEFAULT_CACHE_LIFETIME = 10 * 60 * 1000;

    public Uri mUri;

    public interface OnAuthFailedListener {
        void onAuthFailed(AuthenticateErrorPayload errorType);
    }
    public interface BaseErrorListener {
        void onErrorResponse(@NonNull BaseNetworkError error);
    }
    public interface OnParseErrorListener {
        void onParseError(OnUnexpectedError event);
    }

    private static final String USER_AGENT_HEADER = "User-Agent";

    protected OnAuthFailedListener mOnAuthFailedListener;
    protected OnParseErrorListener mOnParseErrorListener;
    protected final Map<String, String> mHeaders = new HashMap<>(2);
    private BaseErrorListener mErrorListener;

    private boolean mResetCache;
    private int mCacheTtl;
    private int mCacheSoftTtl;

    public static class BaseNetworkError implements FluxCError {
        public GenericErrorType type;
        public String message;
        public VolleyError volleyError;

        public BaseNetworkError(@NonNull BaseNetworkError error) {
            this.message = error.message;
            this.type = error.type;
            this.volleyError = error.volleyError;
        }

        public BaseNetworkError(@NonNull GenericErrorType error, @NonNull String message,
                                @NonNull VolleyError volleyError) {
            this.message = message;
            this.type = error;
            this.volleyError = volleyError;
        }
        public BaseNetworkError(@NonNull GenericErrorType error, @NonNull VolleyError volleyError) {
            this.message = "";
            this.type = error;
            this.volleyError = volleyError;
        }
        public BaseNetworkError(@NonNull VolleyError volleyError) {
            this.type = GenericErrorType.UNKNOWN;
            this.message = "";
            this.volleyError = volleyError;
        }
        public BaseNetworkError(@NonNull GenericErrorType error) {
            this.type = error;
        }
        public boolean isGeneric() {
            return type != null;
        }
        public boolean hasVolleyError() {
            return volleyError != null;
        }
    }

    public enum GenericErrorType {
        // Network Layer
        TIMEOUT,
        NO_CONNECTION,
        NETWORK_ERROR,

        // HTTP Layer
        NOT_FOUND,
        CENSORED,
        SERVER_ERROR,
        INVALID_SSL_CERTIFICATE,
        HTTP_AUTH_ERROR,

        // Web Application Layer
        INVALID_RESPONSE,
        AUTHORIZATION_REQUIRED,
        NOT_AUTHENTICATED,
        PARSE_ERROR,

        // Other
        UNKNOWN,
    }

    public BaseRequest(int method, @NonNull String url, BaseErrorListener errorListener) {
        super(method, url, null);
        if (url != null) {
            mUri = Uri.parse(url);
        } else {
            mUri = Uri.EMPTY;
        }
        mErrorListener = errorListener;
        // Make sure all our custom Requests are never cached.
        setShouldCache(false);
        setRetryPolicy(new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    @Override
    public String getUrl() {
        return mUri.toString();
    }

    public void addQueryParameter(String key, String value) {
        mUri = mUri.buildUpon().appendQueryParameter(key, value).build();
    }

    public void addQueryParameters(Map<String, String> parameters) {
        if (parameters == null) return;
        Builder builder = mUri.buildUpon();
        for (String key : parameters.keySet()) {
            builder.appendQueryParameter(key, parameters.get(key));
        }
        mUri = builder.build();
    }

    /**
     * Enable caching for this request. The {@code timeToLive} param corresponds to the
     * {@code ttl} field in {@link Cache}.
     *
     * Disables soft expiry, which is when the cached result is returned, but a network request is also dispatched
     * to update the cache.
     *
     * @param timeToLive the amount of time before the cache expires
     */
    public void enableCaching(int timeToLive) {
        enableCaching(timeToLive, timeToLive);
    }

    /**
     * Enable caching for this request. The {@code timeToLive} and {@code softTimeToLive} params correspond to the
     * {@code ttl} and {@code softTtl} fields in {@link Cache}.
     *
     * @param timeToLive the amount of time before the cache expires
     * @param softTimeToLive the amount of time before the cache soft expires (the cached result is returned,
     *                       but a network request is also dispatched to update the cache)
     */
    public void enableCaching(int timeToLive, int softTimeToLive) {
        setShouldCache(true);
        mCacheTtl = timeToLive;
        mCacheSoftTtl = softTimeToLive;
    }

    /**
     * Reset the cache for this request, to force an update over the network.
     */
    public void setShouldForceUpdate() {
        mResetCache = true;
    }

    /**
     * Returns true if this request should ignore the cache and force a fresh update over the network.
     */
    public boolean shouldForceUpdate() {
        return mResetCache;
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

    public void setOnParseErrorListener(OnParseErrorListener onParseErrorListener) {
        mOnParseErrorListener = onParseErrorListener;
    }

    public void setUserAgent(String userAgent) {
        mHeaders.put(USER_AGENT_HEADER, userAgent);
    }

    public void addHeader(String header, String value) {
        mHeaders.put(header, value);
    }

    /**
     * Convenience method for setting a {@link com.android.volley.RetryPolicy} with no retries.
     */
    public void disableRetries() {
        setRetryPolicy(new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    /**
     * Generate a cache entry for this request.
     *
     * If caching has been enabled through {@link BaseRequest#enableCaching(int, int)}, the expiry parameters that were
     * given are used to configure the cache entry.
     *
     * Otherwise, just generate a cache entry from the response's cache headers (default behaviour).
     */
    protected Cache.Entry createCacheEntry(NetworkResponse response) {
        Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);

        if (!shouldCache()) {
            return cacheEntry;
        }

        if (cacheEntry == null) {
            cacheEntry = new Cache.Entry();

            String headerValue = response.headers.get("Date");
            if (headerValue != null) {
                cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
            }

            headerValue = response.headers.get("Last-Modified");
            if (headerValue != null) {
                cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
            }

            cacheEntry.data = response.data;
            cacheEntry.responseHeaders = response.headers;
        }

        long now = System.currentTimeMillis();
        cacheEntry.ttl = now + mCacheTtl;
        cacheEntry.softTtl = now + mCacheSoftTtl;

        return cacheEntry;
    }

    @NonNull
    private BaseNetworkError getBaseNetworkError(VolleyError volleyError) {
        // No connection
        if (volleyError.getCause() instanceof NoConnectionError) {
            return new BaseNetworkError(GenericErrorType.NO_CONNECTION, volleyError);
        }

        // Network error
        if (volleyError.getCause() instanceof NetworkError) {
            return new BaseNetworkError(GenericErrorType.NETWORK_ERROR, volleyError);
        }

        // Invalid SSL Handshake
        if (volleyError.getCause() instanceof SSLHandshakeException) {
            return new BaseNetworkError(GenericErrorType.INVALID_SSL_CERTIFICATE, volleyError);
        }

        // Invalid HTTP Auth
        if (volleyError instanceof AuthFailureError) {
            return new BaseNetworkError(GenericErrorType.HTTP_AUTH_ERROR, volleyError);
        }

        // Timeout
        if (volleyError instanceof TimeoutError) {
            return new BaseNetworkError(GenericErrorType.TIMEOUT, volleyError);
        }

        // Parse Error
        if (volleyError instanceof ParseError) {
            return new BaseNetworkError(GenericErrorType.PARSE_ERROR, volleyError);
        }

        // Null networkResponse? Can't get more infos
        if (volleyError.networkResponse == null) {
            return new BaseNetworkError(volleyError);
        }

        // Get Error by HTTP response code
        switch (volleyError.networkResponse.statusCode) {
            case 404:
                return new BaseNetworkError(GenericErrorType.NOT_FOUND, volleyError.getMessage(), volleyError);
            case 451:
                return new BaseNetworkError(GenericErrorType.CENSORED, volleyError.getMessage(), volleyError);
            case 500:
                return new BaseNetworkError(GenericErrorType.SERVER_ERROR, volleyError.getMessage(), volleyError);
            default:
                break;
        }

        // Nothing found
        return new BaseNetworkError(volleyError);
    }

    public abstract BaseNetworkError deliverBaseNetworkError(@NonNull BaseNetworkError error);

    @Override
    public final void deliverError(VolleyError volleyError) {
        AppLog.e(AppLog.T.API, "Volley error on " + getUrl(), volleyError);
        if (volleyError instanceof ParseError && mOnParseErrorListener != null) {
            OnUnexpectedError error = new OnUnexpectedError(volleyError, "API response parse error");
            error.addExtra(OnUnexpectedError.KEY_URL, getUrl());
            mOnParseErrorListener.onParseError(error);
        }
        BaseNetworkError baseNetworkError = getBaseNetworkError(volleyError);
        BaseNetworkError modifiedBaseNetworkError = deliverBaseNetworkError(baseNetworkError);
        mErrorListener.onErrorResponse(modifiedBaseNetworkError);
    }
}
