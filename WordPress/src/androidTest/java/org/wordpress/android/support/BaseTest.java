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

    protected void logoutIfNecessary() {
        if (isElementDisplayed(R.id.login_button)) {
            return;
        }

        if (isElementDisplayed(R.id.switch_site)) { // Logged in from self hosted connected site
            new SitesPage().go().removeSite(E2E_SELF_HOSTED_USER_SITE_ADDRESS);
        } else {
            new MePage().go().verifyUsername(E2E_WP_COM_USER_USERNAME).logout();
        }
    }
    protected void wpLogin() {
        logoutIfNecessary();
        new LoginFlow().loginEmailPassword();
    }

    protected void wpLogout() {
        new MePage()
                .go()
                .logout();
    }

    protected void sleep(int timeout) {
        // TODO: The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        // https://developer.android.com/training/testing/espresso/idling-resource
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void sleep() {
        sleep(2000);
    }
}
