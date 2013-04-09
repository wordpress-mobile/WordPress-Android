package com.wordpress.notereader;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

import android.widget.Button;
import android.widget.EditText;

import android.util.Log;
import android.view.View;

import com.wordpress.rest.Oauth;
import com.wordpress.rest.OauthToken;
import com.wordpress.rest.OauthTokenResponseHandler;

import java.util.Properties;
import java.io.InputStream;

import org.json.JSONObject;

public class LoginActivity extends Activity {
    
    private static final String OAUTH_ID_NAME="oauth.appid";
    private static final String OAUTH_SECRET_NAME="oauth.appsecret";
    private static final String OAUTH_REDIRECT_URI="oauth.redirect_uri";
    public static final String OAUTH_TOKEN_EXTRA="oauth-access-token";
    private static final String TAG="NotesLogin";
    private Properties mConfig;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getConfigProperties();
        
        setContentView(R.layout.login);
                
        final Oauth oauth = new Oauth(
            mConfig.getProperty(OAUTH_ID_NAME),
            mConfig.getProperty(OAUTH_SECRET_NAME),
            mConfig.getProperty(OAUTH_REDIRECT_URI)
        );
        
        Button button = (Button) findViewById(R.id.signin_button);
        final EditText usernameField = (EditText) findViewById(R.id.username);
        final EditText passwordField = (EditText) findViewById(R.id.password);
        
        button.setOnClickListener( new View.OnClickListener(){
            public void onClick(View v){
                oauth.requestAccessToken(
                    usernameField.getText().toString(),
                    passwordField.getText().toString(),
                    new OauthTokenResponseHandler(){
                        @Override
                        public void onSuccess(final OauthToken token){
                            runOnUiThread(new Runnable(){
                                @Override
                                public void run(){
                                    Intent result = new Intent();
                                    result.putExtra(OAUTH_TOKEN_EXTRA, token.toString());
                                    setResult(Activity.RESULT_OK, result);
                                    finish();
                                }
                            });
                        }
                        @Override
                        public void onFailure(Throwable e, JSONObject response){
                            Log.d(TAG, String.format("Failed %s", response));
                        }
                    }
                );
            }
        });
        
    }
    
    protected Properties getConfigProperties(){
        if (mConfig == null) {
            mConfig = new Properties();
            InputStream stream = getResources().openRawResource(R.raw.oauth);
            try {
                mConfig.load(stream);               
            } catch(java.io.IOException e){
                mConfig = null;
                Log.e(TAG, "Could not load config", e);
            }
        }
        return mConfig;
    }
    
}