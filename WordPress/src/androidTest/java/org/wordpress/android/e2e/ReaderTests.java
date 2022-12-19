package org.wordpress.android.e2e;

import android.Manifest.permission;

import androidx.test.rule.GrantPermissionRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.wordpress.android.e2e.pages.ReaderPage;
import org.wordpress.android.support.BaseTest;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
public class ReaderTests extends BaseTest {
    @Rule
    public GrantPermissionRule mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE);

    @Before
    public void setUp() {
        logoutIfNecessary();
        wpLogin();
        new ReaderPage().go();
    }

    String mCoachingPostTitle = "Let's check out the coaching team!";
    String mCompetitionPostTitle = "Let's focus on the competition.";

    @Test
    public void e2eNavigateThroughPosts() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mCoachingPostTitle)
                .verifyPostDisplayed(mCoachingPostTitle)
                .slideToPreviousPost()
                .verifyPostDisplayed(mCompetitionPostTitle)
                .slideToNextPost()
                .verifyPostDisplayed(mCoachingPostTitle)
                .goBackToReader();
    }

    @Test
    public void e2eLikePost() {
        new ReaderPage()
                .tapFollowingTab()
                .openPost(mCoachingPostTitle)
                .likePost()
                .verifyPostLiked()
                .unlikePost()
                .verifyPostNotLiked()
                .goBackToReader();
    }
}
