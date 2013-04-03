package com.wordpress.rest;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import org.json.*;

import android.util.Log;

public class Oauth {
    
    public static final String TAG="WordPressREST";
    
    public static final String AUTHORIZE_ENDPOINT="https://public-api.wordpress.com/oauth2/authorize";
    private static final String AUTHORIZED_ENDPOINT_FORMAT="%s?client_id=%s&redirect_uri=%s&response_type=code";
    
    public static final String TOKEN_ENDPOINT="https://public-api.wordpress.com/oauth2/token";
    
    
    //     'client_id' => your_client_id,
    //     'redirect_uri' => your_redirect_url,
    //     'client_secret' => your_client_secret_key,
    //     'code' => $_GET['code'], // The code from the previous request
    //     'grant_type' => 'authorization_code'
    
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
    
    public static class AccessTokenResponseHandler {
        
    }
    
    public Oauth(String appId, String appSecret, String redirectURI){
        this(appId, appSecret, redirectURI, new AsyncHttpClient());
    }
    
    public Oauth(String appId, String appSecret, String redirectURI, AsyncHttpClient client){
        mAppId = appId;
        mAppSecret = appSecret;
        mAppRedirectURI = redirectURI;
        mClient = client;
    }
    
    public void requestAccessToken(String username, String password, AccessTokenResponseHandler handler){
        RequestParams params = new RequestParams();
        params.put(GRANT_TYPE_PARAM_NAME, PASSWORD_GRANT_TYPE);
        params.put(USERNAME_PARAM_NAME, username);
        params.put(PASSWORD_PARAM_NAME, password);
        requestAccessToken(params, handler);
    }
    
    public void requestAccessToken(String code, AccessTokenResponseHandler handler){
        RequestParams params = new RequestParams();
        params.put(GRANT_TYPE_PARAM_NAME, AUTHORIZATION_CODE_GRANT_TYPE);
        params.put(CODE_PARAM_NAME, code);
        requestAccessToken(params, handler);
    }
    
    protected void requestAccessToken(RequestParams params, AccessTokenResponseHandler handler){
        params.put(REDIRECT_URI_PARAM_NAME, getAppRedirectURI());
        params.put(CLIENT_ID_PARAM_NAME, getAppID());
        params.put(CLIENT_SECRET_PARAM_NAME, getAppSecret());
        mClient.post(TOKEN_ENDPOINT, params, new JsonHttpResponseHandler(){
            
        });
    }
    
    public AsyncHttpClient getHttpClient(){
        return mClient;
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