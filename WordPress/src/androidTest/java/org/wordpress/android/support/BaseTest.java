package org.wordpress.android.support;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.wordpress.android.R;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.e2e.pages.SitesPage;
import org.wordpress.android.ui.WPLaunchActivity;

import static org.wordpress.android.BuildConfig.E2E_SELF_HOSTED_USER_SITE_ADDRESS;
import static org.wordpress.android.BuildConfig.E2E_WP_COM_USER_USERNAME;
import static org.wordpress.android.support.WPSupportUtils.isElementDisplayed;

public class BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    protected void logout() {
        if (isElementDisplayed(R.id.switch_site)) { // Logged in from self hosted connected site
            new SitesPage().go().removeSite(E2E_SELF_HOSTED_USER_SITE_ADDRESS);
        } else {
            wpLogout();
        }
    }

    protected void logoutIfNecessary() {
        if (isElementDisplayed(R.id.login_button)) {
            return;
        }

        logout();
    }
    protected void wpLogin() {
        logoutIfNecessary();
        new LoginFlow().loginEmailPassword();
    }

    protected void wpLogout() {
        new MePage().go().verifyUsername(E2E_WP_COM_USER_USERNAME).logout();
    }
}
