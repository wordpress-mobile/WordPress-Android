package org.wordpress.android.fluxc.network.rest.wpcom;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.BaseRequest.OnParseErrorListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.account.AccountSocialRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;
import org.wordpress.android.util.LanguageUtils;

public abstract class BaseWPComRestClient {
    private AccessToken mAccessToken;
    private final RequestQueue mRequestQueue;

    protected final Context mAppContext;
    protected final Dispatcher mDispatcher;
    protected UserAgent mUserAgent;

    private OnAuthFailedListener mOnAuthFailedListener;
    private OnParseErrorListener mOnParseErrorListener;

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
        mOnParseErrorListener = new OnParseErrorListener() {
            @Override
            public void onParseError(OnUnexpectedError event) {
                mDispatcher.emitChange(event);
            }
        };
    }

    protected Request add(WPComGsonRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return add(request, true);
    }

    protected Request add(WPComGsonRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            request.addQueryParameter("locale", LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));
        }
        // TODO: If !mAccountToken.exists() then trigger the mOnAuthFailedListener
        return mRequestQueue.add(setRequestAuthParams(request, true));
    }

    protected Request addUnauthedRequest(AccountSocialRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return addUnauthedRequest(request, true);
    }

    protected Request addUnauthedRequest(AccountSocialRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            request.addQueryParameter("locale", LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));
            request.setOnParseErrorListener(mOnParseErrorListener);
            request.setUserAgent(mUserAgent.getUserAgent());
        }
        return mRequestQueue.add(request);
    }

    protected Request addUnauthedRequest(WPComGsonRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return addUnauthedRequest(request, true);
    }

    protected Request addUnauthedRequest(WPComGsonRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            request.addQueryParameter("locale", LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));
        }
        return mRequestQueue.add(setRequestAuthParams(request, false));
    }

    protected AccessToken getAccessToken() {
        return mAccessToken;
    }

    private WPComGsonRequest setRequestAuthParams(WPComGsonRequest request, boolean shouldAuth) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setOnParseErrorListener(mOnParseErrorListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setAccessToken(shouldAuth ? mAccessToken.get() : null);
        return request;
    }
}
