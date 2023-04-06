package org.wordpress.android.e2e

import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.wordpress.android.e2e.pages.ReaderPage
import org.wordpress.android.support.BaseTest

@HiltAndroidTest
class ReaderTests : BaseTest() {
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
