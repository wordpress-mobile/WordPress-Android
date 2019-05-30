package org.wordpress.android.e2e;

import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.flows.SignupFlow;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.accounts.LoginMagicLinkInterceptActivity;

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
        signupFlow.enterEmail("e2eflowtestingmobile@example.com", mMagicLinkActivityTestRule);
        signupFlow.checkEpilogue(
                "e2eflowtestingmobile@example.com",
                "Eeflowtestingmobile",
                "e2eflowtestingmobile");
        signupFlow.enterPassword("Pa$$word");
        signupFlow.confirmSignup();
    }
}
