package org.wordpress.android.fluxc.network.rest.wpcom;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.util.LanguageUtils;

public class BaseWPComRestClient {
    private AccessToken mAccessToken;
    private final RequestQueue mRequestQueue;
    protected final Dispatcher mDispatcher;
    protected final Context mAppContext;
    protected UserAgent mUserAgent;

    protected OnAuthFailedListener mOnAuthFailedListener;

    public BaseWPComRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                               AccessToken accessToken, UserAgent userAgent) {
        mRequestQueue = requestQueue;
        mDispatcher = dispatcher;
        mAccessToken = accessToken;
        mUserAgent = userAgent;
        mAppContext = appContext;
        mOnAuthFailedListener = new OnAuthFailedListener() {
            @Override
            public void onAuthFailed(AuthenticateErrorPayload authError) {
                mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateErrorAction(authError));
            }
        };
    }

    public Request add(WPComGsonRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return add(request, true);
    }

    public Request add(WPComGsonRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            request.addQueryParameter("locale", LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));
        }
        // TODO: If !mAccountToken.exists() then trigger the mOnAuthFailedListener
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    protected AccessToken getAccessToken() {
        return mAccessToken;
    }

    private WPComGsonRequest setRequestAuthParams(WPComGsonRequest request) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setAccessToken(mAccessToken.get());
        return request;
    }
}
