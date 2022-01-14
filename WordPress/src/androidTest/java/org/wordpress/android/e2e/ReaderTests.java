package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiObjectNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.pages.ReaderPage;
import org.wordpress.android.support.BaseTest;
import org.wordpress.android.ui.WPLaunchActivity;


public class ReaderTests extends BaseTest {
    @Rule
    public ActivityTestRule<WPLaunchActivity> mActivityTestRule = new ActivityTestRule<>(WPLaunchActivity.class);

    @Rule
    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
        new ReaderPage().go();
    }

    @After
    public void tearDown() {
        new ReaderPage().dismissReaderViewIfNeeded();
    }

    String mPostATitle = "Sit Elit Adipiscing Elit Dolor Lorem";
    String mPostAText = "Aenean vehicula nunc in sapien rutrum, nec vehicula enim iaculis. "
                        + "Aenean vehicula nunc in sapien rutrum, nec vehicula enim iaculis. "
                        + "Proin dictum non ligula aliquam varius. Nam ornare accumsan ante, "
                        + "sollicitudin bibendum erat bibendum nec. "
                        + "Aenean vehicula nunc in sapien rutrum, nec vehicula enim iaculis.";
    String mPostBTitle = "Dolor Sit Elit";
    String mPostBText = "Proin dictum non ligula aliquam varius. "
                        + "Nam congue efficitur leo eget porta. "
                        + "Nam congue efficitur leo eget porta. "
                        + "Nam congue efficitur leo eget porta.";

    @Test
    public void navigateThroughPosts() throws UiObjectNotFoundException {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mPostATitle)
                .verifyPostDisplayed(mPostATitle, mPostAText)
                .slideToPreviousPost()
                .verifyPostDisplayed(mPostBTitle, mPostBText)
                .slideToNextPost()
                .verifyPostDisplayed(mPostATitle, mPostAText)
                .goBackToReader();
    }

    @Test
    public void likePost() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mPostATitle)
                .like()
                .verifyPostLiked()
                .unlike()
                .verifyPostNotLiked()
                .goBackToReader();
    }
}
