package org.wordpress.android.e2e

import android.Manifest.permission
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.e2e.pages.ReaderPage
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class ReaderTests : BaseTest() {
    @Rule
    var mRuntimeImageAccessRule = GrantPermissionRule.grant(permission.WRITE_EXTERNAL_STORAGE)
    @Before
    fun setUp() {
        logoutIfNecessary()
        wpLogin()
        ReaderPage().go()
    }

    var mCoachingPostTitle = "Let's check out the coaching team!"
    var mCompetitionPostTitle = "Let's focus on the competition."
    @Test
    fun e2eNavigateThroughPosts() {
        ReaderPage()
            .tapFollowingTab()
            .openPost(mCoachingPostTitle)
            .verifyPostDisplayed(mCoachingPostTitle)
            .slideToPreviousPost()
            .verifyPostDisplayed(mCompetitionPostTitle)
            .slideToNextPost()
            .verifyPostDisplayed(mCoachingPostTitle)
            .goBackToReader()
    }

    @Test
    fun e2eLikePost() {
        ReaderPage()
            .tapFollowingTab()
            .openPost(mCoachingPostTitle)
            .likePost()
            .verifyPostLiked()
            .unlikePost()
            .verifyPostNotLiked()
            .goBackToReader()
    }
}
