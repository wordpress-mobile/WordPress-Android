package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.pages.ReaderPage;
import org.wordpress.android.e2e.pages.ReaderViewPage;
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
        new ReaderViewPage().goBackToReaderIfNecessary();
    }

    String mPostATitle = "Sit Elit Adipiscing Elit Dolor Lorem";
    String mPostBTitle = "Dolor Sit Elit";

    @Test
    public void navigateThroughPosts() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mPostATitle)
                .verifyPostDisplayed(mPostATitle)
                .slideToPreviousPost()
                .verifyPostDisplayed(mPostBTitle)
                .slideToNextPost()
                .verifyPostDisplayed(mPostATitle);
    }

    @Test
    public void likePost() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mPostATitle)
                .likePost()
                .verifyPostLiked()
                .unlikePost()
                .verifyPostNotLiked();
    }
}
