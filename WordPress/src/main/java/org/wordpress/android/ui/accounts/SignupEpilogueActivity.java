package org.wordpress.android.ui.accounts;

import android.os.Bundle;

import androidx.fragment.app.FragmentTransaction;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.accounts.signup.SignupEpilogueFragment;
import org.wordpress.android.ui.accounts.signup.SignupEpilogueListener;

import javax.inject.Inject;

public class SignupEpilogueActivity extends LocaleAwareActivity implements SignupEpilogueListener {
    public static final String EXTRA_SIGNUP_DISPLAY_NAME = "EXTRA_SIGNUP_DISPLAY_NAME";
    public static final String EXTRA_SIGNUP_EMAIL_ADDRESS = "EXTRA_SIGNUP_EMAIL_ADDRESS";
    public static final String EXTRA_SIGNUP_IS_EMAIL = "EXTRA_SIGNUP_IS_EMAIL";
    public static final String EXTRA_SIGNUP_PHOTO_URL = "EXTRA_SIGNUP_PHOTO_URL";
    public static final String EXTRA_SIGNUP_USERNAME = "EXTRA_SIGNUP_USERNAME";
    public static final String MAGIC_SIGNUP_PARAMETER = "new_user";
    public static final String MAGIC_SIGNUP_VALUE = "1";

    @Inject SiteStore mSiteStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        LoginFlowThemeHelper.injectMissingCustomAttributes(getTheme());

        setContentView(R.layout.signup_epilogue_activity);

        if (savedInstanceState == null) {
            String name = getIntent().getStringExtra(EXTRA_SIGNUP_DISPLAY_NAME);
            String email = getIntent().getStringExtra(EXTRA_SIGNUP_EMAIL_ADDRESS);
            String photoUrl = getIntent().getStringExtra(EXTRA_SIGNUP_PHOTO_URL);
            String username = getIntent().getStringExtra(EXTRA_SIGNUP_USERNAME);
            boolean isEmail = getIntent().getBooleanExtra(EXTRA_SIGNUP_IS_EMAIL, false);
            addSignupEpilogueFragment(name, email, photoUrl, username, isEmail);
        }
    }

    protected void addSignupEpilogueFragment(String name, String email, String photoUrl, String username,
                                             boolean isEmail) {
        SignupEpilogueFragment signupEpilogueSocialFragment = SignupEpilogueFragment.newInstance(
                name, email, photoUrl, username, isEmail);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, signupEpilogueSocialFragment, SignupEpilogueFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onContinue() {
        if (!mSiteStore.hasSite()) {
            ActivityLauncher.showPostSignupInterstitial(this);
        }

        setResult(RESULT_OK);
        finish();
    }
}
