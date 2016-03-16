package org.wordpress.android.stores.network.xmlrpc;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.AuthenticationAction;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.AuthError;
import org.wordpress.android.stores.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.stores.network.HTTPAuthManager;
import org.wordpress.android.stores.network.UserAgent;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;

public class BaseXMLRPCClient {
    private AccessToken mAccessToken;
    private SiteModel mSiteModel;
    private final RequestQueue mRequestQueue;
    protected final Dispatcher mDispatcher;
    private UserAgent mUserAgent;
    protected OnAuthFailedListener mOnAuthFailedListener;
    private HTTPAuthManager mHTTPAuthManager;

    public BaseXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, AccessToken accessToken,
                            UserAgent userAgent, HTTPAuthManager httpAuthManager) {
        mRequestQueue = requestQueue;
        mDispatcher = dispatcher;
        mAccessToken = accessToken;
        mUserAgent = userAgent;
        mHTTPAuthManager = httpAuthManager;
        mOnAuthFailedListener = new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AuthError authError) {
                mDispatcher.dispatch(AuthenticationAction.AUTHENTICATE_ERROR, authError);
            }
        };
    }

    public Request add(XMLRPCRequest request) {
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    private XMLRPCRequest setRequestAuthParams(XMLRPCRequest request) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setHTTPAuthHeaderOnMatchingURL(mHTTPAuthManager);
        return request;
    }
}
