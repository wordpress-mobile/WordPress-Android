package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
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
    public ActivityScenarioRule<WPLaunchActivity> mActivityScenarioRule
            = new ActivityScenarioRule<>(WPLaunchActivity.class);
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

    String mCoachingPostTitle = "Let's check out the coaching team!";
    String mCompetitionPostTitle = "Let's focus on the competition.";

    @Test
    public void navigateThroughPosts() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mCoachingPostTitle)
                .verifyPostDisplayed(mCoachingPostTitle)
                .slideToPreviousPost()
                .verifyPostDisplayed(mCompetitionPostTitle)
                .slideToNextPost()
                .verifyPostDisplayed(mCoachingPostTitle);
    }

    @Test
    public void likePost() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mCoachingPostTitle)
                .likePost()
                .verifyPostLiked()
                .unlikePost()
                .verifyPostNotLiked();
    }
}
