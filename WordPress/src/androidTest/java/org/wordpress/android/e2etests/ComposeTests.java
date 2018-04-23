package org.wordpress.android.e2etests;



import android.support.test.rule.ActivityTestRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.TestUtils;
import org.wordpress.android.e2etests.robots.BlogPostsRobot;
import org.wordpress.android.e2etests.robots.ComposePostRobot;
import org.wordpress.android.e2etests.robots.LoginRobot;
import org.wordpress.android.e2etests.robots.MySitesMenuRobot;
import org.wordpress.android.e2etests.robots.NavigationRobot;
import org.wordpress.android.ui.WPLaunchActivity;

public class ComposeTests {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    String mUsername = BuildConfig.ESPRESSO_USERNAME;
    String mPassword = BuildConfig.ESPRESSO_PASSWORD;

    @After
    public void tearDown() {
        TestUtils.clearApplicationState(mActivityTestRule.getActivity());
    }

    @Test
    public void testBlogPosting() {
        new LoginRobot()
                .selectLoginOption()
                .typeUsername(mUsername)
                .tapNextButton()
                .tapToEnterPasswordInstead()
                .typePassword(mPassword)
                .tapNextButton()
                .tapToContinueOnSiteSelection();

        new NavigationRobot()
                .selectMyWordpressSites();

        MySitesMenuRobot mySitesMenu = new MySitesMenuRobot();
        mySitesMenu.tapToWriteNewPost();

        new ComposePostRobot()
                .writeTitle("Hello")
                .writePost("World")
                .tapToPublish();

        mySitesMenu.tapBlogPosts();

        new BlogPostsRobot.ResultRobot()
                .hasTitleAndTextAtPosition("Hello", "World", 0);
    }
}
