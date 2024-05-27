package org.wordpress.android.fluxc.network.rest.wpcom;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import static org.wordpress.android.fluxc.utils.WPComRestClientUtils.getLocaleParamName;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.AcceptHeaderStrategy;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.BaseRequest.OnParseErrorListener;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.OnJetpackTimeoutError;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.OnJetpackTunnelTimeoutListener;
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
    protected AcceptHeaderStrategy mAcceptHeaderStrategy;

    private OnAuthFailedListener mOnAuthFailedListener;
    private OnParseErrorListener mOnParseErrorListener;
    private OnJetpackTunnelTimeoutListener mOnJetpackTunnelTimeoutListener;

    public BaseWPComRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                               AccessToken accessToken, UserAgent userAgent) {
        this(appContext, dispatcher, requestQueue, accessToken, userAgent, null);
    }
    public BaseWPComRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                               AccessToken accessToken, UserAgent userAgent,
                               AcceptHeaderStrategy acceptHeaderStrategy) {
        mRequestQueue = requestQueue;
        mDispatcher = dispatcher;
        mAccessToken = accessToken;
        mUserAgent = userAgent;
        mAppContext = appContext;
        mAcceptHeaderStrategy = acceptHeaderStrategy;
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
        mOnJetpackTunnelTimeoutListener = new OnJetpackTunnelTimeoutListener() {
            @Override
            public void onJetpackTunnelTimeout(OnJetpackTimeoutError onTimeoutError) {
                mDispatcher.emitChange(onTimeoutError);
            }
        };
    }

    public Request add(WPComGsonRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return add(request, true);
    }

    protected Request add(WPComGsonRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            addLocaleToRequest(request);
        }
        // TODO: If !mAccountToken.exists() then trigger the mOnAuthFailedListener
        return addRequest(setRequestAuthParams(request, true));
    }

    protected Request addUnauthedRequest(AccountSocialRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return addUnauthedRequest(request, true);
    }

    protected Request addUnauthedRequest(AccountSocialRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            addLocaleToRequest(request);
            request.setUserAgent(mUserAgent.getUserAgent());
        }
        return addRequest(request);
    }

    protected Request addUnauthedRequest(WPComGsonRequest request) {
        // Add "locale=xx_XX" query parameter to all request by default
        return addUnauthedRequest(request, true);
    }

    protected Request addUnauthedRequest(WPComGsonRequest request, boolean addLocaleParameter) {
        if (addLocaleParameter) {
            addLocaleToRequest(request);
        }
        return addRequest(setRequestAuthParams(request, false));
    }

    protected AccessToken getAccessToken() {
        return mAccessToken;
    }

    private WPComGsonRequest setRequestAuthParams(WPComGsonRequest request, boolean shouldAuth) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setOnJetpackTunnelTimeoutListener(mOnJetpackTunnelTimeoutListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setAccessToken(shouldAuth ? mAccessToken.get() : null);
        return request;
    }

    private Request addRequest(BaseRequest request) {
        request.setOnParseErrorListener(mOnParseErrorListener);
        if (request.shouldCache() && request.shouldForceUpdate()) {
            mRequestQueue.getCache().invalidate(request.mUri.toString(), true);
        }
        addAcceptHeaderIfNeeded(request);
        return mRequestQueue.add(request);
    }

    private void addLocaleToRequest(BaseRequest request) {
        String url = request.getUrl();
        // Sanity check
        if (url != null) {
            // WPCOM V2 endpoints use a different locale parameter than other endpoints
            String localeParamName = getLocaleParamName(url);
            request.addQueryParameter(localeParamName, LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));
        }
    }

    private void addAcceptHeaderIfNeeded(BaseRequest request) {
        if (mAcceptHeaderStrategy == null) return;

        request.addHeader(mAcceptHeaderStrategy.getHeader(), mAcceptHeaderStrategy.getValue());
    }
}
