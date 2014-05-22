package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.Window;

import org.wordpress.android.R;

// TODO: this will probably be merged with New Account Activity (maybe add a tab bar)
public class WelcomeActivity extends FragmentActivity {
    public static final int SIGN_IN_REQUEST = 1;
    public static final int ADD_SELF_HOSTED_BLOG = 2;
    public static final int CREATE_ACCOUNT_REQUEST = 3;
    public static final int SHOW_CERT_DETAILS = 4;
    public static String START_FRAGMENT_KEY = "start-fragment";

    private WelcomeFragmentSignIn mWelcomeFragmentSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);
        FragmentManager fragmentManager = getSupportFragmentManager();
        mWelcomeFragmentSignIn = (WelcomeFragmentSignIn) fragmentManager.
                findFragmentById(R.id.sign_in_fragment);
        actionMode(getIntent().getExtras());
    }

    private void actionMode(Bundle extras) {
        int actionMode = SIGN_IN_REQUEST;
        if (extras != null) {
            actionMode = extras.getInt(START_FRAGMENT_KEY, -1);
        }
        switch (actionMode) {
            case ADD_SELF_HOSTED_BLOG:
                mWelcomeFragmentSignIn.forceSelfHostedMode();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHOW_CERT_DETAILS) {
            mWelcomeFragmentSignIn.askForSslTrust();
        } else if (resultCode == RESULT_OK && data != null) {
            String username = data.getStringExtra("username");
            if (username != null) {
                mWelcomeFragmentSignIn.signInDotComUser();
            }
        }
    }
}