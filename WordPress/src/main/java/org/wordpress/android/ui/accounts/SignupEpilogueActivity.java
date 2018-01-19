package org.wordpress.android.ui.accounts;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.signup.SignupEpilogueListener;
import org.wordpress.android.ui.accounts.signup.SignupEpilogueSocialFragment;

public class SignupEpilogueActivity extends AppCompatActivity implements SignupEpilogueListener {
    public static final String EXTRA_SIGNUP_DISPLAY_NAME = "EXTRA_SIGNUP_DISPLAY_NAME";
    public static final String EXTRA_SIGNUP_EMAIL_ADDRESS = "EXTRA_SIGNUP_EMAIL_ADDRESS";
    public static final String EXTRA_SIGNUP_PHOTO_URL = "EXTRA_SIGNUP_PHOTO_URL";
    public static final String EXTRA_SIGNUP_USERNAME = "EXTRA_SIGNUP_USERNAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_epilogue_activity);

        if (savedInstanceState == null) {
            String name = getIntent().getStringExtra(EXTRA_SIGNUP_DISPLAY_NAME);
            String email = getIntent().getStringExtra(EXTRA_SIGNUP_EMAIL_ADDRESS);
            String photoUrl = getIntent().getStringExtra(EXTRA_SIGNUP_PHOTO_URL);
            String username = getIntent().getStringExtra(EXTRA_SIGNUP_USERNAME);
            addPostSignupFragment(name, email, photoUrl, username);
        }
    }

    protected void addPostSignupFragment(String name, String email, String photoUrl, String username) {
        SignupEpilogueSocialFragment signupEpilogueSocialFragment = SignupEpilogueSocialFragment.newInstance(
                name, email, photoUrl, username);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, signupEpilogueSocialFragment, SignupEpilogueSocialFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onContinue() {
        setResult(RESULT_OK);
        finish();
    }
}
