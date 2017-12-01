package org.wordpress.android.ui.end2end.tests;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.WPLaunchActivity;
import org.wordpress.android.ui.end2end.flows.LoginFlow;
import org.wordpress.android.ui.end2end.pages.MePage;

@RunWith(AndroidJUnit4.class)
public class LoginTests extends BaseTest {

    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Test
    public void testWpcomLoginLogout() {

        String email = (String) WordPress.getBuildConfigValue(mActivityTestRule.getActivity().getApplication(), "DEBUG_E2ETEST_DEFAULT_EMAIL");
        String username = (String) WordPress.getBuildConfigValue(mActivityTestRule.getActivity().getApplication(), "DEBUG_E2ETEST_DEFAULT_USERNAME");
        String password = (String) WordPress.getBuildConfigValue(mActivityTestRule.getActivity().getApplication(), "DEBUG_E2ETEST_DEFAULT_PASSWORD");

        new LoginFlow()
                .wpcomLoginEmailPassword(email, password);

        new MePage()
                .verifyUsername(username)
                .logout();
    }
}
