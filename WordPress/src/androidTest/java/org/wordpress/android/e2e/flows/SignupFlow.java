package org.wordpress.android.e2e.flows;

import android.content.Intent;
import android.net.Uri;

import androidx.compose.ui.test.junit4.ComposeTestRule;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.ui.pages.LoginPage;
import org.wordpress.android.util.compose.ComposeUiTestingUtils;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.dismissJetpackAdIfPresent;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class SignupFlow {
    public SignupFlow chooseContinueWithWpCom(ComposeTestRule composeTestRule) {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        if (BuildConfig.IS_JETPACK_APP) {
            // See LoginPrologueRevampedFragment
            return tapContinueWithWpComOnRevampedLandingScreen(composeTestRule);
        } else {
            // See LoginPrologueFragment
            return tapContinueWithWpComOnOldLandingScreen();
        }
    }

    private SignupFlow tapContinueWithWpComOnOldLandingScreen() {
        clickOn(R.id.continue_with_wpcom_button);
        return this;
    }

    private SignupFlow tapContinueWithWpComOnRevampedLandingScreen(ComposeTestRule composeTestRule) {
        new ComposeUiTestingUtils(composeTestRule)
                .performClickOnNodeWithText(getTranslatedString(LoginPage.continueWithWpComButtonStringRes));
        return this;
    }

    public SignupFlow enterEmail(String email) {
        // Email file = id/input
        populateTextField(onView(withId(R.id.input)), email);
        clickOn(onView(withId(R.id.login_continue_button)));
        return this;
    }

    public SignupFlow openMagicLink() {
        // Should see "Check email" button
        // See SignupMagicLinkFragment
        waitForElementToBeDisplayed(R.id.signup_magic_link_button);

        // Follow the magic link to continue login
        // Intent is invoked directly rather than through a browser as WireMock is unavailable once in the background
        final String appVariant = BuildConfig.FLAVOR_app; // Either "wordpress" or "jetpack"
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse(appVariant + "://magic-login?token=valid_token&new_user=1")
        ).setPackage(getApplicationContext().getPackageName());

        ActivityScenario.launch(intent);

        return this;
    }

    public SignupFlow checkEpilogue(String displayName, String username) {
        // Check Epilogue data
        ViewInteraction emailHeaderView = onView(withId(R.id.login_epilogue_header_subtitle));
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
        clickOn(onView(withId(R.id.bottom_button)));

        return this;
    }

    public SignupFlow dismissInterstitial() {
        // Dismiss post-signup interstitial
        clickOn(onView(withId(R.id.dismiss_button)));

        return this;
    }

    public SignupFlow dismissJetpackAd() {
        dismissJetpackAdIfPresent();
        return this;
    }

    public void confirmSignup() {
        // Confirm signup
        waitForElementToBeDisplayed(R.id.nav_sites);
    }
}
