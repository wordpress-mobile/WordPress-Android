package org.wordpress.android.e2e.flows;

import org.wordpress.android.R;

import static org.wordpress.android.BuildConfig.SCREENSHOT_LOGINPASSWORD;
import static org.wordpress.android.BuildConfig.SCREENSHOT_LOGINUSERNAME;
import static org.wordpress.android.ui.screenshots.support.WPScreenshotSupport.clickOn;
import static org.wordpress.android.ui.screenshots.support.WPScreenshotSupport.waitForElementToBeDisplayed;
import static org.wordpress.android.ui.screenshots.support.WPScreenshotSupport.isElementDisplayed;
import static org.wordpress.android.ui.screenshots.support.WPScreenshotSupport.populateTextField;
import static org.wordpress.android.ui.screenshots.support.WPScreenshotSupport.scrollToThenClickOn;

public class LoginFlow {

    public void login() {
        // If we're already logged in, log out before starting
        if (!isElementDisplayed(R.id.login_button)) {
             this.logout();
        }

        // Login Prologue – We want to log in, not sign up
        // See LoginPrologueFragment
        clickOn(R.id.login_button);

        // Email Address Screen – Fill it in and click "Next"
        // See LoginEmailFragment
        populateTextField(R.id.input, SCREENSHOT_LOGINUSERNAME);
        clickOn(R.id.primary_button);

        // Receive Magic Link or Enter Password Screen – Choose "Enter Password"
        // See LoginMagicLinkRequestFragment
        clickOn(R.id.login_enter_password);

        // Password Screen – Fill it in and click "Next"
        // See LoginEmailPasswordFragment
        populateTextField(R.id.input, SCREENSHOT_LOGINPASSWORD);
        clickOn(R.id.primary_button);

        // Login Confirmation Screen – Click "Continue"
        // See LoginEpilogueFragment
        clickOn(R.id.primary_button);

        waitForElementToBeDisplayed(R.id.nav_me);

        // TODO: Investigate idling resources as the solution for this sleep
        // https://developer.android.com/training/testing/espresso/idling-resource
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        // Click on the "Me" tab in the nav, then choose "Log Out"
        clickOn(R.id.nav_me);
        scrollToThenClickOn(R.id.row_logout);

        // Confirm that we want to log out
        clickOn(android.R.id.button1);
    }
}
