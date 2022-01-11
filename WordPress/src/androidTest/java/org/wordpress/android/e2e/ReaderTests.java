package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiObjectNotFoundException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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

    String mPostTitle = "Sit Elit Adipiscing Elit Dolor Lorem";
    String mPostText = "Aenean vehicula nunc in sapien rutrum, nec vehicula enim iaculis. "
                      + "Aenean vehicula nunc in sapien rutrum, nec vehicula enim iaculis. "
                      + "Proin dictum non ligula aliquam varius. Nam ornare accumsan ante, "
                      + "sollicitudin bibendum erat bibendum nec. "
                      + "Aenean vehicula nunc in sapien rutrum, nec vehicula enim iaculis.";

    @Test
    public void viewPost() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mPostTitle)
                .verifyPostDisplayed(mPostTitle, mPostText)
                .goBackToReader();
    }

    @Ignore
    @Test
    public void likePost() throws UiObjectNotFoundException {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mPostTitle)
                .verifyPostNotLiked()
                .like()
                .verifyPostLiked()
                .unlike()
                .verifyPostNotLiked()
                .goBackToReader();
    }
}
