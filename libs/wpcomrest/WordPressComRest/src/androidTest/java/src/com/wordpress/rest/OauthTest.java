package com.wordpress.rest;

import android.test.AndroidTestCase;

public class OauthTest extends AndroidTestCase {

    public final String TAG = "WordPressTest";

    private String mAppId;
    private String mAppSecret;
    private String mAppRedirectURI;
    private Oauth mClient;

    @Override
    public void setUp() {
        mClient = new Oauth(mAppId, mAppSecret, mAppRedirectURI);
    }

    public void testRequestAuthorizationURL() {
        String url = mClient.getAuthorizationURL();

        String expected = String.format(
                "https://public-api.wordpress.com/oauth2/authorize?client_id=%s&redirect_uri=%s&response_type=code",
                mClient.getAppID(), mClient.getAppSecret(), mClient.getAppRedirectURI());
        assertEquals(expected, url);
    }
}
