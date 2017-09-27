package org.wordpress.android.fluxc.network.wporg;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;

public abstract class BaseWPOrgAPIClient {
    private final RequestQueue mRequestQueue;
    private final Dispatcher mDispatcher;
    private UserAgent mUserAgent;

    private OnAuthFailedListener mOnAuthFailedListener;

    public BaseWPOrgAPIClient(Dispatcher dispatcher, RequestQueue requestQueue,
                              UserAgent userAgent) {
        mDispatcher = dispatcher;
        mRequestQueue = requestQueue;
        mUserAgent = userAgent;
        mOnAuthFailedListener = new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AuthenticateErrorPayload authError) {
                mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateErrorAction(authError));
            }
        };
    }

    protected Request add(WPOrgAPIGsonRequest request) {
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    private BaseRequest setRequestAuthParams(BaseRequest request) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        return request;
    }
}
