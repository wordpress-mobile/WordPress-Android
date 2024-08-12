package org.wordpress.android.fluxc.network.rest.wpcom.auth;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComErrorListener;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnChallengeRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnToken;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.webauthn.WebauthnTokenRequest;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailError;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailErrorType;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayload;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme;
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LanguageUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

public class Authenticator {
    private static final String WPCOM_OAUTH_PREFIX = "https://public-api.wordpress.com/oauth2";
    private static final String WPCOM_PREFIX = "https://wordpress.com";
    private static final String AUTHORIZE_ENDPOINT = WPCOM_OAUTH_PREFIX + "/authorize";
    private static final String TOKEN_ENDPOINT = WPCOM_OAUTH_PREFIX + "/token";
    private static final String AUTHORIZE_ENDPOINT_FORMAT = "%s?client_id=%s&response_type=code";
    private static final String LOGIN_BASE_ENDPOINT = WPCOM_PREFIX + "/wp-login.php?action=login-endpoint";
    public static final String CLIENT_ID_PARAM_NAME = "client_id";
    public static final String CLIENT_SECRET_PARAM_NAME = "client_secret";
    public static final String CODE_PARAM_NAME = "code";
    public static final String GRANT_TYPE_PARAM_NAME = "grant_type";
    public static final String USERNAME_PARAM_NAME = "username";
    public static final String PASSWORD_PARAM_NAME = "password";
    public static final String WITH_AUTH_TYPES = "with_auth_types";
    public static final String GET_BEARER_TOKEN = "get_bearer_token";

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

    public interface Listener extends Response.Listener<OauthResponse> {
    }

    public interface ErrorListener extends Response.ErrorListener {
    }

    public static class AuthEmailResponsePayload extends Payload<AuthEmailError> {
        public final boolean isSignup;

        public AuthEmailResponsePayload(boolean isSignup) {
            this.isSignup = isSignup;
        }
    }

    @Inject public Authenticator(Context appContext,
                         Dispatcher dispatcher,
                         @Named("regular") RequestQueue requestQueue,
                         AppSecrets secrets) {
        mAppContext = appContext;
        mDispatcher = dispatcher;
        mRequestQueue = requestQueue;
        mAppSecrets = secrets;
    }

    public void authenticate(String username, String password, Listener listener, ErrorListener errorListener) {
        OauthRequest request = makeRequest(username, password, listener, errorListener);
        mRequestQueue.add(request);
    }

    public void authenticate(String username, String password, String twoStepCode, boolean shouldSendTwoStepSMS,
                             Listener listener, ErrorListener errorListener) {
        OauthRequest request = makeRequest(username, password, twoStepCode, shouldSendTwoStepSMS, listener,
                errorListener);
        mRequestQueue.add(request);
    }

    public String getAuthorizationURL() {
        return String.format(AUTHORIZE_ENDPOINT_FORMAT, AUTHORIZE_ENDPOINT, mAppSecrets.getAppId());
    }

    public OauthRequest makeRequest(String username, String password, Listener listener, ErrorListener errorListener) {
        return new PasswordRequest(mAppSecrets.getAppId(), mAppSecrets.getAppSecret(),
                username, password, listener, errorListener);
    }

    public OauthRequest makeRequest(String username, String password, String twoStepCode, boolean shouldSendTwoStepSMS,
                                    Listener listener, ErrorListener errorListener) {
        return new TwoFactorRequest(mAppSecrets.getAppId(), mAppSecrets.getAppSecret(),
                username, password, twoStepCode, shouldSendTwoStepSMS, listener, errorListener);
    }

    public void makeRequest(String userId, String webauthnNonce,
                            Response.Listener<JSONObject> listener,
                            ErrorListener errorListener) {
        WebauthnChallengeRequest request = new WebauthnChallengeRequest(
                userId,
                webauthnNonce,
                mAppSecrets.getAppId(),
                mAppSecrets.getAppSecret(),
                listener,
                errorListener
        );
        mRequestQueue.add(request);
    }

    public void makeRequest(String userId, String twoStepNonce,
                            String clientData, Response.Listener<WebauthnToken> listener,
                            ErrorListener errorListener) {
        WebauthnTokenRequest request = new WebauthnTokenRequest(
                userId,
                twoStepNonce,
                mAppSecrets.getAppId(),
                mAppSecrets.getAppSecret(),
                clientData,
                listener,
                errorListener
        );
        mRequestQueue.add(request);
    }

    private static class OauthRequest extends Request<OauthResponse> {
        private static final String DATA = "data";
        private static final String BEARER_TOKEN = "bearer_token";
        private static final String ACCESS_TOKEN = "access_token";
        private final Listener mListener;
        protected Map<String, String> mParams = new HashMap<>();

        OauthRequest(String url, String appId, String appSecret, Listener listener, ErrorListener errorListener) {
            super(Method.POST, url, errorListener);
            mListener = listener;
            mParams.put(CLIENT_ID_PARAM_NAME, appId);
            mParams.put(CLIENT_SECRET_PARAM_NAME, appSecret);
        }

        @Override
        public Map<String, String> getParams() {
            return mParams;
        }

        @Override
        public void deliverResponse(OauthResponse response) {
            mListener.onResponse(response);
        }

        @Override
        protected Response<OauthResponse> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                JSONObject responseJson = new JSONObject(jsonString);
                JSONObject responseData = responseJson.optJSONObject(DATA);
                Cache.Entry headers = HttpHeaderParser.parseCacheHeaders(response);
                if (responseData != null) {
                    return handleDataObjectResponse(headers, responseData);
                } else {
                    String accessToken = responseJson.getString(ACCESS_TOKEN);
                    return Response.success(new Token(accessToken), headers);
                }
            } catch (UnsupportedEncodingException | JSONException e) {
                return Response.error(new ParseError(e));
            }
        }

        @NonNull
        private static Response<OauthResponse> handleDataObjectResponse(Cache.Entry headers, JSONObject responseData)
                throws JSONException {
            String bearerToken = responseData.optString(BEARER_TOKEN);
            if (bearerToken.isEmpty()) {
                return Response.success(new TwoFactorResponse(responseData), headers);
            }

            return Response.success(new Token(bearerToken), headers);
        }
    }

    public static class PasswordRequest extends OauthRequest {
        public PasswordRequest(String appId, String appSecret, String username, String password,
                               Listener listener, ErrorListener errorListener) {
            super(LOGIN_BASE_ENDPOINT, appId, appSecret, listener, errorListener);
            mParams.put(USERNAME_PARAM_NAME, username);
            mParams.put(PASSWORD_PARAM_NAME, password);
            mParams.put(GRANT_TYPE_PARAM_NAME, PASSWORD_GRANT_TYPE);
            mParams.put(GET_BEARER_TOKEN, "true");
            mParams.put("wpcom_supports_2fa", "true");
        }
    }

    public static class TwoFactorRequest extends OauthRequest {
        public TwoFactorRequest(String appId, String appSecret, String username, String password, String twoStepCode,
                                boolean shouldSendTwoStepSMS, Listener listener, ErrorListener errorListener) {
            super(TOKEN_ENDPOINT, appId, appSecret, listener, errorListener);
            mParams.put(USERNAME_PARAM_NAME, username);
            mParams.put(PASSWORD_PARAM_NAME, password);
            mParams.put(GRANT_TYPE_PARAM_NAME, PASSWORD_GRANT_TYPE);
            mParams.put(GET_BEARER_TOKEN, "true");
            mParams.put("wpcom_otp", twoStepCode);
            if (shouldSendTwoStepSMS && TextUtils.isEmpty(twoStepCode)) {
                mParams.put("wpcom_resend_otp", "true");
            }
            mParams.put("wpcom_supports_2fa", "true");
        }
    }

    public static class BearerRequest extends OauthRequest {
        public BearerRequest(String appId, String appSecret, String code, Listener listener,
                             ErrorListener errorListener) {
            super(TOKEN_ENDPOINT, appId, appSecret, listener, errorListener);
            mParams.put(CODE_PARAM_NAME, code);
            mParams.put(GRANT_TYPE_PARAM_NAME, BEARER_GRANT_TYPE);
        }
    }

    public interface OauthResponse {}

    public static class Token implements OauthResponse {
        private String mAccessToken;

        public Token(String accessToken) {
            mAccessToken = accessToken;
        }

        public String getAccessToken() {
            return mAccessToken;
        }

        public String toString() {
            return getAccessToken();
        }
    }

    public static class TwoFactorResponse implements OauthResponse {
        private static final String USER_ID = "user_id";
        private static final String TWO_STEP_WEBAUTHN_NONCE = "two_step_nonce_webauthn";
        private static final String TWO_STEP_BACKUP_NONCE = "two_step_nonce_backup";
        private static final String TWO_STEP_AUTHENTICATOR_NONCE = "two_step_nonce_authenticator";
        private static final String TWO_STEP_PUSH_NONCE = "two_step_nonce_push";
        private static final String TWO_STEP_SUPPORTED_AUTH_TYPES = "two_step_supported_auth_types";
        public final String mUserId;
        public final String mWebauthnNonce;
        public final String mBackupNonce;
        public final String mAuthenticatorNonce;
        public final String mPushNonce;
        public final List<String> mSupportedAuthTypes;

        public TwoFactorResponse(JSONObject data) throws JSONException {
            mUserId = data.getString(USER_ID);
            mWebauthnNonce = data.optString(TWO_STEP_WEBAUTHN_NONCE);
            mBackupNonce = data.optString(TWO_STEP_BACKUP_NONCE);
            mAuthenticatorNonce = data.optString(TWO_STEP_AUTHENTICATOR_NONCE);
            mPushNonce = data.optString(TWO_STEP_PUSH_NONCE);
            JSONArray supportedTypes = data.getJSONArray(TWO_STEP_SUPPORTED_AUTH_TYPES);
            if (supportedTypes.length() == 0) {
                throw new JSONException("No supported auth types found");
            }

            ArrayList<String> supportedAuthTypes = new ArrayList<>();
            for (int i = 0; i < supportedTypes.length(); i++) {
                supportedAuthTypes.add(supportedTypes.getString(i));
            }
            mSupportedAuthTypes = supportedAuthTypes;
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
            params.put("flow", payload.flow.getName());
        }

        if (payload.source != null) {
            params.put("source", payload.source.getName());
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
