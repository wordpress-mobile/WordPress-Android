package org.wordpress.android.support;

import android.support.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.ui.WPLaunchActivity;

public class BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    protected void wpLogin() {
        new LoginFlow().login();
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
