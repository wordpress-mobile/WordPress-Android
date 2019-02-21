package org.wordpress.android.e2e;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.e2e.flows.LoginFlow;
import org.wordpress.android.e2e.pages.MePage;
import org.wordpress.android.ui.WPLaunchActivity;

@RunWith(AndroidJUnit4.class)
public class LoginTests {

    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Test
    public void testWPComLoginLogout() {
        LoginFlow loginFlow = new LoginFlow();
        loginFlow.login();

        new MePage()
                .verifyUsername("thenomadicwordsmith")
                .logout();
    }


}
