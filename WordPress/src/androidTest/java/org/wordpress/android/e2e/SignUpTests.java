package org.wordpress.android.e2e;

import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.flows.SignupFlow;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.accounts.LoginMagicLinkInterceptActivity;

import static org.wordpress.android.BuildConfig.E2E_SIGNUP_DISPLAY_NAME;
import static org.wordpress.android.BuildConfig.E2E_SIGNUP_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_SIGNUP_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_SIGNUP_USERNAME;

public class SignUpTests extends BaseTest {
    @Rule
    public ActivityTestRule<LoginMagicLinkInterceptActivity> mMagicLinkActivityTestRule =
            new ActivityTestRule<>(LoginMagicLinkInterceptActivity.class);

    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @Test
    public void signUpWithEmail() {
        SignupFlow signupFlow = new SignupFlow();
        signupFlow.chooseSignupWithEmail();
        signupFlow.enterEmail(E2E_SIGNUP_EMAIL, mMagicLinkActivityTestRule);
        signupFlow.checkEpilogue(
                E2E_SIGNUP_DISPLAY_NAME,
                E2E_SIGNUP_USERNAME);
        signupFlow.enterPassword(E2E_SIGNUP_PASSWORD);
        signupFlow.dismissInterstitial();
        signupFlow.confirmSignup();
    }
}
