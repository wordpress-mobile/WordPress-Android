package org.wordpress.android.e2e.flows;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;

import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Matchers;
import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.LoginMagicLinkInterceptActivity;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;
import static org.wordpress.android.support.WPSupportUtils.atLeastOneElementWithIdIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class LoginFlow {
    public LoginFlow chooseContinueWithWpCom() {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        // See LoginPrologueFragment
        clickOn(R.id.first_button);
        return this;
    }

    public LoginFlow enterEmailAddress(String emailAddress) {
        // Email Address Screen – Fill it in and click "Continue"
        // See LoginEmailFragment
        populateTextField(R.id.input, emailAddress);
        clickOn(R.id.login_continue_button);
        return this;
    }

    public LoginFlow enterPassword(String password) {
        // Password Screen – Fill it in and click "Continue"
        // See LoginEmailPasswordFragment
        populateTextField(R.id.input, password);
        clickOn(R.id.primary_button);
        return this;
    }

    public void confirmLogin() {
        // If we get bumped to the "enter your username and password" screen, fill it in
        if (atLeastOneElementWithIdIsDisplayed(R.id.login_password_row)) {
            enterUsernameAndPassword(E2E_WP_COM_USER_USERNAME, E2E_WP_COM_USER_PASSWORD);
        }

        ViewInteraction continueButton = onView(withId(R.id.primary_button));

        waitForElementToBeDisplayed(continueButton);
        clickOn(continueButton);

        waitForElementToBeDisplayed(R.id.nav_sites);
    }

    public LoginFlow chooseMagicLink() {
        // Password Screen – Choose "Get a login link by email"
        // See LoginEmailPasswordFragment
        clickOn(R.id.login_get_email_link);
        return this;
    }

    public LoginFlow openMagicLink(ActivityTestRule<LoginMagicLinkInterceptActivity> magicLinkActivityTestRule) {
        // Magic Link Sent Screen – Should see "Check email" button
        // See LoginMagicLinkSentFragment
        waitForElementToBeDisplayed(R.id.login_open_email_client);

        // Follow the magic link to continue login
        // Intent is invoked directly rather than through a browser as WireMock is unavailable once in the background
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("wordpress://magic-login?token=valid_token"))
                .setPackage(getApplicationContext().getPackageName());
        magicLinkActivityTestRule.launchActivity(intent);

        return this;
    }

    public LoginFlow enterUsernameAndPassword(String username, String password) {
        ViewInteraction usernameElement = onView(allOf(isDescendantOfA(withId(R.id.login_username_row)),
                Matchers.<View>instanceOf(EditText.class)));
        ViewInteraction passwordElement = onView(allOf(isDescendantOfA(withId(R.id.login_password_row)),
                Matchers.<View>instanceOf(EditText.class)));
        populateTextField(usernameElement, username + "\n");
        populateTextField(passwordElement, password + "\n");
        clickOn(R.id.primary_button);
        return this;
    }

    public LoginFlow chooseEnterYourSiteAddress() {
        // Login Prologue – We want to continue with a site address not a WordPress.com account
        // See LoginPrologueFragment
        clickOn(R.id.second_button);
        return this;
    }

    public LoginFlow enterSiteAddress(String siteAddress) {
        // Site Address Screen – Fill it in and click "Continue"
        // See LoginSiteAddressFragment
        populateTextField(R.id.input, siteAddress);
        clickOn(R.id.primary_button);
        return this;
    }
}
