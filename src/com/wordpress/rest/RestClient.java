package com.wordpress.rest;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class RestClient {
    
    public static final String REST_API_ENDPOINT_URL="https://public-api.wordpress.com/rest/v1/";
    
    private String mAccessToken;
    private AsyncHttpClient mHttpClient;
    
    public RestClient(){
        mHttpClient = new AsyncHttpClient();
    }
    
    public RestClient(String accessToken){
        this();
        mAccessToken = accessToken;
    }
    
    public RestClient(OauthToken token){
        this(token.toString());
    }
    
    public boolean isAuthenticated(){
        return mAccessToken != null;
    }
    
    public void get(String path, RequestParams params, AsyncHttpResponseHandler handler){
        
    }
    
    public void post(String path, RequestParams params, AsyncHttpResponseHandler handler){
        
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