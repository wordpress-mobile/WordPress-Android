package org.wordpress.android.e2e;

import android.content.Intent;
import android.net.Uri;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.R;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.accounts.LoginMagicLinkInterceptActivity;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;

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
        // Sign up with Wordpress button = R.id.create_site_button
        clickOn(onView(withId(R.id.create_site_button)));

        // Sign up with email = id/signup_email
        clickOn(onView(withId(R.id.signup_email)));
        // Sign up with Google = id/signup_google
//        clickOn(onView(withId(R.id.signup_google)));

        // Email file = id/input
        populateTextField(onView(withId(R.id.input)), "test@test.com");
        clickOn(onView(withId(R.id.primary_button)));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
