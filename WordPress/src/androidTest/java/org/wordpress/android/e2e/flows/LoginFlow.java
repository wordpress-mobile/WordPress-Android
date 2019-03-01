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
    private void chooseLogin() {
        // Login Prologue – We want to log in, not sign up
        // See LoginPrologueFragment
        clickOn(R.id.login_button);
    }

    private void enterEmailAddress() {
        // Email Address Screen – Fill it in and click "Next"
        // See LoginEmailFragment
        populateTextField(R.id.input, E2E_WP_COM_USER_EMAIL);
        clickOn(R.id.primary_button);
    }

    private void enterPassword() {
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
    }

    private void confirmLogin() {
        waitForElementToBeDisplayed(R.id.nav_me);
    }

    private void chooseMagicLink() {
        // Receive Magic Link or Enter Password Screen – Choose "Send Link"
        // See LoginMagicLinkRequestFragment
        clickOn(R.id.login_request_magic_link);

        // Should See Open Mail button
        waitForElementToBeDisplayed(R.id.login_open_email_client);

        // TODO: Continue flow after mocking complete
    }

    public void loginEmailPassword() {
        chooseLogin();
        enterEmailAddress();
        enterPassword();
        confirmLogin();
    }

    public void loginMagicLink() {
        chooseLogin();
        enterEmailAddress();
        chooseMagicLink();
    }
}
