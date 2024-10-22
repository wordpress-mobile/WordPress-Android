package org.wordpress.android.fluxc.network.xmlrpc;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.OnAuthFailedListener;
import org.wordpress.android.fluxc.network.BaseRequest.OnParseErrorListener;
import org.wordpress.android.fluxc.network.HTTPAuthManager;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.discovery.DiscoveryRequest;
import org.wordpress.android.fluxc.network.discovery.DiscoveryXMLRPCRequest;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateErrorPayload;
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError;

public abstract class BaseXMLRPCClient {
    private final RequestQueue mRequestQueue;
    protected final Dispatcher mDispatcher;
    protected UserAgent mUserAgent;
    protected HTTPAuthManager mHTTPAuthManager;

    protected OnAuthFailedListener mOnAuthFailedListener;
    protected OnParseErrorListener mOnParseErrorListener;

    public BaseXMLRPCClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent,
                            HTTPAuthManager httpAuthManager) {
        mRequestQueue = requestQueue;
        mDispatcher = dispatcher;
        mUserAgent = userAgent;
        mHTTPAuthManager = httpAuthManager;
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

    protected Request add(XMLRPCRequest request) {
        if (request.shouldCache() && request.shouldForceUpdate()) {
            mRequestQueue.getCache().invalidate(request.mUri.toString(), true);
        }
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    protected Request add(DiscoveryRequest request) {
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    protected Request add(DiscoveryXMLRPCRequest request) {
        return mRequestQueue.add(setRequestAuthParams(request));
    }

    private BaseRequest setRequestAuthParams(BaseRequest request) {
        request.setOnAuthFailedListener(mOnAuthFailedListener);
        request.setOnParseErrorListener(mOnParseErrorListener);
        request.setUserAgent(mUserAgent.getUserAgent());
        request.setHTTPAuthHeaderOnMatchingURL(mHTTPAuthManager);
        return request;
    }

    protected void reportParseError(Object response, String xmlrpcUrl, Class clazz) {
        if (response == null) return;

        try {
            clazz.cast(response);
        } catch (ClassCastException e) {
            OnUnexpectedError onUnexpectedError = new OnUnexpectedError(e,
                    "XML-RPC response parse error: " + e.getMessage());
            if (xmlrpcUrl != null) {
                onUnexpectedError.addExtra(OnUnexpectedError.KEY_URL, xmlrpcUrl);
            }
            onUnexpectedError.addExtra(OnUnexpectedError.KEY_RESPONSE, response.toString());
            mOnParseErrorListener.onParseError(onUnexpectedError);
        }
    }
}
