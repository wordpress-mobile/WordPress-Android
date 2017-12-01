package org.wordpress.android.ui.end2end.tests;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.end2end.components.MasterbarComponent;
import org.wordpress.android.ui.end2end.pages.MePage;
import org.wordpress.android.ui.end2end.pages.login.LoginProloguePage;

public class BaseTest {

    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Before
    public void logoutIfNeeded() {
        try {
            new LoginProloguePage();
        } catch (NoMatchingViewException e) {
            new MasterbarComponent()
                    .goToMeTab();

            new MePage()
                    .logout();
        }
    }
}
