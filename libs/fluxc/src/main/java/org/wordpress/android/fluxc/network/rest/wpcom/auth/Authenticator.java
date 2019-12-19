package org.wordpress.android.fluxc.network.rest.wpcom.auth;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailError;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailErrorType;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LanguageUtils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Authenticator {
    private static final String WPCOM_OAUTH_PREFIX = "https://public-api.wordpress.com/oauth2";
    private static final String AUTHORIZE_ENDPOINT = WPCOM_OAUTH_PREFIX + "/authorize";
    private static final String TOKEN_ENDPOINT = WPCOM_OAUTH_PREFIX + "/token";
    private static final String AUTHORIZE_ENDPOINT_FORMAT = "%s?client_id=%s&response_type=code";

    public static final String CLIENT_ID_PARAM_NAME = "client_id";
    public static final String CLIENT_SECRET_PARAM_NAME = "client_secret";
    public static final String CODE_PARAM_NAME = "code";
    public static final String GRANT_TYPE_PARAM_NAME = "grant_type";
    public static final String USERNAME_PARAM_NAME = "username";
    public static final String PASSWORD_PARAM_NAME = "password";

    public static final String PASSWORD_GRANT_TYPE = "password";
    public static final String BEARER_GRANT_TYPE = "bearer";
    public static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    // Authentication error response keys/descriptions recognized
    private static final String INVALID_REQUEST_ERROR = "invalid_request";
    private static final String NEEDS_TWO_STEP_ERROR = "needs_2fa";
    private static final String INVALID_OTP_ERROR = "invalid_otp";
    private static final String INVALID_CREDS_ERROR = "Incorrect username or password.";

    private final Context mAppContext;
    private final Dispatcher mDispatcher;
    private final RequestQueue mRequestQueue;
    private AppSecrets mAppSecrets;

    public interface Listener extends Response.Listener<Token> {
    }

    public interface ErrorListener extends Response.ErrorListener {
    }

    public static class AuthEmailResponsePayload extends Payload<AuthEmailError> {
        public final boolean isSignup;

        public AuthEmailResponsePayload(boolean isSignup) {
            this.isSignup = isSignup;
        }
    }

    public Authenticator(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue, AppSecrets secrets) {
        mAppContext = appContext;
        mDispatcher = dispatcher;
        mRequestQueue = requestQueue;
        mAppSecrets = secrets;
    }

    public void authenticate(String username, String password, String twoStepCode, boolean shouldSendTwoStepSMS,
                             Listener listener, ErrorListener errorListener) {
        TokenRequest tokenRequest = makeRequest(username, password, twoStepCode, shouldSendTwoStepSMS, listener,
                errorListener);
        mRequestQueue.add(tokenRequest);
    }

    public String getAuthorizationURL() {
        return String.format(AUTHORIZE_ENDPOINT_FORMAT, AUTHORIZE_ENDPOINT, mAppSecrets.getAppId());
    }

    public TokenRequest makeRequest(String username, String password, String twoStepCode, boolean shouldSendTwoStepSMS,
                                    Listener listener, ErrorListener errorListener) {
        return new PasswordRequest(mAppSecrets.getAppId(), mAppSecrets.getAppSecret(), username, password, twoStepCode,
                shouldSendTwoStepSMS, listener, errorListener);
    }

    public TokenRequest makeRequest(String code, Listener listener, ErrorListener errorListener) {
        return new BearerRequest(mAppSecrets.getAppId(), mAppSecrets.getAppSecret(), code, listener, errorListener);
    }

    private static class TokenRequest extends Request<Token> {
        private final Listener mListener;
        protected Map<String, String> mParams = new HashMap<>();

        TokenRequest(String appId, String appSecret, Listener listener, ErrorListener errorListener) {
            super(Method.POST, TOKEN_ENDPOINT, errorListener);
            mListener = listener;
            mParams.put(CLIENT_ID_PARAM_NAME, appId);
            mParams.put(CLIENT_SECRET_PARAM_NAME, appSecret);
        }

        @Override
        public Map<String, String> getParams() {
            return mParams;
        }

        @Override
        public void deliverResponse(Token token) {
            mListener.onResponse(token);
        }

        @Override
        protected Response<Token> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                JSONObject tokenData = new JSONObject(jsonString);
                return Response.success(Token.fromJSONObject(tokenData), HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }
    }

    public static class PasswordRequest extends TokenRequest {
        public PasswordRequest(String appId, String appSecret, String username, String password, String twoStepCode,
                               boolean shouldSendTwoStepSMS, Listener listener, ErrorListener errorListener) {
            super(appId, appSecret, listener, errorListener);
            mParams.put(USERNAME_PARAM_NAME, username);
            mParams.put(PASSWORD_PARAM_NAME, password);
            mParams.put(GRANT_TYPE_PARAM_NAME, PASSWORD_GRANT_TYPE);

            if (!TextUtils.isEmpty(twoStepCode)) {
                mParams.put("wpcom_otp", twoStepCode);
            } else {
                mParams.put("wpcom_supports_2fa", "true");
                if (shouldSendTwoStepSMS) {
                    mParams.put("wpcom_resend_otp", "true");
                }
            }
        }
    }

    public static class BearerRequest extends TokenRequest {
        public BearerRequest(String appId, String appSecret, String code, Listener listener,
                             ErrorListener errorListener) {
            super(appId, appSecret, listener, errorListener);
            mParams.put(CODE_PARAM_NAME, code);
            mParams.put(GRANT_TYPE_PARAM_NAME, BEARER_GRANT_TYPE);
        }
    }

    public static class Token {
        private static final String TOKEN_TYPE_FIELD_NAME = "token_type";
        private static final String ACCESS_TOKEN_FIELD_NAME = "access_token";
        private static final String SITE_URL_FIELD_NAME = "blog_url";
        private static final String SCOPE_FIELD_NAME = "scope";
        private static final String SITE_ID_FIELD_NAME = "blog_id";

        private String mTokenType;
        private String mScope;
        private String mAccessToken;
        private String mSiteUrl;
        private String mSiteId;

        public Token(String accessToken, String siteUrl, String siteId, String scope, String tokenType) {
            mAccessToken = accessToken;
            mSiteUrl = siteUrl;
            mSiteId = siteId;
            mScope = scope;
            mTokenType = tokenType;
        }

        public String getAccessToken() {
            return mAccessToken;
        }

        public String toString() {
            return getAccessToken();
        }

        public static Token fromJSONObject(JSONObject tokenJSON) throws JSONException {
            return new Token(tokenJSON.getString(ACCESS_TOKEN_FIELD_NAME), tokenJSON.getString(SITE_URL_FIELD_NAME),
                    tokenJSON.getString(SITE_ID_FIELD_NAME), tokenJSON.getString(SCOPE_FIELD_NAME), tokenJSON.getString(
                    TOKEN_TYPE_FIELD_NAME));
        }
    }

    public void sendAuthEmail(final AuthEmailPayload payload) {
        String url = payload.isSignup ? WPCOMREST.auth.send_signup_email.getUrlV1_1()
                : WPCOMREST.auth.send_login_email.getUrlV1_3();

        Map<String, Object> params = new HashMap<>();
        params.put("email", payload.emailOrUsername);
        params.put("client_id", mAppSecrets.getAppId());
        params.put("client_secret", mAppSecrets.getAppSecret());

        AuthEmailPayloadScheme scheme = payload.scheme;
        if (scheme == null) {
            scheme = AuthEmailPayloadScheme.WORDPRESS;
        }
        params.put("scheme", scheme.toString());

        if (payload.flow != null) {
            params.put("flow", payload.flow.toString());
        }

        if (payload.source != null) {
            params.put("source", payload.source.toString());
        }

        if (payload.signupFlowName != null && !TextUtils.isEmpty(payload.signupFlowName)) {
            params.put("signup_flow_name", payload.signupFlowName);
        }

        WPComGsonRequest request = WPComGsonRequest.buildPostRequest(url, params, AuthEmailWPComRestResponse.class,
                new Response.Listener<AuthEmailWPComRestResponse>() {
                    @Override
                    public void onResponse(AuthEmailWPComRestResponse response) {
                        AuthEmailResponsePayload responsePayload = new AuthEmailResponsePayload(payload.isSignup);

                        if (!response.success) {
                            responsePayload.error = new AuthEmailError(AuthEmailErrorType.UNSUCCESSFUL, "");
                        }
                        mDispatcher.dispatch(AuthenticationActionBuilder.newSentAuthEmailAction(responsePayload));
                    }
                }, new WPComErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull WPComGsonNetworkError error) {
                        AuthEmailResponsePayload responsePayload = new AuthEmailResponsePayload(payload.isSignup);
                        responsePayload.error = new AuthEmailError(error.apiError, error.message);
                        mDispatcher.dispatch(AuthenticationActionBuilder.newSentAuthEmailAction(responsePayload));
                    }
                }
        );
        request.addQueryParameter("locale", LanguageUtils.getPatchedCurrentDeviceLanguage(mAppContext));

        mRequestQueue.add(request);
    }

    public static AuthenticationErrorType volleyErrorToAuthenticationError(VolleyError error) {
        if (error != null && error.networkResponse != null && error.networkResponse.data != null) {
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                return jsonErrorToAuthenticationError(jsonObject);
            } catch (JSONException e) {
                AppLog.e(T.API, e);
            }
        }
        return AuthenticationErrorType.GENERIC_ERROR;
    }

    public static String volleyErrorToErrorMessage(VolleyError error) {
        if (error != null && error.networkResponse != null && error.networkResponse.data != null) {
            String jsonString = new String(error.networkResponse.data);
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                return jsonObject.getString("error_description");
            } catch (JSONException e) {
                AppLog.e(T.API, e);
            }
        }
        return null;
    }

    public static AuthenticationErrorType jsonErrorToAuthenticationError(JSONObject jsonObject) {
        AuthenticationErrorType error = AuthenticationErrorType.GENERIC_ERROR;
        if (jsonObject != null) {
            String errorType = jsonObject.optString("error", "");
            String errorMessage = jsonObject.optString("error_description", "");
            error = wpComApiErrorToAuthenticationError(errorType, errorMessage);
        }
        return error;
    }

    public static AuthenticationErrorType wpComApiErrorToAuthenticationError(String errorType, String errorMessage) {
        AuthenticationErrorType error = AuthenticationErrorType.fromString(errorType);
        // Special cases for vague error types
        if (error == AuthenticationErrorType.INVALID_REQUEST) {
            // Try to parse the error message to specify the error
            if (errorMessage.contains("Incorrect username or password.")) {
                return AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD;
            }
        }
        return error;
    }
}
