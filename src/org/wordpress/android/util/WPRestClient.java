/**
 * Wraps the Oauth and RestClient classes and configures them with application specific settings
 */
package org.wordpress.android.util;

import com.wordpress.rest.Oauth;
import com.wordpress.rest.OauthToken;
import com.wordpress.rest.OauthTokenResponseHandler;
import com.wordpress.rest.RestClient;

import android.content.SharedPreferences;

import java.util.Properties;

import org.json.JSONObject;

public class WPRestClient {
    
    private static final String ACCESS_TOKEN_PREFERNCE="wpcom-access-token";
    private static final String APP_ID_PROPERTY="oauth.app_id";
    private static final String APP_SECRET_PROPERTY="oauth.app_secret";
    private static final String APP_REDIRECT_PROPERTY="oauth.redirect_uri";
    
    private Oauth mOauth;
    private RestClient mRestClient;
    private SharedPreferences mPrefs;
    
    public WPRestClient(Properties config, SharedPreferences prefs){
        mPrefs = prefs;
        // configure Oauth with app credentials
        mOauth = new Oauth(config.getProperty(APP_ID_PROPERTY),
                           config.getProperty(APP_SECRET_PROPERTY),
                           config.getProperty(APP_REDIRECT_PROPERTY));
        // load an existing access token from prefs if we have one
        mRestClient = new RestClient(prefs.getString(ACCESS_TOKEN_PREFERNCE, null));
    }
    /**
     * Authenticate the user using WordPress.com Oauth
     */
    public void requestAccessToken(String username, String password, OauthTokenResponseHandler handler){
        mOauth.requestAccessToken(username, password, new OauthTokenResponseHandler() {
            @Override
            public void onStart(){}
            /**
             * Save the token to a preference
             */
            @Override
            public void onSuccess(OauthToken token){
                
            }
            
            @Override
            public void onFailure(Throwable e, JSONObject respose){
                
            }
            
            @Override
            public void onFinish(){
                
            }
        });
    }
    
}