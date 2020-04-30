package org.wordpress.android.fluxc.network.rest.wpcom;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
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
    private static final String WPCOM_V2_PREFIX = "/wpcom/v2";
    private static final String LOCALE_PARAM_NAME_FOR_V1 = "locale";
    private static final String LOCALE_PARAM_NAME_FOR_V2 = "_locale";

    private AccessToken mAccessToken;
    private final RequestQueue mRequestQueue;

    protected final Context mAppContext;
    protected final Dispatcher mDispatcher;
    protected UserAgent mUserAgent;

    private OnAuthFailedListener mOnAuthFailedListener;
    private OnParseErrorListener mOnParseErrorListener;
    private OnJetpackTunnelTimeoutListener mOnJetpackTunnelTimeoutListener;

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
        return mRequestQueue.add(request);
    }

    private void addLocaleToRequest(BaseRequest request) {
        String url = request.getUrl();
        // Sanity check
        if (url != null) {
            // WPCOM V2 endpoints use a different locale parameter than other endpoints
            String localeParamName =
                    url.contains(WPCOM_V2_PREFIX) ? LOCALE_PARAM_NAME_FOR_V2 : LOCALE_PARAM_NAME_FOR_V1;
            request.addQueryParameter(localeParamName, LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));
        }
    }
}
