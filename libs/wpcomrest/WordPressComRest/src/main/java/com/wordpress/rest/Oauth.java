package com.wordpress.rest;

import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Oauth {
    public static final String TAG = "WordPressREST";

    public static final String AUTHORIZE_ENDPOINT = "https://public-api.wordpress.com/oauth2/authorize";
    private static final String AUTHORIZED_ENDPOINT_FORMAT = "%s?client_id=%s&redirect_uri=%s&response_type=code";

    public static final String TOKEN_ENDPOINT = "https://public-api.wordpress.com/oauth2/token";

    public static final String CLIENT_ID_PARAM_NAME = "client_id";
    public static final String REDIRECT_URI_PARAM_NAME = "redirect_uri";
    public static final String CLIENT_SECRET_PARAM_NAME = "client_secret";
    public static final String CODE_PARAM_NAME = "code";
    public static final String GRANT_TYPE_PARAM_NAME = "grant_type";
    public static final String USERNAME_PARAM_NAME = "username";
    public static final String PASSWORD_PARAM_NAME = "password";

    public static final String PASSWORD_GRANT_TYPE = "password";
    public static final String BEARER_GRANT_TYPE = "bearer";
    public static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    private String mAppId;
    private String mAppSecret;
    private String mAppRedirectURI;

    public interface Listener extends Response.Listener<Token> {
    }
    public interface ErrorListener extends Response.ErrorListener {
    }

    public Oauth(String appId, String appSecret, String redirectURI) {
        mAppId = appId;
        mAppSecret = appSecret;
        mAppRedirectURI = redirectURI;
    }

    public String getAppID() {
        return mAppId;
    }

    public String getAppSecret() {
        return mAppSecret;
    }

    public String getAppRedirectURI() {
        return mAppRedirectURI;
    }

    public String getAuthorizationURL() {
        return String.format(AUTHORIZED_ENDPOINT_FORMAT, AUTHORIZE_ENDPOINT, getAppID(), getAppRedirectURI());
    }

    public Request makeRequest(String username, String password, String twoStepCode, boolean shouldSendTwoStepSMS, Listener listener, ErrorListener errorListener) {
        return new PasswordRequest(getAppID(), getAppSecret(), getAppRedirectURI(), username, password, twoStepCode, shouldSendTwoStepSMS, listener,
                errorListener);
    }

    public Request makeRequest(String code, Listener listener, ErrorListener errorListener) {
        return new BearerRequest(getAppID(), getAppSecret(), getAppRedirectURI(), code, listener, errorListener);
    }

    private static class Request extends com.android.volley.Request<Token> {
        private final Listener mListener;
        protected Map<String, String> mParams = new HashMap<String, String>();

        Request(String appId, String appSecret, String redirectUri, Listener listener, ErrorListener errorListener) {
            super(Method.POST, TOKEN_ENDPOINT, errorListener);
            mListener = listener;
            mParams.put(CLIENT_ID_PARAM_NAME, appId);
            mParams.put(CLIENT_SECRET_PARAM_NAME, appSecret);
            mParams.put(REDIRECT_URI_PARAM_NAME, redirectUri);
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

    public static class PasswordRequest extends Request {

        public PasswordRequest(String appId, String appSecret, String redirectUri, String username, String password, String twoStepCode,
                               boolean shouldSendTwoStepSMS, Listener listener, ErrorListener errorListener) {
            super(appId, appSecret, redirectUri, listener, errorListener);
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

    public static class BearerRequest extends Request {

        public BearerRequest(String appId, String appSecret, String redirectUri, String code, Listener listener,
                             ErrorListener errorListener) {
            super(appId, appSecret, redirectUri, listener, errorListener);
            mParams.put(CODE_PARAM_NAME, code);
            mParams.put(GRANT_TYPE_PARAM_NAME, BEARER_GRANT_TYPE);
        }
    }

    public static class Token {

        private static final String TOKEN_TYPE_FIELD_NAME = "token_type";
        private static final String ACCESS_TOKEN_FIELD_NAME = "access_token";
        private static final String BLOG_URL_FIELD_NAME = "blog_url";
        private static final String SCOPE_FIELD_NAME = "scope";
        private static final String BLOG_ID_FIELD_NAME = "blog_id";

        private String mTokenType;
        private String mScope;
        private String mAccessToken;
        private String mBlogUrl;
        private String mBlogId;

        public Token(String accessToken, String blogUrl, String blogId, String scope, String tokenType) {
            mAccessToken = accessToken;
            mBlogUrl = blogUrl;
            mBlogId = blogId;
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
            return new Token(tokenJSON.getString(ACCESS_TOKEN_FIELD_NAME), tokenJSON.getString(BLOG_URL_FIELD_NAME),
                    tokenJSON.getString(BLOG_ID_FIELD_NAME), tokenJSON.getString(SCOPE_FIELD_NAME), tokenJSON.getString(
                    TOKEN_TYPE_FIELD_NAME));
        }
    }
}