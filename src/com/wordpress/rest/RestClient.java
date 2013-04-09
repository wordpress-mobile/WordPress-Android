package com.wordpress.rest;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class RestClient {
    
    public static final String TAG="WordPressREST";
    public static final String REST_API_ENDPOINT_URL="https://public-api.wordpress.com/rest/v1/";
    
    public static final String REST_AUTHORIZATION_HEADER="Authorization";
    public static final String REST_AUTHORIZATION_FORMAT="Bearer %s";
    
    private String mAccessToken;
    private AsyncHttpClient mHttpClient;
    
    public RestClient(){
        mHttpClient = new AsyncHttpClient();
    }
    
    public RestClient(String accessToken){
        this();
        setAccessToken(accessToken);
    }
    
    public RestClient(OauthToken token){
        this(token.toString());
    }
    
    public void setAccessToken(String token){
        mAccessToken = token;
        this.mHttpClient.addHeader(REST_AUTHORIZATION_HEADER, String.format(REST_AUTHORIZATION_FORMAT, token));
    }
    
    public String getAccessToken(){
        return mAccessToken;
    }
    
    public boolean isAuthenticated(){
        return getAccessToken() != null;
    }
    
    public void get(String path, AsyncHttpResponseHandler handler){
        get(path, null, handler);
    }
    
    public void get(String path, RequestParams params, AsyncHttpResponseHandler handler){
        String url = getAbsoluteURL(path);
        Log.d(TAG, String.format("Requesting GET %s", url));
        mHttpClient.get(url, params, handler);
    }
    
    public void post(String path, RequestParams params, AsyncHttpResponseHandler handler){
        mHttpClient.post(getAbsoluteURL(path), params, handler);
    }
    
    public static String getAbsoluteURL(String url){
        // if it already starts with our endpoint, let it pass through
        if (url.indexOf(REST_API_ENDPOINT_URL) == 0) return url;
        // if it has a leading slash, remove it
        if (url.indexOf("/") == 0) url = url.substring(1);
        // prepend the endpoint
        return String.format("%s%s", REST_API_ENDPOINT_URL, url);
    }
    
    
    
}