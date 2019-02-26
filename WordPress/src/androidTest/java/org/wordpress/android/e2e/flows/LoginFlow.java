package org.wordpress.android.e2e.flows;

import org.wordpress.android.R;
import org.wordpress.android.e2e.pages.MePage;

import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;
import static org.wordpress.android.support.WPSupportUtils.populateTextField;
import static org.wordpress.android.support.WPSupportUtils.waitForElementToBeDisplayed;

public class LoginFlow {
    public void login() {
        // If we're already logged in, log out before starting
        if (!isElementDisplayed(R.id.login_button)) {
             new MePage().go().verifyUsername(E2E_WP_COM_USER_USERNAME).logout();
        }

        // Login Prologue – We want to log in, not sign up
        // See LoginPrologueFragment
        clickOn(R.id.login_button);

        // Email Address Screen – Fill it in and click "Next"
        // See LoginEmailFragment
        populateTextField(R.id.input, E2E_WP_COM_USER_EMAIL);
        clickOn(R.id.primary_button);

        // Receive Magic Link or Enter Password Screen – Choose "Enter Password"
        // See LoginMagicLinkRequestFragment
        clickOn(R.id.login_enter_password);

        // Password Screen – Fill it in and click "Next"
        // See LoginEmailPasswordFragment
        populateTextField(R.id.input, E2E_WP_COM_USER_PASSWORD);
        clickOn(R.id.primary_button);

        // Login Confirmation Screen – Click "Continue"
        // See LoginEpilogueFragment
        clickOn(R.id.primary_button);

        waitForElementToBeDisplayed(R.id.nav_me);
    }
}
