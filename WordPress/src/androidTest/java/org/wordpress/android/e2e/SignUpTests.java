package org.wordpress.android.e2e;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.wordpress.android.e2e.flows.SignupFlow;
import org.wordpress.android.support.BaseTest;

import static org.wordpress.android.BuildConfig.E2E_SIGNUP_DISPLAY_NAME;
import static org.wordpress.android.BuildConfig.E2E_SIGNUP_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_SIGNUP_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_SIGNUP_USERNAME;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class SignUpTests extends BaseTest {
    @Before
    public void setUp() {
        logoutIfNecessary();
    }

    @After
    public void tearDown() {
        logoutIfNecessary();
    }

    @Test
    public void e2eSignUpWithMagicLink() {
        new SignupFlow().chooseContinueWithWpCom()
                        .enterEmail(E2E_SIGNUP_EMAIL)
                        .openMagicLink()
                        .checkEpilogue(
                                E2E_SIGNUP_DISPLAY_NAME,
                                E2E_SIGNUP_USERNAME)
                        .enterPassword(E2E_SIGNUP_PASSWORD)
                        .dismissInterstitial()
                        .dismissJetpackAd()
                        .confirmSignup();
    }
}
