package org.wordpress.android.e2e.flows;

import android.content.Intent;
import android.net.Uri;

import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.LoginMagicLinkInterceptActivity;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class SignupFlow {
    public SignupFlow chooseSignupWithEmail() {
        clickOn(onView(withId(R.id.create_site_button)));
        clickOn(onView(withId(R.id.signup_email)));

        return this;
    }

    public SignupFlow enterEmail(String email) {
        // Email file = id/input
        populateTextField(onView(withId(R.id.input)), email);
        clickOn(onView(withId(R.id.primary_button)));

        // Should See Open Mail button
        waitForElementToBeDisplayed(R.id.signup_magic_link_button);

        return this;
    }

    public SignupFlow openMagicLink(ActivityTestRule<LoginMagicLinkInterceptActivity> magicLinkActivityTestRule) {
        // Follow the magic link to continue login
        // Intent is invoked directly rather than through a browser as WireMock is unavailable once in the background
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("wordpress://magic-login?token=valid_token&new_user=1")
        ).setPackage(getApplicationContext().getPackageName());

        magicLinkActivityTestRule.launchActivity(intent);

        return this;
    }

    public SignupFlow checkEpilogue(String displayName, String username) {
        // Check Epilogue data
        ViewInteraction emailHeaderView = onView(withId(R.id.signup_epilogue_header_email));
        waitForElementToBeDisplayed(emailHeaderView);

        ViewInteraction displayNameField = onView(allOf(withId(R.id.input), withText(displayName)));
        ViewInteraction usernameField = onView(allOf(withId(R.id.input), withText(username)));

        waitForElementToBeDisplayed(displayNameField);
        waitForElementToBeDisplayed(usernameField);

        return this;
    }

    public SignupFlow enterPassword(String password) {
        // Enter Password
        ViewInteraction passwordField = onView(allOf(withId(R.id.input), withHint("Password (optional)")));
        waitForElementToBeDisplayed(passwordField);
        populateTextField(passwordField, password);

        // Click continue
        clickOn(onView(withId(R.id.primary_button)));

        return this;
    }

    public SignupFlow dismissInterstitial() {
        // Dismiss post-signup interstitial
        clickOn(onView(withId(R.id.dismiss_button)));

        return this;
    }

    public void confirmSignup() {
        // Confirm signup
        waitForElementToBeDisplayed(R.id.nav_sites);
    }
}
