package com.wordpress.rest;

import android.test.AndroidTestCase;
import com.wordpress.rest.Oauth;

import com.wordpress.util.TestExecutorService;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.protocol.HttpContext;

import com.loopj.android.http.AsyncHttpClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;

import android.util.Log;
    
public class OauthTest extends AndroidTestCase {
    
    public final String TAG="WordPressTest";
    
    private String mAppId;
    private String mAppSecret;
    private String mAppRedirectURI;
    private Oauth mClient;

    @Override
    public void setUp(){
        mClient = new Oauth(mAppId, mAppSecret, mAppRedirectURI);
        Log.d(TAG, "Preparing Client");
        AsyncHttpClient client = mClient.getClient();
        //     public TestExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {

        // client.setThreadPool(new TestExecutorService(1, 1, (long) 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1)));
        AbstractHttpClient httpClient = (AbstractHttpClient) mClient.getHttpClient();
        // client.addRequestInterceptor( new HttpRequestInterceptor(){
        //     @Override
        //     public void process(HttpRequest request, HttpContext context) {
        //         Log.d(TAG, String.format("Process request %s", request));
        //     }
        // });
    }
    
    public void testRequestAuthorizationURL(){
        String url = mClient.getAuthorizationURL();
        
        String expected = String.format("https://public-api.wordpress.com/oauth2/authorize?client_id=%s&redirect_uri=%s&response_type=code", mClient.getAppID(), mClient.getAppSecret(), mClient.getAppRedirectURI());
        assertEquals(expected, url);
    }
    
    public void testRequestAccessTokenWithCode() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        mClient.requestAccessToken("acode", new Oauth.AccessTokenResponseHandler(){
            @Override
            public void onPreflight(){
                signal.countDown();
            }
        });
        signal.await();
        fail("Did not respond");
    }
    
    public void testRequestAccessTokenWithUsernameAndPassword(){
        mClient.requestAccessToken("username", "password", new Oauth.AccessTokenResponseHandler());
    }
    
}