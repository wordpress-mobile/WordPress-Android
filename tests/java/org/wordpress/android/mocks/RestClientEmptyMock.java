package org.wordpress.android.mocks;

import com.android.volley.Request.Method;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import hugo.weaving.DebugLog;

public class RestClientEmptyMock extends RestClient {
    public RestClientEmptyMock(com.android.volley.RequestQueue queue) {
        super(queue);
    }

    public RestClientEmptyMock(com.android.volley.RequestQueue queue, java.lang.String token) {
        super(queue, token);
    }

    @DebugLog
    public com.wordpress.rest.RestRequest get(java.lang.String path, com.wordpress.rest.RestRequest.Listener listener,
                                              com.wordpress.rest.RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": get(" + path + ")");
        return new RestRequest(Method.GET, path, null, listener, errorListener);
    }

    @DebugLog
    public com.wordpress.rest.RestRequest post(java.lang.String path,
                                               java.util.Map<java.lang.String, java.lang.String> body,
                                               com.wordpress.rest.RestRequest.Listener listener,
                                               com.wordpress.rest.RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": post(" + path + ")");
        return new RestRequest(Method.POST, path, body, listener, errorListener);
    }

    @DebugLog
    public com.wordpress.rest.RestRequest makeRequest(int method, java.lang.String url,
                                                      java.util.Map<java.lang.String, java.lang.String> params,
                                                      com.wordpress.rest.RestRequest.Listener listener,
                                                      com.wordpress.rest.RestRequest.ErrorListener errorListener) {
        AppLog.v(T.TESTS, this.getClass() + ": makeRequest(" + url + ")");
        return new RestRequest(method, url, params, listener, errorListener);
    }

    @DebugLog
    public com.wordpress.rest.RestRequest send(com.wordpress.rest.RestRequest request) {
        return request;
    }

    @DebugLog
    public static java.lang.String getAbsoluteURL(java.lang.String url) {
        return null;
    }

    @DebugLog
    public static java.lang.String getAbsoluteURL(java.lang.String path,
                                                  java.util.Map<java.lang.String, java.lang.String> params) {
        return null;
    }

    @DebugLog
    public void setUserAgent(java.lang.String userAgent) {
    }

    @DebugLog
    public void setAccessToken(java.lang.String token) {
    }

    @DebugLog
    public boolean isAuthenticated() {
        return true;
    }
}
