package org.wordpress.android.e2e.flows;

import android.support.test.espresso.ViewInteraction;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Matchers;
import org.wordpress.android.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_EMAIL;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_PASSWORD;
import static org.wordpress.android.support.WPSupportUtils.clickOn;
import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
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
    }

    private void confirmLogin() {
        ViewInteraction continueButton = onView(withText(getCurrentActivity().getString(R.string.login_continue)));

        waitForElementToBeDisplayed(continueButton);
        clickOn(continueButton);

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

    private void enterUsernameAndPassword(String username, String password) {
        ViewInteraction usernameElement = onView(allOf(isDescendantOfA(withId(R.id.login_username_row)),
                Matchers.<View>instanceOf(EditText.class)));
        ViewInteraction passwordElement = onView(allOf(isDescendantOfA(withId(R.id.login_password_row)),
                Matchers.<View>instanceOf(EditText.class)));
        populateTextField(usernameElement, username + "\n");
        populateTextField(passwordElement, password + "\n");
        clickOn(R.id.primary_button);
    }

    private void chooseAndEnterSiteAddress(String siteAddress) {
        clickOn(onView(withText(R.string.enter_site_address_instead)));
        populateTextField(R.id.input, siteAddress);
        clickOn(R.id.primary_button);
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

    public void loginSiteAddress(String siteAddress, String username, String password) {
        chooseLogin();
        chooseAndEnterSiteAddress(siteAddress);
        enterUsernameAndPassword(username, password);
        confirmLogin();
    }
}
