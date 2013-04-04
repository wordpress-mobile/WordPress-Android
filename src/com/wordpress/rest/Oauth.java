package com.wordpress.rest;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.apache.http.client.HttpClient;

import org.json.JSONObject;
import org.json.JSONException;

import android.util.Log;

public class Oauth {
    
    public static final String TAG="WordPressREST";
    
    public static final String AUTHORIZE_ENDPOINT="https://public-api.wordpress.com/oauth2/authorize";
    private static final String AUTHORIZED_ENDPOINT_FORMAT="%s?client_id=%s&redirect_uri=%s&response_type=code";
    
    public static final String TOKEN_ENDPOINT="https://public-api.wordpress.com/oauth2/token";
        
    public static final String CLIENT_ID_PARAM_NAME="client_id";
    public static final String REDIRECT_URI_PARAM_NAME="redirect_uri";
    public static final String CLIENT_SECRET_PARAM_NAME="client_secret";
    public static final String CODE_PARAM_NAME="code";
    public static final String GRANT_TYPE_PARAM_NAME="grant_type";
    public static final String USERNAME_PARAM_NAME="username";
    public static final String PASSWORD_PARAM_NAME="password";
    
    public static final String PASSWORD_GRANT_TYPE="password";
    public static final String AUTHORIZATION_CODE_GRANT_TYPE="authorization_code";
    
    private String mAppId;
    private String mAppSecret;
    private String mAppRedirectURI;
    protected AsyncHttpClient mClient;

    public Oauth(String appId, String appSecret, String redirectURI){
        this(appId, appSecret, redirectURI, new AsyncHttpClient());
    }
    
    public Oauth(String appId, String appSecret, String redirectURI, AsyncHttpClient client){
        mAppId = appId;
        mAppSecret = appSecret;
        mAppRedirectURI = redirectURI;
        mClient = client;
    }
    
    public void requestAccessToken(String username, String password, OauthTokenResponseHandler handler){
        RequestParams params = new RequestParams();
        params.put(GRANT_TYPE_PARAM_NAME, PASSWORD_GRANT_TYPE);
        params.put(USERNAME_PARAM_NAME, username);
        params.put(PASSWORD_PARAM_NAME, password);
        requestAccessToken(params, handler);
    }
    
    public void requestAccessToken(String code, OauthTokenResponseHandler handler){
        RequestParams params = new RequestParams();
        params.put(GRANT_TYPE_PARAM_NAME, AUTHORIZATION_CODE_GRANT_TYPE);
        params.put(CODE_PARAM_NAME, code);
        requestAccessToken(params, handler);
    }
    
    protected void requestAccessToken(RequestParams params, final OauthTokenResponseHandler handler){
        params.put(REDIRECT_URI_PARAM_NAME, getAppRedirectURI());
        params.put(CLIENT_ID_PARAM_NAME, getAppID());
        params.put(CLIENT_SECRET_PARAM_NAME, getAppSecret());
        mClient.post(TOKEN_ENDPOINT, params, new JsonHttpResponseHandler(){
            @Override
            public void onStart(){
                handler.onStart();
            }
            @Override
            public void onSuccess(int statusCode, JSONObject response){
                try {
                    OauthToken token = OauthToken.fromJSONObject(response);
                    handler.onSuccess(token);                    
                } catch (JSONException e) {
                    handler.onFailure(e, response);
                }
            }
            @Override
            public void onFailure(Throwable e, JSONObject content){
                handler.onFailure(e, content);
            }
            @Override
            public void onFinish(){
                handler.onFinish();
            }
        });
    }
    
    public AsyncHttpClient getClient(){
        return mClient;
    }
    
    public HttpClient getHttpClient(){
        return getClient().getHttpClient();
    }
    
    public String getAppID(){
        return mAppId;
    }
    
    public String getAppSecret(){
        return mAppSecret;
    }
    
    public String getAppRedirectURI(){
        return mAppRedirectURI;
    }
    
    public String getAuthorizationURL(){
        return String.format(AUTHORIZED_ENDPOINT_FORMAT, AUTHORIZE_ENDPOINT, getAppID(), getAppRedirectURI());
    }
	
}