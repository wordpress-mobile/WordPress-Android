package org.wordpress.android.e2etests;


import android.support.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.TestUtils;
import org.wordpress.android.e2etests.robots.LoginRobot;
import org.wordpress.android.e2etests.robots.NavigationRobot;
import org.wordpress.android.ui.WPLaunchActivity;


public class NavigationTests {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    private static final String USERNAME = BuildConfig.ESPRESSO_USERNAME;
    private static final String PASSWORD = BuildConfig.ESPRESSO_PASSWORD;

    @After
    public void tearDown() {
        TestUtils.clearApplicationState(mActivityTestRule.getActivity());
    }

    @Test
    public void testNavigation() {
        new LoginRobot()
                .selectLoginOption()
                .typeUsername(USERNAME)
                .tapNextButton()
                .tapToEnterPasswordInstead()
                .typePassword(PASSWORD)
                .tapNextButton()
                .tapToContinueOnSiteSelection();

        NavigationRobot navigationRobot = new NavigationRobot();
        navigationRobot.selectMyWordpressSites();

        NavigationRobot.ResultRobot resultRobot = new NavigationRobot.ResultRobot();
        resultRobot.displaysSiteMenuPageSucessfully();

        navigationRobot.selectMyPosts();
        resultRobot.displaysPostsPageSucessfully();

        navigationRobot.selectProfile();
        resultRobot.displaysProfilePageSucessfully();

        navigationRobot.selectNotifications();
        resultRobot.displaysNotificationsPageSucessfully();
    }
}
