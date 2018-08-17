package org.wordpress.android.e2etests;


import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.TestUtils;
import org.wordpress.android.e2etests.robots.LoginRobot;
import org.wordpress.android.e2etests.robots.LoginRobot.ResultRobot;
import org.wordpress.android.ui.WPLaunchActivity;



@RunWith(AndroidJUnit4.class)
public class LoginTests {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

     private static final String USERNAME = BuildConfig.ESPRESSO_USERNAME;
     private static final String PASSWORD = BuildConfig.ESPRESSO_PASSWORD;

    @After
    public void tearDown() {
        TestUtils.clearApplicationState(mActivityTestRule.getActivity());
    }

    @Test
    public void testLoginSuccess() {
        new LoginRobot()
                .selectLoginOption()
                .typeUsername(USERNAME)
                .tapNextButton()
                .tapToEnterPasswordInstead()
                .typePassword(PASSWORD)
                .tapNextButton();

        new ResultRobot().isSucessfullyLoggedIn();
    }
}

