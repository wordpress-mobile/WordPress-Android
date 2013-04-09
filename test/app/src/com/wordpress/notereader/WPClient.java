package com.wordpress.notereader;

import com.wordpress.rest.RestClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class WPClient {
    
    public static RestClient restClient = new RestClient();
    
    public static void setAccessToken(String token){
        restClient.setAccessToken(token);
    }
    
    public static boolean isAuthenticated(){
        return restClient.isAuthenticated();
    }
    
    public static void get(String path, AsyncHttpResponseHandler handler){
        restClient.get(path, handler);
    }
    
    public static void get(String path, RequestParams params, AsyncHttpResponseHandler handler){
        restClient.get(path, params, handler);
    }
    
    public static void notifications(AsyncHttpResponseHandler handler){
        notifications(null, handler);
    }
    
    public static void notifications(RequestParams params, AsyncHttpResponseHandler handler){
        get("notifications", params, handler);
    }
        
}