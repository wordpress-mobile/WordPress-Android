package com.wordpress.notereader;

// import com.wordpress.notereader.R;

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;

import android.widget.EditText;
import android.widget.Button;

public class LoginActivityTest extends ActivityInstrumentationTestCase2<LoginActivity> {
    
    private LoginActivity mActivity;
    
    public LoginActivityTest(){
        super("com.wordpress.notereader", LoginActivity.class);
    }
    
    public void setUp(){
        setActivityInitialTouchMode(false);
        mActivity = getActivity();
    }
    
    public void testInitialViewState(){
        EditText usernameField = (EditText) mActivity.findViewById(R.id.username);
        Button signInButton = (Button) mActivity.findViewById(R.id.signin_button);
        
        assertEquals("", usernameField.getText().toString());
        assertEquals("Sign In", signInButton.getText().toString());
    }
    
    @UiThreadTest
    public void testLogin(){
        EditText usernameField = (EditText) mActivity.findViewById(R.id.username);
        EditText passwordField = (EditText) mActivity.findViewById(R.id.password);
        Button signInButton = (Button) mActivity.findViewById(R.id.signin_button);
        
        usernameField.setText("mobiletestuser");
        passwordField.setText("password");
        
        signInButton.performClick();
        
    }
}