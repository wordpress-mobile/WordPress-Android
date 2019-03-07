package org.wordpress.android.support;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.wordpress.android.R;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.e2e.pages.MySitesPage;
import org.wordpress.android.ui.WPLaunchActivity;

import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

public class BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    private void logout() {
        boolean isSelfHosted = new MePage().go().isSelfHosted();
        if (isSelfHosted) { // Logged in from self hosted connected
            new MySitesPage().go().removeSite(E2E_SELF_HOSTED_USER_SITE_ADDRESS);
        } else {
            wpLogout();
        }
    }

    protected void logoutIfNecessary() {
        if (isElementDisplayed(R.id.login_button) || isElementDisplayed(R.id.login_open_email_client)) {
            return;
        }

        if (isElementDisplayed(R.id.nav_me)) {
            logout();
        }
    }
    protected void wpLogin() {
        new LoginFlow().loginEmailPassword();
    }

    protected void wpLogout() {
        new MePage().go().verifyUsername(E2E_WP_COM_USER_USERNAME).logout();
    }
}
