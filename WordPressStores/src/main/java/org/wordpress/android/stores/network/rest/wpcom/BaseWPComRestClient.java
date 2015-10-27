package org.wordpress.android.stores.network.rest.wpcom;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;

public class BaseWPComRestClient {
    private AccessToken mAccessToken;
    private final RequestQueue mRequestQueue;
    protected final Dispatcher mDispatcher;
    private UserAgent mUserAgent;

    private static final String WPCOM_REST_PREFIX = "https://public-api.wordpress.com/rest";
    protected static final String WPCOM_PREFIX_V1 = WPCOM_REST_PREFIX + "/v1";
    protected static final String WPCOM_PREFIX_V1_1 = WPCOM_REST_PREFIX + "/v1.1";
    protected static final String WPCOM_PREFIX_V1_2 = WPCOM_REST_PREFIX + "/v1.2";
    protected static final String WPCOM_PREFIX_V1_3 = WPCOM_REST_PREFIX + "/v1.3";

    protected OnAuthFailedListener mOnAuthFailedListener;

    public BaseWPComRestClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                               UserAgent userAgent) {
        mRequestQueue = requestQueue;
        mDispatcher = dispatcher;
        mAccessToken = accessToken;
        mUserAgent = userAgent;
        mOnAuthFailedListener = new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AuthError authError) {
                mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE_ERROR, authError);
            }
        };
    }

    public Request add(WPComGsonRequest request) {
        // TODO: If !mAccountToken.exists() then trigger the mOnAuthFailedListener
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    private WPComGsonRequest setRequestAuthParams(WPComGsonRequest request) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setAccessToken(mAccessToken.get());
        return request;
    }
}
