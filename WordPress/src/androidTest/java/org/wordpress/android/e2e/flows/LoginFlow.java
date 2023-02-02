package org.wordpress.android.e2e.flows;

import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;

import androidx.compose.ui.test.junit4.ComposeTestRule;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;

import org.hamcrest.Matchers;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.HelpAndSupportScreen;
import org.wordpress.android.ui.pages.LoginPage;
import org.wordpress.android.util.compose.ComposeUiTestingUtils;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.allOf;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;
import static org.wordpress.android.support.WPSupportUtils.atLeastOneElementWithIdIsDisplayed;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class LoginFlow {
    public LoginFlow chooseContinueWithWpCom() {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        // See LoginPrologueFragment
        clickOn(R.id.continue_with_wpcom_button);
        return this;
    }

    public LoginFlow chooseContinueWithWpCom(ComposeTestRule composeTestRule) {
        // Login Prologue – We want to Continue with WordPress.com, not a site address
        if (BuildConfig.IS_JETPACK_APP) {
            // See LoginPrologueRevampedFragment
            return tapContinueWithWpComOnRevampedLoginScreen(composeTestRule);
        } else {
            // See LoginPrologueFragment
            return chooseContinueWithWpCom();
        }
    }

    private LoginFlow tapContinueWithWpComOnRevampedLoginScreen(ComposeTestRule composeTestRule) {
        new ComposeUiTestingUtils(composeTestRule)
                .performClickOnNodeWithText(getTranslatedString(LoginPage.continueWithWpComButtonStringRes));
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
        clickOn(R.id.bottom_button);
        return this;
    }

    public void confirmLogin(boolean isSelfHosted) {
        // If we get bumped to the "enter your username and password" screen, fill it in
        if (atLeastOneElementWithIdIsDisplayed(R.id.login_password_row)) {
            enterUsernameAndPassword(E2E_WP_COM_USER_USERNAME, E2E_WP_COM_USER_PASSWORD);
        }

        // New Epilogue Screen - Choose the first site from the list of site.
        // See LoginEpilogueFragment
        ViewInteraction sitesList = onView(withId(R.id.recycler_view));
        waitForElementToBeDisplayed(sitesList);
        sitesList.perform(actionOnItemAtPosition(1, click()));

        if (!isSelfHosted) {
            // Quick Start Prompt Dialog - Click the "No thanks" negative button to continue.
            // See QuickStartPromptDialogFragment
            ViewInteraction negativeButton = onView(withId(R.id.quick_start_prompt_dialog_button_negative));
            waitForElementToBeDisplayed(negativeButton);
            clickOn(negativeButton);
        }

        if (BuildConfig.IS_JETPACK_APP) {
            dismissNewFeaturesDialogIfDisplayed();
        }

        waitForElementToBeDisplayed(R.id.nav_sites);
    }

    public LoginFlow chooseMagicLink() {
        // Password Screen – Choose "Get a login link by email"
        // See LoginEmailPasswordFragment
        clickOn(R.id.login_get_email_link);
        return this;
    }

    public LoginFlow openMagicLink() {
        // Magic Link Sent Screen – Should see "Check email" button
        // See LoginMagicLinkSentFragment
        waitForElementToBeDisplayed(R.id.login_open_email_client);

        // Follow the magic link to continue login
        // Intent is invoked directly rather than through a browser as WireMock is unavailable once in the background
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("wordpress://magic-login?token=valid_token"))
                .setPackage(getApplicationContext().getPackageName());
        ActivityScenario.launch(intent);

        return this;
    }

    public LoginFlow enterUsernameAndPassword(String username, String password) {
        ViewInteraction usernameElement = onView(allOf(isDescendantOfA(withId(R.id.login_username_row)),
                Matchers.instanceOf(EditText.class)));
        ViewInteraction passwordElement = onView(allOf(isDescendantOfA(withId(R.id.login_password_row)),
                Matchers.instanceOf(EditText.class)));
        populateTextField(usernameElement, username + "\n");
        populateTextField(passwordElement, password + "\n");
        clickOn(R.id.bottom_button);
        return this;
    }

    public LoginFlow chooseEnterYourSiteAddress() {
        // Login Prologue – We want to continue with a site address not a WordPress.com account
        // See LoginPrologueFragment
        clickOn(R.id.enter_your_site_address_button);
        return this;
    }

    public LoginFlow enterSiteAddress(String siteAddress) {
        // Site Address Screen – Fill it in and click "Continue"
        // See LoginSiteAddressFragment
        populateTextField(R.id.input, siteAddress);
        clickOn(R.id.bottom_button);
        return this;
    }

    public HelpAndSupportScreen tapHelp() {
        clickOn(onView(withId(R.id.help)));
        return new HelpAndSupportScreen();
    }

    public static void dismissNewFeaturesDialogIfDisplayed() {
        if (isElementDisplayed(R.id.blogging_prompts_onboarding_button_container)) {
            clickOn(R.id.close_button);
        }
    }
}
